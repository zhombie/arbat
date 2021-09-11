package kz.zhombie.arbat

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


/**
 * Creates an [AutoClearedValue] associated with this [AppCompatActivity].
 */
fun <T : Any> LifecycleOwner.bindAutoClearedValue() = AutoClearedValue<T>(this)


/**
 * Safe any value wrapper, which controls [Lifecycle] states.
 * Event the [value] is [androidx.annotation.Nullable], which operates safe calls
 */
class AutoClearedValue<T : Any> constructor(
    private val owner: LifecycleOwner
) : ReadWriteProperty<LifecycleOwner, T?>, LifecycleObserver {

    companion object {
        private val TAG = AutoClearedValue::class.java.simpleName
    }

    private var value: T? = null

    init {
        Logger.debug(TAG, "[$owner] init()")

        initInternally()
    }

    private fun initInternally() {
        /**
         * On [Lifecycle.State.CREATED] subscribe to [LifecycleObserver],
         * in order to clear subscription of [value] on [onDestroy]
         */
        owner.lifecycleScope.launchWhenCreated {
            Logger.debug(TAG, "[$owner] lifecycleScope.launchWhenCreated()")

            owner.lifecycle.addObserver(this@AutoClearedValue)

            /**
             * If the [value] has also implemented [LifecycleObserver],
             * then subscribe to it, too.
             * It helps us to automatically control subscription/un-subscription on [Lifecycle.State]
             */
            value?.let {
                if (it is LifecycleObserver) {
                    Logger.debug(TAG, "[$owner] lifecycleScope.launchWhenCreated() -> observer is added: $it")
                    owner.lifecycle.addObserver(it)
                }
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        Logger.debug(TAG, "[$owner] onDestroy() -> value: $value")

        /**
         * Unsubscribe from [value]'s [LifecycleObserver] implementation
         */
        value?.let {
            if (it is LifecycleObserver) {
                Logger.debug(TAG, "[$owner] onDestroy() -> observer is removed: $it")
                owner.lifecycle.removeObserver(it)
            }
        }

        /**
         * Clear the [value]
         */
        value = null

        owner.lifecycle.removeObserver(this)
    }

    override fun getValue(thisRef: LifecycleOwner, property: KProperty<*>): T? {
        Logger.debug(TAG, "[$owner] getValue() -> property: $property, value: $value")
        return value
    }

    override fun setValue(thisRef: LifecycleOwner, property: KProperty<*>, value: T?) {
        Logger.debug(TAG, "[$owner] setValue() -> property: $property, value: $value")
        this.value = value
    }

}