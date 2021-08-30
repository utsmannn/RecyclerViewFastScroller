package xyz.danoz.recyclerviewfastscroller.vertical

import android.view.View

object AnimateUtils {

    @JvmStatic
    fun animateYSimple(view: View, valueY: Float) {
        view.animate()
            .setDuration(300)
            .y(valueY)
            .start()
    }
}