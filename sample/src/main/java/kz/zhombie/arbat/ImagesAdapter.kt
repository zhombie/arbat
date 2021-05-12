package kz.zhombie.arbat

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class ImagesAdapter constructor(
    private val imageLoader: CoilImageLoader,
    private val callback: (position: Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        fun getImageView(holder: RecyclerView.ViewHolder): ImageView? {
            return if (holder is ViewHolder) {
                holder.imageView
            } else {
                null
            }
        }
    }

    var images: List<Uri> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount(): Int = images.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val holder = ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.cell_image, parent, false)
        )
        holder.imageView.setOnClickListener(::onImageClick)
        return holder
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder) {
            holder.bind(images[position])
        }
    }

    private inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)

        fun bind(image: Uri) {
            imageLoader.loadSmallImage(itemView.context, imageView, image)

            imageView.setTag(R.id.tag_selected_image, image)
        }
    }

    private fun onImageClick(view: View) {
        val tag = view.getTag(R.id.tag_selected_image)
        if (tag is Uri) {
            val position: Int = images.indexOf(tag)
            callback(position)
        }
    }

}