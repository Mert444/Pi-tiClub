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
    val starterPlayerIndex: Int = 0, // Player who started current round
    val lastCaptorIndex: Int = -1,
    val roundNumber: Int = 1,
    val targetScore: Int = 101,
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
    val lastScoreEvent: ScoreGainEvent? = null,
    val dealAnimTrigger: Long = 0L
)

class GameViewModel : ViewModel() {
    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state
    private var timerJob: kotlinx.coroutines.Job? = null
    private var globalMatchStarterIndex = 0

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
        val initialStarter = globalMatchStarterIndex
        globalMatchStarterIndex = (globalMatchStarterIndex + 1) % 4

        _state.update {
            it.copy(
                roundNumber = 1,
                elapsedSeconds = 0,
                isMatchOver = false,
                matchWinnerName = null,
                showRoundEndSummary = false,
                starterPlayerIndex = initialStarter,
                players = listOf(
                    PlayerInfo(0, _state.value.playerName, isHuman = true, totalScore = 0),
                    PlayerInfo(1, "Plato", isHuman = false, totalScore = 0),
                    PlayerInfo(2, "Euklid", isHuman = false, totalScore = 0),
                    PlayerInfo(3, "Sokrates", isHuman = false, totalScore = 0)
                )
            )
        }
        startMatchTimer()
        startNewRound(isMatchStart = true)
    }

    private fun dealNewRoundDeck(currentPlayers: List<PlayerInfo>): Triple<List<PlayerInfo>, List<Card>, List<Card>> {
        val fullDeck = mutableListOf<Card>()
        Suit.values().forEach { suit ->
            Rank.values().forEach { rank ->
                fullDeck.add(Card(suit, rank))
            }
        }

        val secureRandom = java.security.SecureRandom()
        repeat(7) {
            fullDeck.shuffle(secureRandom)
        }

        val cutPoint = 12 + secureRandom.nextInt(28)
        val cutDeck = (fullDeck.drop(cutPoint) + fullDeck.take(cutPoint)).toMutableList()
        cutDeck.shuffle(secureRandom)

        // Deal 4 initial center cards (3 face down, 1 face up)
        val centerCards = cutDeck.take(4).toMutableList()
        val remDeck = cutDeck.drop(4).toMutableList()

        // Ensure top card of center pile is not a Jack
        if (centerCards.last().rank == Rank.JACK) {
            val nonJackIndex = remDeck.indexOfFirst { it.rank != Rank.JACK }
            if (nonJackIndex != -1) {
                val temp = centerCards[3]
                centerCards[3] = remDeck[nonJackIndex]
                remDeck[nonJackIndex] = temp
            }
        }

        val centerPile = listOf(
            centerCards[0].copy(isFaceUp = false),
            centerCards[1].copy(isFaceUp = false),
            centerCards[2].copy(isFaceUp = false),
            centerCards[3].copy(isFaceUp = true)
        )

        // Deal 16 cards to 4 players using fair Pişti-opportunity distribution
        val (updatedPlayersWithHands, remainingDeck) = dealSubHand(remDeck, currentPlayers)

        val updatedPlayers = updatedPlayersWithHands.map { player ->
            player.copy(
                capturedCount = 0,
                roundScore = 0
            )
        }

        return Triple(updatedPlayers, centerPile, remainingDeck)
    }

    private fun dealSubHand(remainingDeck: List<Card>, players: List<PlayerInfo>): Pair<List<PlayerInfo>, List<Card>> {
        if (remainingDeck.isEmpty()) return Pair(players, emptyList())

        val cardsPerPlayer = if (remainingDeck.size >= 16) 4 else (remainingDeck.size / 4)
        if (cardsPerPlayer <= 0) return Pair(players, remainingDeck)

        val totalToDeal = cardsPerPlayer * 4
        val dealBatch = remainingDeck.take(totalToDeal).toMutableList()
        val leftoverDeck = remainingDeck.drop(totalToDeal)

        // Separate Jacks and non-Jacks
        val jacks = dealBatch.filter { it.rank == Rank.JACK }.toMutableList()
        val nonJacks = dealBatch.filter { it.rank != Rank.JACK }.toMutableList()

        val hands = List(4) { mutableListOf<Card>() }

        // 1. Distribute Jacks evenly (at most 1 per player if jacks <= 4)
        val playerIndices = mutableListOf(0, 1, 2, 3)
        playerIndices.shuffle()
        while (jacks.isNotEmpty() && playerIndices.isNotEmpty()) {
            val p = playerIndices.removeAt(0)
            hands[p].add(jacks.removeAt(0))
        }

        // 2. Interleave non-Jacks by rank so matching ranks land in different players' hands (creating Pişti chances!)
        val rankGroupMap = nonJacks.groupBy { it.rank }.mapValues { it.value.toMutableList() }.toMutableMap()
        val interleavedCards = mutableListOf<Card>()

        while (rankGroupMap.isNotEmpty()) {
            val keys = rankGroupMap.keys.toList().shuffled()
            keys.forEach { rankKey ->
                val list = rankGroupMap[rankKey]
                if (!list.isNullOrEmpty()) {
                    interleavedCards.add(list.removeAt(0))
                    if (list.isEmpty()) rankGroupMap.remove(rankKey)
                }
            }
        }

        // Add remaining jacks if any
        interleavedCards.addAll(jacks)

        // 3. Fill hands round-robin so players receive matching pairs across different hands
        var cardIdx = 0
        repeat(cardsPerPlayer) {
            for (p in 0..3) {
                if (hands[p].size < cardsPerPlayer && cardIdx < interleavedCards.size) {
                    hands[p].add(interleavedCards[cardIdx++])
                }
            }
        }

        val updatedPlayers = players.mapIndexed { idx, player ->
            player.copy(hand = hands[idx].shuffled())
        }

        return Pair(updatedPlayers, leftoverDeck)
    }

    fun startNewRound(isMatchStart: Boolean = false) {
        val (resetPlayers, centerPile, remainingDeck) = dealNewRoundDeck(_state.value.players)

        // Select starting player:
        // On match start: use current starter.
        // On subsequent rounds: rotate starting player clockwise (0 -> 1 -> 2 -> 3 -> 0)
        val startingPlayerIndex = if (isMatchStart) {
            _state.value.starterPlayerIndex
        } else {
            (_state.value.starterPlayerIndex + 1) % 4
        }

        val starterName = resetPlayers[startingPlayerIndex].name
        val statusMsg = if (startingPlayerIndex == 0) "DU FÄNGST DIESE RUNDE AN!" else "$starterName fängt an..."

        _state.update {
            it.copy(
                deck = remainingDeck,
                centerPile = centerPile,
                players = resetPlayers,
                currentTurnIndex = startingPlayerIndex,
                starterPlayerIndex = startingPlayerIndex,
                lastCaptorIndex = -1,
                showRoundEndSummary = false,
                statusMessage = statusMsg,
                lastPistiMessage = null,
                dealAnimTrigger = System.currentTimeMillis()
            )
        }

        if (startingPlayerIndex != 0) {
            viewModelScope.launch {
                delay(900)
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

        // Check if all hands are empty
        val allHandsEmpty = updatedPlayers.all { it.hand.isEmpty() }
        var finalDeck = currentState.deck
        var finalPlayers = updatedPlayers

        var dealTriggered = false
        if (allHandsEmpty && finalDeck.isNotEmpty()) {
            val (newPlayers, leftoverDeck) = dealSubHand(finalDeck, updatedPlayers)
            finalPlayers = newPlayers
            finalDeck = leftoverDeck
            dealTriggered = true
        }

        // Turn calculation:
        // If new sub-hands were dealt, the last captor gets to start ONCE.
        // Otherwise, if no captor or for subsequent deals, turn rotates clockwise.
        var nextTurn = (currentState.currentTurnIndex + 1) % 4
        var nextStarterIndex = currentState.starterPlayerIndex
        var nextLastCaptor = newLastCaptor

        if (dealTriggered) {
            if (newLastCaptor in 0..3) {
                // Last captor gets 1-time starting privilege
                nextTurn = newLastCaptor
                nextStarterIndex = newLastCaptor
                nextLastCaptor = -1 // Reset after granting 1-time starter privilege
            } else {
                // Otherwise rotate starter clockwise
                nextStarterIndex = (currentState.starterPlayerIndex + 1) % 4
                nextTurn = nextStarterIndex
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
                val (newResetPlayers, newCenter, newRemaining) = dealNewRoundDeck(finalPlayers)

                val startingPlayerIndex = (currentState.starterPlayerIndex + 1) % 4
                val starterName = newResetPlayers[startingPlayerIndex].name
                val statusMsg = if (startingPlayerIndex == 0) "KARTEN NEU GEMISCHT - DU FÄNGST AN!" else "NEU GEMISCHT - $starterName fängt an..."

                _state.update {
                    it.copy(
                        deck = newRemaining,
                        centerPile = newCenter,
                        players = newResetPlayers,
                        currentTurnIndex = startingPlayerIndex,
                        starterPlayerIndex = startingPlayerIndex,
                        lastCaptorIndex = -1,
                        roundNumber = nextRoundNum,
                        showRoundEndSummary = false,
                        statusMessage = statusMsg,
                        lastPistiMessage = pistiNotice,
                        lastScoreEvent = scoreAnimEvent ?: it.lastScoreEvent,
                        dealAnimTrigger = System.currentTimeMillis()
                    )
                }

                if (startingPlayerIndex != 0) {
                    viewModelScope.launch {
                        delay(900)
                        executeBotTurn(startingPlayerIndex)
                    }
                }
            }
        } else {
            val nextPlayerName = finalPlayers[nextTurn].name
            val statusMsg = if (dealTriggered) {
                if (nextTurn == 0) "NEUE HANDKARTEN AUSGETEILT! DU BIST AM ZUG." else "NEUE HANDKARTEN AUSGETEILT! $nextPlayerName überlegt..."
            } else {
                if (nextTurn == 0) "DU BIST AM ZUG" else "$nextPlayerName überlegt..."
            }

            _state.update {
                it.copy(
                    deck = finalDeck,
                    centerPile = finalCenterPile,
                    players = finalPlayers,
                    currentTurnIndex = nextTurn,
                    starterPlayerIndex = nextStarterIndex,
                    lastCaptorIndex = nextLastCaptor,
                    statusMessage = statusMsg,
                    lastPistiMessage = pistiNotice,
                    lastScoreEvent = scoreAnimEvent ?: it.lastScoreEvent,
                    dealAnimTrigger = if (dealTriggered) System.currentTimeMillis() else it.dealAnimTrigger
                )
            }

            // Trigger Bot Turn if next player is a bot
            if (!finalPlayers[nextTurn].isHuman) {
                viewModelScope.launch {
                    delay(900)
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

        if (centerPile.isEmpty()) {
            // Table is empty: bot plays a safe non-Jack, non-point card if available
            val safeCards = botHand.filter { card ->
                card.rank != Rank.JACK &&
                card.rank != Rank.ACE &&
                !(card.suit == Suit.DIAMONDS && card.rank == Rank.TEN) &&
                !(card.suit == Suit.CLUBS && card.rank == Rank.TWO)
            }
            cardToPlay = safeCards.shuffled().firstOrNull()
                ?: botHand.filter { it.rank != Rank.JACK }.shuffled().firstOrNull()
                ?: botHand.first()
        } else if (centerPile.size == 1 && topCard != null) {
            // 1 Card on table (Pişti opportunity for human player):
            val matchingCard = botHand.find { it.rank == topCard.rank }
            val jackCard = botHand.find { it.rank == Rank.JACK }

            // Natural AI behavior: bots don't ruthlessly snatch every Pişti opportunity
            // Plato (1): 20%, Euklid (2): 25%, Sokrates (3): 15%
            val pistiProbability = when (botIndex) {
                1 -> 0.20f
                2 -> 0.25f
                else -> 0.15f
            }

            val takePisti = matchingCard != null && (kotlin.random.Random.nextFloat() < pistiProbability)
            val useJackOnHighCard = jackCard != null && (topCard.rank == Rank.ACE || (topCard.suit == Suit.DIAMONDS && topCard.rank == Rank.TEN)) && (kotlin.random.Random.nextFloat() < 0.20f)

            if (takePisti) {
                cardToPlay = matchingCard
            } else if (useJackOnHighCard) {
                cardToPlay = jackCard
            } else {
                // Play a safe non-matching, non-Jack card to leave table pile open for human player
                val safeCards = botHand.filter { it.rank != Rank.JACK && it.rank != topCard.rank }
                cardToPlay = safeCards.shuffled().firstOrNull()
                    ?: botHand.filter { it.rank != Rank.JACK }.shuffled().firstOrNull()
                    ?: botHand.first()
            }
        } else {
            // 2+ Cards on table (Capture opportunity):
            val topCardRank = topCard?.rank
            val matchingCard = botHand.find { it.rank == topCardRank }
            val jackCard = botHand.find { it.rank == Rank.JACK }

            val hasHighPoints = centerPile.any {
                it.rank == Rank.ACE ||
                (it.suit == Suit.DIAMONDS && it.rank == Rank.TEN) ||
                (it.suit == Suit.CLUBS && it.rank == Rank.TWO)
            }

            if (matchingCard != null) {
                cardToPlay = matchingCard
            } else if (jackCard != null && (centerPile.size >= 3 || hasHighPoints)) {
                // Bots save Jacks for valuable or large table piles (3+ cards)
                cardToPlay = jackCard
            } else {
                val safeCards = botHand.filter { it.rank != Rank.JACK }
                cardToPlay = safeCards.shuffled().firstOrNull() ?: botHand.first()
            }
        }

        playCard(cardToPlay, botIndex)
    }
}
