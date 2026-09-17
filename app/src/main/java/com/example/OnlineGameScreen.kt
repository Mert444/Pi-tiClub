package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineLobbyScreen(navController: NavController, gameViewModel: GameViewModel) {
    val onlineManager = gameViewModel.onlineManager
    val onlineState by onlineManager.onlineState.collectAsState()
    val isJoining by onlineManager.isJoining.collectAsState()
    val connectionStatus by onlineManager.connectionStatus.collectAsState()
    val state by gameViewModel.state.collectAsState()
    val context = LocalContext.current

    // Tab 0 = Raum Beitreten (Join Friend), Tab 1 = Raum Erstellen (Host)
    var selectedTab by remember { mutableStateOf(0) }
    var inputRoomCode by remember { mutableStateOf("") }
    var joinError by remember { mutableStateOf<String?>(null) }

    // Always reset lobby state when entering lobby screen
    LaunchedEffect(Unit) {
        onlineManager.resetLobby()
    }

    // Auto-navigate when game starts
    LaunchedEffect(onlineState.isGameStarted) {
        if (onlineState.isGameStarted) {
            navController.navigate("online_game") {
                popUpTo("online_lobby") { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MenuTeal)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Bar: Back button + Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = {
                        onlineManager.leaveRoom()
                        navController.navigate("menu") {
                            popUpTo("online_lobby") { inclusive = true }
                        }
                    }
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Zurück",
                        tint = Color.White
                    )
                }

                Text(
                    text = "ONLINE (1-GEGEN-1)",
                    color = PrimaryYellow,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.size(48.dp))
            }

            Text(
                text = "Spiele direkt mit deinen Freunden per 4-stelligem Raum-Code",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            // Segmented Tab Control: [ Raum Beitreten ] | [ Raum Erstellen ]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF042016), RoundedCornerShape(12.dp))
                    .border(1.5.dp, PrimaryYellow.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                // Tab 0: Raum Beitreten
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedTab == 0) PrimaryYellow else Color.Transparent)
                        .clickable { selectedTab = 0 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.GroupAdd,
                            contentDescription = null,
                            tint = if (selectedTab == 0) OnPrimaryYellow else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Raum Beitreten",
                            color = if (selectedTab == 0) OnPrimaryYellow else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                // Tab 1: Raum Erstellen
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedTab == 1) PrimaryYellow else Color.Transparent)
                        .clickable { selectedTab = 1 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.AddCircle,
                            contentDescription = null,
                            tint = if (selectedTab == 1) OnPrimaryYellow else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Raum Erstellen",
                            color = if (selectedTab == 1) OnPrimaryYellow else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // TAB 0: RAUM BEITRETEN (JOIN ROOM)
            if (selectedTab == 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = BackgroundGreen),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryYellow)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🎮 RUNDE DEINES FREUNDES BEITRETEN",
                            color = PrimaryYellow,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Gib hier den 4-stelligen Raum-Code ein, den dir dein Freund geschickt hat:",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (!isJoining) {
                            // Large 4-digit code visual display
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (i in 0 until 4) {
                                    val digit = inputRoomCode.getOrNull(i)?.toString() ?: ""
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp)
                                            .size(54.dp)
                                            .background(Color(0xFF072B1E), RoundedCornerShape(10.dp))
                                            .border(
                                                width = if (inputRoomCode.length == i) 2.dp else 1.dp,
                                                color = if (inputRoomCode.length == i) PrimaryYellow else OutlineYellow.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(10.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = digit,
                                            color = PrimaryYellow,
                                            fontSize = 26.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Outlined text field for direct numerical entry
                            OutlinedTextField(
                                value = inputRoomCode,
                                onValueChange = {
                                    val digitsOnly = it.filter { c -> c.isDigit() }
                                    if (digitsOnly.length <= 4) {
                                        inputRoomCode = digitsOnly
                                        joinError = null
                                    }
                                },
                                label = { Text("4-stelligen Code eingeben", color = OutlineYellow) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryYellow,
                                    unfocusedBorderColor = OutlineYellow,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Paste from Clipboard Button
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                        val digits = clipText.filter { it.isDigit() }.take(4)
                                        if (digits.length == 4) {
                                            inputRoomCode = digits
                                            joinError = null
                                            Toast.makeText(context, "Code $digits eingefügt!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Kein 4-stelliger Code in Zwischenablage gefunden", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Konnte nicht aus Zwischenablage lesen", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, OutlineYellow),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Aus Zwischenablage einfügen", fontSize = 12.sp)
                            }

                            if (joinError != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = joinError ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Main Join Button
                            Button(
                                onClick = {
                                    onlineManager.joinRoom(inputRoomCode, state.playerName) { success, msg ->
                                        if (!success) {
                                            joinError = msg
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryYellow,
                                    contentColor = OnPrimaryYellow
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                enabled = inputRoomCode.length == 4
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Runde Beitreten & Spielen",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            // ACTIVE JOINING PROGRESS STATE
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF072B1E), RoundedCornerShape(12.dp))
                                    .border(1.5.dp, PrimaryYellow, RoundedCornerShape(12.dp))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = PrimaryYellow,
                                        strokeWidth = 3.dp,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = "Verbinde mit Raum #$inputRoomCode...",
                                        color = PrimaryYellow,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = connectionStatus,
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "Warte auf Antwort deines Freundes...\nSobald die Verbindung steht, startet das Spiel automatisch.",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    OutlinedButton(
                                        onClick = {
                                            onlineManager.cancelJoin()
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Abbrechen / Code ändern", fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // How it works info box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(LightGreen.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "💡 So einfach funktioniert's:",
                                    color = PrimaryYellow,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "1. Dein Freund tippt auf 'Raum Erstellen' und nennt dir den Code.",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "2. Du gibst den Code hier ein (oder tippst auf 'Aus Zwischenablage einfügen').",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "3. Tippe auf 'Beitreten' – das Spiel startet bei beiden synchron!",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            } else {
                // TAB 1: RAUM ERSTELLEN (CREATE ROOM / HOST)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = BackgroundGreen),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryYellow)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "➕ NEUEN RAUM FÜR FREUND ERSTELLEN",
                            color = PrimaryYellow,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Erstelle einen Raum-Code und lade deinen Freund ein:",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (onlineState.roomCode.isEmpty()) {
                            Button(
                                onClick = { onlineManager.createRoom(state.playerName) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryYellow,
                                    contentColor = OnPrimaryYellow
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.AddCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Raum-Code Generieren", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            // Display Room Code
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF072B1E), RoundedCornerShape(12.dp))
                                    .border(2.dp, PrimaryYellow, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 28.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = onlineState.roomCode,
                                    color = PrimaryYellow,
                                    fontSize = 38.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 8.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Share Button (WhatsApp / SMS / etc.)
                                Button(
                                    onClick = {
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, "Lass uns Pişti spielen! Tritt meiner Runde bei mit Raum-Code: ${onlineState.roomCode}")
                                            type = "text/plain"
                                        }
                                        val shareIntent = Intent.createChooser(sendIntent, "Raum-Code teilen")
                                        context.startActivity(shareIntent)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PrimaryYellow,
                                        contentColor = OnPrimaryYellow
                                    ),
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Per WhatsApp teilen", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                // Copy Code Button
                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Raum Code", onlineState.roomCode)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Code ${onlineState.roomCode} kopiert!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryYellow),
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Kopieren", fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Waiting indicator
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = PrimaryYellow,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Warte auf Mitspieler...",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Sobald dein Freund den Code bei sich eingibt, startet das Spiel für euch beide automatisch!",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Back to Main Menu Button
            TextButton(
                onClick = {
                    onlineManager.leaveRoom()
                    navController.navigate("menu") {
                        popUpTo("online_lobby") { inclusive = true }
                    }
                }
            ) {
                Text("← Zurück zum Hauptmenü", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
            }
        }
    }
}

// --- ONLINE GAME SCREEN (2-PLAYER LIVE GAME BOARD) ---

@Composable
fun OnlineGameScreen(navController: NavController, gameViewModel: GameViewModel) {
    val onlineManager = gameViewModel.onlineManager
    val onlineState by onlineManager.onlineState.collectAsState()
    val myPlayerId by onlineManager.myPlayerId.collectAsState()
    var showExitDialog by remember { mutableStateOf(false) }

    val opponentId = if (myPlayerId == 0) 1 else 0
    val myPlayer = onlineState.players.find { it.id == myPlayerId } ?: OnlinePlayerDto(myPlayerId, "Du")
    val opponentPlayer = onlineState.players.find { it.id == opponentId } ?: OnlinePlayerDto(opponentId, "Mitspieler")

    val isMyTurn = onlineState.currentTurnIndex == myPlayerId

    val feltGradient = Brush.radialGradient(
        colors = listOf(Color(0xFF0F3B6A), Color(0xFF0A2548), Color(0xFF051428)),
        radius = 1200f
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(feltGradient)
    ) {
        // Gold Border Inlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
                .border(1.5.dp, Color(0xFFD4AF37).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
        )

        // --- TOP AREA: HEADER & LIVE 101 SCOREBOARD ---
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 10.dp, start = 8.dp, end = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar with Room Code, Sync & Exit Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Room Code Pill
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFFD4AF37), RoundedCornerShape(16.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF00E676), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ONLINE # ${onlineState.roomCode}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Action Buttons (Resync + Exit)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { onlineManager.resyncState() },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF072B1E), CircleShape)
                            .border(1.dp, Color(0xFFD4AF37).copy(alpha = 0.8f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Synchronisieren",
                            tint = PrimaryYellow,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { showExitDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF072B1E), CircleShape)
                            .border(1.dp, Color(0xFFD4AF37).copy(alpha = 0.8f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.ExitToApp,
                            contentDescription = "Raum verlassen",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // DEDICATED LIVE SCOREBOARD (PUNKTE-ZÄHLER)
            OnlineLiveScoreboard(
                myPlayer = myPlayer,
                opponentPlayer = opponentPlayer,
                currentTurnIndex = onlineState.currentTurnIndex,
                myPlayerId = myPlayerId,
                roundNumber = onlineState.roundNumber,
                targetScore = onlineState.targetScore,
                deckSize = onlineState.deck.size
            )

            Spacer(modifier = Modifier.height(10.dp))

            // OPPONENT HAND (FACEDOWN CARDS AT TOP)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val opponentCount = opponentPlayer.hand.size
                if (opponentCount > 0) {
                    repeat(opponentCount) {
                        PlaidCardBack(
                            modifier = Modifier
                                .width(48.dp)
                                .height(70.dp)
                                .shadow(3.dp, RoundedCornerShape(6.dp))
                        )
                    }
                } else {
                    Text(
                        text = "Keine Handkarten",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }
            }
        }

        // --- DRAW DECK STOCK (TOP-LEFT OF CENTER TABLE) ---
        if (onlineState.deck.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                PlaidCardBack(
                    modifier = Modifier
                        .width(50.dp)
                        .height(74.dp)
                        .shadow(4.dp, RoundedCornerShape(6.dp))
                )
                Box(
                    modifier = Modifier
                        .offset(x = 6.dp, y = (-6).dp)
                        .background(PrimaryYellow, RoundedCornerShape(8.dp))
                        .border(1.dp, Color.Black, RoundedCornerShape(8.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${onlineState.deck.size}",
                        color = OnPrimaryYellow,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // --- CENTER TABLE: STACKED PLAYED CARDS & ANNOUNCEMENTS ---
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.75f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pişti Alert or Overshoot Banner
            if (onlineState.lastPistiMessage != null) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFD32F2F), RoundedCornerShape(12.dp))
                        .border(1.5.dp, PrimaryYellow, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = onlineState.lastPistiMessage ?: "",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            } else if (onlineState.overshootMessage != null) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFB71C1C), RoundedCornerShape(12.dp))
                        .border(1.5.dp, PrimaryYellow, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = onlineState.overshootMessage ?: "",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Turn Info Pill
            Box(
                modifier = Modifier
                    .background(
                        if (isMyTurn) Color(0xFF1B5E20) else Color.Black.copy(alpha = 0.65f),
                        RoundedCornerShape(16.dp)
                    )
                    .border(
                        1.2.dp,
                        if (isMyTurn) PrimaryYellow else Color.White.copy(alpha = 0.3f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isMyTurn) "⭐ DU BIST AM ZUG!" else "⏳ ${opponentPlayer.name} IST AM ZUG...",
                    color = if (isMyTurn) PrimaryYellow else Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Center Table Stack (Identical stack styling to offline mode)
            Box(
                modifier = Modifier
                    .height(115.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (onlineState.centerPile.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .width(68.dp)
                            .height(100.dp)
                            .border(1.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tisch leer",
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    val topCardDto = onlineState.centerPile.last()
                    val topCard = topCardDto.toCard()

                    Box(contentAlignment = Alignment.TopEnd) {
                        Box(contentAlignment = Alignment.Center) {
                            if (onlineState.centerPile.size > 1) {
                                PlaidCardBack(
                                    modifier = Modifier
                                        .width(72.dp)
                                        .height(106.dp)
                                        .offset(x = (-4).dp, y = 4.dp)
                                        .shadow(3.dp, RoundedCornerShape(8.dp))
                                )
                            }
                            PlayingCardView(
                                card = topCard,
                                cardWidth = 72.dp,
                                cardHeight = 106.dp,
                                modifier = Modifier.shadow(6.dp, RoundedCornerShape(8.dp))
                            )
                        }

                        // Center Pile Count Badge
                        if (onlineState.centerPile.size > 1) {
                            Box(
                                modifier = Modifier
                                    .offset(x = 10.dp, y = (-10).dp)
                                    .background(PrimaryYellow, RoundedCornerShape(10.dp))
                                    .border(1.dp, Color.Black, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${onlineState.centerPile.size}",
                                    color = Color.Black,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- BOTTOM AREA: HUMAN PLAYER HAND (IDENTICAL FANNED STYLING) ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val domainHand = myPlayer.hand.map { it.toCard() }
            val domainCenter = onlineState.centerPile.map { it.toCard() }

            // Fanned Hand using the exact same smooth, non-flickering Composable as offline mode!
            FannedPlayerHand(
                hand = domainHand,
                isMyTurn = isMyTurn,
                centerPile = domainCenter,
                onCardClick = { clickedCard ->
                    if (isMyTurn) {
                        val cardIndex = myPlayer.hand.indexOfFirst {
                            it.suit == clickedCard.suit.name && it.rank == clickedCard.rank.name
                        }
                        if (cardIndex != -1) {
                            onlineManager.playCard(cardIndex)
                        }
                    }
                },
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        // --- SMOOTH 2-PLAYER CARD DEALING OVERLAY ---
        if (onlineState.dealAnimTrigger > 0L) {
            OnlineDealingAnimation(
                trigger = onlineState.dealAnimTrigger,
                humanHand = myPlayer.hand
            )
        }
    }

    // ROUND END SUMMARY DIALOG (WITH EXACT 101 RULES EXPLANATION)
    if (onlineState.showRoundEndSummary && !onlineState.isMatchOver) {
        val isHost = myPlayerId == 0
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text(
                    "📊 RUNDE ${onlineState.roundNumber} BEENDET",
                    color = PrimaryYellow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "🎯 Ziel: Genau 101 Punkte!",
                        color = PrimaryYellow,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        "Der Gewinner muss exakt 101 Punkte erreichen. Wer mehr als 101 Punkte erzielt, überwirft sich und fällt auf 50 Punkte zurück!",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    onlineState.players.forEach { p ->
                        val isMe = p.id == myPlayerId
                        val needed = 101 - p.totalScore
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = p.name + if (isMe) " (Du)" else "",
                                        color = if (isMe) PrimaryYellow else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "${p.totalScore} / 101 Pkt",
                                        color = PrimaryYellow,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "+${p.roundScore} in dieser Rd • ${p.capturedCount} Karten",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = if (needed > 0) "Noch $needed bis 101" else "Exakt 101!",
                                        color = if (needed > 0) Color.White.copy(alpha = 0.9f) else Color(0xFF00E676),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    if (!isHost) {
                        Text(
                            "Warte auf Host für nächste Runde...",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                if (isHost) {
                    Button(
                        onClick = { onlineManager.startNextRound() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryYellow, contentColor = OnPrimaryYellow)
                    ) {
                        Text("Nächste Runde Starten", fontWeight = FontWeight.Bold)
                    }
                }
            },
            containerColor = BackgroundGreen
        )
    }

    // MATCH OVER DIALOG (EXACT 101 HIT)
    if (onlineState.isMatchOver) {
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text(
                    "🏆 MATCH BEENDET!",
                    color = PrimaryYellow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "GEWINNER: ${onlineState.matchWinnerName ?: "Unentschieden"}",
                        color = Color(0xFF00E676),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                    Text(
                        "Hat genau 101 Punkte erreicht!",
                        color = PrimaryYellow,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    onlineState.players.forEach { p ->
                        Text(
                            "${p.name}: ${p.totalScore} Punkte",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 14.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onlineManager.leaveRoom()
                        navController.navigate("menu") {
                            popUpTo("online_game") { inclusive = true }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryYellow, contentColor = OnPrimaryYellow)
                ) {
                    Text("Zurück zum Hauptmenü", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = BackgroundGreen
        )
    }

    // EXIT DIALOG
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Raum verlassen?", color = PrimaryYellow, fontWeight = FontWeight.Bold) },
            text = { Text("Möchtest du das Online-Spiel wirklich abbrechen?", color = Color.White) },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        onlineManager.leaveRoom()
                        navController.navigate("menu") {
                            popUpTo("online_game") { inclusive = true }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Ja, Verlassen", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Abbrechen", color = Color.White)
                }
            },
            containerColor = BackgroundGreen
        )
    }
}

// --- DEDICATED LIVE 2-PLAYER SCOREBOARD COMPOSABLE (PUNKTE-ZÄHLER) ---

@Composable
fun OnlineLiveScoreboard(
    myPlayer: OnlinePlayerDto,
    opponentPlayer: OnlinePlayerDto,
    currentTurnIndex: Int,
    myPlayerId: Int,
    roundNumber: Int,
    targetScore: Int = 101,
    deckSize: Int = 0,
    modifier: Modifier = Modifier
) {
    val isMyTurn = currentTurnIndex == myPlayerId
    val isOpponentTurn = currentTurnIndex == opponentPlayer.id

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.Black.copy(alpha = 0.75f),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryYellow.copy(alpha = 0.85f)),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Target & Round
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🏆 PUNKTESTAND",
                        color = PrimaryYellow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• Rd $roundNumber",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "🎯 Ziel: Exakt $targetScore Pkt",
                    color = PrimaryYellow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Player Columns (Side-by-Side: You vs Friend)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Player Box: YOU
                PlayerScoreCard(
                    name = "${myPlayer.name} (DU)",
                    totalScore = myPlayer.totalScore,
                    roundScore = myPlayer.roundScore,
                    capturedCount = myPlayer.capturedCount,
                    pistiCount = myPlayer.pistiCount,
                    isTurn = isMyTurn,
                    modifier = Modifier.weight(1f)
                )

                // Player Box: OPPONENT
                PlayerScoreCard(
                    name = opponentPlayer.name,
                    totalScore = opponentPlayer.totalScore,
                    roundScore = opponentPlayer.roundScore,
                    capturedCount = opponentPlayer.capturedCount,
                    pistiCount = opponentPlayer.pistiCount,
                    isTurn = isOpponentTurn,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PlayerScoreCard(
    name: String,
    totalScore: Int,
    roundScore: Int,
    capturedCount: Int,
    pistiCount: Int,
    isTurn: Boolean,
    modifier: Modifier = Modifier
) {
    val progress = (totalScore.coerceIn(0, 101) / 101f)
    val needed = 101 - totalScore

    Box(
        modifier = modifier
            .background(
                color = if (isTurn) Color(0xFF1B5E20) else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = if (isTurn) 1.5.dp else 0.5.dp,
                color = if (isTurn) PrimaryYellow else Color.White.copy(alpha = 0.25f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(vertical = 5.dp, horizontal = 6.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Player Name + Turn Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = name,
                    color = if (isTurn) PrimaryYellow else Color.White,
                    fontSize = 11.sp,
                    fontWeight = if (isTurn) FontWeight.ExtraBold else FontWeight.Bold,
                    maxLines = 1
                )
                if (isTurn) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .background(PrimaryYellow, RoundedCornerShape(3.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "AM ZUG",
                            color = Color.Black,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Score Counter (X / 101)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$totalScore",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = " / 101 Pkt",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                if (roundScore > 0) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "(+$roundScore)",
                        color = PrimaryYellow,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Progress Bar towards 101
            Spacer(modifier = Modifier.height(3.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = PrimaryYellow,
                trackColor = Color.White.copy(alpha = 0.15f)
            )

            // Subtitle Details: Karten, Piştis, Needed
            Spacer(modifier = Modifier.height(3.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$capturedCount K | $pistiCount P",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 9.sp
                )
                Text(
                    text = if (needed > 0) "Noch $needed" else "Ziel 101!",
                    color = if (needed > 0) Color.White.copy(alpha = 0.85f) else Color(0xFF00E676),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// --- SMOOTH 2-PLAYER CARD DEALING OVERLAY ---

@Composable
fun OnlineDealingAnimation(
    trigger: Long,
    humanHand: List<OnlineCardDto>,
    modifier: Modifier = Modifier
) {
    if (trigger <= 0L) return

    var isVisible by remember(trigger) { mutableStateOf(true) }
    val animProgress = remember(trigger) { Animatable(0f) }

    LaunchedEffect(trigger) {
        isVisible = true
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
        )
        isVisible = false
    }

    if (isVisible) {
        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val w = maxWidth.value
            val h = maxHeight.value
            val progress = animProgress.value

            val deckX = 16f
            val deckY = h * 0.5f - 37f

            val totalCards = 8
            for (i in 0 until totalCards) {
                val pIdx = i % 2 // 0 = Human, 1 = Opponent
                val slot = i / 2
                val cardStart = (i * 0.06f).coerceAtMost(0.55f)
                val cardEnd = (cardStart + 0.40f).coerceAtMost(1f)

                if (progress in cardStart..cardEnd) {
                    val cardProgress = ((progress - cardStart) / (cardEnd - cardStart)).coerceIn(0f, 1f)
                    val eased = FastOutSlowInEasing.transform(cardProgress)

                    val targetX: Float
                    val targetY: Float
                    val targetAngle: Float

                    if (pIdx == 0) {
                        // Human Player (Bottom)
                        val handCount = 4
                        val centerIndex = (handCount - 1) / 2f
                        val xOffset = (slot - centerIndex) * 52f
                        targetX = w * 0.5f + xOffset - 36f
                        targetY = h - 130f
                        targetAngle = -8f + slot * 5f
                    } else {
                        // Opponent (Top)
                        val handCount = 4
                        val centerIndex = (handCount - 1) / 2f
                        val xOffset = (slot - centerIndex) * 44f
                        targetX = w * 0.5f + xOffset - 24f
                        targetY = 80f
                        targetAngle = -6f + slot * 4f
                    }

                    val arcHeight = kotlin.math.sin(cardProgress * Math.PI).toFloat() * 35f
                    val curX = deckX + (targetX - deckX) * eased
                    val curY = deckY + (targetY - deckY) * eased - arcHeight

                    Box(
                        modifier = Modifier
                            .offset(x = curX.dp, y = curY.dp)
                            .rotate(targetAngle * cardProgress)
                    ) {
                        PlaidCardBack(
                            modifier = Modifier
                                .width(if (pIdx == 0) 64.dp else 48.dp)
                                .height(if (pIdx == 0) 94.dp else 70.dp)
                                .shadow(4.dp, RoundedCornerShape(6.dp))
                        )
                    }
                }
            }
        }
    }
}

// --- BACKWARDS COMPATIBLE ONLINE CARD RENDERER COMPOSABLE ---

@Composable
fun OnlineCardView(
    card: OnlineCardDto,
    modifier: Modifier = Modifier,
    highlightType: CardHighlightType = CardHighlightType.NONE,
    onClick: (() -> Unit)? = null
) {
    PlayingCardView(
        card = card.toCard(),
        cardWidth = 64.dp,
        cardHeight = 92.dp,
        highlightType = highlightType,
        modifier = modifier,
        onClick = onClick
    )
}


