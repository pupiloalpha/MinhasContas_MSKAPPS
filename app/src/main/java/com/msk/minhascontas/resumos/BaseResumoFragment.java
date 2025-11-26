package com.msk.minhascontas.resumos;

import android.app.Activity; // Importação adicionada para RESULT_OK
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher; // Importação adicionada
import androidx.activity.result.contract.ActivityResultContracts; // Importação adicionada
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider; // NOVO

import com.google.android.material.card.MaterialCardView;
import com.msk.minhascontas.R;
import com.msk.minhascontas.viewmodel.ContasViewModel; // NOVO
import com.msk.minhascontas.viewmodel.ContasViewModel.DateState; // NOVO
import com.msk.minhascontas.db.DBContas;
import com.msk.minhascontas.db.DBContas.ContaFilter;
import com.msk.minhascontas.db.ContasContract.Colunas;
import com.msk.minhascontas.MinhasContas; // Importa a Activity host

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Classe Base abstrata para os fragmentos de Resumo (Diário e Mensal, Tipo e Categoria).
 * Contém a lógica de inicialização de DB, preferências, cálculo de saldo, e tratamento de cliques.
 */
public abstract class BaseResumoFragment extends Fragment implements View.OnClickListener {

    // --- CHAVE DE ARGUMENTO DO FRAGMENTO ---
    public static final String ARG_NR_PAGINA = "nrPagina";

    // Variáveis de Data e Página (Serão atualizadas no onClick)
    protected int dia = 0, mes, ano;
    protected int nrPagina; // Usado para a página (offset) no ViewPager2

    // NOVO: ViewModel compartilhado
    private ContasViewModel contasViewModel;

    // Arrays para armazenar valores
    protected double[] valores;
    protected double[] valoresDesp;
    protected double[] valoresRec;
    protected double[] valoresSaldo;
    protected double[] valoresAplicados;

    // Variáveis de Banco de Dados e Preferências
    protected DBContas dbContas;
    protected SharedPreferences buscaPreferencias = null, preferences;
    protected final Bundle dados_mes = new Bundle();

    // Views para Eventos de Clique (LinearLayouts)
    protected MaterialCardView layoutAplicacoes;
    protected MaterialCardView layoutDespesas;
    protected MaterialCardView layoutReceitas;
    protected MaterialCardView layoutSaldo;

    // ActivityResultLauncher para PaginadorListas.java
    private final ActivityResultLauncher<Intent> mContasMesLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    // Verifica se a Activity host é MinhasContas e se a posição foi retornada
                    if (data != null && data.hasExtra(MinhasContas.RETURN_KEY_PAGINA) && getActivity() instanceof MinhasContas) {
                        int returnedPosition = data.getIntExtra(MinhasContas.RETURN_KEY_PAGINA, nrPagina);
                        // Chama o método de sincronização da Activity host
                        ((MinhasContas) getActivity()).syncViewPagerPositionAndRefresh(returnedPosition);
                        Log.d("BaseResumoFragment", "Posição retornada: " + returnedPosition + ". Sincronizando ViewPager principal.");
                    } else {
                        // Se não houver posição retornada ou o host não for MinhasContas, assume-se que os dados mudaram.
                        Log.d("BaseResumoFragment", "Dados alterados. Confiando na recarga do Adapter/Fragmento.");
                    }
                }
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbContas = DBContas.getInstance(requireContext());
        preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());

        // NOVO: Inicializa o ViewModel (Escopo da Activity Host)
        contasViewModel = new ViewModelProvider(requireActivity()).get(ContasViewModel.class);

        if (getArguments() != null) {
            mes = getArguments().getInt("mes", 0);
            ano = getArguments().getInt("ano", 0);
            dia = getArguments().getInt("dia", 0);
            // **MUDANÇA: Recupera a posição do ViewPager dos argumentos**
            nrPagina = getArguments().getInt(ARG_NR_PAGINA, MinhasContas.START_PAGE);
            Log.d("BaseResumoFragment", "onCreate: Posição recebida: " + nrPagina);
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        dbContas = DBContas.getInstance(context);
        buscaPreferencias = PreferenceManager.getDefaultSharedPreferences(context);

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(getLayoutResId(), container, false);

        Bundle args = getArguments();
        if (args != null) {
            // Inicializa com os dados de criação (mesmo que possam ficar obsoletos)
            ano = args.getInt("ano");
            mes = args.getInt("mes");
            nrPagina = args.getInt(ARG_NR_PAGINA); // Changed for consistency
            if (args.containsKey("dia")) {
                dia = args.getInt("dia");
            }
        }

        initializeArrays();
        iniciarViews(rootView); // Subclasse encontra as views
        return rootView;
    }

    // Método para ser implementado pelos filhos para recarregar os dados
    public abstract void onDadosAtualizados();

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Log.d("BaseResumoFragment", "onViewCreated: Initializing click listeners.");

        // Define o listener para as áreas de resumo para que elas sejam clicáveis.
        if (layoutAplicacoes != null) {
            layoutAplicacoes.setOnClickListener(this);
            Log.d("BaseResumoFragment", "onViewCreated: layoutAplicacoes click listener set.");
        } else {
            Log.w("BaseResumoFragment", "onViewCreated: layoutAplicacoes is null. Click listener NOT set.");
        }
        if (layoutDespesas != null) {
            layoutDespesas.setOnClickListener(this);
            Log.d("BaseResumoFragment", "onViewCreated: layoutDespesas click listener set.");
        } else {
            Log.w("BaseResumoFragment", "onViewCreated: layoutDespesas is null. Click listener NOT set.");
        }
        if (layoutReceitas != null) {
            layoutReceitas.setOnClickListener(this);
            Log.d("BaseResumoFragment", "onViewCreated: layoutReceitas click listener set.");
        } else {
            Log.w("BaseResumoFragment", "onViewCreated: layoutReceitas is null. Click listener NOT set.");
        }
        if (layoutSaldo != null) {
            layoutSaldo.setOnClickListener(this);
            Log.d("BaseResumoFragment", "onViewCreated: layoutSaldo click listener set.");
        } else {
            Log.w("BaseResumoFragment", "onViewCreated: layoutSaldo is null. Click listener NOT set.");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Recalcula e insere os valores sempre que o fragmento se torna visível
        refreshData(); // Chamada para o método que agora irá sincronizar
    }

    // --- MÉTODOS ABSTRATOS ---
    protected abstract int getLayoutResId();
    protected abstract void initializeArrays();
    protected abstract void iniciarViews(View view); // Encontra todas as Views
    protected abstract void saldo(); // Calcula os valores
    protected abstract void insereValores(); // Define os valores nas TextViews
    protected abstract ContaFilter getContaFilter(); // Filtro base para o período

    // --- LÓGICA DE CLIQUE (AGORA SINCRONIZADA COM A ACTIVITY HOST E API MODERNA) ---
    @Override
    public void onClick(View v) {
        Log.d("BaseResumoFragment", "onClick: Clique detectado na view com ID: " + v.getId());

        // 1. OBTÉM OS DADOS ATUALIZADOS DO VIEWMODEL
        DateState currentData = contasViewModel.getCurrentDateState().getValue();

        if (currentData != null) {
            // Atualiza as variáveis de cache de instância do Fragmento
            mes = currentData.mes;
            ano = currentData.ano;
            nrPagina = currentData.nrPagina; // Get the current nrPagina from ViewModel
        } else {
            Log.e("BaseResumoFragment", "onClick: LiveData do ViewModel é nulo. Usando valores de cache.");
            // Continua com os valores de cache (mes/ano/nrPagina) se o ViewModel estiver vazio
        }

        dados_mes.putInt("mes", mes);
        dados_mes.putInt("ano", ano);
        // Passa a posição ATUAL do ViewPager para a Activity filha
        dados_mes.putInt(MinhasContas.KEY_PAGINA, nrPagina);

        int viewId = v.getId();
        int tipo = -1; // -1 para Saldo (default)

        // IDs ajustados para R.id.l_...
        if (viewId == R.id.resumo_saldo) {
            tipo = -1;
        } else if (viewId == R.id.resumo_aplicacoes) {
            tipo = 2;
        } else if (viewId == R.id.resumo_despesas) {
            tipo = 0;
        } else if (viewId == R.id.resumo_receitas) {
            tipo = 1;
        }

        dados_mes.putInt("tipo", tipo);
        Log.d("BaseResumoFragment", "onClick: Preparando Intent com extras - mes: " + (mes + 1) + ", ano: " + ano + ", nrPagina (KEY_PAGINA): " + nrPagina + ", tipo: " + tipo);

        Intent mostra_resumo = new Intent("com.msk.minhascontas.CONTASDOMES");
        mostra_resumo.putExtras(dados_mes);

        if (mContasMesLauncher != null) {
            mContasMesLauncher.launch(mostra_resumo);
        }
    }

    // --- MÉTODOS AUXILIARES ---

    protected NumberFormat getCurrencyFormat() {
        return NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    }

    /**
     * Calcula a soma dos valores das contasListadas para o filtro fornecido, fechando o Cursor.
     */
    protected double getSumForFilter(ContaFilter filter) {
        double sum = 0.0D;
        try (Cursor cursor = dbContas.getContasByFilter(filter, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    sum += cursor.getDouble(cursor.getColumnIndexOrThrow(Colunas.COLUNA_VALOR_CONTA));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("BaseResumoFragment", "Erro ao buscar e somar contasListadas: " + e.getMessage());
        }
        return sum;
    }

    public void refreshData() {
        // NOVO: Sincroniza as variáveis de data do fragmento com o ViewModel
        DateState currentData = contasViewModel.getCurrentDateState().getValue();

        if (currentData != null) {
            mes = currentData.mes;
            ano = currentData.ano;
            nrPagina = currentData.nrPagina;
            // Se a lógica do dia for usada (apenas em resumo diário), ela precisará de refatoração futura.
            Log.d("BaseResumoFragment", "refreshData: Sincronizando data com ViewModel: " + (mes + 1) + "/" + ano);
        } else {
            Log.e("BaseResumoFragment", "refreshData: LiveData do ViewModel é nulo. Dados de UI não atualizados.");
        }

        saldo();
        insereValores();
    }
}