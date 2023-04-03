package com.freyr.readmynotify

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.freyr.readmynotify.databinding.RowNotifyBinding

class NotifyAdapter : RecyclerView.Adapter<NotifyViewHolder>() {
    var dataList: List<NotifyModel> = emptyList()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ) = NotifyViewHolder(
        RowNotifyBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
    )

    override fun onBindViewHolder(holder: NotifyViewHolder, position: Int) {
        val item = dataList[position]

        with(holder.binding) {
            tvTitle.text = item.title

            tvContent.text = item.content
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }
}

data class NotifyModel(
    val title: String,
    val content: String
)