package com.msk.minhascontas.info

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.msk.minhascontas.R
import com.msk.minhascontas.databinding.LinhaPastasBinding
import java.text.SimpleDateFormat
import java.util.*

class FileListAdapter(
    initial: MutableList<DocumentFile>,
    private val onClick: (DocumentFile) -> Unit
) : ListAdapter<DocumentFile, FileListAdapter.Holder>(DIFF) {

    init {
        submitList(initial.toList())
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<DocumentFile>() {
            override fun areItemsTheSame(oldItem: DocumentFile, newItem: DocumentFile): Boolean =
                oldItem.uri == newItem.uri

            override fun areContentsTheSame(oldItem: DocumentFile, newItem: DocumentFile): Boolean =
                oldItem.uri == newItem.uri
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = LinhaPastasBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding, parent.context)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(getItem(position), onClick)
    }

    class Holder(private val binding: LinhaPastasBinding, private val ctx: Context) : RecyclerView.ViewHolder(binding.root) {
        fun bind(file: DocumentFile, onClick: (DocumentFile) -> Unit) {
            binding.tvPasta.text = file.name ?: file.uri.lastPathSegment

            if (file.isDirectory) {
                binding.ivFolder.setImageResource(R.drawable.ic_folder)
                binding.ivFolder.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.cinza_claro))
                binding.tvData.visibility = android.view.View.GONE
            } else {
                binding.ivFolder.setImageResource(R.drawable.ic_archive)
                // cor especial se arquivo bater com tipo é responsabilidade da Activity (não temos 'tipo' aqui)
                binding.ivFolder.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.cinza_claro))

                // lastModified pode retornar 0 se não suportado; tratamos com try/catch
                val lastModified = try { file.lastModified() } catch (e: Exception) { 0L }
                if (lastModified > 0) {
                    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                    binding.tvData.text = sdf.format(Date(lastModified))
                    binding.tvData.visibility = android.view.View.VISIBLE
                } else {
                    binding.tvData.visibility = android.view.View.GONE
                }
            }

            binding.root.setOnClickListener { onClick(file) }
        }
    }
}
