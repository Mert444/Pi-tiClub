package com.example

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.random.Random

data class OnlineCardDto(
    val suit: String,
    val rank: String,
    val isFaceUp: Boolean = true
)

data class OnlinePlayerDto(
    val id: Int, // 0 = Host, 1 = Guest
    val name: String,
    val hand: List<OnlineCardDto> = emptyList(),
    val capturedCount: Int = 0,
    val roundScore: Int = 0,
    val totalScore: Int = 0
)

data class OnlineGameStateDto(
    val stateVersion: Long = 0L,
    val roomCode: String = "",
    val hostName: String = "Host",
    val guestName: String? = null,
    val centerPile: List<OnlineCardDto> = emptyList(),
    val deck: List<OnlineCardDto> = emptyList(),
    val players: List<OnlinePlayerDto> = emptyList(),
    val currentTurnIndex: Int = 0, // 0 = Host, 1 = Guest
    val starterPlayerIndex: Int = 0,
    val lastCaptorIndex: Int = -1,
    val roundNumber: Int = 1,
    val targetScore: Int = 101,
    val isMatchOver: Boolean = false,
    val matchWinnerName: String? = null,
    val statusMessage: String = "Warte auf Mitspieler...",
    val lastPistiMessage: String? = null,
    val isGameStarted: Boolean = false,
    val isGuestConnected: Boolean = false,
    val showRoundEndSummary: Boolean = false,
    val lastScorePoints: Int = 0,
    val lastScorePlayerName: String? = null,
    val dealAnimTrigger: Long = 0L
)

data class OnlineNetworkMessage(
    val type: String, // "JOIN_ROOM", "SYNC_STATE", "PLAY_CARD", "LEAVE_ROOM"
    val senderId: Int, // 0 = Host, 1 = Guest
    val roomCode: String,
    val cardIndex: Int = -1,
    val stateJson: String = ""
)

class OnlineManager {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val messageAdapter = moshi.adapter(OnlineNetworkMessage::class.java)
    private val stateAdapter = moshi.adapter(OnlineGameStateDto::class.java)

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private var streamJob: Job? = null
    private var pollJob: Job? = null

    private val _onlineState = MutableStateFlow(OnlineGameStateDto())
    val onlineState: StateFlow<OnlineGameStateDto> = _onlineState.asStateFlow()

    private val _myPlayerId = MutableStateFlow<Int>(0) // 0 = Host, 1 = Guest
    val myPlayerId: StateFlow<Int> = _myPlayerId.asStateFlow()

    private val _connectionStatus = MutableStateFlow("Bereit")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private val _isJoining = MutableStateFlow(false)
    val isJoining: StateFlow<Boolean> = _isJoining.asStateFlow()

    private var isHost = false
    private var activeRoomCode = ""
    private var myName = "Spieler"
    private var currentVersion: Long = 0L

    private var joinJob: Job? = null

    fun createRoom(hostName: String): String {
        resetLobby()
        val code = (1000..9999).random().toString()
        activeRoomCode = code
        isHost = true
        myName = hostName
        _myPlayerId.value = 0

        val initialState = OnlineGameStateDto(
            roomCode = code,
            hostName = hostName,
            statusMessage = "RAUM $code ERSTELLT! WARTE AUF FREUND...",
            players = listOf(
                OnlinePlayerDto(0, hostName),
                OnlinePlayerDto(1, "Warte...")
            )
        )
        _onlineState.value = initialState
        _connectionStatus.value = "Raum $code aktiv – Warte auf Mitspieler"

        startRawStream(code)
        startSafetyPoller(code)
        return code
    }

    fun joinRoom(code: String, guestName: String, onResult: (Boolean, String) -> Unit) {
        val cleanCode = code.trim().filter { it.isDigit() }
        if (cleanCode.length != 4) {
            onResult(false, "Bitte 4-stellige Raumnummer eingeben (z.B. 4829)")
            return
        }

        activeRoomCode = cleanCode
        isHost = false
        myName = guestName
        _myPlayerId.value = 1
        _isJoining.value = true

        _connectionStatus.value = "Verbinde mit Raum $cleanCode..."
        _onlineState.value = OnlineGameStateDto(
            roomCode = cleanCode,
            hostName = "Warte auf Host...",
            statusMessage = "VERBINDE MIT RAUM $cleanCode...",
            players = listOf(
                OnlinePlayerDto(0, "Host (Freund)"),
                OnlinePlayerDto(1, guestName)
            ),
            isGameStarted = false
        )

        startRawStream(cleanCode)
        startSafetyPoller(cleanCode)

        joinJob?.cancel()
        joinJob = scope.launch(Dispatchers.IO) {
            val joinMsg = OnlineNetworkMessage(
                type = "JOIN_ROOM",
                senderId = 1,
                roomCode = cleanCode,
                stateJson = guestName
            )
            var attempt = 0
            while (isActive && !_onlineState.value.isGameStarted) {
                attempt++
                _connectionStatus.value = "Suche Freund in Raum $cleanCode... ($attempt)"
                sendNetworkMessage(joinMsg)
                delay(1000L)
            }
        }
        onResult(true, "Verbindungsanfrage an Raum $cleanCode gesendet!")
    }

    fun cancelJoin() {
        joinJob?.cancel()
        joinJob = null
        _isJoining.value = false
        resetLobby()
    }

    private fun startRawStream(roomCode: String) {
        streamJob?.cancel()
        streamJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val req = Request.Builder()
                        .url("https://ntfy.sh/pisti_room_$roomCode/raw")
                        .build()
                    client.newCall(req).execute().use { response ->
                        if (response.isSuccessful) {
                            _connectionStatus.value = "Online Verbunden"
                            val source = response.body?.source() ?: return@use
                            while (!source.exhausted() && isActive) {
                                val line = source.readUtf8Line() ?: break
                                val trimmed = line.trim()
                                if (trimmed.isNotEmpty()) {
                                    handleIncomingMessageText(trimmed)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("OnlineManager", "Stream error: ${e.message}")
                }
                delay(1000L) // Reconnect after 1 second if closed
            }
        }
    }

    private fun startSafetyPoller(roomCode: String) {
        pollJob?.cancel()
        pollJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val req = Request.Builder()
                        .url("https://ntfy.sh/pisti_room_$roomCode/json?poll=1&since=8s")
                        .build()
                    client.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val bodyText = resp.body?.string() ?: ""
                            bodyText.lineSequence().forEach { line ->
                                val trimmed = line.trim()
                                if (trimmed.isNotEmpty()) {
                                    handleIncomingMessageText(trimmed)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("OnlineManager", "Polling Error: ${e.message}")
                }
                delay(1000L) // Non-blocking 1.0s redundant sync
            }
        }
    }

    private fun handleIncomingMessageText(text: String) {
        try {
            var rawMsg = text
            if (text.contains("\"event\":")) {
                val mapAdapter = moshi.adapter(Map::class.java)
                val map = mapAdapter.fromJson(text)
                val event = map?.get("event") as? String
                if (event == "open" || event == "keepalive") return
                rawMsg = map?.get("message") as? String ?: text
            }

            val msg = messageAdapter.fromJson(rawMsg) ?: return
            if (msg.roomCode != activeRoomCode) return

            when (msg.type) {
                "JOIN_ROOM" -> {
                    if (isHost && msg.senderId == 1) {
                        val guestName = if (msg.stateJson.isNotBlank()) msg.stateJson else "Freund"
                        if (!_onlineState.value.isGameStarted) {
                            startNewOnlineGame(guestName)
                        } else {
                            // Re-broadcast state so guest connects immediately
                            broadcastState(_onlineState.value)
                        }
                    }
                }
                "SYNC_STATE" -> {
                    if (msg.senderId != _myPlayerId.value) {
                        val newGameState = stateAdapter.fromJson(msg.stateJson)
                        if (newGameState != null) {
                            if (!isHost || newGameState.stateVersion > _onlineState.value.stateVersion) {
                                _onlineState.value = newGameState
                                if (newGameState.isGameStarted) {
                                    _isJoining.value = false
                                    joinJob?.cancel()
                                    joinJob = null
                                    _connectionStatus.value = "Verbunden – Spiel läuft!"
                                }
                            }
                        }
                    }
                }
                "PLAY_CARD" -> {
                    if (isHost && msg.senderId == 1) {
                        if (_onlineState.value.currentTurnIndex == 1) {
                            processOnlineCardPlay(1, msg.cardIndex)
                        }
                    }
                }
                "LEAVE_ROOM" -> {
                    if (msg.senderId != _myPlayerId.value) {
                        _onlineState.update {
                            it.copy(
                                statusMessage = "DEIN MITSPIELER HAT DEN RAUM VERLASSEN",
                                isGameStarted = false
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("OnlineManager", "Message parse error: ${e.message}")
        }
    }

    private fun sendNetworkMessage(msg: OnlineNetworkMessage) {
        val jsonStr = messageAdapter.toJson(msg).replace("\n", " ").replace("\r", "")
        try {
            val body = jsonStr.toRequestBody("text/plain".toMediaType())
            val req = Request.Builder()
                .url("https://ntfy.sh/pisti_room_${msg.roomCode}")
                .post(body)
                .build()

            client.newCall(req).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("OnlineManager", "Async send failed: ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        Log.e("OnlineManager", "POST returned HTTP ${response.code}")
                    }
                    response.close()
                }
            })
        } catch (e: Exception) {
            Log.e("OnlineManager", "Send error: ${e.message}")
        }
    }

    fun playCard(cardIndex: Int) {
        val currentState = _onlineState.value
        val myId = _myPlayerId.value

        if (!currentState.isGameStarted || currentState.isMatchOver) return
        if (currentState.currentTurnIndex != myId) return

        if (isHost) {
            processOnlineCardPlay(0, cardIndex)
        } else {
            // Apply optimistic card play locally on Guest device so UI updates with 0ms lag
            optimisticGuestCardPlay(cardIndex)

            val playMsg = OnlineNetworkMessage(
                type = "PLAY_CARD",
                senderId = 1,
                roomCode = activeRoomCode,
                cardIndex = cardIndex
            )
            sendNetworkMessage(playMsg)
            // Burst send to ensure delivery
            scope.launch {
                delay(120)
                sendNetworkMessage(playMsg)
            }
        }
    }

    private fun optimisticGuestCardPlay(cardIndex: Int) {
        val currentState = _onlineState.value
        val guest = currentState.players.getOrNull(1) ?: return
        if (cardIndex !in guest.hand.indices) return

        val playedCard = guest.hand[cardIndex]
        val newHand = guest.hand.filterIndexed { idx, _ -> idx != cardIndex }

        val oldCenter = currentState.centerPile
        val topCard = oldCenter.lastOrNull()

        var captured = false
        var isPisti = false
        var pistiPoints = 0
        var newCenterPile = oldCenter + playedCard.copy(isFaceUp = true)

        if (topCard != null) {
            if (playedCard.rank == topCard.rank) {
                captured = true
                if (oldCenter.size == 1) {
                    isPisti = true
                    pistiPoints = if (playedCard.rank == "JACK") 20 else 10
                }
            } else if (playedCard.rank == "JACK") {
                captured = true
            }
        }

        var newCapturedCount = guest.capturedCount
        var newRoundScore = guest.roundScore
        var pistiNotice: String? = null

        if (captured) {
            newCapturedCount += newCenterPile.size
            val cardPoints = newCenterPile.sumOf { getCardPoints(it) }
            newRoundScore += cardPoints + pistiPoints
            if (isPisti) {
                pistiNotice = "🔥 PIŞTI (+${pistiPoints} P) FÜR ${guest.name.uppercase()}!"
            }
            newCenterPile = emptyList()
        }

        val updatedGuest = guest.copy(
            hand = newHand,
            capturedCount = newCapturedCount,
            roundScore = newRoundScore
        )

        val updatedPlayers = currentState.players.map {
            if (it.id == 1) updatedGuest else it
        }

        val hostName = currentState.hostName

        val optimisticState = currentState.copy(
            stateVersion = currentState.stateVersion,
            centerPile = newCenterPile,
            players = updatedPlayers,
            currentTurnIndex = 0, // Now Host turn
            statusMessage = "$hostName IST AM ZUG...",
            lastPistiMessage = pistiNotice
        )

        _onlineState.value = optimisticState
    }

    private fun startNewOnlineGame(guestName: String) {
        val fullDeck = createShuffledDeck()

        // 4 center cards (3 face-down, 1 face-up on top)
        val center = fullDeck.take(4).mapIndexed { idx, card ->
            card.copy(isFaceUp = idx == 3)
        }
        val remainingAfterCenter = fullDeck.drop(4)

        // Deal 4 to host (0) and 4 to guest (1)
        val hostHand = remainingAfterCenter.take(4)
        val guestHand = remainingAfterCenter.drop(4).take(4)
        val remainingDeck = remainingAfterCenter.drop(8) // 40 cards left in draw stock

        currentVersion = 1L

        val hostPlayer = OnlinePlayerDto(0, myName, hostHand, 0, 0, 0)
        val guestPlayer = OnlinePlayerDto(1, guestName, guestHand, 0, 0, 0)

        val newState = OnlineGameStateDto(
            stateVersion = currentVersion,
            roomCode = activeRoomCode,
            hostName = myName,
            guestName = guestName,
            centerPile = center,
            deck = remainingDeck,
            players = listOf(hostPlayer, guestPlayer),
            currentTurnIndex = 0,
            starterPlayerIndex = 0,
            lastCaptorIndex = -1,
            roundNumber = 1,
            targetScore = 101,
            isMatchOver = false,
            matchWinnerName = null,
            statusMessage = "SPIEL GESTARTET! $myName IST AM ZUG",
            lastPistiMessage = null,
            isGameStarted = true,
            isGuestConnected = true,
            showRoundEndSummary = false,
            dealAnimTrigger = System.currentTimeMillis()
        )

        _onlineState.value = newState
        broadcastState(newState)
        // Repeat to guarantee guest receives game start
        scope.launch {
            delay(150)
            broadcastState(newState)
        }
    }

    private fun processOnlineCardPlay(playerIndex: Int, cardIndex: Int) {
        val currentState = _onlineState.value
        val player = currentState.players.getOrNull(playerIndex) ?: return
        if (cardIndex !in player.hand.indices) return

        val playedCard = player.hand[cardIndex]
        val newHand = player.hand.filterIndexed { idx, _ -> idx != cardIndex }

        val oldCenter = currentState.centerPile
        val topCard = oldCenter.lastOrNull()

        var captured = false
        var isPisti = false
        var pistiPoints = 0
        var newCenterPile = oldCenter + playedCard.copy(isFaceUp = true)

        if (topCard != null) {
            if (playedCard.rank == topCard.rank) {
                captured = true
                if (oldCenter.size == 1) {
                    isPisti = true
                    pistiPoints = if (playedCard.rank == "JACK") 20 else 10
                }
            } else if (playedCard.rank == "JACK") {
                captured = true
            }
        }

        var newCapturedCount = player.capturedCount
        var newRoundScore = player.roundScore
        var pistiNotice: String? = null

        if (captured) {
            newCapturedCount += newCenterPile.size
            val cardPoints = newCenterPile.sumOf { getCardPoints(it) }
            newRoundScore += cardPoints + pistiPoints
            if (isPisti) {
                pistiNotice = "🔥 PIŞTI (+${pistiPoints} P) FÜR ${player.name.uppercase()}!"
            }
            newCenterPile = emptyList()
        }

        val updatedPlayer = player.copy(
            hand = newHand,
            capturedCount = newCapturedCount,
            roundScore = newRoundScore
        )

        val updatedPlayers = currentState.players.map {
            if (it.id == playerIndex) updatedPlayer else it
        }

        val newLastCaptor = if (captured) playerIndex else currentState.lastCaptorIndex

        // Check if both hands are empty
        val allHandsEmpty = updatedPlayers.all { it.hand.isEmpty() }
        var finalDeck = currentState.deck
        var finalPlayers = updatedPlayers
        var dealTriggered = false

        if (allHandsEmpty && finalDeck.isNotEmpty()) {
            val p0Hand = finalDeck.take(4)
            val p1Hand = finalDeck.drop(4).take(4)
            finalDeck = finalDeck.drop(8)

            finalPlayers = updatedPlayers.map {
                when (it.id) {
                    0 -> it.copy(hand = p0Hand)
                    1 -> it.copy(hand = p1Hand)
                    else -> it
                }
            }
            dealTriggered = true
        }

        val isRoundOver = allHandsEmpty && finalDeck.isEmpty()

        val nextTurn = if (isRoundOver) {
            0
        } else if (dealTriggered) {
            if (newLastCaptor in 0..1) newLastCaptor else (playerIndex + 1) % 2
        } else {
            (playerIndex + 1) % 2
        }

        val nextStarterIndex = currentState.starterPlayerIndex

        currentVersion++

        if (isRoundOver) {
            var roundFinalPlayers = finalPlayers
            if (newLastCaptor in 0..1 && newCenterPile.isNotEmpty()) {
                val tablePoints = newCenterPile.sumOf { getCardPoints(it) }
                roundFinalPlayers = roundFinalPlayers.map {
                    if (it.id == newLastCaptor) {
                        it.copy(
                            capturedCount = it.capturedCount + newCenterPile.size,
                            roundScore = it.roundScore + tablePoints
                        )
                    } else it
                }
                newCenterPile = emptyList()
            }

            // Award +3 for most cards
            val p0Count = roundFinalPlayers[0].capturedCount
            val p1Count = roundFinalPlayers[1].capturedCount
            var p0Bonus = 0
            var p1Bonus = 0
            if (p0Count > p1Count) {
                p0Bonus = 3
            } else if (p1Count > p0Count) {
                p1Bonus = 3
            }

            val ratedPlayers = roundFinalPlayers.map { p ->
                val bonus = if (p.id == 0) p0Bonus else p1Bonus
                val totalWithBonus = p.roundScore + bonus
                p.copy(
                    roundScore = totalWithBonus,
                    totalScore = p.totalScore + totalWithBonus
                )
            }

            val maxTotal = ratedPlayers.maxOf { it.totalScore }
            val matchOver = maxTotal >= currentState.targetScore
            val winner = if (matchOver) {
                ratedPlayers.maxByOrNull { it.totalScore }?.name
            } else null

            val finalState = currentState.copy(
                stateVersion = currentVersion,
                deck = emptyList(),
                centerPile = newCenterPile,
                players = ratedPlayers,
                currentTurnIndex = 0,
                lastCaptorIndex = newLastCaptor,
                isMatchOver = matchOver,
                matchWinnerName = winner,
                showRoundEndSummary = true,
                statusMessage = if (matchOver) "MATCH BEENDET! GEWINNER: $winner" else "RUNDE ${currentState.roundNumber} BEENDET!",
                lastPistiMessage = pistiNotice,
                isGameStarted = true
            )
            _onlineState.value = finalState
            broadcastState(finalState)
            scope.launch {
                delay(150)
                broadcastState(finalState)
            }
        } else {
            val nextPlayerName = finalPlayers[nextTurn].name
            val statusMsg = if (dealTriggered) {
                "NEUE HANDKARTEN! $nextPlayerName IST AM ZUG"
            } else {
                "$nextPlayerName IST AM ZUG"
            }

            val updatedState = currentState.copy(
                stateVersion = currentVersion,
                deck = finalDeck,
                centerPile = newCenterPile,
                players = finalPlayers,
                currentTurnIndex = nextTurn,
                starterPlayerIndex = nextStarterIndex,
                lastCaptorIndex = newLastCaptor,
                statusMessage = statusMsg,
                lastPistiMessage = pistiNotice,
                lastScorePoints = if (captured) (newRoundScore - player.roundScore) else 0,
                lastScorePlayerName = if (captured) player.name else null,
                isGameStarted = true,
                dealAnimTrigger = if (dealTriggered) System.currentTimeMillis() else 0L
            )

            _onlineState.value = updatedState
            broadcastState(updatedState)
            scope.launch {
                delay(120)
                broadcastState(updatedState)
            }
        }
    }

    fun startNextRound() {
        val currentState = _onlineState.value
        val fullDeck = createShuffledDeck()
        val center = fullDeck.take(4).mapIndexed { idx, card ->
            card.copy(isFaceUp = idx == 3)
        }
        val remainingAfterCenter = fullDeck.drop(4)

        val hostHand = remainingAfterCenter.take(4)
        val guestHand = remainingAfterCenter.drop(4).take(4)
        val remainingDeck = remainingAfterCenter.drop(8)

        val nextStarter = (currentState.starterPlayerIndex + 1) % 2
        val resetPlayers = currentState.players.map { p ->
            val hand = if (p.id == 0) hostHand else guestHand
            p.copy(hand = hand, capturedCount = 0, roundScore = 0)
        }

        val starterName = resetPlayers[nextStarter].name

        currentVersion++

        val newState = currentState.copy(
            stateVersion = currentVersion,
            centerPile = center,
            deck = remainingDeck,
            players = resetPlayers,
            currentTurnIndex = nextStarter,
            starterPlayerIndex = nextStarter,
            lastCaptorIndex = -1,
            roundNumber = currentState.roundNumber + 1,
            showRoundEndSummary = false,
            statusMessage = "RUNDE ${currentState.roundNumber + 1}! $starterName IST AM ZUG",
            lastPistiMessage = null,
            dealAnimTrigger = System.currentTimeMillis()
        )

        _onlineState.value = newState
        broadcastState(newState)
        scope.launch {
            delay(150)
            broadcastState(newState)
        }
    }

    private fun broadcastState(state: OnlineGameStateDto) {
        val stateJson = stateAdapter.toJson(state)
        sendNetworkMessage(
            OnlineNetworkMessage(
                type = "SYNC_STATE",
                senderId = _myPlayerId.value,
                roomCode = activeRoomCode,
                stateJson = stateJson
            )
        )
    }

    fun resyncState() {
        val code = activeRoomCode
        if (code.isBlank()) return
        scope.launch(Dispatchers.IO) {
            try {
                val req = Request.Builder()
                    .url("https://ntfy.sh/pisti_room_$code/json?poll=1&since=12s")
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val bodyText = resp.body?.string() ?: ""
                        bodyText.lineSequence().forEach { line ->
                            val trimmed = line.trim()
                            if (trimmed.isNotEmpty()) {
                                handleIncomingMessageText(trimmed)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("OnlineManager", "Manual resync error: ${e.message}")
            }
        }
    }

    fun resetLobby() {
        joinJob?.cancel()
        joinJob = null
        _isJoining.value = false
        streamJob?.cancel()
        streamJob = null
        pollJob?.cancel()
        pollJob = null
        activeRoomCode = ""
        isHost = false
        _onlineState.value = OnlineGameStateDto()
        _connectionStatus.value = "Bereit"
    }

    fun leaveRoom() {
        joinJob?.cancel()
        joinJob = null
        _isJoining.value = false
        sendNetworkMessage(
            OnlineNetworkMessage(
                type = "LEAVE_ROOM",
                senderId = _myPlayerId.value,
                roomCode = activeRoomCode
            )
        )
        streamJob?.cancel()
        streamJob = null
        pollJob?.cancel()
        pollJob = null
        _onlineState.value = OnlineGameStateDto()
        activeRoomCode = ""
        _connectionStatus.value = "Bereit"
    }

    private fun getCardPoints(card: OnlineCardDto): Int {
        return when {
            card.rank == "ACE" -> 1
            card.rank == "JACK" -> 1
            card.suit == "DIAMONDS" && card.rank == "TEN" -> 3
            card.suit == "CLUBS" && card.rank == "TWO" -> 2
            else -> 0
        }
    }

    private fun createShuffledDeck(): List<OnlineCardDto> {
        val suits = listOf("HEARTS", "DIAMONDS", "CLUBS", "SPADES")
        val ranks = listOf("TWO", "THREE", "FOUR", "FIVE", "SIX", "SEVEN", "EIGHT", "NINE", "TEN", "JACK", "QUEEN", "KING", "ACE")

        val list = mutableListOf<OnlineCardDto>()
        for (s in suits) {
            for (r in ranks) {
                list.add(OnlineCardDto(s, r, isFaceUp = true))
            }
        }

        val secureRandom = java.security.SecureRandom()
        repeat(7) {
            list.shuffle(secureRandom)
        }

        val cutPoint = 12 + secureRandom.nextInt(28)
        val cutDeck = (list.drop(cutPoint) + list.take(cutPoint)).toMutableList()
        cutDeck.shuffle(secureRandom)

        // Ensure top card of center pile is not a Jack
        val centerCards = cutDeck.take(4).toMutableList()
        val remDeck = cutDeck.drop(4).toMutableList()

        if (centerCards.last().rank == "JACK") {
            val nonJackIndex = remDeck.indexOfFirst { it.rank != "JACK" }
            if (nonJackIndex != -1) {
                val temp = centerCards[3]
                centerCards[3] = remDeck[nonJackIndex]
                remDeck[nonJackIndex] = temp
            }
        }
        return centerCards + remDeck
    }
}
