package kz.zhombie.museum

import android.view.View
import android.widget.LinearLayout
import androidx.viewpager.widget.ViewPager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textview.MaterialTextView

internal class ViewHolder constructor(view: View) {

    val toolbar: MaterialToolbar = view.findViewById(R.id.toolbar)
    val backgroundView: View = view.findViewById(R.id.backgroundView)
    val viewPager: ViewPager = view.findViewById(R.id.viewPager)
    val footerView: LinearLayout = view.findViewById(R.id.footerView)
    val titleView: MaterialTextView = view.findViewById(R.id.titleView)
    val subtitleView: MaterialTextView = view.findViewById(R.id.subtitleView)

}