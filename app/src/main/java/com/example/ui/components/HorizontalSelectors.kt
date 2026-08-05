package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.AccountEntity
import com.example.data.local.CategoryEntity
import com.example.util.CurrencyHelper
import com.example.util.CurrencyUnit

@Composable
fun AccountHorizontalSelector(
    title: String = "حساب بانکی / کارت",
    accounts: List<AccountEntity>,
    selectedAccountId: Long,
    currencyUnit: CurrencyUnit,
    onAccountSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (title.isNotBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(accounts, key = { it.id }) { account ->
                val isSelected = account.id == selectedAccountId
                val color = CategoryIconHelper.parseColor(account.colorHex)

                FilterChip(
                    selected = isSelected,
                    onClick = { onAccountSelected(account.id) },
                    leadingIcon = {
                        val icon = CategoryIconHelper.getIcon(account.iconName)
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else color,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    label = {
                        Column {
                            Text(
                                text = account.name,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = CurrencyHelper.formatAmount(account.balance, currencyUnit),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else Color.Gray
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
}

@Composable
fun CategoryHorizontalSelector(
    title: String = "دسته‌بندی",
    categories: List<CategoryEntity>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    CategoryTwoLevelSelector(
        title = title,
        allCategories = categories,
        selectedCategoryId = selectedCategoryId,
        selectedSubcategoryId = null,
        onCategorySelected = { mainCatId, _ ->
            if (mainCatId != null) onCategorySelected(mainCatId)
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryTwoLevelSelector(
    title: String = "دسته‌بندی",
    allCategories: List<CategoryEntity>,
    selectedCategoryId: Long?,
    selectedSubcategoryId: Long?,
    onCategorySelected: (mainCatId: Long?, subCatId: Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showBottomSheet by remember { mutableStateOf(false) }

    val mainCategories = remember(allCategories) {
        allCategories.filter { it.parentId == null }
    }

    val selectedMainCat = remember(mainCategories, selectedCategoryId) {
        mainCategories.find { it.id == selectedCategoryId }
    }
    val selectedSubCat = remember(allCategories, selectedSubcategoryId) {
        if (selectedSubcategoryId == null) null
        else allCategories.find { it.id == selectedSubcategoryId }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Trigger Field to open Bottom Sheet
        Surface(
            onClick = { showBottomSheet = true },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = if (selectedMainCat != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (selectedMainCat != null) {
                        val icon = CategoryIconHelper.getIcon(selectedMainCat.iconName)
                        val color = CategoryIconHelper.parseColor(selectedMainCat.colorHex)

                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column {
                            val catText = if (selectedSubCat != null) "${selectedMainCat.name} > ${selectedSubCat.name}" else selectedMainCat.name
                            Text(
                                text = catText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "جهت انتخاب دسته‌بندی کلیک کنید...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selectedMainCat != null) {
                        IconButton(
                            onClick = { onCategorySelected(null, null) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "پاک کردن",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.UnfoldMore,
                        contentDescription = "باز کردن لیست دسته‌بندی‌ها",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    if (showBottomSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            CategoryBottomSheetContent(
                allCategories = allCategories,
                selectedCategoryId = selectedCategoryId,
                selectedSubcategoryId = selectedSubcategoryId,
                onCategorySelected = { mainId, subId ->
                    onCategorySelected(mainId, subId)
                    showBottomSheet = false
                },
                onClose = { showBottomSheet = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryBottomSheetContent(
    allCategories: List<CategoryEntity>,
    selectedCategoryId: Long?,
    selectedSubcategoryId: Long?,
    onCategorySelected: (mainId: Long, subId: Long?) -> Unit,
    onClose: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val mainCategories = remember(allCategories) {
        allCategories.filter { it.parentId == null }
    }

    val subCategoriesMap = remember(allCategories) {
        allCategories.filter { it.parentId != null }.groupBy { it.parentId!! }
    }

    // Keep track of expanded main categories
    var expandedCategoryIds by remember {
        mutableStateOf(
            if (selectedCategoryId != null) setOf(selectedCategoryId) else emptySet()
        )
    }

    // Filter logic
    val filteredMainCategories = remember(searchQuery, mainCategories, subCategoriesMap) {
        if (searchQuery.isBlank()) mainCategories
        else {
            val q = searchQuery.trim().lowercase()
            mainCategories.filter { main ->
                main.name.lowercase().contains(q) ||
                (subCategoriesMap[main.id]?.any { sub -> sub.name.lowercase().contains(q) } == true)
            }
        }
    }

    // Auto-expand categories if search query is present
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            expandedCategoryIds = filteredMainCategories.map { it.id }.toSet()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 560.dp)
            .padding(bottom = 16.dp)
    ) {
        // Sheet Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "انتخاب دسته‌بندی",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "بستن")
            }
        }

        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("جستجو در دسته‌بندی‌ها و زیردسته‌ها...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "پاک کردن")
                    }
                }
            } else null,
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Accordion Category List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredMainCategories, key = { it.id }) { mainCat ->
                val color = CategoryIconHelper.parseColor(mainCat.colorHex)
                val icon = CategoryIconHelper.getIcon(mainCat.iconName)
                val subCats = subCategoriesMap[mainCat.id] ?: emptyList()
                val isExpanded = expandedCategoryIds.contains(mainCat.id)
                val isMainSelected = selectedCategoryId == mainCat.id

                val filteredSubCats = if (searchQuery.isBlank()) subCats
                else {
                    val q = searchQuery.trim().lowercase()
                    subCats.filter { sub -> sub.name.lowercase().contains(q) || mainCat.name.lowercase().contains(q) }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isMainSelected) color.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (isMainSelected) color.copy(alpha = 0.4f) else Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Main Category Row (Accordion Header)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (subCats.isEmpty()) {
                                        onCategorySelected(mainCat.id, null)
                                    } else {
                                        expandedCategoryIds = if (isExpanded) {
                                            expandedCategoryIds - mainCat.id
                                        } else {
                                            expandedCategoryIds + mainCat.id
                                        }
                                    }
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(color.copy(alpha = 0.2f))
                                        .clickable { onCategorySelected(mainCat.id, null) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                                }

                                Column {
                                    Text(
                                        text = mainCat.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isMainSelected) color else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (subCats.isNotEmpty()) {
                                        Text(
                                            text = "${subCats.size} زیردسته",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            if (subCats.isNotEmpty()) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (isExpanded) "بستن زیردسته‌ها" else "نمایش زیردسته‌ها",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                if (isMainSelected && selectedSubcategoryId == null) {
                                    Icon(Icons.Default.Check, contentDescription = "انتخاب شده", tint = color)
                                }
                            }
                        }

                        // Expanded Accordion Content (Subcategories List)
                        AnimatedVisibility(visible = isExpanded && subCats.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 12.dp, bottom = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )

                                // Option 1: "انتخاب کل [نام دسته اصلی]"
                                val isOverallMainSelected = isMainSelected && selectedSubcategoryId == null
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isOverallMainSelected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)
                                        .clickable { onCategorySelected(mainCat.id, null) }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FolderOpen,
                                            contentDescription = null,
                                            tint = color,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "انتخاب کل ${mainCat.name}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (isOverallMainSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isOverallMainSelected) color else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    if (isOverallMainSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "انتخاب شده",
                                            tint = color,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                // Option 2: Subcategories
                                filteredSubCats.forEach { subCat ->
                                    val isSubSelected = isMainSelected && selectedSubcategoryId == subCat.id
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSubSelected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)
                                            .clickable { onCategorySelected(mainCat.id, subCat.id) }
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "↳ ${subCat.name}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (isSubSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSubSelected) color else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isSubSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "انتخاب شده",
                                                tint = color,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

