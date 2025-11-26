package com.msk.minhascontas.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

import com.msk.minhascontas.MinhasContas;
import com.msk.minhascontas.R;

import java.util.Calendar;

public class ContasViewModel extends AndroidViewModel {

    // VARIÁVEL DE RECURSO: Array de meses, carregado pelo Application Context
    private final String[] mMeses; // NOVO

    // View State Holder (Mantido)
    public static class ViewState {
        public final boolean isMonthlySummary;
        public final boolean isCategorySummary;

        public ViewState(boolean isMonthlySummary, boolean isCategorySummary) {
            this.isMonthlySummary = isMonthlySummary;
            this.isCategorySummary = isCategorySummary;
        }
    }

    // Data Holder (Mantido)
    public static class DateState {
        public final int mes;
        public final int ano;
        public final int nrPagina;
        public final int dia;

        public DateState(int mes, int ano, int nrPagina, int dia) {
            this.mes = mes;
            this.ano = ano;
            this.nrPagina = nrPagina;
            this.dia = dia;
        }
    }

    // LiveData para o estado de Configuração da View
    private final MutableLiveData<ViewState> mViewState = new MutableLiveData<>();

    // LiveData para a posição do ViewPager.
    private final MutableLiveData<Integer> mViewPagerPosition = new MutableLiveData<>(MinhasContas.START_PAGE);

    // LiveData que contém a data e a posição calculada.
    private final MutableLiveData<DateState> mCurrentDateState = new MutableLiveData<>();

    public ContasViewModel(@NonNull Application application) {
        super(application);
        // NOVO: Carrega o array de meses usando o Application Context
        mMeses = application.getResources().getStringArray(R.array.MesResumido);

        loadViewState(application);
        calculateAndSetDateState(MinhasContas.START_PAGE);
    }

    // NOVO: Getter para o array de meses (USADO PELO ADAPTER)
    public String[] getStringMonths() {
        return mMeses;
    }

    // ... (restante do código LiveData e métodos de cálculo inalterado)

    private void loadViewState(Application application) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(application);

        boolean resumoMensal = prefs.getBoolean(application.getString(R.string.pref_key_resumo), true);
        boolean resumoCategoria = prefs.getBoolean(application.getString(R.string.pref_key_categoria), false);

        ViewState newState = new ViewState(resumoMensal, resumoCategoria);
        mViewState.setValue(newState);
    }

    public LiveData<ViewState> getViewState() {
        return mViewState;
    }

    public LiveData<Integer> getViewPagerPosition() {
        return mViewPagerPosition;
    }

    public LiveData<DateState> getCurrentDateState() {
        return mCurrentDateState;
    }

    public void setViewPagerPosition(int position) {
        Integer currentPosition = mViewPagerPosition.getValue();
        if (currentPosition == null || !currentPosition.equals(position)) {
            mViewPagerPosition.setValue(position);
            calculateAndSetDateState(position);
        }
    }

    private void calculateAndSetDateState(int position) {
        ViewState viewState = mViewState.getValue();
        boolean isMonthly = viewState == null || viewState.isMonthlySummary;

        DateState newState = calculateDateState(position, isMonthly);
        mCurrentDateState.setValue(newState);
    }

    public static DateState calculateDateState(int position, boolean isMonthlySummary) {
        Calendar currentCalendar = Calendar.getInstance();
        int positionOffset = position - MinhasContas.START_PAGE;

        if (isMonthlySummary) {
            currentCalendar.add(Calendar.MONTH, positionOffset);
        } else {
            currentCalendar.add(Calendar.DAY_OF_YEAR, positionOffset);
        }

        int mes = currentCalendar.get(Calendar.MONTH);
        int ano = currentCalendar.get(Calendar.YEAR);
        int dia = currentCalendar.get(Calendar.DAY_OF_MONTH);

        return new DateState(mes, ano, position, dia);
    }
}