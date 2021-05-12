package kz.zhombie.museum

import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

interface MuseumDialogFragmentListener {
    fun setImageView(imageView: ImageView?): MuseumDialogFragment
    fun setRecyclerView(recyclerView: RecyclerView?): MuseumDialogFragment
}