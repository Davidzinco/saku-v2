package com.saku.v2.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.saku.v2.data.WidgetUpdaterContract
import com.saku.v2.data.model.SakuAppState

/**
 * Broadcasts state updates from [com.saku.v2.data.store.SakuStore] to all active Saku home screen widgets.
 */
class SakuWidgetUpdater(
    private val context: Context
) : WidgetUpdaterContract {

    override fun updateAllWidgets(state: SakuAppState) {
        runCatching {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val component = ComponentName(context, SakuWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(component)
            if (appWidgetIds.isNotEmpty()) {
                SakuWidgetProvider.updateAppWidgets(context, appWidgetManager, appWidgetIds, state)
            }
        }
    }
}
