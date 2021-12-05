package kz.zhombie.arbat.sample

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import kz.zhombie.museum.PaintingLoader
import kz.zhombie.museum.dispose
import kz.zhombie.museum.load

class ImagesAdapter constructor(
    private val callback: (position: Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var images: List<Uri> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount(): Int = images.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.cell_image, parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder) {
            holder.bind(images[position])
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is ViewHolder) {
            holder.getImageView().dispose()
        }
    }

    private inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val imageView: ImageView = view.findViewById(R.id.imageView)

        init {
            imageView.setOnClickListener(::onImageClick)
        }

        fun bind(image: Uri) {
            imageView.load(image) {
                setCrossfade(PaintingLoader.Request.Crossfade.DefaultEnabled())
                setSize(300, 300)
            }

            imageView.setTag(R.id.museum_tag_selected_image, image)
        }

        fun getImageView(): ImageView {
            return imageView
        }

        private fun onImageClick(view: View) {
            val tag = view.getTag(R.id.museum_tag_selected_image)
            if (tag is Uri) {
                val position: Int = images.indexOf(tag)
                callback(position)
            }
        }
    }

}