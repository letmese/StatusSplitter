package com.statussplitter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

class ChunkAdapter(
    private val chunks: MutableList<VideoChunk>,
    private val onChunkClick: (VideoChunk) -> Unit,
    private val onReorder: (Int, Int) -> Unit
) : RecyclerView.Adapter<ChunkAdapter.ChunkViewHolder>() {

    inner class ChunkViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvLabel: TextView = view.findViewById(R.id.tv_chunk_label)
        val tvDetails: TextView = view.findViewById(R.id.tv_chunk_details)
        val cbSelect: CheckBox = view.findViewById(R.id.cb_select)
        val btnUp: ImageButton = view.findViewById(R.id.btn_move_up)
        val btnDown: ImageButton = view.findViewById(R.id.btn_move_down)
        val btnPlay: ImageButton = view.findViewById(R.id.btn_play)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChunkViewHolder {
        return ChunkViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_chunk, parent, false))
    }

    override fun onBindViewHolder(holder: ChunkViewHolder, position: Int) {
        val chunk = chunks[position]
        holder.tvLabel.text = chunk.label
        holder.tvDetails.text = String.format("%.2f MB | %.1f s", chunk.sizeMB, chunk.durationSeconds)
        holder.cbSelect.setOnCheckedChangeListener(null)
        holder.cbSelect.isChecked = chunk.selected
        holder.itemView.setOnClickListener { onChunkClick(chunk) }
        holder.cbSelect.setOnCheckedChangeListener { _, isChecked -> chunk.selected = isChecked }
        holder.btnUp.isEnabled = position > 0
        holder.btnDown.isEnabled = position < chunks.size - 1
        holder.btnUp.setOnClickListener {
            val current = chunks.indexOfFirst { it === chunk }
            if (current > 0) onReorder(current, current - 1)
        }
        holder.btnDown.setOnClickListener {
            val current = chunks.indexOfFirst { it === chunk }
            if (current >= 0 && current < chunks.lastIndex) onReorder(current, current + 1)
        }
        holder.btnPlay.setOnClickListener { onChunkClick(chunk) }
    }

    override fun getItemCount(): Int = chunks.size

    fun moveItem(from: Int, to: Int) {
        if (from !in chunks.indices || to !in chunks.indices || from == to) return
        val item = chunks.removeAt(from)
        chunks.add(to, item)
        notifyItemMoved(from, to)
        notifyItemRangeChanged(minOf(from,to), kotlin.math.abs(from-to)+1)
        for (i in chunks.indices) {
            chunks[i].index = i
        }
    }
}