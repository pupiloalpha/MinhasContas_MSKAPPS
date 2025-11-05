package com.msk.minhascontas.info

import android.content.ContentResolver
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.MenuItem
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.msk.minhascontas.R
import com.msk.minhascontas.databinding.ListaPastasBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * Activity para escolher pasta/arquivo usando SAF (Storage Access Framework) + RecyclerView + ViewBinding.
 * Retorna CHOSEN_DIRECTORY como URI string (ex.: content://...).
 *
 * extras:
 * - "tipo": extensão de arquivo desejada (ex: ".xls" / ".xlsx"). Se vazio => aceita diretório.
 * - START_DIR (não usado mais; deixado para compatibilidade)
 */
class EscolhePasta : AppCompatActivity() {

    companion object {
        const val CHOSEN_DIRECTORY = "chosenDir"
        private const val EXTRA_TIPO = "tipo"
    }

    private lateinit var binding: ListaPastasBinding
    private lateinit var openTreeLauncher: ActivityResultLauncher<Uri?>
    private var tipo: String = ""
    private var currentDir: DocumentFile? = null
    private var rootTreeUri: Uri? = null
    private lateinit var adapter: FileListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ListaPastasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        tipo = intent.extras?.getString(EXTRA_TIPO) ?: ""

        // Configura RecyclerView
        adapter = FileListAdapter(mutableListOf(), ::onItemClicked)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        )

        binding.tvSemResultados.visibility = View.GONE

        // Registrar launcher para ACTION_OPEN_DOCUMENT_TREE
        openTreeLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                // Persistir permissão de leitura/escrita para este tree
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                // Ajustar root e exibir conteúdo
                rootTreeUri = uri
                // Converter o uri da árvore ao DocumentFile raiz, usando DocumentsContract to get exact root if needed
                val pickedDir = DocumentFile.fromTreeUri(this, uri)
                // Se possível, tente normalizar para a "real" root dentro da árvore
                currentDir = pickedDir
                updateListing()
            } else {
                // nenhum resultado: exibir mensagem
                binding.tvSemResultados.text = getString(R.string.sem_resultados)
                binding.tvSemResultados.visibility = View.VISIBLE
            }
        }

        // Botões
        binding.btnChoose.setOnClickListener {
            // Se não há currentDir -> abrir seletor
            if (currentDir == null) {
                // Abre seletor de árvore
                openTreeLauncher.launch(null)
            } else {
                // Retorna a URI atual (diretório)
                returnResult(currentDir!!.uri)
            }
        }

        binding.btnPickTree.setOnClickListener {
            openTreeLauncher.launch(null)
        }

        // Se já havia uma árvore persistida (opcional): poderia abrir automaticamente.
        // Aqui não buscamos permissões persistidas automaticamente para não agir sem intenção do usuário.

        // Ao abrir a Activity, perguntamos ao usuário qual tree ele quer usar (abrir seletor)
        // para manter compatibilidade com Scoped Storage. Usuário pode navegar após escolher.
        // Se desejar abrir o seletor imediatamente ao entrar, descomente:
        // openTreeLauncher.launch(null)
    }

    private fun updateListing() {
        val dir = currentDir
        if (dir == null) {
            adapter.submitList(emptyList())
            binding.tvSemResultados.visibility = View.VISIBLE
            binding.tvSemResultados.text = getString(R.string.sem_resultados)
            title = getString(R.string.app_name)
            return
        }

        title = dir.name ?: dir.uri.toString()

        // Listar e filtrar
        val children = dir.listFiles().filter { file ->
            // Excluir ocultos e inacessíveis
            try {
                if (file.isHidden) return@filter false
                if (!file.canRead()) return@filter false
            } catch (e: Exception) {
                // em alguns casos canRead/pesquisa pode lançar; ignoramos estes arquivos
                return@filter false
            }
            // Se for diretório, mostramos sempre
            if (file.isDirectory) return@filter true
            // Se for arquivo, só se bater com 'tipo' (se tipo não vazio)
            if (tipo.isNotEmpty()) {
                return@filter file.name?.lowercase(Locale.getDefault())?.endsWith(tipo.lowercase(Locale.getDefault())) ?: false
            }
            // Se tipo vazio, também mostramos arquivos
            true
        }.sortedWith(compareBy({ !it.isDirectory }, { it.name?.lowercase(Locale.getDefault()) }))

        adapter.submitList(children)
        binding.tvSemResultados.visibility = if (children.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun onItemClicked(item: DocumentFile) {
        if (item.isDirectory) {
            // entrar na pasta
            currentDir = item
            updateListing()
        } else {
            // arquivo: se tipo vazio -> retornar, ou se terminar com tipo -> retornar
            val accept = if (tipo.isEmpty()) true else (item.name?.lowercase(Locale.getDefault())
                ?.endsWith(tipo.lowercase(Locale.getDefault())) ?: false)
            if (accept) {
                returnResult(item.uri)
            } else {
                // mensagem simples
                android.widget.Toast.makeText(this, R.string.formato_nao_suportado, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun returnResult(uri: Uri) {
        val result = Intent()
        result.putExtra(CHOSEN_DIRECTORY, uri.toString())
        setResult(RESULT_OK, result)
        finish()
    }

    override fun onBackPressed() {
        // navegar para cima na árvore quando possível (até o root selecionado)
        val parent = currentDir?.let { findParentDocumentFile(it) }
        if (parent != null && rootTreeUri != null && parent.uri != DocumentFile.fromTreeUri(this, rootTreeUri!!)?.uri) {
            currentDir = parent
            updateListing()
        } else if (parent != null && rootTreeUri != null && parent.uri == DocumentFile.fromTreeUri(this, rootTreeUri!!)?.uri) {
            // se voltar ao root da árvore, faz isso normalmente
            currentDir = parent
            updateListing()
        } else {
            super.onBackPressed()
        }
    }

    private fun findParentDocumentFile(child: DocumentFile): DocumentFile? {
        // DocumentFile não tem getParent direto; jogamos uma busca simples na árvore raiz
        val root = rootTreeUri?.let { DocumentFile.fromTreeUri(this, it) } ?: return null
        if (child.uri == root.uri) return null // já no root

        // busca recursiva simples para achar parent do child. Para árvores grandes pode ser custoso,
        // mas em uso normal de pastas não haverá problema significativo.
        fun findParent(current: DocumentFile): DocumentFile? {
            if (!current.isDirectory) return null
            val kids = current.listFiles()
            for (k in kids) {
                if (k.uri == child.uri) return current
                if (k.isDirectory) {
                    val found = findParent(k)
                    if (found != null) return found
                }
            }
            return null
        }
        return findParent(root)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
