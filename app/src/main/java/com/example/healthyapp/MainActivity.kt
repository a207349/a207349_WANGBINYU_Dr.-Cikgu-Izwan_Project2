package com.example.healthyapp

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ⚠️ 核心导入：引入我们刚刚拆分出去的 data 包组件
import com.example.healthyapp.data.WaterRecord
import com.example.healthyapp.data.WaterDao
import com.example.healthyapp.data.HealthDatabase
import com.example.healthyapp.data.HealthRepository
import com.example.healthyapp.ui.theme.HealthyAPPTheme

// ==========================================
// 5. ViewModel 及 Factory (视图模型层)
// ==========================================
data class HealthProfile(
    val userName: String = "WANGBINYU",
    val studentId: String = "a207349",
    val waterIntake: Int = 0,
    val dailyGoal: Int = 2000,
    val steps: Int = 432,
    val heartRate: Int = 85,
    val sleepQuality: Int = 85
)

class HealthViewModel(private val repository: HealthRepository) : ViewModel() {
    private val _uiState = mutableStateOf(HealthProfile())
    val uiState: State<HealthProfile> = _uiState

    // 暴露给 UI 的数据流
    val waterHistory: Flow<List<WaterRecord>> = repository.allRecords

    init {
        // 自动计算总饮水量：实时监听数据库中的所有记录，求和后更新 UI 状态
        viewModelScope.launch {
            repository.allRecords.collect { records ->
                val totalIntake = records.sumOf { it.amount }
                _uiState.value = _uiState.value.copy(waterIntake = totalIntake)
            }
        }
    }

    fun updateProfile(name: String, id: String, goal: Int) {
        _uiState.value = _uiState.value.copy(
            userName = name,
            studentId = id,
            dailyGoal = goal
        )
    }

    fun addWater(amount: Int) {
        if (amount > 0) {
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val message = "Added $amount ml (Time: $time)"

            // 使用协程将数据异步写入 Room 数据库
            viewModelScope.launch {
                repository.insert(WaterRecord(amount = amount, logMessage = message))
            }
        }
    }
}

class HealthViewModelFactory(private val repository: HealthRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HealthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HealthViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// ==========================================
// MainActivity (主 Activity 入口)
// ==========================================
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化 Room 数据库和 Repository
        val database = HealthDatabase.getDatabase(applicationContext)
        val repository = HealthRepository(database.waterDao())
        val viewModelFactory = HealthViewModelFactory(repository)

        setContent {
            HealthyAPPTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HealthApp(factory = viewModelFactory)
                }
            }
        }
    }
}

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Input : Screen("input")
    data object Detail : Screen("detail")
    data object History : Screen("history")
    data object SdgInfo : Screen("sdg_info")
}

@Composable
fun HealthApp(factory: ViewModelProvider.Factory) {
    val healthViewModel: HealthViewModel = viewModel(factory = factory)
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Dashboard.route) { DashboardScreen(navController, healthViewModel) }
        composable(Screen.Input.route) { InputScreen(navController, healthViewModel) }
        composable(Screen.Detail.route) { DetailScreen(navController, healthViewModel) }
        composable(Screen.History.route) { HistoryScreen(navController, healthViewModel) }
        composable(Screen.SdgInfo.route) { SdgInfoScreen(navController) }
    }
}

// ==========================================
// UI 页面组件
// ==========================================
@Composable
fun DashboardScreen(navController: NavHostController, healthViewModel: HealthViewModel) {
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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

            Button(
                onClick = { navController.navigate(Screen.SdgInfo.route) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
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
fun InputScreen(navController: NavHostController, healthViewModel: HealthViewModel) {
    val uiState = healthViewModel.uiState.value
    var name by remember { mutableStateOf(uiState.userName) }
    var studentId by remember { mutableStateOf(uiState.studentId) }
    var goalText by remember { mutableStateOf(uiState.dailyGoal.toString()) }
    var waterText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Update Health Profile") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
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
                value = name, onValueChange = { name = it }, label = { Text("User Name") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = studentId, onValueChange = { studentId = it }, label = { Text("Student ID") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = goalText, onValueChange = { goalText = it }, label = { Text("Daily Water Goal (ml)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = waterText, onValueChange = { waterText = it }, label = { Text("Add Water Intake (ml)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(), singleLine = true
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
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)
            ) {
                Text("Save and Continue")
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { navController.navigate(Screen.Dashboard.route) },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Back to Dashboard")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(navController: NavHostController, healthViewModel: HealthViewModel) {
    val uiState = healthViewModel.uiState.value
    val remaining = (uiState.dailyGoal - uiState.waterIntake).coerceAtLeast(0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Health Summary") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
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

            Button(
                onClick = { navController.navigate(Screen.History.route) },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("View Water Intake History")
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { navController.navigate(Screen.Dashboard.route) },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Back to Dashboard")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navController: NavHostController, healthViewModel: HealthViewModel) {
    val historyList by healthViewModel.waterHistory.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Water Intake History") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
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
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(
                                text = "💧 ${record.logMessage}",
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
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)
            ) {
                Text("Back")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SdgInfoScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SDG 3 Impact") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFC8E6C9))
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
            Text(text = "Sustainable Development Goal 3", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Good Health and Well-being", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Ensure healthy lives and promote well-being for all at all ages.", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "App Impact Statement:", fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Proper hydration and health metric tracking can significantly prevent illnesses, improve daily energy levels, and encourage a healthier community lifestyle.", color = Color(0xFF2E7D32))
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("Back to Dashboard")
            }
        }
    }
}

@Composable
fun WaterTrackerCard(totalWater: Int, dailyGoal: Int, onAddWaterClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "💧", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = "Water Intake", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text(text = "Today: $totalWater ml / Goal: $dailyGoal ml", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onAddWaterClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Text("Go to Input Screen")
            }
        }
    }
}

@Composable
fun DashboardCard(title: String, icon: String, value: String, unit: String, detailText: String, bgColor: Color, textColor: Color) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium))
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = icon, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = textColor)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = value, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
                Spacer(modifier = Modifier.width(4.dp))
                if (unit.isNotEmpty()) {
                    Text(text = unit, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = textColor.copy(alpha = 0.8f), modifier = Modifier.padding(bottom = 6.dp))
                }
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = textColor.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = detailText, fontSize = 14.sp, color = textColor, lineHeight = 20.sp)
            }
        }
    }
}

@Composable
fun SummaryCard(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
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
        Text(text = label, fontSize = 12.sp, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
    }
}