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

    private var webSocket: WebSocket? = null
    private var pollJob: Job? = null

    private val _onlineState = MutableStateFlow(OnlineGameStateDto())
    val onlineState: StateFlow<OnlineGameStateDto> = _onlineState.asStateFlow()

    private val _myPlayerId = MutableStateFlow<Int>(0) // 0 = Host, 1 = Guest
    val myPlayerId: StateFlow<Int> = _myPlayerId.asStateFlow()

    private val _connectionStatus = MutableStateFlow("Bereit")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private var isHost = false
    private var activeRoomCode = ""
    private var myName = "Spieler"
    private var currentVersion: Long = 0L

    fun createRoom(hostName: String): String {
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
        _connectionStatus.value = "Raum $code erstellt"

        connectWebSocket(code)
        startHttpPolling(code)
        return code
    }

    fun joinRoom(code: String, guestName: String, onResult: (Boolean, String) -> Unit) {
        val cleanCode = code.trim()
        if (cleanCode.length != 4) {
            onResult(false, "Bitte 4-stellige Raumnummer eingeben")
            return
        }

        activeRoomCode = cleanCode
        isHost = false
        myName = guestName
        _myPlayerId.value = 1

        _connectionStatus.value = "Verbinde mit Raum $cleanCode..."

        connectWebSocket(cleanCode)
        startHttpPolling(cleanCode)

        // Send JOIN message instantly and repeat to guarantee instant pairing
        scope.launch {
            val joinMsg = OnlineNetworkMessage(
                type = "JOIN_ROOM",
                senderId = 1,
                roomCode = cleanCode,
                stateJson = guestName
            )
            repeat(4) {
                sendNetworkMessage(joinMsg)
                delay(200)
            }
        }
        onResult(true, "Verbindung zum Raum wird aufgebaut...")
    }

    private fun connectWebSocket(roomCode: String) {
        webSocket?.close(1000, "Neuer Raum")
        val request = Request.Builder()
            .url("https://ntfy.sh/pisti_room_$roomCode/ws?since=1m")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d("OnlineManager", "WebSocket Verbunden: $roomCode")
                _connectionStatus.value = "Online Verbunden"
            }

            override fun onMessage(ws: WebSocket, text: String) {
                handleIncomingMessageText(text)
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e("OnlineManager", "WebSocket Fehler: ${t.message}")
                _connectionStatus.value = "Fallback Active"
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.d("OnlineManager", "WebSocket Geschlossen: $reason")
            }
        })
    }

    private fun startHttpPolling(roomCode: String) {
        pollJob?.cancel()
        pollJob = scope.launch {
            while (isActive) {
                try {
                    val req = Request.Builder()
                        .url("https://ntfy.sh/pisti_room_$roomCode/json?since=10s")
                        .build()
                    client.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val bodyText = resp.body?.string() ?: ""
                            bodyText.lineSequence().forEach { line ->
                                if (line.isNotBlank()) {
                                    handleIncomingMessageText(line)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("OnlineManager", "Polling Error: ${e.message}")
                }
                delay(1200L) // Lightweight 1.2s fallback check
            }
        }
    }

    private fun handleIncomingMessageText(text: String) {
        try {
            var rawMsg = text
            if (text.contains("\"message\":")) {
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
                        }
                    }
                }
                "SYNC_STATE" -> {
                    if (msg.senderId != _myPlayerId.value) {
                        val newGameState = stateAdapter.fromJson(msg.stateJson)
                        if (newGameState != null && newGameState.stateVersion > _onlineState.value.stateVersion) {
                            _onlineState.value = newGameState
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
        val jsonStr = messageAdapter.toJson(msg)
        try {
            // Try sending over WebSocket if open
            webSocket?.send(jsonStr)

            // Publish asynchronously via HTTP POST so UI thread and coroutine never block
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
            // Apply optimistic card play locally on Guest device so UI updates instantly (0ms lag!)
            optimisticGuestCardPlay(cardIndex)

            val playMsg = OnlineNetworkMessage(
                type = "PLAY_CARD",
                senderId = 1,
                roomCode = activeRoomCode,
                cardIndex = cardIndex
            )
            sendNetworkMessage(playMsg)
            // Burst send to ensure zero loss
            scope.launch {
                delay(100)
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

        val hostName = currentState.hostName
        currentVersion++

        val optimisticState = currentState.copy(
            stateVersion = currentVersion,
            deck = finalDeck,
            centerPile = newCenterPile,
            players = finalPlayers,
            currentTurnIndex = 0, // Now Host turn
            statusMessage = if (dealTriggered) "NEUE HANDKARTEN! $hostName IST AM ZUG" else "$hostName IST AM ZUG",
            lastPistiMessage = pistiNotice,
            dealAnimTrigger = if (dealTriggered) System.currentTimeMillis() else 0L
        )

        _onlineState.value = optimisticState
    }

    private fun startNewOnlineGame(guestName: String) {
        val fullDeck = createShuffledDeck()
        val center = fullDeck.take(4).mapIndexed { idx, card ->
            card.copy(isFaceUp = idx == 3) // Only 4th card face up
        }
        val remainingAfterCenter = fullDeck.drop(4)

        val hostHand = remainingAfterCenter.take(4)
        val guestHand = remainingAfterCenter.drop(4).take(4)
        val remainingDeck = remainingAfterCenter.drop(8)

        currentVersion = 1L

        val newState = OnlineGameStateDto(
            stateVersion = currentVersion,
            roomCode = activeRoomCode,
            hostName = myName,
            guestName = guestName,
            centerPile = center,
            deck = remainingDeck,
            players = listOf(
                OnlinePlayerDto(0, myName, hand = hostHand),
                OnlinePlayerDto(1, guestName, hand = guestHand)
            ),
            currentTurnIndex = 0,
            starterPlayerIndex = 0,
            roundNumber = 1,
            statusMessage = "SPIEL GESTARTET! $myName IST AM ZUG",
            isGameStarted = true,
            isGuestConnected = true,
            dealAnimTrigger = System.currentTimeMillis()
        )

        _onlineState.value = newState
        broadcastState(newState)
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

        // Check sub-hands deal
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

        // Determine next turn
        var nextTurn = (playerIndex + 1) % 2
        var nextStarterIndex = currentState.starterPlayerIndex
        var nextLastCaptor = newLastCaptor

        if (dealTriggered) {
            if (newLastCaptor in 0..1) {
                nextTurn = newLastCaptor
                nextStarterIndex = newLastCaptor
                nextLastCaptor = -1
            } else {
                nextStarterIndex = (currentState.starterPlayerIndex + 1) % 2
                nextTurn = nextStarterIndex
            }
        }

        val isRoundOver = finalPlayers.all { it.hand.isEmpty() } && finalDeck.isEmpty()

        currentVersion++

        if (isRoundOver) {
            // Calculate final round scores including majority cards (+3)
            val p0 = finalPlayers[0]
            val p1 = finalPlayers[1]

            val p0Extra = if (p0.capturedCount > p1.capturedCount) 3 else 0
            val p1Extra = if (p1.capturedCount > p0.capturedCount) 3 else 0

            val ratedPlayers = finalPlayers.map { p ->
                val extra = if (p.id == 0) p0Extra else p1Extra
                p.copy(
                    roundScore = p.roundScore + extra,
                    totalScore = p.totalScore + p.roundScore + extra
                )
            }

            val highestTotal = ratedPlayers.maxOf { it.totalScore }
            val matchOver = highestTotal >= currentState.targetScore
            val winner = if (matchOver) ratedPlayers.maxByOrNull { it.totalScore }?.name else null

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
                lastCaptorIndex = nextLastCaptor,
                statusMessage = statusMsg,
                lastPistiMessage = pistiNotice,
                lastScorePoints = if (captured) (newRoundScore - player.roundScore) else 0,
                lastScorePlayerName = if (captured) player.name else null,
                isGameStarted = true,
                dealAnimTrigger = if (dealTriggered) System.currentTimeMillis() else 0L
            )

            _onlineState.value = updatedState
            broadcastState(updatedState)
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

    fun resetLobby() {
        webSocket?.close(1000, "Reset")
        webSocket = null
        pollJob?.cancel()
        pollJob = null
        activeRoomCode = ""
        isHost = false
        _onlineState.value = OnlineGameStateDto()
    }

    fun leaveRoom() {
        sendNetworkMessage(
            OnlineNetworkMessage(
                type = "LEAVE_ROOM",
                senderId = _myPlayerId.value,
                roomCode = activeRoomCode
            )
        )
        webSocket?.close(1000, "User Left")
        pollJob?.cancel()
        _onlineState.value = OnlineGameStateDto()
        activeRoomCode = ""
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
