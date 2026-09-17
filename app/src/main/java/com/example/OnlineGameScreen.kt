package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
    val state by gameViewModel.state.collectAsState()
    val context = LocalContext.current

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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Language,
                    contentDescription = null,
                    tint = PrimaryYellow,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "ONLINE LOBBY (1-GEGEN-1)",
                    color = PrimaryYellow,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Text(
                text = "Spiele nur mit deinen Freunden per Raum-Code (Keine Bots)",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            // SECTION 1: CREATE A ROOM
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = BackgroundGreen),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryYellow)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "1. NEUEN RAUM ERSTELLEN",
                        color = PrimaryYellow,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Erstelle einen Raum-Code und sende ihn deinem Freund:",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (onlineState.roomCode.isEmpty()) {
                        Button(
                            onClick = { onlineManager.createRoom(state.playerName) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryYellow,
                                contentColor = OnPrimaryYellow
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Raum-Code Generieren", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // Display Room Code
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF072B1E), RoundedCornerShape(12.dp))
                                .border(2.dp, PrimaryYellow, RoundedCornerShape(12.dp))
                                .padding(horizontal = 24.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = onlineState.roomCode,
                                color = PrimaryYellow,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 6.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Raum Code", onlineState.roomCode)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Code ${onlineState.roomCode} kopiert!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryYellow),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Code Kopieren", fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = PrimaryYellow,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Warte auf Mitspieler...",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // SECTION 2: JOIN A ROOM WITH CODE
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = BackgroundGreen),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryYellow)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "2. RAUM CODE EINGEBEN",
                        color = PrimaryYellow,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Dein Freund hat einen Code? Gib ihn hier ein:",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = inputRoomCode,
                        onValueChange = {
                            if (it.length <= 4) inputRoomCode = it.filter { char -> char.isDigit() }
                        },
                        label = { Text("4-stelliger Code (z.B. 4829)", color = OutlineYellow) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryYellow,
                            unfocusedBorderColor = OutlineYellow,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (joinError != null) {
                        Text(
                            text = joinError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            onlineManager.joinRoom(inputRoomCode, state.playerName) { success, msg ->
                                if (!success) {
                                    joinError = msg
                                } else {
                                    joinError = null
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryYellow,
                            contentColor = OnPrimaryYellow
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        enabled = inputRoomCode.length == 4
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Raum Beitreten & Spielen", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Back to Menu Button
            TextButton(
                onClick = {
                    onlineManager.leaveRoom()
                    navController.navigate("menu") {
                        popUpTo("online_lobby") { inclusive = true }
                    }
                }
            ) {
                Text("Zurück zum Hauptmenü", color = Color.White.copy(alpha = 0.8f))
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

    var landedCounts by remember(onlineState.dealAnimTrigger) {
        mutableStateOf<List<Int>?>(if (onlineState.dealAnimTrigger > 0L) listOf(0, 0) else null)
    }

    LaunchedEffect(onlineState.dealAnimTrigger) {
        if (onlineState.dealAnimTrigger > 0L) {
            landedCounts = listOf(0, 0)
            for (c in 1..4) {
                kotlinx.coroutines.delay(180)
                landedCounts = listOf(c, c)
            }
            landedCounts = null
        }
    }

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

        // Top Left Status Pill
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 16.dp, start = 16.dp)
                .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFFD4AF37), RoundedCornerShape(16.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color.Green, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ONLINE | RAUM: ${onlineState.roomCode}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Top Right Exit Button
        IconButton(
            onClick = { showExitDialog = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp)
                .size(40.dp)
                .border(1.5.dp, Color(0xFFD4AF37).copy(alpha = 0.8f), CircleShape)
                .background(Color(0xFF072B1E), CircleShape)
        ) {
            Icon(
                Icons.Default.ExitToApp,
                contentDescription = "Raum verlassen",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        // --- OPPONENT PLAYER (TOP CENTER) ---
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Opponent Name & Score Header
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .border(
                        1.dp,
                        if (onlineState.currentTurnIndex == opponentId) PrimaryYellow else Color.Transparent,
                        RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "👤 ${opponentPlayer.name} | Pkt: ${opponentPlayer.totalScore} | Karten: ${opponentPlayer.capturedCount}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Opponent Hand (Facedown)
            val opponentCardCount = if (landedCounts != null) landedCounts!![1] else opponentPlayer.hand.size
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(opponentCardCount) {
                    PlaidCardBack(
                        modifier = Modifier
                            .width(52.dp)
                            .height(76.dp)
                            .shadow(3.dp, RoundedCornerShape(6.dp))
                    )
                }
            }
        }

        // --- DRAW DECK STOCK (TOP-LEFT OF CENTER) ---
        if (onlineState.deck.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 24.dp, top = 135.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                PlaidCardBack(
                    modifier = Modifier
                        .width(52.dp)
                        .height(76.dp)
                        .shadow(4.dp, RoundedCornerShape(6.dp))
                )
                Box(
                    modifier = Modifier
                        .offset(x = 8.dp, y = (-8).dp)
                        .background(Color.White, RoundedCornerShape(6.dp))
                        .border(1.2.dp, Color.Black, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${onlineState.deck.size}",
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // --- CENTER TABLE (SCOREBOARD & PILE) ---
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Live Score Board
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFD4AF37), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "RUNDE ${onlineState.roundNumber} | ZIEL: ${onlineState.targetScore} PKT",
                    color = PrimaryYellow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Turn Banner / Pişti Notice
            if (onlineState.lastPistiMessage != null) {
                Text(
                    text = onlineState.lastPistiMessage ?: "",
                    color = PrimaryYellow,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            } else {
                Text(
                    text = if (isMyTurn) "⭐ DU BIST AM ZUG!" else "⏳ ${opponentPlayer.name} IST AM ZUG...",
                    color = if (isMyTurn) PrimaryYellow else Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Center Played Cards Stack
            Box(
                modifier = Modifier
                    .height(105.dp)
                    .fillMaxWidth(0.6f),
                contentAlignment = Alignment.Center
            ) {
                if (onlineState.centerPile.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .width(62.dp)
                            .height(90.dp)
                            .border(1.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Tisch leer", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                    }
                } else {
                    val topCard = onlineState.centerPile.last()
                    Box(contentAlignment = Alignment.TopEnd) {
                        Box(contentAlignment = Alignment.Center) {
                            if (onlineState.centerPile.size > 1) {
                                PlaidCardBack(
                                    modifier = Modifier
                                        .width(62.dp)
                                        .height(90.dp)
                                        .offset(x = (-3).dp, y = 3.dp)
                                        .shadow(2.dp, RoundedCornerShape(6.dp))
                                )
                            }
                            OnlineCardView(
                                card = topCard,
                                modifier = Modifier
                                    .width(62.dp)
                                    .height(90.dp)
                                    .shadow(4.dp, RoundedCornerShape(6.dp))
                            )
                        }

                        if (onlineState.centerPile.size > 1) {
                            Box(
                                modifier = Modifier
                                    .offset(x = 8.dp, y = (-8).dp)
                                    .background(PrimaryYellow, CircleShape)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${onlineState.centerPile.size}",
                                    color = OnPrimaryYellow,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- HUMAN PLAYER (BOTTOM CENTER) ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Your Name & Score Header
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .border(
                        1.5.dp,
                        if (isMyTurn) PrimaryYellow else Color.Transparent,
                        RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "👤 ${myPlayer.name} (DU) | Pkt: ${myPlayer.totalScore} | Karten: ${myPlayer.capturedCount}",
                    color = if (isMyTurn) PrimaryYellow else Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Your Interactive Hand Cards
            val myHandToDisplay = if (landedCounts != null) myPlayer.hand.take(landedCounts!![0]) else myPlayer.hand
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                myHandToDisplay.forEachIndexed { index, card ->
                    OnlineCardView(
                        card = card,
                        modifier = Modifier
                            .width(64.dp)
                            .height(92.dp)
                            .shadow(if (isMyTurn && landedCounts == null) 8.dp else 2.dp, RoundedCornerShape(6.dp))
                            .border(
                                if (isMyTurn && landedCounts == null) 1.5.dp else 0.dp,
                                if (isMyTurn && landedCounts == null) PrimaryYellow else Color.Transparent,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable(enabled = isMyTurn && landedCounts == null) {
                                onlineManager.playCard(index)
                            }
                    )
                }
            }
        }

        // Card Dealing Banner Overlay
        if (landedCounts != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.88f), RoundedCornerShape(16.dp))
                    .border(1.5.dp, PrimaryYellow, RoundedCornerShape(16.dp))
                    .padding(horizontal = 24.dp, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = PrimaryYellow,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "🎴 KARTEN WERDEN GEMISCHT & VERTEILT...",
                        color = PrimaryYellow,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // ROUND END SUMMARY DIALOG
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Punktestand (Ziel: ${onlineState.targetScore} Punkte):",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    onlineState.players.forEach { p ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = p.name + if (p.id == myPlayerId) " (Du)" else "",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "${p.totalScore} Pkt (+${p.roundScore} R)",
                                color = PrimaryYellow,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                    if (!isHost) {
                        Text(
                            "Warte auf Host für nächste Runde...",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp)
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

    // MATCH OVER DIALOG
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
                Column {
                    Text(
                        "GEWINNER: ${onlineState.matchWinnerName ?: "Unentschieden"}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "${myPlayer.name}: ${myPlayer.totalScore} Punkte\n${opponentPlayer.name}: ${opponentPlayer.totalScore} Punkte",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
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

// --- ONLINE CARD RENDERER COMPOSABLE ---

@Composable
fun OnlineCardView(card: OnlineCardDto, modifier: Modifier = Modifier) {
    val suitSymbol = when (card.suit) {
        "HEARTS" -> "♥"
        "DIAMONDS" -> "♦"
        "CLUBS" -> "♣"
        else -> "♠"
    }

    val rankSymbol = when (card.rank) {
        "TWO" -> "2"
        "THREE" -> "3"
        "FOUR" -> "4"
        "FIVE" -> "5"
        "SIX" -> "6"
        "SEVEN" -> "7"
        "EIGHT" -> "8"
        "NINE" -> "9"
        "TEN" -> "10"
        "JACK" -> "J"
        "QUEEN" -> "Q"
        "KING" -> "K"
        "ACE" -> "A"
        else -> card.rank
    }

    val color = if (card.suit == "HEARTS" || card.suit == "DIAMONDS") Color(0xFFD32F2F) else Color(0xFF212121)

    Box(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(6.dp))
            .border(1.dp, Color.Black.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
            .padding(4.dp)
    ) {
        // Top-Left Corner Rank & Suit
        Column(
            modifier = Modifier.align(Alignment.TopStart),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = rankSymbol,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 11.sp
            )
            Text(
                text = suitSymbol,
                color = color,
                fontSize = 9.sp,
                lineHeight = 9.sp
            )
        }

        // Center Suit Icon
        Text(
            text = suitSymbol,
            color = color,
            fontSize = 22.sp,
            modifier = Modifier.align(Alignment.Center)
        )

        // Bottom-Right Corner (Inverted)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .rotate(180f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = rankSymbol,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 11.sp
            )
            Text(
                text = suitSymbol,
                color = color,
                fontSize = 9.sp,
                lineHeight = 9.sp
            )
        }
    }
}
