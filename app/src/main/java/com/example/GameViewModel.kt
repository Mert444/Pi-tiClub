package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Suit(val symbol: String) { HEARTS("♥"), DIAMONDS("♦"), CLUBS("♣"), SPADES("♠") }
enum class Rank(val symbol: String, val value: Int) {
    TWO("2", 0), THREE("3", 0), FOUR("4", 0), FIVE("5", 0), SIX("6", 0),
    SEVEN("7", 0), EIGHT("8", 0), NINE("9", 0), TEN("10", 0),
    JACK("J", 1), QUEEN("Q", 0), KING("K", 0), ACE("A", 1)
}

data class Card(val suit: Suit, val rank: Rank, val isFaceUp: Boolean = true)

data class PlayerInfo(
    val id: Int, // 0: Spieler, 1: Plato, 2: Euklid, 3: Sokrates
    val name: String,
    val isHuman: Boolean,
    val hand: List<Card> = emptyList(),
    val capturedCount: Int = 0,
    val roundScore: Int = 0,
    val totalScore: Int = 0
)

data class ScoreGainEvent(
    val playerId: Int,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class GameState(
    val deck: List<Card> = emptyList(),
    val centerPile: List<Card> = emptyList(),
    val players: List<PlayerInfo> = listOf(
        PlayerInfo(0, "Spieler", isHuman = true),
        PlayerInfo(1, "Plato", isHuman = false),
        PlayerInfo(2, "Euklid", isHuman = false),
        PlayerInfo(3, "Sokrates", isHuman = false)
    ),
    val currentTurnIndex: Int = 0, // 0 = Spieler, 1 = Plato, 2 = Euklid, 3 = Sokrates
    val lastCaptorIndex: Int = -1,
    val roundNumber: Int = 1,
    val targetScore: Int = 501,
    val elapsedSeconds: Int = 0,
    val isMatchOver: Boolean = false,
    val matchWinnerName: String? = null,
    val statusMessage: String = "DU BIST AM ZUG",
    val playerName: String = "Spieler",
    val playerCoins: Int = 630,
    val soundEnabled: Boolean = true,
    val showRulesDialog: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val showStatsDialog: Boolean = false,
    val showPrivacyDialog: Boolean = false,
    val showExitDialog: Boolean = false,
    val showInGameMenu: Boolean = false,
    val showRoundEndSummary: Boolean = false,
    val gamesPlayed: Int = 18,
    val gamesWon: Int = 12,
    val pistiTotalCount: Int = 24,
    val lastPistiMessage: String? = null,
    val lastScoreEvent: ScoreGainEvent? = null
)

class GameViewModel : ViewModel() {
    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state
    private var timerJob: kotlinx.coroutines.Job? = null

    init {
        startNewMatch()
    }

    private fun startMatchTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _state.update {
                    if (!it.isMatchOver && !it.showRoundEndSummary) {
                        it.copy(elapsedSeconds = it.elapsedSeconds + 1)
                    } else {
                        it
                    }
                }
            }
        }
    }

    fun toggleRulesDialog(show: Boolean) {
        _state.update { it.copy(showRulesDialog = show) }
    }

    fun toggleSettingsDialog(show: Boolean) {
        _state.update { it.copy(showSettingsDialog = show) }
    }

    fun toggleStatsDialog(show: Boolean) {
        _state.update { it.copy(showStatsDialog = show) }
    }

    fun togglePrivacyDialog(show: Boolean) {
        _state.update { it.copy(showPrivacyDialog = show) }
    }

    fun toggleExitDialog(show: Boolean) {
        _state.update { it.copy(showExitDialog = show) }
    }

    fun toggleInGameMenu(show: Boolean) {
        _state.update { it.copy(showInGameMenu = show) }
    }

    fun toggleRoundEndSummary(show: Boolean) {
        _state.update { it.copy(showRoundEndSummary = show) }
    }

    fun toggleSound() {
        _state.update { it.copy(soundEnabled = !it.soundEnabled) }
    }

    fun updatePlayerName(newName: String) {
        if (newName.isNotBlank()) {
            _state.update { currentState ->
                val updatedPlayers = currentState.players.map {
                    if (it.id == 0) it.copy(name = newName) else it
                }
                currentState.copy(playerName = newName, players = updatedPlayers)
            }
        }
    }

    fun startNewGame() {
        startNewMatch()
    }

    fun startNewMatch() {
        _state.update {
            it.copy(
                roundNumber = 1,
                elapsedSeconds = 0,
                isMatchOver = false,
                matchWinnerName = null,
                showRoundEndSummary = false,
                players = listOf(
                    PlayerInfo(0, _state.value.playerName, isHuman = true, totalScore = 0),
                    PlayerInfo(1, "Plato", isHuman = false, totalScore = 0),
                    PlayerInfo(2, "Euklid", isHuman = false, totalScore = 0),
                    PlayerInfo(3, "Sokrates", isHuman = false, totalScore = 0)
                )
            )
        }
        startMatchTimer()
        startNewRound()
    }

    fun startNewRound() {
        val fullDeck = mutableListOf<Card>()
        Suit.values().forEach { suit ->
            Rank.values().forEach { rank ->
                fullDeck.add(Card(suit, rank))
            }
        }
        // Use Kotlin Random with current time entropy to ensure distinct random distribution every round
        fullDeck.shuffle(kotlin.random.Random(System.currentTimeMillis() + System.nanoTime()))

        // Start with empty center pile as requested (no pre-laid cards)
        val center = emptyList<Card>()
        var remaining: List<Card> = fullDeck

        // Deal 4 unique cards to each of the 4 players (16 total)
        val resetPlayers = _state.value.players.mapIndexed { idx, p ->
            val playerHand = remaining.drop(idx * 4).take(4)
            p.copy(
                hand = playerHand,
                capturedCount = 0,
                roundScore = 0
            )
        }
        remaining = remaining.drop(16)

        // Randomly select starting player (0 = Human, 1 = Plato, 2 = Euklid, 3 = Sokrates)
        val startingPlayerIndex = kotlin.random.Random.nextInt(4)
        val starterName = resetPlayers[startingPlayerIndex].name
        val statusMsg = if (startingPlayerIndex == 0) "DU BIST AM ZUG" else "$starterName fängt an..."

        _state.update {
            it.copy(
                deck = remaining,
                centerPile = center,
                players = resetPlayers,
                currentTurnIndex = startingPlayerIndex,
                lastCaptorIndex = -1,
                showRoundEndSummary = false,
                statusMessage = statusMsg,
                lastPistiMessage = null
            )
        }

        if (startingPlayerIndex != 0) {
            viewModelScope.launch {
                delay(1400)
                executeBotTurn(startingPlayerIndex)
            }
        }
    }

    fun playCard(card: Card, playerIndex: Int) {
        val currentState = _state.value
        if (currentState.isMatchOver) return
        if (currentState.currentTurnIndex != playerIndex) return

        val currentPlayer = currentState.players[playerIndex]
        if (!currentPlayer.hand.contains(card)) return

        val newHand = currentPlayer.hand - card
        val newCenterPile = currentState.centerPile + card.copy(isFaceUp = true)

        var newCapturedCount = currentPlayer.capturedCount
        var newRoundScore = currentPlayer.roundScore
        var newLastCaptor = currentState.lastCaptorIndex
        var pistiNotice: String? = null
        var captured = false
        var scoreAnimEvent: ScoreGainEvent? = null

        if (currentState.centerPile.isNotEmpty()) {
            val topCard = currentState.centerPile.last()
            if (card.rank == topCard.rank || card.rank == Rank.JACK) {
                captured = true
                val isPisti = currentState.centerPile.size == 1 && card.rank == topCard.rank
                val isJackPisti = isPisti && card.rank == Rank.JACK

                var points = if (isPisti) (if (isJackPisti) 20 else 10) else 0
                newCenterPile.forEach {
                    when {
                        it.suit == Suit.DIAMONDS && it.rank == Rank.TEN -> points += 3
                        it.suit == Suit.CLUBS && it.rank == Rank.TWO -> points += 2
                        it.rank == Rank.ACE -> points += 1
                        it.rank == Rank.JACK -> points += 1
                    }
                }

                newCapturedCount += newCenterPile.size
                newRoundScore += points
                newLastCaptor = playerIndex

                val animText = if (isPisti) {
                    if (isJackPisti) "🔥 JACK PIŞTI! +20" else "🔥 PIŞTI! +10"
                } else if (points > 0) {
                    "+$points PKT"
                } else {
                    "+${newCenterPile.size} Karten"
                }
                scoreAnimEvent = ScoreGainEvent(playerIndex, animText)

                if (isPisti) {
                    pistiNotice = if (playerIndex == 0) "🔥 PIŞTI! +10 PKT!" else "⚡ PIŞTI VON ${currentPlayer.name.uppercase()}!"
                    if (playerIndex == 0) {
                        _state.update { it.copy(pistiTotalCount = it.pistiTotalCount + 1) }
                    }
                }
            }
        }

        val updatedPlayer = currentPlayer.copy(
            hand = newHand,
            capturedCount = newCapturedCount,
            roundScore = newRoundScore
        )

        val updatedPlayers = currentState.players.map {
            if (it.id == playerIndex) updatedPlayer else it
        }

        val finalCenterPile = if (captured) emptyList() else newCenterPile

        // Turn moves clockwise
        val nextTurn = (currentState.currentTurnIndex + 1) % 4

        // Check if all hands are empty
        val allHandsEmpty = updatedPlayers.all { it.hand.isEmpty() }
        var finalDeck = currentState.deck
        var finalPlayers = updatedPlayers

        if (allHandsEmpty && finalDeck.isNotEmpty()) {
            val cardsPerPlayer = if (finalDeck.size >= 16) 4 else (finalDeck.size / 4)
            if (cardsPerPlayer > 0) {
                finalPlayers = updatedPlayers.mapIndexed { idx, player ->
                    val cardsToTake = finalDeck.drop(idx * cardsPerPlayer).take(cardsPerPlayer)
                    player.copy(hand = cardsToTake)
                }
                finalDeck = finalDeck.drop(cardsPerPlayer * 4)
            }
        }

        val isRoundOver = finalPlayers.all { it.hand.isEmpty() } && finalDeck.isEmpty()

        if (isRoundOver) {
            // Give remaining center pile to last captor and calculate points
            if (finalCenterPile.isNotEmpty() && newLastCaptor in 0..3) {
                var leftoverPoints = 0
                finalCenterPile.forEach { card ->
                    when {
                        card.suit == Suit.DIAMONDS && card.rank == Rank.TEN -> leftoverPoints += 3
                        card.suit == Suit.CLUBS && card.rank == Rank.TWO -> leftoverPoints += 2
                        card.rank == Rank.ACE -> leftoverPoints += 1
                        card.rank == Rank.JACK -> leftoverPoints += 1
                    }
                }
                finalPlayers = finalPlayers.map { p ->
                    if (p.id == newLastCaptor) {
                        p.copy(
                            capturedCount = p.capturedCount + finalCenterPile.size,
                            roundScore = p.roundScore + leftoverPoints
                        )
                    } else p
                }
            }

            // Bonus 3 points for majority captured
            val maxCaptured = finalPlayers.maxOf { it.capturedCount }
            val majorityPlayers = finalPlayers.filter { it.capturedCount == maxCaptured }
            if (majorityPlayers.size == 1) {
                val winnerId = majorityPlayers.first().id
                finalPlayers = finalPlayers.map { p ->
                    if (p.id == winnerId) p.copy(roundScore = p.roundScore + 3) else p
                }
            }

            // Update total accumulated scores
            finalPlayers = finalPlayers.map { p ->
                p.copy(totalScore = p.totalScore + p.roundScore)
            }

            // Check if any player hit or exceeded targetScore (501 points)
            val highestTotalScore = finalPlayers.maxOf { it.totalScore }
            val matchOver = highestTotalScore >= currentState.targetScore
            val winner = if (matchOver) finalPlayers.maxByOrNull { it.totalScore }?.name else null

            if (matchOver) {
                _state.update {
                    it.copy(
                        deck = emptyList(),
                        centerPile = emptyList(),
                        players = finalPlayers,
                        currentTurnIndex = 0,
                        lastCaptorIndex = newLastCaptor,
                        isMatchOver = true,
                        matchWinnerName = winner,
                        showRoundEndSummary = true,
                        statusMessage = "MATCH BEENDET! GEWINNER: $winner",
                        lastPistiMessage = pistiNotice,
                        lastScoreEvent = scoreAnimEvent ?: it.lastScoreEvent,
                        gamesPlayed = it.gamesPlayed + 1,
                        gamesWon = if (winner == finalPlayers[0].name) it.gamesWon + 1 else it.gamesWon
                    )
                }
            } else {
                // Continue game automatically: deal next deck without blocking dialog
                val nextRoundNum = currentState.roundNumber + 1

                val fullDeck = mutableListOf<Card>()
                Suit.values().forEach { suit ->
                    Rank.values().forEach { rank ->
                        fullDeck.add(Card(suit, rank))
                    }
                }
                fullDeck.shuffle(kotlin.random.Random(System.currentTimeMillis() + System.nanoTime()))

                val newCenter = emptyList<Card>()
                var newRemaining: List<Card> = fullDeck

                val newResetPlayers = finalPlayers.mapIndexed { idx, p ->
                    val playerHand = newRemaining.drop(idx * 4).take(4)
                    p.copy(
                        hand = playerHand,
                        capturedCount = 0,
                        roundScore = 0
                    )
                }
                newRemaining = newRemaining.drop(16)

                val startingPlayerIndex = kotlin.random.Random.nextInt(4)
                val starterName = newResetPlayers[startingPlayerIndex].name
                val statusMsg = if (startingPlayerIndex == 0) "KARTEN NEU GEMISCHT - DU BIST AM ZUG" else "NEU GEMISCHT - $starterName fängt an..."

                _state.update {
                    it.copy(
                        deck = newRemaining,
                        centerPile = newCenter,
                        players = newResetPlayers,
                        currentTurnIndex = startingPlayerIndex,
                        lastCaptorIndex = -1,
                        roundNumber = nextRoundNum,
                        showRoundEndSummary = false,
                        statusMessage = statusMsg,
                        lastPistiMessage = pistiNotice,
                        lastScoreEvent = scoreAnimEvent ?: it.lastScoreEvent
                    )
                }

                if (startingPlayerIndex != 0) {
                    viewModelScope.launch {
                        delay(1400)
                        executeBotTurn(startingPlayerIndex)
                    }
                }
            }
        } else {
            val nextPlayerName = finalPlayers[nextTurn].name
            val statusMsg = if (nextTurn == 0) "DU BIST AM ZUG" else "$nextPlayerName überlegt..."

            _state.update {
                it.copy(
                    deck = finalDeck,
                    centerPile = finalCenterPile,
                    players = finalPlayers,
                    currentTurnIndex = nextTurn,
                    lastCaptorIndex = newLastCaptor,
                    statusMessage = statusMsg,
                    lastPistiMessage = pistiNotice,
                    lastScoreEvent = scoreAnimEvent ?: it.lastScoreEvent
                )
            }

            // Trigger Bot Turn if next player is a bot
            if (!finalPlayers[nextTurn].isHuman) {
                viewModelScope.launch {
                    delay(1400)
                    executeBotTurn(nextTurn)
                }
            }
        }
    }

    private fun executeBotTurn(botIndex: Int) {
        val state = _state.value
        if (state.isMatchOver || state.showRoundEndSummary || state.currentTurnIndex != botIndex) return

        val botPlayer = state.players[botIndex]
        val botHand = botPlayer.hand
        if (botHand.isEmpty()) return

        val centerPile = state.centerPile
        val topCard = centerPile.lastOrNull()
        var cardToPlay: Card? = null

        // 55% chance for bot to make a casual, relaxed move
        val isCasualMove = kotlin.random.Random.nextFloat() < 0.55f

        if (isCasualMove && botHand.size > 1) {
            val nonJacks = botHand.filter { it.rank != Rank.JACK }
            cardToPlay = if (nonJacks.isNotEmpty()) nonJacks.shuffled().first() else botHand.shuffled().first()
        }

        if (cardToPlay == null) {
            if (centerPile.isEmpty()) {
                // Strategy on EMPTY pile:
                // 50% chance: if human player has a matching rank in hand, bot plays that rank to setup a Pişti for human!
                val humanHand = state.players.find { it.isHuman }?.hand ?: emptyList()
                val humanRanks = humanHand.map { it.rank }.toSet()
                val setupCardForHuman = botHand.find { it.rank in humanRanks && it.rank != Rank.JACK }

                if (setupCardForHuman != null && kotlin.random.Random.nextFloat() < 0.50f) {
                    cardToPlay = setupCardForHuman
                } else {
                    val safeCards = botHand.filter { card ->
                        card.rank != Rank.JACK &&
                        card.rank != Rank.ACE &&
                        !(card.suit == Suit.DIAMONDS && card.rank == Rank.TEN) &&
                        !(card.suit == Suit.CLUBS && card.rank == Rank.TWO)
                    }
                    val rankCounts = botHand.groupingBy { it.rank }.eachCount()
                    val duplicateCard = safeCards.find { (rankCounts[it.rank] ?: 0) > 1 }

                    cardToPlay = duplicateCard ?: safeCards.shuffled().firstOrNull() ?: botHand.minByOrNull {
                        var valRisk = it.rank.value
                        if (it.rank == Rank.JACK) valRisk += 20
                        if (it.suit == Suit.DIAMONDS && it.rank == Rank.TEN) valRisk += 15
                        if (it.suit == Suit.CLUBS && it.rank == Rank.TWO) valRisk += 10
                        valRisk
                    } ?: botHand.first()
                }
            } else if (centerPile.size == 1 && topCard != null) {
                // Strategy on 1 CARD pile:
                val matchingCard = botHand.find { it.rank == topCard.rank }
                val jackCard = botHand.find { it.rank == Rank.JACK }

                // 65% chance to skip stealing Pişti, leaving the single card for the next player / human!
                val missPistiChance = kotlin.random.Random.nextFloat() < 0.65f

                if (matchingCard != null && !missPistiChance) {
                    cardToPlay = matchingCard
                } else if (jackCard != null && !missPistiChance && (topCard.rank == Rank.ACE || (topCard.suit == Suit.DIAMONDS && topCard.rank == Rank.TEN))) {
                    cardToPlay = jackCard
                } else {
                    val safeNonJacks = botHand.filter { it.rank != Rank.JACK }
                    cardToPlay = safeNonJacks.shuffled().firstOrNull() ?: botHand.first()
                }
            } else {
                // Strategy on 2+ CARDS pile:
                val topCardRank = topCard?.rank
                val matchingCard = botHand.find { it.rank == topCardRank }
                val jackCard = botHand.find { it.rank == Rank.JACK }

                // 50% chance bot holds back matching card/jack to let point pile grow for human
                val holdBack = kotlin.random.Random.nextFloat() < 0.50f

                if (matchingCard != null && !holdBack) {
                    cardToPlay = matchingCard
                } else if (jackCard != null && !holdBack) {
                    cardToPlay = jackCard
                } else {
                    val safeNonJacks = botHand.filter { it.rank != Rank.JACK }
                    cardToPlay = safeNonJacks.shuffled().firstOrNull() ?: botHand.first()
                }
            }
        }

        playCard(cardToPlay, botIndex)
    }
}
