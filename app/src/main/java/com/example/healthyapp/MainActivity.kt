package com.example.healthyapp

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.math.sqrt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ⚠️ 核心导入：引入我们刚刚拆分出去的 data 包组件
import com.example.healthyapp.data.WaterRecord
import com.example.healthyapp.data.WaterDao
import com.example.healthyapp.data.HealthDatabase
import com.example.healthyapp.data.HealthRepository
// 🔥 Firebase Firestore 社区数据
import com.example.healthyapp.firebase.CommunityPost
import com.example.healthyapp.firebase.FirestoreService
import com.example.healthyapp.network.HealthyApiClient
import com.example.healthyapp.network.WaterLogItem
import com.example.healthyapp.ui.theme.HealthyAPPTheme

// ==========================================
// 5. ViewModel 及 Factory (视图模型层)
// ==========================================
data class HealthProfile(
    val userName: String = "WANGBINYU",
    val studentId: String = "a207349",
    val age: Int = 20,
    val heightCm: Int = 182,
    val weightKg: Int = 92,
    val waterIntake: Int = 0,
    val dailyGoal: Int = 2000,
    val steps: Int = 432,
    val heartRate: Int = 85,
    val sleepQuality: Int = 85
)

class HealthViewModel(private val repository: HealthRepository) : ViewModel() {
    private val _uiState = mutableStateOf(HealthProfile())
    val uiState: State<HealthProfile> = _uiState
    var healthTip by mutableStateOf("Tap Refresh to get a healthy quote.")
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    // 暴露给 UI 的数据流（Room 本地）
    val waterHistory: Flow<List<WaterRecord>> = repository.allRecords

    // 服务端饮水记录
    var serverHistory by mutableStateOf<List<WaterLogItem>>(emptyList())
        private set
    var isHistoryLoading by mutableStateOf(false)
        private set

    init {
        // 从服务器加载个人资料
        loadProfileFromServer()
        // 自动计算总饮水量：实时监听数据库中的所有记录，求和后更新 UI 状态
        viewModelScope.launch {
            repository.allRecords.collect { records ->
                val totalIntake = records.sumOf { it.amount }
                _uiState.value = _uiState.value.copy(waterIntake = totalIntake)
            }
        }
    }

    fun loadWaterHistoryFromServer() {
        viewModelScope.launch {
            isHistoryLoading = true
            try {
                val response = HealthyApiClient.api.getWaterHistory(studentId = _uiState.value.studentId)
                if (response.success && response.items != null) {
                    serverHistory = response.items
                }
            } catch (_: Exception) {
                // 保留上次数据
            }
            isHistoryLoading = false
        }
    }

    // 🔥 Firestore：实时监听社区帖子
    private val firestoreService = FirestoreService()
    private var firestoreListener: com.google.firebase.firestore.ListenerRegistration? = null

    var communityPosts by mutableStateOf<List<CommunityPost>>(emptyList())
        private set
    var isPosting by mutableStateOf(false)
        private set

    init {
        // 🔥 Firestore：启动实时监听，自动更新 communityPosts
        firestoreListener = firestoreService.listenPosts { posts ->
            communityPosts = posts
        }
    }

    // 🔥 Firestore：发布新帖子
    fun addCommunityPost(studentId: String, tipText: String, source: String) {
        viewModelScope.launch {
            isPosting = true
            try {
                firestoreService.addPost(studentId, tipText, source)
            } catch (_: Exception) {
                errorMessage = "Failed to post"
            }
            isPosting = false
        }
    }

    // 🔥 Firestore：页面销毁时移除监听器
    override fun onCleared() {
        super.onCleared()
        firestoreListener?.remove()
    }

    private fun loadProfileFromServer() {
        viewModelScope.launch {
            try {
                val response = HealthyApiClient.api.getProfile(studentId = _uiState.value.studentId)
                if (response.success && response.profile != null) {
                    val p = response.profile
                    _uiState.value = _uiState.value.copy(
                        userName = p.userName,
                        studentId = p.studentId,
                        age = p.age,
                        heightCm = p.heightCm,
                        weightKg = p.weightKg,
                        dailyGoal = p.dailyWaterGoalMl
                    )
                }
            } catch (_: Exception) {
                // 服务器不可用时静默使用本地默认值
            }
        }
    }

    fun updateProfile(name: String, id: String, age: Int, heightCm: Int, weightKg: Int, goal: Int) {
        _uiState.value = _uiState.value.copy(
            userName = name,
            studentId = id,
            age = age,
            heightCm = heightCm,
            weightKg = weightKg,
            dailyGoal = goal
        )
        // 同步到服务器
        viewModelScope.launch {
            try {
                val response = HealthyApiClient.api.saveProfile(
                    userName = name,
                    studentId = id,
                    age = age,
                    heightCm = heightCm,
                    weightKg = weightKg,
                    dailyGoal = goal
                )
                if (!response.success) {
                    errorMessage = response.message ?: response.error ?: "Failed to save profile"
                }
            } catch (_: Exception) {
                errorMessage = "Server is offline, profile saved locally"
            }
        }
    }

    fun addWater(amount: Int) {
        if (amount > 0) {
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val message = "Added $amount ml (Time: $time)"

            viewModelScope.launch {
                // 写入 Room 数据库
                repository.insert(WaterRecord(amount = amount, logMessage = message))
            }
            // 同步到服务器
            viewModelScope.launch {
                try {
                    val response = HealthyApiClient.api.addWater(
                        studentId = _uiState.value.studentId,
                        amountMl = amount,
                        note = message
                    )
                    if (!response.success) {
                        errorMessage = response.message ?: response.error ?: "Failed to sync water"
                    }
                } catch (_: Exception) {
                    errorMessage = "Server is offline, water saved locally"
                }
            }
        }
    }
    fun refreshHealthTip() {
        healthTip = randomLocalTip()
    }

    private fun randomLocalTip(): String {
        val fallbackTips = listOf(
            "Drink at least 8 glasses of water daily for optimal hydration.",
            "A 20-minute walk can boost your mood and energy levels.",
            "Sleep 7-9 hours per night to support overall health.",
            "Eating a balanced diet rich in fruits and vegetables strengthens your immune system.",
            "Stay hydrated — even mild dehydration can cause fatigue and headaches.",
            "Regular exercise reduces the risk of chronic diseases.",
            "Taking deep breaths for 2 minutes can lower stress levels.",
            "Limit screen time 30 minutes before bed for better sleep quality.",
            "Stretching daily improves flexibility and reduces injury risk.",
            "Drinking water before meals can aid digestion and portion control.",
            "Good posture reduces back and neck pain.",
            "Spending time outdoors boosts vitamin D and mental well-being."
        )
        return "\"${fallbackTips.random()}\""
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
// 📱 传感器：摇一摇功能 ——————————————————————————
class MainActivity : ComponentActivity(), SensorEventListener {
    // 📱 传感器：加速度计相关
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var lastShakeTime = 0L

    private lateinit var viewModel: HealthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 📱 传感器：获取系统加速度计
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val database = HealthDatabase.getDatabase(applicationContext)
        val repository = HealthRepository(database.waterDao())
        val factory = HealthViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[HealthViewModel::class.java]

        setContent {
            HealthyAPPTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HealthApp(viewModel = viewModel)
                }
            }
        }
    }

    // 📱 传感器：页面可见时开始监听
    override fun onResume() {
        super.onResume()
        accelerometer?.also { accel ->
            sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI)
        }
    }

    // 📱 传感器：页面不可见时停止监听
    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // 📱 传感器：摇动检测算法
    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        // 📱 传感器：加速度 > 重力+15 且 2 秒防抖 → 加 100ml 水
        if (magnitude > SensorManager.GRAVITY_EARTH + 15f) {
            val now = System.currentTimeMillis()
            if (now - lastShakeTime > 2000) {
                lastShakeTime = now
                viewModel.addWater(100)
            }
        }
    }
}
// ————————————————————————————————————————————

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Input : Screen("input")
    data object Tips : Screen("tips")
    data object Detail : Screen("detail")
    data object Me : Screen("me")
    data object History : Screen("history")
    data object Community : Screen("community")
}

@Composable
fun HealthApp(viewModel: HealthViewModel) {
    val healthViewModel = viewModel
    val navController = rememberNavController()

    NavHost(

        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Dashboard.route) { DashboardScreen(navController, healthViewModel) }
        composable(Screen.Input.route) { InputScreen(navController, healthViewModel) }
        composable(Screen.Tips.route) { TipsScreen(navController, healthViewModel) }
        composable(Screen.Detail.route) { DetailScreen(navController, healthViewModel) }
        composable(Screen.Me.route) { MeScreen(navController, healthViewModel) }
        composable(Screen.History.route) { HistoryScreen(navController, healthViewModel) }
        composable(Screen.Community.route) { CommunityScreen(navController, healthViewModel) }
    }
}

// ==========================================
// UI 页面组件
// ==========================================
@Composable
fun DashboardScreen(navController: NavHostController, healthViewModel: HealthViewModel) {
    val uiState = healthViewModel.uiState.value

    // 👈 修复点：动态感知系统当前的导航栈顶路由
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route



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
                onClick = { navController.navigate(Screen.Community.route) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Text("Community Tips")
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
            BottomNavIcon(
                label = "Dashboard",
                imageResId = R.drawable.menu1,
                isSelected = currentRoute == Screen.Dashboard.route,
                onClick = { navController.navigate(Screen.Dashboard.route) {
                    popUpTo(Screen.Dashboard.route) { inclusive = true }
                } }
            )
            BottomNavIcon(
                label = "Tips",
                imageResId = R.drawable.menu2,
                isSelected = currentRoute == Screen.Tips.route,
                onClick = { navController.navigate(Screen.Tips.route) }
            )
            BottomNavIcon(
                label = "Me",
                imageResId = R.drawable.menu3,
                isSelected = currentRoute == Screen.Me.route,
                onClick = { navController.navigate(Screen.Me.route) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeScreen(navController: NavHostController, healthViewModel: HealthViewModel) {
    val uiState = healthViewModel.uiState.value
    val bmi = uiState.weightKg / ((uiState.heightCm / 100f) * (uiState.heightCm / 100f))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Me") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.face),
                contentDescription = "WANGBINYU profile photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(128.dp)
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = uiState.userName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Health Profile",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    ProfileMetricCard("Age", "${uiState.age}", "years")
                }
                Spacer(modifier = Modifier.width(12.dp))
                Box(modifier = Modifier.weight(1f)) {
                    ProfileMetricCard("Height", "${uiState.heightCm}", "cm")
                }
                Spacer(modifier = Modifier.width(12.dp))
                Box(modifier = Modifier.weight(1f)) {
                    ProfileMetricCard("Weight", "${uiState.weightKg}", "kg")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Body Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SummaryRow("BMI", String.format(Locale.US, "%.1f", bmi))
                    SummaryRow("Daily Water Goal", "${uiState.dailyGoal} ml")
                    SummaryRow("Today Water", "${uiState.waterIntake} ml")
                    SummaryRow("Resting Heart Rate", "${uiState.heartRate} bpm")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
            ) {
                Text(
                    text = "Focus for today: stay hydrated, keep moving, and protect your recovery time.",
                    modifier = Modifier.padding(20.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF1B5E20),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { navController.navigate(Screen.Dashboard.route) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Back to Dashboard")
            }
        }
    }
}

@Composable
fun TipsScreen(navController: NavHostController, healthViewModel: HealthViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = healthViewModel.healthTip,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { healthViewModel.refreshHealthTip() },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Refresh")
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { navController.popBackStack() },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Back")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputScreen(navController: NavHostController, healthViewModel: HealthViewModel) {
    val uiState = healthViewModel.uiState.value
    var name by remember { mutableStateOf(uiState.userName) }
    var studentId by remember { mutableStateOf(uiState.studentId) }
    var ageText by remember { mutableStateOf(uiState.age.toString()) }
    var heightText by remember { mutableStateOf(uiState.heightCm.toString()) }
    var weightText by remember { mutableStateOf(uiState.weightKg.toString()) }
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
                value = ageText, onValueChange = { ageText = it }, label = { Text("Age") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = heightText, onValueChange = { heightText = it }, label = { Text("Height (cm)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = weightText, onValueChange = { weightText = it }, label = { Text("Weight (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                    val age = ageText.toIntOrNull() ?: uiState.age
                    val height = heightText.toIntOrNull() ?: uiState.heightCm
                    val weight = weightText.toIntOrNull() ?: uiState.weightKg
                    val goal = goalText.toIntOrNull() ?: uiState.dailyGoal
                    val water = waterText.toIntOrNull() ?: 0
                    healthViewModel.updateProfile(name, studentId, age, height, weight, goal)
                    healthViewModel.addWater(water)
                    navController.navigate(Screen.Detail.route)
                },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)
            ) {
                Text("Save and Continue")
            }
            healthViewModel.errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
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
    val localHistory by healthViewModel.waterHistory.collectAsState(initial = emptyList())
    val serverItems = healthViewModel.serverHistory
    val isLoading = healthViewModel.isHistoryLoading

    LaunchedEffect(Unit) {
        healthViewModel.loadWaterHistoryFromServer()
    }

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
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            val displayList = if (serverItems.isNotEmpty()) {
                serverItems.map { "${it.amountMl} ml - ${it.note ?: ""}  (${it.createdAt?.take(16) ?: ""})" }
            } else {
                localHistory.map { it.logMessage }
            }

            if (displayList.isEmpty()) {
                Text(
                    text = "No water intake recorded yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(displayList) { text ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(
                                text = "💧 $text",
                                modifier = Modifier.padding(16.dp),
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { healthViewModel.loadWaterHistoryFromServer() },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                enabled = !isLoading
            ) {
                Text(if (isLoading) "Loading..." else "Refresh from Server")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)
            ) {
                Text("Back")
            }
        }
    }
}

// 🔥 Firestore：社区帖子列表（实时同步）
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(navController: NavHostController, healthViewModel: HealthViewModel) {
    val posts = healthViewModel.communityPosts
    val isPosting = healthViewModel.isPosting
    val uiState = healthViewModel.uiState.value
    var inputText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Community") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
        ) {
            // 🔥 Firestore：实时数据自动刷新，无需手动加载
            if (posts.isEmpty()) {
                Text(
                    text = "No community posts yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(posts) { post ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = post.tipText,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = post.studentId,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "- ${post.source}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 🔥 Firestore：输入并提交新帖子（全宽提交按钮）
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Share a health tip...") },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                // 🔥 Firestore：写入 Firestore 集合 community_posts
                                healthViewModel.addCommunityPost(
                                    studentId = uiState.studentId,
                                    tipText = inputText.trim(),
                                    source = uiState.userName
                                )
                                inputText = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        enabled = inputText.isNotBlank() && !isPosting,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isPosting) "Posting..." else "Share to Community",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)
            ) {
                Text("Back")
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
fun ProfileMetricCard(label: String, value: String, unit: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
fun BottomNavIcon(label: String, imageResId: Int, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() } // 👈 修复点：让图标变得可以点击
    ) {
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
