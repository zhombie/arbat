package kz.zhombie.museum

import android.content.Context
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView

// PaintingLoader

val Context.paintingLoader: PaintingLoader
    get() = Museum.getPaintingLoader(this)

val Fragment.paintingLoader: PaintingLoader
    get() = Museum.getPaintingLoader(requireContext())

val RecyclerView.ViewHolder.paintingLoader: PaintingLoader
    get() = itemView.context.paintingLoader


inline fun ImageView.load(
    data: Any?,
    paintingLoader: PaintingLoader = context.paintingLoader,
    builder: PaintingLoader.Request.Builder.() -> Unit = {}
) {
    val request = PaintingLoader.Request.Builder(context)
        .apply(builder)
        .setData(data)
        .into(this)
        .build()
    paintingLoader.enqueue(request)
}

fun ImageView.dispose() {
    context.paintingLoader.dispose(this)
}
