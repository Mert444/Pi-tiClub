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
import androidx.compose.ui.graphics.Brush
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
        Scaffold(
          modifier = Modifier.fillMaxSize(),
          containerColor = MenuTeal,
          contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
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
    composable("online_lobby") { OnlineLobbyScreen(navController, gameViewModel) }
    composable("online_game") { OnlineGameScreen(navController, gameViewModel) }
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

  if (state.showInitialNicknameDialog) {
    InitialNicknameDialog(
      viewModel = gameViewModel,
      onConfirm = { nickname ->
        val targetRoute = state.pendingRoute
        gameViewModel.confirmInitialNickname(nickname)
        if (targetRoute == "online_lobby") {
          navController.navigate("online_lobby")
        } else if (targetRoute == "game") {
          gameViewModel.startNewGame()
          navController.navigate("game")
        }
      },
      onDismiss = { gameViewModel.toggleInitialNicknameDialog(false) }
    )
  }
}

@Composable
fun MenuScreen(navController: NavController, gameViewModel: GameViewModel) {
  val state by gameViewModel.state.collectAsState()

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
      // 1. Spielen (Starts game directly or prompts for initial nickname once)
      Text(
        text = "Spielen",
        color = Color.White,
        fontSize = 44.sp,
        fontFamily = FontFamily.Cursive,
        textAlign = TextAlign.Center,
        modifier = Modifier
          .clickable {
            if (!state.hasSetNickname) {
              gameViewModel.toggleInitialNicknameDialog(true, "game")
            } else {
              gameViewModel.startNewGame()
              navController.navigate("game")
            }
          }
          .padding(vertical = 8.dp)
      )

      Spacer(modifier = Modifier.height(10.dp))

      // 1b. Online (2 Spieler mit Freunden)
      Text(
        text = "Online (2 Spieler)",
        color = PrimaryYellow,
        fontSize = 40.sp,
        fontFamily = FontFamily.Cursive,
        textAlign = TextAlign.Center,
        modifier = Modifier
          .clickable {
            if (!state.hasSetNickname) {
              gameViewModel.toggleInitialNicknameDialog(true, "online_lobby")
            } else {
              navController.navigate("online_lobby")
            }
          }
          .padding(vertical = 8.dp)
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
        text = "Beta 1.0.0",
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

  // Track cards landed during dealing animation so hands start completely empty and populate as cards land
  var landedCounts by remember(state.dealAnimTrigger) {
    mutableStateOf<List<Int>?>(if (state.dealAnimTrigger > 0L) listOf(0, 0, 0, 0) else null)
  }

  val humanHandToDisplay = if (landedCounts != null) humanPlayer.hand.take(landedCounts!![0]) else humanPlayer.hand
  val euklidCardCount = if (landedCounts != null) landedCounts!![2] else euklidPlayer.hand.size
  val platoCardCount = if (landedCounts != null) landedCounts!![1] else platoPlayer.hand.size
  val sokratesCardCount = if (landedCounts != null) landedCounts!![3] else sokratesPlayer.hand.size

  // 4K Luxury Table Radial Felt Gradient (Royal Blue Felt)
  val feltGradient = Brush.radialGradient(
    colors = listOf(Color(0xFF0F3B6A), Color(0xFF0A2548), Color(0xFF051428)),
    radius = 1200f
  )

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(feltGradient)
  ) {
    // 4K Ultra-crisp Gold Perimeter Border Inlay
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(6.dp)
        .border(1.5.dp, Color(0xFFD4AF37).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
    )

    // Top Left Match Info Pill (Timer)
    Box(
      modifier = Modifier
        .align(Alignment.TopStart)
        .padding(top = 16.dp, start = 16.dp)
        .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
        .border(1.dp, Color(0xFFD4AF37), RoundedCornerShape(16.dp))
        .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
      Text(
        text = "⏱️ $timeFormatted",
        color = Color.White,
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
        .border(1.5.dp, Color(0xFFD4AF37).copy(alpha = 0.8f), CircleShape)
        .background(Color(0xFF072B1E), CircleShape)
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
      TopBotHand(cardCount = euklidCardCount)
    }

    // --- LEFT OPPONENT: PLATO ---
    Column(
      modifier = Modifier
        .align(Alignment.CenterStart)
        .padding(start = 0.dp),
      horizontalAlignment = Alignment.Start
    ) {
      LeftBotHand(cardCount = platoCardCount)
    }

    // --- RIGHT OPPONENT: SOKRATES ---
    Column(
      modifier = Modifier
        .align(Alignment.CenterEnd)
        .padding(end = 0.dp),
      horizontalAlignment = Alignment.End
    ) {
      RightBotHand(cardCount = sokratesCardCount)
    }

    // --- DRAW DECK STOCK (TOP-LEFT OF CENTER, MATCHING VIDEO) ---
    if (state.deck.isNotEmpty()) {
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
            text = "${state.deck.size}",
            color = Color.Black,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold
          )
        }
      }
    }

    // --- CENTER TABLE AREA (LIVE SCOREBOARD & PLAYED CARDS STACK) ---
    Column(
      modifier = Modifier
        .align(Alignment.Center)
        .padding(bottom = 10.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Live UI Score Board for all 4 players
      CenterScoreboardPanel(
        players = state.players,
        currentTurnIndex = state.currentTurnIndex,
        starterPlayerIndex = state.starterPlayerIndex,
        roundNumber = state.roundNumber,
        targetScore = state.targetScore,
        scoreEvent = state.lastScoreEvent
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Status Message / Pişti Banner
      if (state.lastPistiMessage != null) {
        Text(
          text = state.lastPistiMessage ?: "",
          color = PrimaryYellow,
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp,
          modifier = Modifier.padding(bottom = 8.dp)
        )
      } else {
        Text(
          text = state.statusMessage,
          color = Color.White.copy(alpha = 0.9f),
          fontSize = 13.sp,
          fontWeight = FontWeight.Medium,
          modifier = Modifier.padding(bottom = 8.dp)
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
      // Center Fanned Player Hand
      FannedPlayerHand(
        hand = humanHandToDisplay,
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

    // --- ANIMATED CARD DEALING OVERLAY ---
    CardDealAnimOverlay(
      trigger = state.dealAnimTrigger,
      humanHand = humanPlayer.hand,
      onLandedCountsChanged = { newLanded -> landedCounts = newLanded },
      onAnimFinished = { landedCounts = null }
    )
  }
}

// --- CARD DEALING ANIMATION OVERLAY ---

@Composable
fun CardDealAnimOverlay(
  trigger: Long,
  humanHand: List<Card>,
  onLandedCountsChanged: (List<Int>) -> Unit,
  onAnimFinished: () -> Unit,
  modifier: Modifier = Modifier
) {
  if (trigger <= 0L) return

  var isAnimating by remember(trigger) { mutableStateOf(true) }
  val animProgress = remember(trigger) { androidx.compose.animation.core.Animatable(0f) }

  LaunchedEffect(trigger) {
    isAnimating = true
    onLandedCountsChanged(listOf(0, 0, 0, 0))
    animProgress.snapTo(0f)
    animProgress.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 1350, easing = LinearEasing)
    )
    isAnimating = false
    onAnimFinished()
  }

  if (isAnimating) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
      val w = maxWidth.value
      val h = maxHeight.value

      // Deck Stock Position (Top-Left of center table)
      val deckX = 24f
      val deckY = 135f

      val progress = animProgress.value
      val totalCards = 16

      // Calculate which cards have landed so far
      val currentLanded = mutableListOf(0, 0, 0, 0)
      for (i in 0 until totalCards) {
        val pIdx = i % 4
        val slotInHand = i / 4
        val cardStart = (i * 0.045f).coerceAtMost(0.60f)
        val cardEnd = (cardStart + 0.35f).coerceAtMost(1f)

        if (progress >= cardEnd) {
          if (slotInHand + 1 > currentLanded[pIdx]) {
            currentLanded[pIdx] = slotInHand + 1
          }
        }
      }

      SideEffect {
        onLandedCountsChanged(currentLanded.toList())
      }

      for (i in 0 until totalCards) {
        val playerIdx = i % 4
        val slotInPlayerHand = i / 4

        val cardStart = (i * 0.045f).coerceAtMost(0.60f)
        val cardEnd = (cardStart + 0.35f).coerceAtMost(1f)

        if (progress in cardStart..cardEnd) {
          val cardProgress = ((progress - cardStart) / (cardEnd - cardStart)).coerceIn(0f, 1f)
          val eased = FastOutSlowInEasing.transform(cardProgress)

          val targetX: Float
          val targetY: Float
          val targetAngle: Float

          when (playerIdx) {
            0 -> { // Human Player (Bottom Hand)
              val handCount = 4
              val centerIndex = (handCount - 1) / 2f
              val xOffset = (slotInPlayerHand - centerIndex) * 50f
              val dist = Math.abs(slotInPlayerHand - centerIndex)
              val yOffset = dist * dist * 3f

              targetX = w * 0.5f + xOffset - 42f
              targetY = h - 130f - yOffset
              targetAngle = -10f + slotInPlayerHand * (20f / (handCount - 1))
            }
            1 -> { // Plato (Left)
              val yOffset = (slotInPlayerHand - 1.5f) * 20f
              targetX = 10f
              targetY = h * 0.5f + yOffset - 22f
              targetAngle = 90f
            }
            2 -> { // Euklid (Top)
              val xOffset = (slotInPlayerHand - 1.5f) * 24f
              targetX = w * 0.5f + xOffset - 24f
              targetY = 20f
              targetAngle = -12f + slotInPlayerHand * 8f
            }
            else -> { // Sokrates (Right)
              val yOffset = (slotInPlayerHand - 1.5f) * 20f
              targetX = w - 66f
              targetY = h * 0.5f + yOffset - 22f
              targetAngle = 90f
            }
          }

          val arcHeight = kotlin.math.sin(cardProgress * Math.PI).toFloat() * 45f
          val curX = deckX + (targetX - deckX) * eased
          val curY = deckY + (targetY - deckY) * eased - arcHeight

          val flightScale = 1.0f + kotlin.math.sin(cardProgress * Math.PI).toFloat() * 0.20f
          val shadowElevation = (4f + kotlin.math.sin(cardProgress * Math.PI).toFloat() * 12f).dp

          if (playerIdx == 0) {
            // Human Player: 3D Flip effect turning card face-up during flight to hand
            val flipAngle = cardProgress * 180f
            val card = humanHand.getOrNull(slotInPlayerHand)

            Box(
              modifier = Modifier
                .offset(x = curX.dp, y = curY.dp)
                .graphicsLayer {
                  scaleX = flightScale
                  scaleY = flightScale
                  rotationZ = targetAngle * cardProgress
                  rotationY = flipAngle
                  cameraDistance = 12f * density
                }
                .shadow(shadowElevation, RoundedCornerShape(8.dp))
            ) {
              if (flipAngle < 90f) {
                PlaidCardBack(
                  modifier = Modifier
                    .width(84.dp)
                    .height(124.dp)
                )
              } else {
                if (card != null) {
                  PlayingCardView(
                    card = card,
                    cardWidth = 84.dp,
                    cardHeight = 124.dp,
                    modifier = Modifier.graphicsLayer { rotationY = 180f }
                  )
                } else {
                  PlaidCardBack(
                    modifier = Modifier
                      .width(84.dp)
                      .height(124.dp)
                  )
                }
              }
            }
          } else {
            val flightRotation = (cardProgress * 360f * 0.5f + targetAngle) % 360f
            PlaidCardBack(
              modifier = Modifier
                .offset(x = curX.dp, y = curY.dp)
                .width(48.dp)
                .height(72.dp)
                .rotate(flightRotation)
                .shadow(shadowElevation, RoundedCornerShape(6.dp))
            )
          }
        }
      }
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
  playerId: Int,
  isCurrentTurn: Boolean = false,
  isStarter: Boolean = false
) {
  Box(contentAlignment = Alignment.Center) {
    PlayerBadge(
      name = name,
      points = points,
      iconText = iconText,
      isCurrentTurn = isCurrentTurn,
      isStarter = isStarter
    )
    
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
fun PlayerBadge(
  name: String,
  points: Int = 0,
  iconText: String? = null,
  isCurrentTurn: Boolean = false,
  isStarter: Boolean = false
) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Box(
      modifier = Modifier
        .background(if (isCurrentTurn) Color(0xFF1B5E20) else Color.Black, RoundedCornerShape(4.dp))
        .border(
          width = if (isCurrentTurn) 2.dp else 1.dp,
          color = if (isCurrentTurn) PrimaryYellow else Color.DarkGray,
          shape = RoundedCornerShape(4.dp)
        )
        .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        if (isStarter) {
          Text(
            text = "⭐ ",
            fontSize = 11.sp
          )
        }
        Text(
          text = name,
          color = Color.White,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }
    if (isCurrentTurn || iconText != null) {
      Box(
        modifier = Modifier
          .background(if (isCurrentTurn) PrimaryYellow else Color.Black, RoundedCornerShape(2.dp))
          .padding(horizontal = 6.dp, vertical = 2.dp)
      ) {
        Text(
          text = if (isCurrentTurn) "AM ZUG" else (iconText ?: ""),
          color = if (isCurrentTurn) Color.Black else Color.White,
          fontSize = 10.sp,
          fontWeight = FontWeight.ExtraBold
        )
      }
    }
  }
}

// --- CENTER LIVE SCOREBOARD BOARD ---

@Composable
fun CenterScoreboardPanel(
  players: List<PlayerInfo>,
  currentTurnIndex: Int,
  starterPlayerIndex: Int,
  roundNumber: Int,
  targetScore: Int = 501,
  scoreEvent: ScoreGainEvent? = null,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier
      .fillMaxWidth(0.95f)
      .padding(horizontal = 8.dp, vertical = 2.dp),
    shape = RoundedCornerShape(12.dp),
    color = Color.Black.copy(alpha = 0.75f),
    border = androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryYellow.copy(alpha = 0.85f)),
    shadowElevation = 8.dp
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
            text = "• Runde $roundNumber",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
          )
        }
        Text(
          text = "🎯 Ziel: $targetScore Pkt",
          color = PrimaryYellow,
          fontSize = 11.sp,
          fontWeight = FontWeight.ExtraBold
        )
      }

      Spacer(modifier = Modifier.height(4.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
      ) {
        players.forEachIndexed { index, player ->
          val isTurn = index == currentTurnIndex
          val isStarter = index == starterPlayerIndex
          val currentPoints = player.totalScore + player.roundScore

          Box(
            modifier = Modifier
              .weight(1f)
              .padding(horizontal = 2.dp)
              .background(
                color = if (isTurn) Color(0xFF1B5E20) else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(8.dp)
              )
              .border(
                width = if (isTurn) 1.5.dp else 0.5.dp,
                color = if (isTurn) PrimaryYellow else Color.White.copy(alpha = 0.25f),
                shape = RoundedCornerShape(8.dp)
              )
              .padding(vertical = 4.dp, horizontal = 2.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                if (isStarter) {
                  Text(text = "⭐", fontSize = 9.sp)
                }
                Text(
                  text = player.name,
                  color = if (isTurn) PrimaryYellow else Color.White,
                  fontSize = 11.sp,
                  fontWeight = if (isTurn) FontWeight.ExtraBold else FontWeight.Bold,
                  maxLines = 1
                )
              }
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = "$currentPoints Pkt",
                  color = Color.White,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.ExtraBold
                )
                if (player.roundScore > 0) {
                  Spacer(modifier = Modifier.width(2.dp))
                  Text(
                    text = "(+${player.roundScore})",
                    color = PrimaryYellow,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
              Text(
                text = "${player.capturedCount} Karten",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 9.sp
              )
            }

            if (scoreEvent != null && scoreEvent.playerId == index) {
              FloatingScorePill(
                text = scoreEvent.text,
                timestamp = scoreEvent.timestamp,
                modifier = Modifier.offset(y = (-28).dp)
              )
            }
          }
        }
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
  if (!card.isFaceUp) {
    PlaidCardBack(
      modifier = modifier
        .width(cardWidth)
        .height(cardHeight)
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    )
    return
  }

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

  Box(
    modifier = modifier,
    contentAlignment = Alignment.TopCenter
  ) {
    Box(
      modifier = Modifier
        .width(cardWidth)
        .height(cardHeight)
        .shadow(if (highlightType != CardHighlightType.NONE) 8.dp else 3.dp, RoundedCornerShape(8.dp))
        .clip(RoundedCornerShape(8.dp))
        .background(Color.White)
        .border(borderWidth, borderColor, RoundedCornerShape(8.dp))
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
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

      // Center content (Rich Court Card Graphics)
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        when (card.rank) {
          Rank.KING -> {
            Box(
              modifier = Modifier
                .size(if (isLarge) 44.dp else 34.dp)
                .background(Color(0xFF8B0000).copy(alpha = 0.10f), RoundedCornerShape(6.dp))
                .border(1.dp, Color(0xFFD4AF37), RoundedCornerShape(6.dp)),
              contentAlignment = Alignment.Center
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("👑", fontSize = if (isLarge) 16.sp else 12.sp)
                Text("🤴", fontSize = if (isLarge) 20.sp else 16.sp)
              }
            }
          }
          Rank.QUEEN -> {
            Box(
              modifier = Modifier
                .size(if (isLarge) 44.dp else 34.dp)
                .background(Color(0xFF4B0082).copy(alpha = 0.10f), RoundedCornerShape(6.dp))
                .border(1.dp, Color(0xFFD4AF37), RoundedCornerShape(6.dp)),
              contentAlignment = Alignment.Center
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("👑", fontSize = if (isLarge) 15.sp else 11.sp)
                Text("👸", fontSize = if (isLarge) 20.sp else 16.sp)
              }
            }
          }
          Rank.JACK -> {
            Box(
              modifier = Modifier
                .size(if (isLarge) 44.dp else 34.dp)
                .background(Color(0xFF005A70).copy(alpha = 0.10f), RoundedCornerShape(6.dp))
                .border(1.dp, Color(0xFFD4AF37), RoundedCornerShape(6.dp)),
              contentAlignment = Alignment.Center
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("⚔️", fontSize = if (isLarge) 14.sp else 10.sp)
                Text("🧔", fontSize = if (isLarge) 20.sp else 16.sp)
              }
            }
          }
          Rank.ACE -> {
            Box(
              modifier = Modifier
                .size(if (isLarge) 42.dp else 32.dp)
                .background(Color(0xFFFFF8E1), CircleShape)
                .border(1.dp, Color(0xFFD4AF37).copy(alpha = 0.6f), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = card.suit.symbol,
                color = suitColor,
                fontSize = if (isLarge) 28.sp else 22.sp
              )
            }
          }
          else -> {
            Text(
              text = card.suit.symbol,
              color = suitColor,
              fontSize = centerSuitSize
            )
          }
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
  val total = hand.size
  val topCard = centerPile.lastOrNull()
  val centerCount = centerPile.size

  val cardWidth = when (total) {
    1 -> 90.dp
    2 -> 86.dp
    3 -> 82.dp
    else -> 76.dp
  }
  val cardHeight = when (total) {
    1 -> 132.dp
    2 -> 126.dp
    3 -> 122.dp
    else -> 116.dp
  }
  val spacing = when (total) {
    1 -> 0.dp
    2 -> 12.dp
    3 -> 8.dp
    else -> 6.dp
  }

  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterHorizontally),
    verticalAlignment = Alignment.Bottom
  ) {
    hand.forEach { card ->
      val highlightType = if (isMyTurn && topCard != null) {
        val matches = card.rank == topCard.rank || card.rank == Rank.JACK
        if (matches) {
          if (centerCount == 1) CardHighlightType.PISTI else CardHighlightType.STICH
        } else CardHighlightType.NONE
      } else CardHighlightType.NONE

      PlayingCardView(
        card = card,
        cardWidth = cardWidth,
        cardHeight = cardHeight,
        highlightType = highlightType,
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
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text("📖 Pişti Spielregeln (Einfach erklärt)", color = PrimaryYellow, fontWeight = FontWeight.Bold, fontSize = 18.sp)
      }
    },
    text = {
      Column(
        modifier = Modifier
          .verticalScroll(rememberScrollState())
          .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        RuleCardItem(
          icon = "🎯",
          title = "Ziel des Spiels",
          desc = "Wer als Erster genau 101 Punkte erreicht, gewinnt das Match! Triffst du mehr als 101 Punkte (Überwerfen), fällst du als Strafe auf 50 Punkte zurück."
        )

        RuleCardItem(
          icon = "🎲",
          title = "Wer fängt an? (Originale Reihenfolge)",
          desc = "Zu Beginn entscheidet reiner Zufall, wer Geber (Dağıtan) ist. Der andere Spieler (Vorhand) spielt die 1. Karte aus! In jeder neuen Runde wechselt die Geberrolle abwechselnd."
        )

        RuleCardItem(
          icon = "🎴",
          title = "Karten austeilen",
          desc = "4 Karten kommen in die Mitte (3 verdeckt, 1 offen – niemals ein Bube). Jeder Spieler erhält 4 Handkarten. Sind alle 4 Karten gespielt, werden wieder 4 neue ausgeteilt, bis alle 52 Karten durchgespielt sind."
        )

        RuleCardItem(
          icon = "⚡",
          title = "Karten stechen",
          desc = "Triffst du die oberste Karte auf dem Tisch mit demselben Kartenwert (z.B. 8 auf 8) oder legst du einen Buben (Joker), gehört der gesamte Kartenstapel dir!"
        )

        RuleCardItem(
          icon = "🔥",
          title = "Pişti (+10 Punkte) & Buben-Pişti (+20)",
          desc = "• Normales Pişti (+10 Pkt): Liegt nur 1 einzelne Karte auf dem Tisch und du legst denselben Wert drauf.\n• Buben-Pişti (+20 Pkt): Liegt ein einzelner Bube und du legst einen Buben drauf!\n(Hinweis: Ein Bube auf eine normale Einzelkarte sticht normal, ist aber kein Pişti)."
        )

        RuleCardItem(
          icon = "💎",
          title = "Punktwerte der Karten",
          desc = "• Karo 10 (♦10): 3 Punkte\n• Kreuz 2 (♣2): 2 Punkte\n• Alle Asse (A) & Buben (J): je 1 Punkt\n• Kartenmehrheit (+3 Pkt): Wer am Rundenende mindestens 27 Karten gesammelt hat, bekommt +3 Bonuspunkte!"
        )

        RuleCardItem(
          icon = "🏁",
          title = "Letzter Stich (Son Alan)",
          desc = "Bleiben am Ende der 52 Karten noch Karten auf dem Tisch liegen, bekommt diese der Spieler, der den letzten Stich der Runde gemacht hat."
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onDismiss() },
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryYellow, contentColor = OnPrimaryYellow)
      ) {
        Text("Alles klar!", fontWeight = FontWeight.Bold)
      }
    },
    containerColor = BackgroundGreen
  )
}

@Composable
fun RuleCardItem(icon: String, title: String, desc: String) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
      .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
      .padding(10.dp)
  ) {
    Column {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 16.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(title, color = PrimaryYellow, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
      }
      Spacer(modifier = Modifier.height(4.dp))
      Text(desc, color = Color.White.copy(alpha = 0.9f), fontSize = 11.5.sp, lineHeight = 16.sp)
    }
  }
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

@Composable
fun InitialNicknameDialog(viewModel: GameViewModel, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
  val state by viewModel.state.collectAsState()
  var tempName by remember { mutableStateOf(state.playerName) }

  AlertDialog(
    onDismissRequest = { onDismiss() },
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryYellow)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Willkommen bei Pişti 101!", color = PrimaryYellow, fontWeight = FontWeight.Bold, fontSize = 20.sp)
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          "Bitte gib deinen Spielernamen ein. Dieser Name wird dir und deinen Freunden in Online-Duellen sowie im Spiel angezeigt.",
          color = Color.White,
          fontSize = 13.sp
        )
        OutlinedTextField(
          value = tempName,
          onValueChange = { tempName = it },
          label = { Text("Dein Username", color = OutlineYellow) },
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
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(tempName) },
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryYellow, contentColor = OnPrimaryYellow)
      ) {
        Text("Speichern & Weiter", fontWeight = FontWeight.Bold)
      }
    },
    containerColor = BackgroundGreen
  )
}

