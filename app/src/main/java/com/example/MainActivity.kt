package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Scaffold(modifier = Modifier.fillMaxSize(), containerColor = MenuTeal) { innerPadding ->
          AppNavigation(modifier = Modifier.padding(innerPadding))
        }
      }
    }
  }
}

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
  val navController = rememberNavController()
  val gameViewModel: GameViewModel = viewModel()
  val state by gameViewModel.state.collectAsState()
  
  NavHost(navController = navController, startDestination = "menu", modifier = modifier.background(MenuTeal).fillMaxSize()) {
    composable("menu") { MenuScreen(navController, gameViewModel) }
    composable("game") { GameScreen(navController, gameViewModel) }
  }

  if (state.showSettingsDialog) {
    SettingsDialog(viewModel = gameViewModel, onDismiss = { gameViewModel.toggleSettingsDialog(false) })
  }

  if (state.showStatsDialog) {
    StatsDialog(viewModel = gameViewModel, onDismiss = { gameViewModel.toggleStatsDialog(false) })
  }

  if (state.showRulesDialog) {
    RulesDialog(onDismiss = { gameViewModel.toggleRulesDialog(false) })
  }

  if (state.showPrivacyDialog) {
    PrivacyPolicyDialog(onDismiss = { gameViewModel.togglePrivacyDialog(false) })
  }

  if (state.showExitDialog) {
    ExitDialog(onDismiss = { gameViewModel.toggleExitDialog(false) })
  }

  if (state.showInGameMenu) {
    InGameMenuDialog(navController = navController, viewModel = gameViewModel, onDismiss = { gameViewModel.toggleInGameMenu(false) })
  }

  if (state.showRoundEndSummary) {
    RoundEndSummaryDialog(viewModel = gameViewModel)
  }
}

@Composable
fun MenuScreen(navController: NavController, gameViewModel: GameViewModel) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MenuTeal)
      .padding(horizontal = 24.dp, vertical = 28.dp)
  ) {
    // Centered vertical list matching the screenshot
    Column(
      modifier = Modifier
        .align(Alignment.Center)
        .fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      // 1. Spielen (Starts game directly on hard bot mode)
      Text(
        text = "Spielen",
        color = Color.White,
        fontSize = 44.sp,
        fontFamily = FontFamily.Cursive,
        textAlign = TextAlign.Center,
        modifier = Modifier
          .clickable {
            gameViewModel.startNewGame()
            navController.navigate("game")
          }
          .padding(vertical = 10.dp)
      )

      Spacer(modifier = Modifier.height(14.dp))

      // 2. Einstellungen
      Text(
        text = "Einstellungen",
        color = Color.White,
        fontSize = 44.sp,
        fontFamily = FontFamily.Cursive,
        textAlign = TextAlign.Center,
        modifier = Modifier
          .clickable { gameViewModel.toggleSettingsDialog(true) }
          .padding(vertical = 10.dp)
      )

      Spacer(modifier = Modifier.height(14.dp))

      // 3. Statistik
      Text(
        text = "Statistik",
        color = Color.White,
        fontSize = 44.sp,
        fontFamily = FontFamily.Cursive,
        textAlign = TextAlign.Center,
        modifier = Modifier
          .clickable { gameViewModel.toggleStatsDialog(true) }
          .padding(vertical = 10.dp)
      )

      Spacer(modifier = Modifier.height(14.dp))

      // 4. Hilfe
      Text(
        text = "Hilfe",
        color = Color.White,
        fontSize = 44.sp,
        fontFamily = FontFamily.Cursive,
        textAlign = TextAlign.Center,
        modifier = Modifier
          .clickable { gameViewModel.toggleRulesDialog(true) }
          .padding(vertical = 10.dp)
      )

      Spacer(modifier = Modifier.height(14.dp))

      // 5. Austritt
      Text(
        text = "Austritt",
        color = Color.White,
        fontSize = 44.sp,
        fontFamily = FontFamily.Cursive,
        textAlign = TextAlign.Center,
        modifier = Modifier
          .clickable { gameViewModel.toggleExitDialog(true) }
          .padding(vertical = 10.dp)
      )
    }

    // Bottom Bar matching screenshot
    Row(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Version 6.10",
        color = Color.White,
        fontSize = 18.sp,
        fontFamily = FontFamily.Cursive
      )

      Text(
        text = "Privacy Policy",
        color = Color.White,
        fontSize = 18.sp,
        fontFamily = FontFamily.Cursive,
        modifier = Modifier.clickable { gameViewModel.togglePrivacyDialog(true) }
      )
    }
  }
}

// --- GAME SCREEN (EXACT LOBBY DESIGN MATCHING BILD 2) ---

@Composable
fun GameScreen(navController: NavController, viewModel: GameViewModel) {
  val state by viewModel.state.collectAsState()
  val players = state.players
  
  val humanPlayer = players.getOrNull(0) ?: PlayerInfo(0, "Spieler", true)
  val platoPlayer = players.getOrNull(1) ?: PlayerInfo(1, "Plato", false)
  val euklidPlayer = players.getOrNull(2) ?: PlayerInfo(2, "Euklid", false)
  val sokratesPlayer = players.getOrNull(3) ?: PlayerInfo(3, "Sokrates", false)

  val minutes = state.elapsedSeconds / 60
  val seconds = state.elapsedSeconds % 60
  val timeFormatted = String.format("%02d:%02d", minutes, seconds)

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MenuTeal)
  ) {
    // Top Left Match Info Pill (Timer, Target Score, Round)
    Row(
      modifier = Modifier
        .align(Alignment.TopStart)
        .padding(top = 16.dp, start = 16.dp)
        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
        .border(1.dp, PrimaryYellow, RoundedCornerShape(16.dp))
        .padding(horizontal = 10.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Text(
        text = "⏱️ $timeFormatted",
        color = Color.White,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = "🎯 Ziel: ${state.targetScore} Pkt",
        color = PrimaryYellow,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
      )
    }

    // Top Right Circular Menu Icon (Matching Bild 2)
    IconButton(
      onClick = { viewModel.toggleInGameMenu(true) },
      modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(top = 16.dp, end = 16.dp)
        .size(44.dp)
        .border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape)
        .background(MenuTeal, CircleShape)
    ) {
      Icon(
        Icons.Default.List,
        contentDescription = "Menu",
        tint = Color.White,
        modifier = Modifier.size(24.dp)
      )
    }

    // --- TOP OPPONENT: EUKLID ---
    Column(
      modifier = Modifier
        .align(Alignment.TopCenter)
        .padding(top = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      TopBotHand(cardCount = euklidPlayer.hand.size)
      Spacer(modifier = Modifier.height(4.dp))
      PlayerBadgeWithAnim(
        name = "Euklid",
        points = euklidPlayer.totalScore + euklidPlayer.roundScore,
        scoreEvent = state.lastScoreEvent,
        playerId = 2
      )
    }

    // --- LEFT OPPONENT: PLATO ---
    Column(
      modifier = Modifier
        .align(Alignment.CenterStart)
        .padding(start = 0.dp),
      horizontalAlignment = Alignment.Start
    ) {
      PlayerBadgeWithAnim(
        name = "Plato",
        points = platoPlayer.totalScore + platoPlayer.roundScore,
        scoreEvent = state.lastScoreEvent,
        playerId = 1
      )
      Spacer(modifier = Modifier.height(8.dp))
      LeftBotHand(cardCount = platoPlayer.hand.size)
    }

    // --- RIGHT OPPONENT: SOKRATES ---
    Column(
      modifier = Modifier
        .align(Alignment.CenterEnd)
        .padding(end = 0.dp),
      horizontalAlignment = Alignment.End
    ) {
      PlayerBadgeWithAnim(
        name = "Sokrates",
        points = sokratesPlayer.totalScore + sokratesPlayer.roundScore,
        scoreEvent = state.lastScoreEvent,
        playerId = 3
      )
      Spacer(modifier = Modifier.height(8.dp))
      RightBotHand(cardCount = sokratesPlayer.hand.size)
    }

    // --- DRAW DECK STOCK (LEFT TABLE SIDE) ---
    if (state.deck.isNotEmpty()) {
      Box(
        modifier = Modifier
          .align(Alignment.CenterStart)
          .padding(start = 16.dp, top = 80.dp),
        contentAlignment = Alignment.TopEnd
      ) {
        PlaidCardBack(
          modifier = Modifier
            .width(46.dp)
            .height(68.dp)
            .shadow(3.dp, RoundedCornerShape(4.dp))
        )
        Box(
          modifier = Modifier
            .offset(x = 6.dp, y = (-6).dp)
            .background(Color.White, RoundedCornerShape(4.dp))
            .border(1.dp, Color.Black, RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
          Text(
            text = "${state.deck.size}",
            color = Color.Black,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    // --- CENTER TABLE AREA (SINGLE ACTIVE PLAYED CARDS STACK) ---
    Column(
      modifier = Modifier
        .align(Alignment.Center)
        .padding(bottom = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Status Message / Pişti Banner
      if (state.lastPistiMessage != null) {
        Text(
          text = state.lastPistiMessage ?: "",
          color = PrimaryYellow,
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp,
          modifier = Modifier.padding(bottom = 12.dp)
        )
      } else {
        Text(
          text = state.statusMessage,
          color = Color.White.copy(alpha = 0.9f),
          fontSize = 13.sp,
          fontWeight = FontWeight.Medium,
          modifier = Modifier.padding(bottom = 12.dp)
        )
      }

      // Center Played Cards Single Stack Area
      Box(
        modifier = Modifier
          .height(110.dp)
          .fillMaxWidth(0.6f),
        contentAlignment = Alignment.Center
      ) {
        if (state.centerPile.isEmpty()) {
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
          val topCard = state.centerPile.last()
          Box(contentAlignment = Alignment.TopEnd) {
            Box(contentAlignment = Alignment.Center) {
              // Stack effect if more than 1 card in middle
              if (state.centerPile.size > 1) {
                PlaidCardBack(
                  modifier = Modifier
                    .width(62.dp)
                    .height(90.dp)
                    .offset(x = (-3).dp, y = 3.dp)
                    .shadow(2.dp, RoundedCornerShape(6.dp))
                )
              }
              // Top card face up
              PlayingCardView(
                card = topCard,
                modifier = Modifier
                  .width(62.dp)
                  .height(90.dp)
                  .shadow(4.dp, RoundedCornerShape(6.dp))
              )
            }

            // Card count badge
            if (state.centerPile.size > 1) {
              Box(
                modifier = Modifier
                  .offset(x = 8.dp, y = (-8).dp)
                  .background(PrimaryYellow, RoundedCornerShape(10.dp))
                  .border(1.dp, Color.Black, RoundedCornerShape(10.dp))
                  .padding(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Text(
                  text = "${state.centerPile.size}",
                  color = Color.Black,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.ExtraBold
                )
              }
            }
          }
        }
      }
    }

    // --- BOTTOM PLAYER: SPIELER ---
    Box(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .padding(bottom = 12.dp)
    ) {
      // Bottom Left Player Badge
      Box(
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(start = 12.dp, bottom = 120.dp)
      ) {
        PlayerBadgeWithAnim(
          name = state.playerName,
          points = humanPlayer.totalScore + humanPlayer.roundScore,
          scoreEvent = state.lastScoreEvent,
          playerId = 0
        )
      }

      // Center Fanned Player Hand
      FannedPlayerHand(
        hand = humanPlayer.hand,
        isMyTurn = state.currentTurnIndex == 0,
        centerPile = state.centerPile,
        onCardClick = { card ->
          if (state.currentTurnIndex == 0) {
            viewModel.playCard(card, 0)
          }
        },
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(bottom = 8.dp)
      )

      // Bottom Right Action Circle (Matching Bild 2)
      Box(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(end = 16.dp, bottom = 16.dp)
          .size(54.dp)
          .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
          .background(Color.Transparent, CircleShape)
      )
    }
  }
}

// --- PLAYER BADGE COMPOSABLE (BLACK TAG WITH POINTS MATCHING BILD 2) ---

@Composable
fun PlayerBadgeWithAnim(
  name: String,
  points: Int,
  iconText: String? = null,
  scoreEvent: ScoreGainEvent? = null,
  playerId: Int
) {
  Box(contentAlignment = Alignment.Center) {
    PlayerBadge(name = name, points = points, iconText = iconText)
    
    if (scoreEvent != null && scoreEvent.playerId == playerId) {
      FloatingScorePill(
        text = scoreEvent.text,
        timestamp = scoreEvent.timestamp,
        modifier = Modifier.offset(y = (-32).dp)
      )
    }
  }
}

@Composable
fun FloatingScorePill(text: String, timestamp: Long, modifier: Modifier = Modifier) {
  var visible by remember(timestamp) { mutableStateOf(true) }
  
  val scale by animateFloatAsState(
    targetValue = if (visible) 1f else 0.3f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
    label = "scale"
  )
  val alpha by animateFloatAsState(
    targetValue = if (visible) 1f else 0f,
    animationSpec = tween(durationMillis = 350),
    label = "alpha"
  )

  LaunchedEffect(timestamp) {
    visible = true
    kotlinx.coroutines.delay(1800)
    visible = false
  }

  if (alpha > 0.01f) {
    Box(
      modifier = modifier
        .graphicsLayer(
          scaleX = scale,
          scaleY = scale,
          alpha = alpha
        )
        .background(PrimaryYellow, RoundedCornerShape(12.dp))
        .border(1.5.dp, Color.Black, RoundedCornerShape(12.dp))
        .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
      Text(
        text = text,
        color = Color.Black,
        fontSize = 12.sp,
        fontWeight = FontWeight.ExtraBold
      )
    }
  }
}

@Composable
fun PlayerBadge(name: String, points: Int, iconText: String? = null) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Box(
      modifier = Modifier
        .background(Color.Black, RoundedCornerShape(2.dp))
        .border(1.dp, Color.Black, RoundedCornerShape(2.dp))
        .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = name,
          color = Color.White,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "$points Pkt",
          color = PrimaryYellow,
          fontSize = 12.sp,
          fontWeight = FontWeight.ExtraBold
        )
      }
    }
    if (iconText != null) {
      Box(
        modifier = Modifier
          .background(Color.Black, RoundedCornerShape(2.dp))
          .padding(horizontal = 8.dp, vertical = 2.dp)
      ) {
        Text(text = iconText, color = Color.White, fontSize = 11.sp)
      }
    }
  }
}

// --- PLAID CARD BACK COMPOSABLE (IDENTICAL TO BILD 2) ---

@Composable
fun PlaidCardBack(modifier: Modifier = Modifier) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(6.dp))
      .background(Color(0xFF788B63))
      .border(1.dp, Color(0xFF3B6244), RoundedCornerShape(6.dp))
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val strokeW = 1.8.dp.toPx()
      val step = 10.dp.toPx()
      
      var offset = -size.height
      while (offset < size.width + size.height) {
        // Line +45 deg
        drawLine(
          color = Color(0xFFC3CBA1),
          start = Offset(offset, 0f),
          end = Offset(offset + size.height, size.height),
          strokeWidth = strokeW
        )
        // Line -45 deg
        drawLine(
          color = Color(0xFF3B6244),
          start = Offset(offset, size.height),
          end = Offset(offset + size.height, 0f),
          strokeWidth = strokeW
        )
        offset += step
      }
    }
  }
}

// --- PLAYING CARD VIEW (REAL FACE-UP CARDS) ---

enum class CardHighlightType { NONE, STICH, PISTI }

@Composable
fun PlayingCardView(
  card: Card,
  modifier: Modifier = Modifier,
  cardWidth: Dp = 68.dp,
  cardHeight: Dp = 102.dp,
  highlightType: CardHighlightType = CardHighlightType.NONE,
  onClick: (() -> Unit)? = null
) {
  val suitColor = if (card.suit == Suit.HEARTS || card.suit == Suit.DIAMONDS) Color(0xFFD32F2F) else Color(0xFF212121)

  val borderColor = when (highlightType) {
    CardHighlightType.PISTI -> PrimaryYellow
    CardHighlightType.STICH -> Color(0xFF4CAF50)
    CardHighlightType.NONE -> Color(0xFFDCDCDC)
  }
  val borderWidth = if (highlightType != CardHighlightType.NONE) 2.5.dp else 1.dp

  val isLarge = cardWidth.value >= 78f
  val rankFontSize = if (isLarge) 17.sp else 13.sp
  val suitFontSize = if (isLarge) 14.sp else 11.sp
  val centerEmojiSize = if (isLarge) 32.sp else 24.sp
  val centerSuitSize = if (isLarge) 30.sp else 24.sp

  Box(contentAlignment = Alignment.TopCenter) {
    Box(
      modifier = modifier
        .width(cardWidth)
        .height(cardHeight)
        .shadow(if (highlightType != CardHighlightType.NONE) 8.dp else 3.dp, RoundedCornerShape(8.dp))
        .clip(RoundedCornerShape(8.dp))
        .background(Color.White)
        .border(borderWidth, borderColor, RoundedCornerShape(8.dp))
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
        .padding(if (isLarge) 5.dp else 4.dp)
    ) {
      // Top-left rank & suit
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.align(Alignment.TopStart)
      ) {
        Text(
          text = card.rank.symbol,
          color = suitColor,
          fontWeight = FontWeight.ExtraBold,
          fontSize = rankFontSize
        )
        Text(
          text = card.suit.symbol,
          color = suitColor,
          fontSize = suitFontSize
        )
      }

      // Center content
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        if (card.rank == Rank.JACK) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🧔", fontSize = centerEmojiSize)
            Text("🪓", fontSize = if (isLarge) 12.sp else 10.sp)
          }
        } else if (card.rank == Rank.QUEEN) {
          Text("👑", fontSize = centerEmojiSize)
        } else if (card.rank == Rank.KING) {
          Text("🤴", fontSize = centerEmojiSize)
        } else {
          Text(
            text = card.suit.symbol,
            color = suitColor,
            fontSize = centerSuitSize
          )
        }
      }

      // Bottom-right inverted rank & suit
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .rotate(180f)
      ) {
        Text(
          text = card.rank.symbol,
          color = suitColor,
          fontWeight = FontWeight.ExtraBold,
          fontSize = rankFontSize
        )
        Text(
          text = card.suit.symbol,
          color = suitColor,
          fontSize = suitFontSize
        )
      }
    }

    if (highlightType == CardHighlightType.PISTI) {
      Box(
        modifier = Modifier
          .offset(y = (-12).dp)
          .background(PrimaryYellow, RoundedCornerShape(8.dp))
          .border(1.dp, Color.Black, RoundedCornerShape(8.dp))
          .padding(horizontal = 6.dp, vertical = 2.dp)
      ) {
        Text(
          text = "🔥 PIŞTI!",
          color = Color.Black,
          fontSize = if (isLarge) 11.sp else 10.sp,
          fontWeight = FontWeight.ExtraBold
        )
      }
    } else if (highlightType == CardHighlightType.STICH) {
      Box(
        modifier = Modifier
          .offset(y = (-12).dp)
          .background(Color(0xFF4CAF50), RoundedCornerShape(8.dp))
          .border(1.dp, Color.White, RoundedCornerShape(8.dp))
          .padding(horizontal = 6.dp, vertical = 2.dp)
      ) {
        Text(
          text = "✨ STICH",
          color = Color.White,
          fontSize = if (isLarge) 11.sp else 10.sp,
          fontWeight = FontWeight.ExtraBold
        )
      }
    }
  }
}

// --- BOT HAND COMPOSABLES (TOP, LEFT, RIGHT) ---

@Composable
fun TopBotHand(cardCount: Int) {
  Box(contentAlignment = Alignment.TopCenter, modifier = Modifier.height(70.dp)) {
    val count = cardCount.coerceAtLeast(0)
    repeat(count) { idx ->
      val angle = if (count > 1) (-12f + idx * (24f / (count - 1))) else 0f
      val xOffset = if (count > 1) (((idx - (count - 1) / 2f) * 24).dp) else 0.dp

      PlaidCardBack(
        modifier = Modifier
          .width(48.dp)
          .height(72.dp)
          .offset(x = xOffset, y = (-20).dp)
          .rotate(angle)
      )
    }
  }
}

@Composable
fun LeftBotHand(cardCount: Int) {
  Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.width(60.dp).height(120.dp)) {
    val count = cardCount.coerceAtLeast(0)
    repeat(count) { idx ->
      val yOffset = (((idx - (count - 1) / 2f) * 20).dp)
      PlaidCardBack(
        modifier = Modifier
          .width(66.dp)
          .height(44.dp)
          .offset(x = (-22).dp, y = yOffset)
          .rotate(90f)
      )
    }
  }
}

@Composable
fun RightBotHand(cardCount: Int) {
  Box(contentAlignment = Alignment.CenterEnd, modifier = Modifier.width(60.dp).height(120.dp)) {
    val count = cardCount.coerceAtLeast(0)
    repeat(count) { idx ->
      val yOffset = (((idx - (count - 1) / 2f) * 20).dp)
      PlaidCardBack(
        modifier = Modifier
          .width(66.dp)
          .height(44.dp)
          .offset(x = 22.dp, y = yOffset)
          .rotate(90f)
      )
    }
  }
}

// --- FANNED PLAYER HAND AT BOTTOM ---

@Composable
fun FannedPlayerHand(
  hand: List<Card>,
  isMyTurn: Boolean,
  centerPile: List<Card>,
  onCardClick: (Card) -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier.fillMaxWidth(),
    contentAlignment = Alignment.BottomCenter
  ) {
    val total = hand.size
    val topCard = centerPile.lastOrNull()
    val centerCount = centerPile.size

    hand.forEachIndexed { index, card ->
      val angle = if (total > 1) {
        val startAngle = -10f
        val step = 20f / (total - 1)
        startAngle + index * step
      } else 0f

      val yOffset = if (total > 1) {
        val centerIndex = (total - 1) / 2f
        val dist = Math.abs(index - centerIndex)
        (dist * dist * 3).dp
      } else 0.dp

      val xOffset = if (total > 1) {
        (((index - (total - 1) / 2f) * 50).dp)
      } else 0.dp

      val highlightType = if (isMyTurn && topCard != null) {
        val matches = card.rank == topCard.rank || card.rank == Rank.JACK
        if (matches) {
          if (centerCount == 1) CardHighlightType.PISTI else CardHighlightType.STICH
        } else CardHighlightType.NONE
      } else CardHighlightType.NONE

      PlayingCardView(
        card = card,
        cardWidth = 84.dp,
        cardHeight = 124.dp,
        highlightType = highlightType,
        modifier = Modifier
          .offset(x = xOffset, y = -yOffset)
          .rotate(angle),
        onClick = {
          if (isMyTurn) {
            onCardClick(card)
          }
        }
      )
    }
  }
}

// --- DIALOGS (SETTINGS, STATS, RULES, EXIT, IN-GAME MENU) ---

@Composable
fun InGameMenuDialog(navController: NavController, viewModel: GameViewModel, onDismiss: () -> Unit) {
  AlertDialog(
    onDismissRequest = { onDismiss() },
    title = { Text("Pause", color = PrimaryYellow, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
          onClick = { onDismiss() },
          colors = ButtonDefaults.buttonColors(containerColor = PrimaryYellow, contentColor = OnPrimaryYellow),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Weiter spielen", fontWeight = FontWeight.Bold)
        }

        Button(
          onClick = {
            onDismiss()
            viewModel.startNewGame()
          },
          colors = ButtonDefaults.buttonColors(containerColor = LightGreen, contentColor = Color.White),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Neu starten")
        }

        Button(
          onClick = {
            onDismiss()
            navController.navigate("menu")
          },
          colors = ButtonDefaults.buttonColors(containerColor = ButtonRed, contentColor = Color.White),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Hauptmenü")
        }
      }
    },
    confirmButton = {},
    containerColor = BackgroundGreen
  )
}

@Composable
fun SettingsDialog(viewModel: GameViewModel, onDismiss: () -> Unit) {
  val state by viewModel.state.collectAsState()
  var tempName by remember { mutableStateOf(state.playerName) }

  AlertDialog(
    onDismissRequest = { onDismiss() },
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Settings, contentDescription = null, tint = PrimaryYellow)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Einstellungen", color = PrimaryYellow, fontWeight = FontWeight.Bold, fontSize = 20.sp)
      }
    },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Column {
          Text("Spielername:", color = PrimaryYellow, fontWeight = FontWeight.Bold, fontSize = 13.sp)
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = tempName,
            onValueChange = {
              tempName = it
              viewModel.updatePlayerName(it)
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = PrimaryYellow,
              unfocusedBorderColor = OutlineYellow,
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
          )
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(LightGreen, RoundedCornerShape(12.dp))
            .padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              if (state.soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
              contentDescription = null,
              tint = PrimaryYellow
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text("Sound & Effekte", color = Color.White, fontWeight = FontWeight.Bold)
          }
          Switch(
            checked = state.soundEnabled,
            onCheckedChange = { viewModel.toggleSound() },
            colors = SwitchDefaults.colors(
              checkedThumbColor = PrimaryYellow,
              checkedTrackColor = LightGreen,
              uncheckedThumbColor = Color.Gray,
              uncheckedTrackColor = SurfaceDarkGreen
            )
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onDismiss() },
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryYellow, contentColor = OnPrimaryYellow)
      ) {
        Text("Speichern")
      }
    },
    containerColor = BackgroundGreen
  )
}

@Composable
fun StatsDialog(viewModel: GameViewModel, onDismiss: () -> Unit) {
  val state by viewModel.state.collectAsState()

  AlertDialog(
    onDismissRequest = { onDismiss() },
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.BarChart, contentDescription = null, tint = PrimaryYellow)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Statistik", color = PrimaryYellow, fontWeight = FontWeight.Bold, fontSize = 20.sp)
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        StatRow("Gespielte Spiele", "${state.gamesPlayed}")
        StatRow("Gewonnene Duelle", "${state.gamesWon} (${if (state.gamesPlayed > 0) state.gamesWon * 100 / state.gamesPlayed else 0}%)")
        StatRow("Piştis erzielt", "🔥 ${state.pistiTotalCount}")
        StatRow("Aktueller Rang", "🥇 Sultan Meister")
      }
    },
    confirmButton = {
      Button(
        onClick = { onDismiss() },
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryYellow, contentColor = OnPrimaryYellow)
      ) {
        Text("Fertig")
      }
    },
    containerColor = BackgroundGreen
  )
}

@Composable
fun StatRow(label: String, value: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(LightGreen, RoundedCornerShape(8.dp))
      .padding(horizontal = 12.dp, vertical = 10.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(label, color = Color.White, fontSize = 13.sp)
    Text(value, color = PrimaryYellow, fontWeight = FontWeight.Bold, fontSize = 14.sp)
  }
}

@Composable
fun RulesDialog(onDismiss: () -> Unit) {
  AlertDialog(
    onDismissRequest = { onDismiss() },
    title = { Text("Pişti Regeln & Hilfe", color = PrimaryYellow, fontWeight = FontWeight.Bold) },
    text = {
      Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text(
          "• Ziel des Spiels:\nSammle möglichst viele Karten und Punkte.\n\n" +
          "• Karten stechen:\nSpielst du eine Karte mit demselben Wert wie die oberste Karte auf dem Tisch oder einen Buben (J), gehört der Stapel dir.\n\n" +
          "• Pişti (10 Pkt):\nStichst du eine einzelne Karte auf dem Tisch mit demselben Wert, ist das ein Pişti (+10 Punkte).\n\n" +
          "• Bube Pişti (20 Pkt):\nStichst du einen einzelnen Buben auf dem Tisch mit einem Buben, erhältst du 20 Punkte.\n\n" +
          "• Spezial-Punkte:\n- Karo 10 (♦10): +3 Pkt\n- Kreuz 2 (♣2): +2 Pkt\n- Bube (J) / Ass (A): +1 Pkt\n- Mehrheit der Karten: +3 Pkt",
          color = Color.White,
          fontSize = 12.sp
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onDismiss() },
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryYellow, contentColor = OnPrimaryYellow)
      ) {
        Text("Verstanden")
      }
    },
    containerColor = BackgroundGreen
  )
}

@Composable
fun ExitDialog(onDismiss: () -> Unit) {
  AlertDialog(
    onDismissRequest = { onDismiss() },
    title = { Text("Spiel beenden", color = PrimaryYellow, fontWeight = FontWeight.Bold) },
    text = { Text("Möchtest du Pişti wirklich verlassen?", color = Color.White) },
    confirmButton = {
      Button(
        onClick = { onDismiss() },
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryYellow, contentColor = OnPrimaryYellow)
      ) {
        Text("Beenden")
      }
    },
    dismissButton = {
      TextButton(onClick = { onDismiss() }) {
        Text("Zurück", color = Color.White)
      }
    },
    containerColor = BackgroundGreen
  )
}

@Composable
fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
  AlertDialog(
    onDismissRequest = { onDismiss() },
    title = { Text("Privacy Policy", color = PrimaryYellow, fontWeight = FontWeight.Bold) },
    text = {
      Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text(
          "Diese Anwendung respektiert Ihre Privatsphäre. Alle Spielstände und Statistiken werden ausschließlich lokal auf Ihrem Gerät gespeichert.",
          color = Color.White,
          fontSize = 12.sp
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onDismiss() },
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryYellow, contentColor = OnPrimaryYellow)
      ) {
        Text("Schließen")
      }
    },
    containerColor = BackgroundGreen
  )
}

@Composable
fun RoundEndSummaryDialog(viewModel: GameViewModel) {
  val state by viewModel.state.collectAsState()

  AlertDialog(
    onDismissRequest = {},
    title = {
      Text(
        text = if (state.isMatchOver) "🏆 MATCH GEWONNEN!" else "Runde ${state.roundNumber} Beendet",
        color = PrimaryYellow,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (state.isMatchOver && state.matchWinnerName != null) {
          Text(
            text = "🎉 Sieger: ${state.matchWinnerName} (${state.targetScore} Punkte erreicht!)",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
          )
          Spacer(modifier = Modifier.height(4.dp))
        }

        Text(
          text = "Punkteübersicht (Ziel: ${state.targetScore} Pkt):",
          color = Color.White.copy(alpha = 0.8f),
          fontSize = 12.sp
        )

        state.players.forEach { p ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(LightGreen, RoundedCornerShape(8.dp))
              .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(p.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Column(horizontalAlignment = Alignment.End) {
              Text("${p.totalScore} Pkt gesamt", color = PrimaryYellow, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
              Text("+${p.roundScore} in dieser Runde", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (state.isMatchOver) {
            viewModel.startNewMatch()
          } else {
            viewModel.startNewRound()
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryYellow, contentColor = OnPrimaryYellow)
      ) {
        Text(if (state.isMatchOver) "Neues Match starten" else "Nächste Runde starten")
      }
    },
    containerColor = BackgroundGreen
  )
}

