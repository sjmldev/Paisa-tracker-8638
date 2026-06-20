package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import kotlin.math.cos
import kotlin.math.sin

// Theme helper colors (representing a bold premium violet slate brand aesthetic)
object PaisaTheme {
    val DeepPurple = Color(0xFF5D3FD3)
    val LightLavender = Color(0xFFF4F2FF)
    val OffWhite = Color(0xFFFFFFFF)
    val PinkAccent = Color(0xFFE91E63)
    val MintGreen = Color(0xFF16A34A)
    val LightGreen = Color(0xFFDCFCE7)
    val SoftRed = Color(0xFFEF4444)
    val LightRed = Color(0xFFFEE2E2)
    
    // Chart Color Palette
    val ChartColors = listOf(
        Color(0xFF673AB7), // Purple
        Color(0xFF00B0FF), // Cyan
        Color(0xFFFF9100), // Orange
        Color(0xFF4CAF50), // Green
        Color(0xFFE91E63), // Pink
        Color(0xFFFFD600), // Yellow
        Color(0xFF9C27B0), // Purple-red
        Color(0xFF3F51B5), // Indigo
        Color(0xFF009688)  // Teal
    )
}

// ----------------------------------------------------
// VECTOR BRAND LOGO COMPOSABLE (Wallet + Rupee + Upward Arrow)
// ----------------------------------------------------
@Composable
fun PaisaLogo(
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme()
) {
    Box(
        modifier = modifier
            .size(140.dp)
            .clip(CircleShape)
            .background(if (isDark) Color(0xFF2A1C4E) else PaisaTheme.DeepPurple),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val w = size.width
            val h = size.height

            // 1. Draw a clean, stylish Wallet Shape (Middle-Bottom area)
            val walletLeft = w * 0.22f
            val walletTop = h * 0.40f
            val walletWidth = w * 0.56f
            val walletHeight = h * 0.45f
            val walletCornerRadius = 14f

            // Background of Wallet
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(walletLeft, walletTop),
                size = Size(walletWidth, walletHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(walletCornerRadius, walletCornerRadius)
            )

            // Wallet Flap
            val flapLeft = w * 0.48f
            val flapTop = h * 0.48f
            val flapWidth = w * 0.30f
            val flapHeight = h * 0.18f
            drawRoundRect(
                color = if (isDark) Color(0xFFE91E63) else Color(0xFF6200EE),
                topLeft = Offset(flapLeft, flapTop),
                size = Size(flapWidth, flapHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
            )

            // Wallet Snap (Circle)
            drawCircle(
                color = Color.White,
                radius = 6f,
                center = Offset(w * 0.62f, h * 0.57f)
            )

            // 2. Draw modern Rupee Symbol "₹" inside Wallet face
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor(if (isDark) "#2A1C4E" else "#4F378B")
                textSize = w * 0.16f
                isAntiAlias = true
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            drawContext.canvas.nativeCanvas.drawText(
                "₹",
                walletLeft + 22f,
                walletTop + (walletHeight * 0.65f),
                textPaint
            )

            // 3. Draw diagonal Upward Growth Arrow emerging from Wallet center
            val arrowPath = Path().apply {
                // Diagonal path starting from top left of wallet up to top right of canvas
                val startX = w * 0.45f
                val startY = h * 0.42f
                val endX = w * 0.78f
                val endY = h * 0.16f

                // Under-line/Stroke path
                moveTo(startX, startY)
                lineTo(endX, endY)
            }
            
            drawPath(
                path = arrowPath,
                color = if (isDark) Color(0xFF00E676) else Color(0xFF00C853),
                style = Stroke(width = 12f, cap = StrokeCap.Round)
            )

            // Arrow Head
            val tipX = w * 0.78f
            val tipY = h * 0.16f
            val arrowHeadPath = Path().apply {
                moveTo(tipX, tipY)
                lineTo(tipX - 25f, tipY + 4f)
                lineTo(tipX, tipY)
                lineTo(tipX - 4f, tipY + 25f)
                close()
            }
            drawPath(
                path = arrowHeadPath,
                color = if (isDark) Color(0xFF00E676) else Color(0xFF00C853),
                style = Stroke(width = 10f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

// ----------------------------------------------------
// DYNAMIC SHIMMER (SKELETON LOADERS) COMPONENTS
// ----------------------------------------------------
@Composable
fun ShimmerBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_anim"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color.LightGray.copy(alpha = 0.6f),
            Color.LightGray.copy(alpha = 0.2f),
            Color.LightGray.copy(alpha = 0.6f)
        ),
        start = Offset(10f, 10f),
        end = Offset(translateAnim.value, translateAnim.value)
    )

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(shimmerBrush)
        )
        content()
    }
}

@Composable
fun SkeletonDashboardCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ShimmerBackground(
                modifier = Modifier
                    .width(140.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {}
            ShimmerBackground(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            ) {}
        }
        Spacer(modifier = Modifier.height(16.dp))
        ShimmerBackground(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(48.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {}
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                ShimmerBackground(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {}
                Spacer(modifier = Modifier.height(8.dp))
                ShimmerBackground(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(24.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {}
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                ShimmerBackground(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {}
                Spacer(modifier = Modifier.height(8.dp))
                ShimmerBackground(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(24.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {}
            }
        }
    }
}

@Composable
fun SkeletonTransactionItem() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShimmerBackground(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {}
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            ShimmerBackground(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(18.dp)
                    .clip(RoundedCornerShape(6.dp))
            ) {}
            Spacer(modifier = Modifier.height(8.dp))
            ShimmerBackground(
                modifier = Modifier
                    .fillMaxWidth(0.3f)
                .height(12.dp)
                .clip(RoundedCornerShape(4.dp))
            ) {}
        }
        ShimmerBackground(
            modifier = Modifier
                .width(64.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(6.dp))
        ) {}
    }
}

// ----------------------------------------------------
// FULLY RESPONSIVE CANVAS PIE CHART COMPOSABLE
// ----------------------------------------------------
@Composable
fun PaisaPieChart(
    data: Map<String, Double>,
    modifier: Modifier = Modifier,
    currencySymbol: String = "₹"
) {
    val total = data.values.sum()

    if (total == 0.0) {
        // Aesthetic empty state indicator
        Column(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "No Categorized Outlays Tracked Yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Save expense transactions to generate charts.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.LightGray
            )
        }
        return
    }

    val sortedData = data.toList().sortedByDescending { it.second }
    val proportions = sortedData.map { it.second / total }

    // Animate arc angles on entry
    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(key1 = data) {
        startAnimation = true
    }
    val animatedProgress by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "pie_animation"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Draw Chart Canvas
        Box(
            modifier = Modifier
                .size(160.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                var currentAngle = -90f
                proportions.forEachIndexed { index, proportion ->
                    val sweepAngle = (proportion * 360f * animatedProgress).toFloat()
                    val color = PaisaTheme.ChartColors[index % PaisaTheme.ChartColors.size]
                    
                    drawArc(
                        color = color,
                        startAngle = currentAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        size = size
                    )
                    currentAngle += (proportion * 360f).toFloat()
                }

                // Inner circle for donut styling
                drawCircle(
                    color = Color.White,
                    radius = size.width * 0.28f
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Total", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text(
                    "$currencySymbol${String.format("%.0f", total)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Legend Grid
        Column(
            modifier = Modifier
                .weight(1f)
                .wrapContentHeight()
        ) {
            sortedData.take(5).forEachIndexed { index, item ->
                val color = PaisaTheme.ChartColors[index % PaisaTheme.ChartColors.size]
                val percentString = String.format("%.1f", (item.second / total) * 100)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        item.first,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    Text(
                        "$percentString%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                }
            }
            if (sortedData.size > 5) {
                val remainingSum = sortedData.drop(5).sumOf { it.second }
                val remainingPercent = String.format("%.1f", (remainingSum / total) * 100)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Others",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    Text(
                        "$remainingPercent%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// FULLY RESPONSIVE CANVAS BAR CHART COMPOSABLE
// ----------------------------------------------------
@Composable
fun PaisaBarChart(
    income: Double,
    expense: Double,
    modifier: Modifier = Modifier,
    currencySymbol: String = "₹"
) {
    val maxVal = maxOf(income, expense, 1000.0)

    // Entry anim
    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(key1 = income, key2 = expense) {
        startAnimation = true
    }
    val animatedProgress by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "bar_animation"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        colors = CardDefaults.cardColors(containerColor = PaisaTheme.OffWhite),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                "Inflows vs Outlays Summary",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = PaisaTheme.DeepPurple
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Grid lines (3 horizontal helper lines)
                    val linesCount = 3
                    for (i in 1..linesCount) {
                        val yPos = h - (h * (i.toFloat() / (linesCount.toFloat() + 1)))
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.4f),
                            start = Offset(0f, yPos),
                            end = Offset(w, yPos),
                            strokeWidth = 2f
                        )
                    }

                    // Bar configurations
                    val barWidth = w * 0.16f
                    val groupSpacing = w * 0.15f
                    val barSpacing = w * 0.05f

                    // 1. Income Bar (Green)
                    val incomeHeight = (h * 0.70f * (income / maxVal) * animatedProgress).toFloat()
                    val incomeLeft = (w / 2f) - barWidth - barSpacing
                    drawRoundRect(
                        color = PaisaTheme.MintGreen,
                        topLeft = Offset(incomeLeft, h - incomeHeight - 30f),
                        size = Size(barWidth, incomeHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
                    )

                    // 2. Expense Bar (Red)
                    val expenseHeight = (h * 0.70f * (expense / maxVal) * animatedProgress).toFloat()
                    val expenseLeft = (w / 2f) + barSpacing
                    drawRoundRect(
                        color = PaisaTheme.SoftRed,
                        topLeft = Offset(expenseLeft, h - expenseHeight - 30f),
                        size = Size(barWidth, expenseHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
                    )
                }

                // Bar Labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "$currencySymbol${String.format("%.0f", income)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = PaisaTheme.MintGreen
                        )
                        Text(
                            "Income",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "$currencySymbol${String.format("%.0f", expense)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = PaisaTheme.SoftRed
                        )
                        Text(
                            "Expense",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}
