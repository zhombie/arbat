package kz.zhombie.museum

import android.os.Bundle
import kz.zhombie.museum.model.Painting

internal class BundleManager private constructor() {

    companion object {
        private object Key {
            const val PAINTINGS = "paintings"
            const val IS_FOOTER_VIEW_ENABLED = "is_footer_view_enabled"
        }

        fun get(arguments: Bundle?): Params {
            require(arguments != null) { "Provide with arguments!" }

            val paintings = requireNotNull(arguments.getParcelableArrayList<Painting>(Key.PAINTINGS))

            val isFooterViewEnabled = arguments.getBoolean(Key.IS_FOOTER_VIEW_ENABLED)

            return Params(
                paintings = paintings,
                isFooterViewEnabled = isFooterViewEnabled
            )
        }

        fun build(params: Params): Bundle {
            val bundle = Bundle()

            bundle.putParcelableArrayList(Key.PAINTINGS, ArrayList(params.paintings))

            bundle.putBoolean(Key.IS_FOOTER_VIEW_ENABLED, params.isFooterViewEnabled)

            return bundle
        }
    }

}