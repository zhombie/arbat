package kz.zhombie.cinema

import android.os.Bundle
import kz.zhombie.cinema.model.Movie
import kz.zhombie.cinema.model.Params

internal class BundleManager private constructor() {

    companion object {
        private object Key {
            const val MOVIE = "movie"
            const val IS_FOOTER_VIEW_ENABLED = "is_footer_view_enabled"
        }

        fun parse(arguments: Bundle?): Params {
            require(arguments != null) { "Provide with arguments!" }

            val movie = requireNotNull(arguments.getParcelable<Movie>(Key.MOVIE))

            val isFooterViewEnabled = arguments.getBoolean(Key.IS_FOOTER_VIEW_ENABLED, Params.DEFAULT_FOOTER_VIEW_ENABLED)

            return Params(
                movie = movie,
                isFooterViewEnabled = isFooterViewEnabled
            )
        }

        fun build(params: Params): Bundle {
            val bundle = Bundle()

            bundle.putParcelable(Key.MOVIE, params.movie)

            bundle.putBoolean(Key.IS_FOOTER_VIEW_ENABLED, params.isFooterViewEnabled)

            return bundle
        }
    }

}