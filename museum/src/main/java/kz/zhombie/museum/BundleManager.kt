package kz.zhombie.museum

import android.os.Bundle
import kz.zhombie.museum.model.Painting
import kz.zhombie.museum.model.Params

internal class BundleManager private constructor() {

    companion object {
        private object Key {
            const val PAINTINGS = "paintings"
            const val START_POSITION = "start_position"
            const val IS_FOOTER_VIEW_ENABLED = "is_footer_view_enabled"
        }

        fun parse(arguments: Bundle?): Params {
            require(arguments != null) { "Provide with arguments!" }

            val paintings = requireNotNull(arguments.getParcelableArrayList<Painting>(Key.PAINTINGS))

            val startPosition = arguments.getInt(Key.START_POSITION, Params.DEFAULT_START_POSITION)

            val isFooterViewEnabled = arguments.getBoolean(Key.IS_FOOTER_VIEW_ENABLED, Params.DEFAULT_FOOTER_VIEW_ENABLED)

            return Params(
                paintings = paintings,
                startPosition = startPosition,
                isFooterViewEnabled = isFooterViewEnabled
            )
        }

        fun build(params: Params): Bundle {
            val bundle = Bundle()

            bundle.putParcelableArrayList(Key.PAINTINGS, ArrayList(params.paintings))

            bundle.putInt(Key.START_POSITION, params.startPosition)

            bundle.putBoolean(Key.IS_FOOTER_VIEW_ENABLED, params.isFooterViewEnabled)

            return bundle
        }
    }

}