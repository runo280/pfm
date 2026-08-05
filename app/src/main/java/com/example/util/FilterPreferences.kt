package com.example.util

import android.content.Context
import android.content.SharedPreferences

class FilterPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_filters_prefs", Context.MODE_PRIVATE)

    // Transactions Screen
    var txSearchQuery: String
        get() = prefs.getString("tx_search_query", "") ?: ""
        set(value) = prefs.edit().putString("tx_search_query", value).apply()

    var txFilterType: String
        get() = prefs.getString("tx_filter_type", "ALL") ?: "ALL"
        set(value) = prefs.edit().putString("tx_filter_type", value).apply()

    var txDateMode: String
        get() = prefs.getString("tx_date_mode", "MONTHLY") ?: "MONTHLY"
        set(value) = prefs.edit().putString("tx_date_mode", value).apply()

    var txCategoryId: Long?
        get() {
            val v = prefs.getLong("tx_category_id", -1L)
            return if (v == -1L) null else v
        }
        set(value) = prefs.edit().putLong("tx_category_id", value ?: -1L).apply()

    var txAccountId: Long?
        get() {
            val v = prefs.getLong("tx_account_id", -1L)
            return if (v == -1L) null else v
        }
        set(value) = prefs.edit().putLong("tx_account_id", value ?: -1L).apply()

    var txIsFilterVisible: Boolean
        get() = prefs.getBoolean("tx_filter_visible", false)
        set(value) = prefs.edit().putBoolean("tx_filter_visible", value).apply()

    var txYear: Int
        get() = prefs.getInt("tx_year", -1)
        set(value) = prefs.edit().putInt("tx_year", value).apply()

    var txMonth: Int
        get() = prefs.getInt("tx_month", -1)
        set(value) = prefs.edit().putInt("tx_month", value).apply()

    var txStartDate: String
        get() = prefs.getString("tx_start_date", "") ?: ""
        set(value) = prefs.edit().putString("tx_start_date", value).apply()

    var txEndDate: String
        get() = prefs.getString("tx_end_date", "") ?: ""
        set(value) = prefs.edit().putString("tx_end_date", value).apply()

    fun resetTxFilters() {
        prefs.edit()
            .remove("tx_search_query")
            .remove("tx_filter_type")
            .remove("tx_date_mode")
            .remove("tx_category_id")
            .remove("tx_account_id")
            .remove("tx_year")
            .remove("tx_month")
            .remove("tx_start_date")
            .remove("tx_end_date")
            .apply()
    }

    // Analytics Screen
    var analyticsAccountId: Long?
        get() {
            val v = prefs.getLong("analytics_account_id", -1L)
            return if (v == -1L) null else v
        }
        set(value) = prefs.edit().putLong("analytics_account_id", value ?: -1L).apply()

    var analyticsCategoryId: Long?
        get() {
            val v = prefs.getLong("analytics_category_id", -1L)
            return if (v == -1L) null else v
        }
        set(value) = prefs.edit().putLong("analytics_category_id", value ?: -1L).apply()

    var analyticsDateMode: String
        get() = prefs.getString("analytics_date_mode", "MONTH") ?: "MONTH"
        set(value) = prefs.edit().putString("analytics_date_mode", value).apply()

    var analyticsYear: Int
        get() = prefs.getInt("analytics_year", -1)
        set(value) = prefs.edit().putInt("analytics_year", value).apply()

    var analyticsMonth: Int
        get() = prefs.getInt("analytics_month", -1)
        set(value) = prefs.edit().putInt("analytics_month", value).apply()

    var analyticsStartDate: String
        get() = prefs.getString("analytics_start_date", "") ?: ""
        set(value) = prefs.edit().putString("analytics_start_date", value).apply()

    var analyticsEndDate: String
        get() = prefs.getString("analytics_end_date", "") ?: ""
        set(value) = prefs.edit().putString("analytics_end_date", value).apply()

    var analyticsIsFilterVisible: Boolean
        get() = prefs.getBoolean("analytics_filter_visible", false)
        set(value) = prefs.edit().putBoolean("analytics_filter_visible", value).apply()

    fun resetAnalyticsFilters() {
        prefs.edit()
            .remove("analytics_account_id")
            .remove("analytics_category_id")
            .remove("analytics_date_mode")
            .remove("analytics_year")
            .remove("analytics_month")
            .remove("analytics_start_date")
            .remove("analytics_end_date")
            .apply()
    }

    // Installments & Cheques Screen
    var instSelectedTab: Int
        get() = prefs.getInt("inst_selected_tab", 0)
        set(value) = prefs.edit().putInt("inst_selected_tab", value).apply()

    var instDebtFilterTab: Int
        get() = prefs.getInt("inst_debt_filter_tab", 0)
        set(value) = prefs.edit().putInt("inst_debt_filter_tab", value).apply()

    var instFilterMode: String
        get() = prefs.getString("inst_filter_mode", "MONTHLY") ?: "MONTHLY"
        set(value) = prefs.edit().putString("inst_filter_mode", value).apply()

    var instYear: Int
        get() = prefs.getInt("inst_year", -1)
        set(value) = prefs.edit().putInt("inst_year", value).apply()

    var instMonth: Int
        get() = prefs.getInt("inst_month", -1)
        set(value) = prefs.edit().putInt("inst_month", value).apply()

    var instSummaryExpanded: Boolean
        get() = prefs.getBoolean("inst_summary_expanded", false)
        set(value) = prefs.edit().putBoolean("inst_summary_expanded", value).apply()

    fun resetInstFilters() {
        prefs.edit()
            .remove("inst_selected_tab")
            .remove("inst_debt_filter_tab")
            .remove("inst_filter_mode")
            .remove("inst_year")
            .remove("inst_month")
            .apply()
    }
}
