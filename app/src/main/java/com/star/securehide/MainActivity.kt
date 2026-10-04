package com.star.securehide

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppRootNavigator(this)
                }
            }
        }
    }
}

@Composable
fun AppRootNavigator(context: Context) {
    var isUnlocked by remember { mutableStateOf(false) }

    if (!isUnlocked) {
        DisguiseStopwatchScreen(onUnlock = { isUnlocked = true })
    } else {
        SecretManagerScreen(context = context, onLock = { isUnlocked = false })
    }
}

@Composable
fun DisguiseStopwatchScreen(onUnlock: () -> Unit) {
    var timeInMillis by remember { mutableLongStateOf(0L) }
    var isRunning by remember { mutableStateOf(false) }
    var secretClickCount by remember { mutableIntStateOf(0) }
    var lastClickTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(10L)
            timeInMillis += 10L
        }
    }

    val minutes = (timeInMillis / 1000) / 60
    val seconds = (timeInMillis / 1000) % 60
    val millis = (timeInMillis % 1000) / 10

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Stopwatch", style = MaterialTheme.typography.titleLarge, color = Color.Gray)
        Spacer(modifier = Modifier.height(40.dp))

        Box(
            modifier = Modifier
                .size(260.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable {
                    val now = System.currentTimeMillis()
                    if (now - lastClickTime < 600) {
                        secretClickCount++
                    } else {
                        secretClickCount = 1
                    }
                    lastClickTime = now

                    if (secretClickCount >= 5) {
                        secretClickCount = 0
                        onUnlock()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = String.format("%02d:%02d.%02d", minutes, seconds, millis),
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(50.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            FilledTonalButton(onClick = { timeInMillis = 0L; isRunning = false }) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("إعادة")
            }

            Button(
                onClick = { isRunning = !isRunning },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isRunning) "إيقاف" else "تشغيل")
            }
        }
    }
}

data class InstalledApp(
    val name: String,
    val packageName: String,
    val icon: Drawable,
    var isHidden: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecretManagerScreen(context: Context, onLock: () -> Unit) {
    var appList by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var hasShizukuPermission by remember { mutableStateOf(AppHiderManager.isShizukuAvailable()) }

    fun loadApps() {
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val filtered = packages.filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
            .map {
                InstalledApp(
                    name = it.loadLabel(pm).toString(),
                    packageName = it.packageName,
                    icon = it.loadIcon(pm),
                    isHidden = !it.enabled
                )
            }.sortedBy { it.name }
        appList = filtered
        isLoading = false
    }

    LaunchedEffect(Unit) {
        loadApps()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("لوحة التحكم السرية") },
                actions = {
                    IconButton(onClick = onLock) {
                        Icon(Icons.Default.VisibilityOff, contentDescription = "قفل")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            
            // إذا لم يتوفر روت ولا Shizuku، نظهر زر لطلب صلاحية Shizuku
            if (!AppHiderManager.isRoot() && !hasShizukuPermission) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("مطلوب صلاحية للعمل بدون روت", fontWeight = FontWeight.Bold)
                        Text("الرجاء تشغيل تطبيق Shizuku على هاتفك ثم الضغط على الزر أدناه لتفعيل الإخفاء.", fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            try {
                                Shizuku.requestPermission(101)
                                hasShizukuPermission = AppHiderManager.isShizukuAvailable()
                            } catch (e: Exception) {
                                Toast.makeText(context, "تأكد من تشغيل تطبيق Shizuku أولاً!", Toast.LENGTH_LONG).show()
                            }
                        }) {
                            Text("ربط مع Shizuku")
                        }
                    }
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                    items(appList) { app ->
                        var isChecked by remember { mutableStateOf(app.isHidden) }

                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    bitmap = app.icon.toBitmap(96, 96).asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(app.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                    Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                                Switch(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        val success = AppHiderManager.setAppHidden(app.packageName, checked)
                                        if (success) {
                                            isChecked = checked
                                            Toast.makeText(context, if (checked) "تم إخفاء التطبيق" else "تم إظهار التطبيق", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "فشل الإجراء! تأكد من تفعيل Shizuku أو الروت.", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        SupportSection(context)
                        Spacer(modifier = Modifier.height(30.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SupportSection(context: Context) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("الدعم الفني والمساعدة", fontWeight = FontWeight.Bold)
            Text("SecureHide v1.2.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth().clickable {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:starsyria2500@gmail.com"))
                    context.startActivity(intent)
                }.padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("starsyria2500@gmail.com", fontSize = 14.sp)
            }
            Row(
                modifier = Modifier.fillMaxWidth().clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=963938466549"))
                    context.startActivity(intent)
                }.padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF25D366), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("+963 938 466 549", color = Color(0xFF25D366), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}
