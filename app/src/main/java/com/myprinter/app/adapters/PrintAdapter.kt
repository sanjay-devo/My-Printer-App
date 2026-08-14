package com.myprinter.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.myprinter.app.R
import com.myprinter.app.models.FileType
import com.myprinter.app.models.PrintItem
import com.myprinter.app.utils.PrintUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PrintAdapter(private val coroutineScope: CoroutineScope) : RecyclerView.Adapter<PrintAdapter.PageViewHolder>() {

    private val pages = mutableListOf<PrintPage>()

    fun setItems(items: List<PrintItem>) {
        pages.clear()
        items.forEach { item ->
            for (i in 0 until item.pageCount) {
                pages.add(PrintPage(item, i))
            }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_print_page, parent, false)
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bind(pages[position])
    }

    override fun getItemCount(): Int = pages.size

    inner class PageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ivPageContent: ImageView = view.findViewById(R.id.ivPageContent)

        fun bind(printPage: PrintPage) {
            ivPageContent.setImageBitmap(null)
            
            coroutineScope.launch {
                val bitmap = withContext(Dispatchers.IO) {
                    if (printPage.item.fileType == FileType.IMAGE) {
                        PrintUtils.decodeSampledBitmap(itemView.context, printPage.item.uri, 1000, 1000)
                    } else {
                        PrintUtils.renderPdfPage(itemView.context, printPage.item.uri, printPage.pageIndex)
                    }
                }
                ivPageContent.setImageBitmap(bitmap)
            }
        }
    }

    data class PrintPage(val item: PrintItem, val pageIndex: Int)
}
