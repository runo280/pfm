package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIconHelper {

    val availableIcons = mapOf(
        "ShoppingCart" to Icons.Default.ShoppingCart,
        "DirectionsCar" to Icons.Default.DirectionsCar,
        "Home" to Icons.Default.Home,
        "Receipt" to Icons.Default.Receipt,
        "Restaurant" to Icons.Default.Restaurant,
        "MedicalServices" to Icons.Default.MedicalServices,
        "Payments" to Icons.Default.Payments,
        "Work" to Icons.Default.Work,
        "VolunteerActivism" to Icons.Default.VolunteerActivism,
        "TrendingUp" to Icons.AutoMirrored.Filled.TrendingUp,
        "AttachMoney" to Icons.Default.AttachMoney,
        "CreditCard" to Icons.Default.CreditCard,
        "AccountBalanceWallet" to Icons.Default.AccountBalanceWallet,
        "Savings" to Icons.Default.Savings,
        "School" to Icons.Default.School,
        "Flight" to Icons.Default.Flight,
        "PhoneAndroid" to Icons.Default.PhoneAndroid,
        "Pets" to Icons.Default.Pets,
        "CardGiftcard" to Icons.Default.CardGiftcard,
        "ChildCare" to Icons.Default.ChildCare,
        "Face" to Icons.Default.Face,
        "Store" to Icons.Default.Store,
        "SportsEsports" to Icons.Default.SportsEsports,
        "FitnessCenter" to Icons.Default.FitnessCenter,
        "MoreHoriz" to Icons.Default.MoreHoriz
    )

    fun getIcon(iconName: String): ImageVector {
        return availableIcons[iconName] ?: Icons.Default.Category
    }

    fun parseColor(colorHex: String): Color {
        return try {
            Color(android.graphics.Color.parseColor(colorHex))
        } catch (e: Exception) {
            Color(0xFF3B82F6)
        }
    }
}
