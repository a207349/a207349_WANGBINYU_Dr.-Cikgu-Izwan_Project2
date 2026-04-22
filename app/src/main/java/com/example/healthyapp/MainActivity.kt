package com.example.healthyapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn // 【新增】用于历史记录列表
import androidx.compose.foundation.lazy.items // 【新增】用于历史记录列表项
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf // 【新增】用于保存列表状态
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import java.text.SimpleDateFormat // 【新增】用于时间格式化
import java.util.Date // 【新增】用于获取当前时间
import java.util.Locale // 【新增】

import com.example.healthyapp.R
import com.example.healthyapp.ui.theme.HealthyAPPTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HealthyAPPTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HealthApp()
                }
            }
        }
    }
}

data class HealthProfile(
    val userName: String = "WANGBINYU",
    val studentId: String = "a123456",
    val waterIntake: Int = 0,
    val dailyGoal: Int = 2000,
    val steps: Int = 432,
    val heartRate: Int = 85,
    val sleepQuality: Int = 85
)

class HealthViewModel : ViewModel() {
    private val _uiState = mutableStateOf(HealthProfile())
    val uiState: State<HealthProfile> = _uiState

    // 【新增】使用 mutableStateListOf 来保存历史记录列表
    private val _waterHistory = mutableStateListOf<String>()
    val waterHistory: List<String> get() = _waterHistory

    fun updateProfile(name: String, id: String, goal: Int) {
        _uiState.value = _uiState.value.copy(
            userName = name,
            studentId = id,
            dailyGoal = goal
        )
    }

    fun addWater(amount: Int) {
        if (amount > 0) {
            _uiState.value = _uiState.value.copy(
                waterIntake = _uiState.value.waterIntake + amount
            )
            // 【新增】每次加水时，记录一条带有时间的历史
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            _waterHistory.add("Added $amount ml (Time: $time)")
        }
    }
}

// 【修改】定义 5 个页面 route 满足指南要求
sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Input : Screen("input")
    data object Detail : Screen("detail")
    data object History : Screen("history")   // 【新增】第 4 个页面
    data object SdgInfo : Screen("sdg_info")  // 【新增】第 5 个页面
}

@Composable
fun HealthApp(
    healthViewModel: HealthViewModel = viewModel()
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(navController, healthViewModel)
        }
        composable(Screen.Input.route) {
            InputScreen(navController, healthViewModel)
        }
        composable(Screen.Detail.route) {
            DetailScreen(navController, healthViewModel)
        }
        // 【新增】注册第 4 和 第 5 个页面
        composable(Screen.History.route) {
            HistoryScreen(navController, healthViewModel)
        }
        composable(Screen.SdgInfo.route) {
            SdgInfoScreen(navController)
        }
    }
}

@Composable
fun DashboardScreen(
    navController: NavHostController,
    healthViewModel: HealthViewModel
) {
    val uiState = healthViewModel.uiState.value

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Hello, ${uiState.userName}!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ID: ${uiState.studentId}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Image(
                painter = painterResource(id = R.drawable.face),
                contentDescription = "Profile",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(52.dp)
                    .shadow(4.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Image(
                painter = painterResource(id = R.drawable.header),
                contentDescription = "Header",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .padding(bottom = 16.dp),
                contentScale = ContentScale.Fit
            )

            WaterTrackerCard(
                totalWater = uiState.waterIntake,
                dailyGoal = uiState.dailyGoal,
                onAddWaterClick = { navController.navigate(Screen.Input.route) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            DashboardCard(
                title = "Calories Burned",
                icon = "🔥",
                value = "1,234",
                unit = "kcal",
                detailText = "Great progress! You are 300 kcal away from your daily goal.",
                bgColor = Color(0xFFFFDBCF),
                textColor = Color(0xFF8A3D00)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    DashboardCard(
                        title = "Steps",
                        icon = "👟",
                        value = uiState.steps.toString(),
                        unit = "steps",
                        detailText = "Keep moving and stay active today.",
                        bgColor = Color(0xFFD1E4FF),
                        textColor = Color(0xFF0B3D91)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Box(modifier = Modifier.weight(1f)) {
                    DashboardCard(
                        title = "Heart Rate",
                        icon = "❤️",
                        value = uiState.heartRate.toString(),
                        unit = "bpm",
                        detailText = "Resting heart rate is in a healthy range.",
                        bgColor = Color(0xFFFFDAD6),
                        textColor = Color(0xFF7A1C1C)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            DashboardCard(
                title = "Sleep Quality",
                icon = "😴",
                value = uiState.sleepQuality.toString(),
                unit = "%",
                detailText = "You had a restful night with good recovery.",
                bgColor = Color(0xFFEADDFF),
                textColor = Color(0xFF4A2C7F)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🫀", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ECG Live",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Image(
                        painter = painterResource(id = R.drawable.heart),
                        contentDescription = "ECG",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { navController.navigate(Screen.Detail.route) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("View Health Details")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 【新增】第 5 个页面入口：关于 SDG
            Button(
                onClick = { navController.navigate(Screen.SdgInfo.route) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Text("About SDG 3 (Health & Well-being)")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 16.dp)
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            BottomNavIcon("Dashboard", R.drawable.menu1, true)
            BottomNavIcon("Input", R.drawable.menu2, false)
            BottomNavIcon("Details", R.drawable.menu3, false)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputScreen(
    navController: NavHostController,
    healthViewModel: HealthViewModel
) {
    val uiState = healthViewModel.uiState.value

    var name by remember { mutableStateOf(uiState.userName) }
    var studentId by remember { mutableStateOf(uiState.studentId) }
    var goalText by remember { mutableStateOf(uiState.dailyGoal.toString()) }
    var waterText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Update Health Profile") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("User Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = studentId,
                onValueChange = { studentId = it },
                label = { Text("Student ID") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = goalText,
                onValueChange = { goalText = it },
                label = { Text("Daily Water Goal (ml)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = waterText,
                onValueChange = { waterText = it },
                label = { Text("Add Water Intake (ml)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val goal = goalText.toIntOrNull() ?: uiState.dailyGoal
                    val water = waterText.toIntOrNull() ?: 0

                    healthViewModel.updateProfile(name, studentId, goal)
                    healthViewModel.addWater(water)

                    navController.navigate(Screen.Detail.route)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Save and Continue")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { navController.navigate(Screen.Dashboard.route) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Back to Dashboard")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    navController: NavHostController,
    healthViewModel: HealthViewModel
) {
    val uiState = healthViewModel.uiState.value
    val remaining = (uiState.dailyGoal - uiState.waterIntake).coerceAtLeast(0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Health Summary") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SummaryCard("User Name", uiState.userName)
            Spacer(modifier = Modifier.height(12.dp))

            SummaryCard("Student ID", uiState.studentId)
            Spacer(modifier = Modifier.height(12.dp))

            SummaryCard("Water Intake", "${uiState.waterIntake} ml")
            Spacer(modifier = Modifier.height(12.dp))

            SummaryCard("Daily Goal", "${uiState.dailyGoal} ml")
            Spacer(modifier = Modifier.height(12.dp))

            SummaryCard("Remaining to Goal", "$remaining ml")
            Spacer(modifier = Modifier.height(12.dp))

            SummaryCard("Heart Rate", "${uiState.heartRate} bpm")
            Spacer(modifier = Modifier.height(12.dp))

            SummaryCard("Sleep Quality", "${uiState.sleepQuality}%")
            Spacer(modifier = Modifier.height(24.dp))

            // 【新增】第 4 个页面入口：查看饮水历史
            Button(
                onClick = { navController.navigate(Screen.History.route) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("View Water Intake History")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { navController.navigate(Screen.Dashboard.route) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Back to Dashboard")
            }
        }
    }
}

// ==========================================
// 【新增】第 4 个页面：历史记录列表页
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navController: NavHostController,
    healthViewModel: HealthViewModel
) {
    val historyList = healthViewModel.waterHistory

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Water Intake History") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
        ) {
            if (historyList.isEmpty()) {
                Text(
                    text = "No water intake recorded yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(historyList) { record ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = "💧 $record",
                                modifier = Modifier.padding(16.dp),
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Back")
            }
        }
    }
}

// ==========================================
// 【新增】第 5 个页面：SDG 目标介绍页
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SdgInfoScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SDG 3 Impact") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFC8E6C9) // 浅绿色背景匹配 SDG3
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Sustainable Development Goal 3",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Good Health and Well-being",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Ensure healthy lives and promote well-being for all at all ages.",
                style = MaterialTheme.typography.bodyLarge,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "App Impact Statement:",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B5E20)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Proper hydration and health metric tracking can significantly prevent illnesses, improve daily energy levels, and encourage a healthier community lifestyle.",
                        color = Color(0xFF2E7D32)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("Back to Dashboard")
            }
        }
    }
}


@Composable
fun WaterTrackerCard(
    totalWater: Int,
    dailyGoal: Int,
    onAddWaterClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "💧", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Water Intake",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Today: $totalWater ml / Goal: $dailyGoal ml",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onAddWaterClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Go to Input Screen")
            }
        }
    }
}

@Composable
fun DashboardCard(
    title: String,
    icon: String,
    value: String,
    unit: String,
    detailText: String,
    bgColor: Color,
    textColor: Color
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = icon, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor
                )
                Spacer(modifier = Modifier.width(4.dp))
                if (unit.isNotEmpty()) {
                    Text(
                        text = unit,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = textColor.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = detailText,
                    fontSize = 14.sp,
                    color = textColor,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
fun SummaryCard(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun BottomNavIcon(label: String, imageResId: Int, isSelected: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(id = imageResId),
            contentDescription = label,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .alpha(if (isSelected) 1f else 0.4f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    HealthyAPPTheme {
        HealthApp()
    }
}