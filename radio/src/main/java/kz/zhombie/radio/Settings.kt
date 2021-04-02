package kz.zhombie.radio

internal object Settings {

    private var isLoggingEnabled: Boolean = false

    fun isLoggingEnabled(): Boolean {
        return isLoggingEnabled
    }

    fun setLoggingEnabled(isLoggingEnabled: Boolean) {
        this.isLoggingEnabled = isLoggingEnabled
    }

}