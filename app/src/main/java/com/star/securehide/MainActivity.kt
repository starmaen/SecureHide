package com.star.securehide

import android.content.*
import android.content.pm.*
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.*
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.star.securehide.core.AppHiderManager
import kotlinx.coroutines.delay
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF00E5FF),
                    background = Color(0xFF0A0E17),
                    surface = Color(0xFF121824)
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(this)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(context: Context) {
    var isUnlocked by remember { mutableStateOf(false) }
    AnimatedContent(targetState = isUnlocked, label = "") { unlocked ->
        if (!unlocked) {
            StopwatchScreen(context) { isUnlocked = true }
        } else {
            DashboardScreen(context) { isUnlocked = false }
        }
    }
}

@Composable
fun StopwatchScreen(context: Context, onUnlock: () -> Unit) {
    var time by remember { mutableLongStateOf(0L) }
    var running by remember { mutableStateOf(false) }
    var clicks by remember { mutableIntStateOf(0) }
    var lastClick by remember { mutableLongStateOf(0L) }
    val vib = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    LaunchedEffect(running) {
        while (running) {
            delay(10)
            time += 10
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "CHRONOMETER",
            letterSpacing = 4.sp,
            color = Color(0xFF00E5FF).copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 20.dp)
        )

        Box(
            modifier = Modifier
                .size(260.dp)
                .border(2.dp, Color(0xFF00E5FF), CircleShape)
                .background(Color(0xFF121824))
                .clip(CircleShape)
                .clickable {
                    val now = System.currentTimeMillis()
                    vib.vibrate(VibrationEffect.createOneShot(40, 255))
                    if (now - lastClick < 500) clicks++ else clicks = 1
                    lastClick = now
                    if (clicks >= 5) {
                        clicks = 0
                        vib.vibrate(VibrationEffect.createOneShot(150, 255))
                        onUnlock()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = String.format("%02d:%02d.%02d", (time / 1000) / 60, (time / 1000) % 60, (time % 1000) / 10),
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color.White
            )
        }

        Row(
            modifier = Modifier.padding(bottom = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            OutlinedButton(onClick = { time = 0; running = false }) {
                Text("إعادة ضبط", color = Color.White)
            }
            Button(
                onClick = { running = !running },
                colors = ButtonDefaults.buttonColors(containerColor = if (running) Color.Red else Color(0xFF00E5FF))
            ) {
                Text(if (running) "إيقاف" else "ابدأ", color = Color.Black)
            }
        }
    }
}

data class AppItem(val name: String, val pkg: String, val icon: Drawable, var hidden: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(context: Context, onLock: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var tab by remember { mutableIntStateOf(0) }
    var apps by remember { mutableStateOf<List<AppItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    val root = AppHiderManager.isRootAvailable()
    val shizuku = AppHiderManager.isShizukuAvailable()

    LaunchedEffect(Unit) {
        val pm = context.packageManager
        apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
            .map {
                AppItem(it.loadLabel(pm).toString(), it.packageName, it.loadIcon(pm), !it.enabled)
            }.sortedBy { it.name }
        loading = false
    }

    val filtered = apps.filter {
        (it.name.contains(query, true) || it.pkg.contains(query, true)) &&
                when (tab) {
                    1 -> it.hidden
                    2 -> !it.hidden
                    else -> true
                }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SecureHide Pro", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0A0E17)),
                actions = {
                    IconButton(onClick = onLock) {
                        Icon(Icons.Default.Lock, null, tint = Color(0xFF00E5FF))
                    }
                }
            )
        }
    ) { pad ->
        Column(modifier = Modifier.padding(pad).fillMaxSize().background(Color(0xFF0A0E17))) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFF121824))
                        .border(1.dp, if (root) Color(0xFF00E5FF) else Color.Red, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("الروت: ${if (root) "نشط" else "متوقف"}", color = Color.White, fontSize = 12.sp)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFF121824))
                        .border(1.dp, if (shizuku) Color(0xFF00E5FF) else Color.Red, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("شيزوكو: ${if (shizuku) "نشط" else "متوقف"}", color = Color.White, fontSize = 12.sp)
                }
            }

            if (!root && !shizuku) {
                Button(
                    onClick = { try { Shizuku.requestPermission(101) } catch (e: Exception) {} },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("ربط مع Shizuku الآن")
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("بحث...") },
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )

            TabRow(selectedTabIndex = tab, containerColor = Color(0xFF0A0E17)) {
                Tab(selected = tab == 0, onClick = { tab = 0 }) { Text("الكل", modifier = Modifier.padding(8.dp)) }
                Tab(selected = tab == 1, onClick = { tab = 1 }) { Text("المخفية", modifier = Modifier.padding(8.dp)) }
                Tab(selected = tab == 2, onClick = { tab = 2 }) { Text("النشطة", modifier = Modifier.padding(8.dp)) }
            }

            if (loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f).padding(16.dp)) {
                    items(filtered) { app ->
                        var checked by remember { mutableStateOf(app.hidden) }
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF121824))
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Image(bitmap = app.icon.toBitmap(96, 96).asImageBitmap(), null, modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(app.name, color = Color.White, fontWeight = FontWeight.Bold)
                                    Text(app.pkg, color = Color.Gray, fontSize = 10.sp, maxLines = 1)
                                }
                                Switch(
                                    checked = checked,
                                    onCheckedChange = {
                                        if (AppHiderManager.setAppHiddenState(app.pkg, it)) {
                                            checked = it
                                            app.hidden = it
                                        }
                                    }
                                )
                            }
                        }
                    }
                    item { SupportCard(context) }
                }
            }
        }
    }
}

@Composable
fun SupportCard(context: Context) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121824))
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("الدعم الفني والمساعدة", fontWeight = FontWeight.Bold, color = Color.White)
            Text("SecureHide Pro v1.3.0", color = Color(0xFF00E5FF), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text("starsyria2500@gmail.com", color = Color.Gray, modifier = Modifier.clickable {
                context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:starsyria2500@gmail.com")))
            })
            Spacer(modifier = Modifier.height(8.dp))
            Text("واتساب: +963 938 466 549", color = Color(0xFF25D366), fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=963938466549")))
            })
        }
    }
}
