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

    var selectedTab by remember { mutableStateOf(0) } // 0 = Room Create, 1 = Room Join
    var inputRoomCode by remember { mutableStateOf("") }
    var joinError by remember { mutableStateOf<String?>(null) }

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
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .shadow(12.dp, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = BackgroundGreen),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(2.dp, PrimaryYellow)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
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
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ONLINE MIT FREUNDEN",
                        color = PrimaryYellow,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tab Switcher
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF072B1E),
                    contentColor = PrimaryYellow,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, OutlineYellow, RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                "Raum Erstellen",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 0) PrimaryYellow else Color.White
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                "Raum Beitreten",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 1) PrimaryYellow else Color.White
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (selectedTab == 0) {
                    // TAB 0: CREATE ROOM
                    if (onlineState.roomCode.isEmpty()) {
                        Button(
                            onClick = {
                                onlineManager.createRoom(state.playerName)
                            },
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
                            Text("Neuen Raum Erstellen", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // Display Room Code
                        Text(
                            text = "DEIN RAUM-CODE",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Box(
                            modifier = Modifier
                                .background(Color(0xFF072B1E), RoundedCornerShape(12.dp))
                                .border(2.dp, PrimaryYellow, RoundedCornerShape(12.dp))
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = onlineState.roomCode,
                                color = PrimaryYellow,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 6.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

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
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Code Kopieren", fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Waiting indicator
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = PrimaryYellow,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Warte auf Mitspieler...",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Gib deinem Freund den Code ${onlineState.roomCode}. Sobald er beitritt, startet das Spiel!",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // TAB 1: JOIN ROOM
                    Text(
                        text = "Gib den 4-stelligen Raum-Code deines Freundes ein:",
                        color = Color.White,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = inputRoomCode,
                        onValueChange = {
                            if (it.length <= 4) inputRoomCode = it.filter { char -> char.isDigit() }
                        },
                        label = { Text("Raum Code (z.B. 4829)", color = OutlineYellow) },
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
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

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
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = inputRoomCode.length == 4
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Raum Beitreten & Spielen", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

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
        colors = listOf(Color(0xFF0C4A31), Color(0xFF052B1C), Color(0xFF02170E)),
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
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(opponentPlayer.hand.size) {
                    PlaidCardBack(
                        modifier = Modifier
                            .width(52.dp)
                            .height(76.dp)
                            .shadow(3.dp, RoundedCornerShape(6.dp))
                    )
                }
            }
        }

        // --- CENTER TABLE (PILE & DECK) ---
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                myPlayer.hand.forEachIndexed { index, card ->
                    OnlineCardView(
                        card = card,
                        modifier = Modifier
                            .width(64.dp)
                            .height(92.dp)
                            .shadow(if (isMyTurn) 8.dp else 2.dp, RoundedCornerShape(6.dp))
                            .border(
                                if (isMyTurn) 1.5.dp else 0.dp,
                                if (isMyTurn) PrimaryYellow else Color.Transparent,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable(enabled = isMyTurn) {
                                onlineManager.playCard(index)
                            }
                    )
                }
            }
        }
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
