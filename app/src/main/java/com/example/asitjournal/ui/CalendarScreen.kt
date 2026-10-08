package com.example.asitjournal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.asitjournal.data.Injection
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

data class CalendarDay(
    val date: LocalDate,
    val injections: List<Injection>,
    val isCurrentMonth: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    injections: List<Injection>,
    onBack: () -> Unit,
    onDayClick: (LocalDate) -> Unit = {}
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    val daysInMonth = remember(currentMonth, injections) {
        generateCalendarDays(currentMonth, injections)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Календарь") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            // Навигация по месяцам
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Предыдущий")
                }
                Text(
                    currentMonth.month.getDisplayName(TextStyle.FULL, Locale("ru")).replaceFirstChar { it.uppercase() } +
                            " ${currentMonth.year}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Следующий")
                }
            }

            // Дни недели
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                val dayNames = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
                dayNames.forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Сетка дней
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(daysInMonth) { day ->
                    DayCell(day = day, onClick = { onDayClick(day.date) })
                }
            }
        }
    }
}

@Composable
fun DayCell(day: CalendarDay, onClick: () -> Unit) {
    val isToday = day.date == LocalDate.now()
    val hasInjections = day.injections.isNotEmpty()

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(
                when {
                    isToday -> MaterialTheme.colorScheme.primaryContainer
                    !day.isCurrentMonth -> Color.Transparent
                    else -> Color.Transparent
                }
            )
            .clickable { if (day.isCurrentMonth) onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.date.dayOfMonth.toString(),
                color = when {
                    !day.isCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurface
                },
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp
            )

            // Цветные точки для прививок
            if (hasInjections && day.isCurrentMonth) {
                Row(horizontalArrangement = Arrangement.Center) {
                    day.injections.take(3).forEach { inj ->
                        val color = try {
                            Color(android.graphics.Color.parseColor(inj.colorHex))
                        } catch (e: Exception) {
                            Color.Gray
                        }
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .padding(horizontal = 1.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }
            }
        }
    }
}

fun generateCalendarDays(yearMonth: YearMonth, injections: List<Injection>): List<CalendarDay> {
    val days = mutableListOf<CalendarDay>()
    val firstDay = yearMonth.atDay(1)
    val lastDay = yearMonth.atEndOfMonth()

    // Определяем день недели первого дня (понедельник = 1)
    var startOffset = firstDay.dayOfWeek.value - 1 // 0 = понедельник

    // Добавляем дни предыдущего месяца
    val prevMonth = yearMonth.minusMonths(1)
    for (i in startOffset downTo 1) {
        val date = prevMonth.atEndOfMonth().minusDays((i - 1).toLong())
        days.add(CalendarDay(date, getInjectionsForDate(date, injections), false))
    }

    // Дни текущего месяца
    for (day in 1..lastDay.dayOfMonth) {
        val date = yearMonth.atDay(day)
        days.add(CalendarDay(date, getInjectionsForDate(date, injections), true))
    }

    // Дни следующего месяца (дополняем до 42 = 6 недель)
    val remaining = 42 - days.size
    val nextMonth = yearMonth.plusMonths(1)
    for (day in 1..remaining) {
        val date = nextMonth.atDay(day)
        days.add(CalendarDay(date, getInjectionsForDate(date, injections), false))
    }

    return days
}

fun getInjectionsForDate(date: LocalDate, injections: List<Injection>): List<Injection> {
    return injections.filter { inj ->
        val injDate = Instant.ofEpochMilli(inj.plannedDate)
            .atZone(ZoneId.systemDefault()).toLocalDate()
        injDate == date
    }
}