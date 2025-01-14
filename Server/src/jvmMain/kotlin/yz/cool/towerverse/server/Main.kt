package yz.cool.towerverse.server

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.html.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import yz.cool.towerverse.official.loadOfficialContent
import yz.cool.towerverse.simulation.GameSimulation
import yz.cool.towerverse.simulation.VerificationFailedException
import yz.cool.towerverse.simulation.verification.GameSimulationVerificationPayload

data class GameResult(
    val playerName: String,
    val modList: List<String>,
    val score: Long
)

/**
 * A simple server that can verify game results sent from clients by re-running the game simulation and checking its
 * outcome against that sent by the client. This can be done with full consistency even in areas such as
 * random number generation thanks to Kotlin Multiplatform.
 *
 * It also stores summaries of game results sent by clients in-memory and can display a leaderboard of all game results
 * played using the vanilla game. Since we verify client game results, this leaderboard is totally resistant to
 * cheaters that try to submit fake results.
 *
 * However, the server is currently only a very simple proof of concept due to time constraints.
 * For example, it does not have a pretty web UI nor store game results in a database.
 * Also, while the server can ensure that no fake game results are ever added to the scoreboard,
 * it does not prevent other attacks such as replay and DoS attacks that can, for example,
 * submit the same real game result multiple times or abuse the server's relatively high CPU cost for verification
 * to crash the server. Ideas that could prevent these attacks in real-world games are written in the project's README.
 *
 * Go to http://localhost:8079 to see the leaderboard.
 */
fun main() {
    // List of all game results that have been successfully verified
    val allGameResults = ArrayList<GameResult>()

    val officialContent = loadOfficialContent()

    embeddedServer(Netty, port = 8079, host = "0.0.0.0") {
        // Allow the client running on a different origin to call this server
        install(CORS) {
            allowMethod(HttpMethod.Post)
            allowMethod(HttpMethod.Get)
            allowHeader(HttpHeaders.AccessControlAllowOrigin)
            allowHeader(HttpHeaders.ContentType)
            anyHost()
        }
        routing {
            post("/result") {
                val json = call.receiveText()
                val payload = Json.decodeFromString<GameSimulationVerificationPayload>(json)

                val simulation = GameSimulation.createSimulation(
                    officialGameObjects = officialContent,
                    mods = payload.mods,
                    isVerificationMode = true,
                    onVictory = {
                        throw VictoryException()
                    }
                )
                // Add all game actions that the client performed so that the server can replay them
                simulation.actions.putAll(payload.actions)
                try {
                    var startTime = System.currentTimeMillis()
                    while(true) {
                        simulation.gameTick()
                        /*
                        Limit the verification to 30 seconds.
                        Note that matches in the vanilla game (with 5 waves) can typically be verified
                        in well under a second.
                         */
                        if(System.currentTimeMillis() - startTime > 30000) {
                            call.respondText(
                                "Game verification timed out", status = HttpStatusCode.BadRequest)
                        }
                    }
                }
                catch(ex: VictoryException) {
                    // Check game end hash
                    if(payload.gameEndStateHash != simulation.getGameStateHash()) {
                        call.respondText("Game end state hash mismatch. Server score: ${simulation.score}",
                            status = HttpStatusCode.BadRequest)
                        println("Game end state mismatch. Server end state:")
                        println(Json.encodeToString(simulation.state))
                    }
                    else {
                        // Successfully verified the game result
                        allGameResults += GameResult(
                            payload.playerName,
                            payload.mods.map{it.modInformation.name},
                            simulation.score
                        )

                        println("Successfully verified game result, all results: $allGameResults")
                        call.respondText("Your result has been verified!", status = HttpStatusCode.OK)
                    }
                }
                catch(ex: VerificationFailedException) {
                    println("Verification failed: $ex")
                    call.respondText("Verification Failed at Wave ${
                        simulation.enemySpawner.waveNumber} due to: ${
                        ex.message}", status = HttpStatusCode.InternalServerError)
                }
                catch(ex: Exception) {
                    println("Exception when verifying game result: $ex")
                    ex.printStackTrace()
                    call.respondText("Internal Server Error", status = HttpStatusCode.InternalServerError)
                }
            }
            get("/") {
                // Display a HTML page with all game results
                call.respondHtml {
                    head {
                        style {
                            +"""
                                html, body {
                                    margin: 0 auto;
                                    max-width: 800px;
                                    font-family: sans-serif;
                                }
                            """.trimIndent()
                        }
                    }
                    body {
                        h1 {
                            +"Towerverse Game Verification Server"
                        }
                        h2 {
                            +"Verified Game Results"
                        }
                        ul {
                            allGameResults.forEach {
                                li {
                                    +"Player: ${it.playerName} | Score: ${it.score}"
                                    if(it.modList.isNotEmpty()) +" with mods: ${it.modList.joinToString(", ")}"
                                    else +" with no mods"
                                }
                            }
                            if(allGameResults.isEmpty()) {
                                li {
                                    +("No results yet. Win a match with the game client, " +
                                            "send the result to the server, and refresh the page!")
                                }
                            }
                        }
                        h2 {
                            +"Leaderboard"
                        }
                        ul {
                            val vanillaGameResults = allGameResults.filter { it.modList.isEmpty() }
                                .sortedByDescending{it.score}
                            vanillaGameResults.forEach {
                                li {
                                    +"Player: ${it.playerName} | Score: ${it.score}"
                                }
                            }
                            if(vanillaGameResults.isEmpty()) {
                                li {
                                    +("No results yet. Win a match with no mods loaded to appear on the leaderboard!")
                                }
                            }
                        }
                        h2 {
                            +"Server Information"
                        }
                        p {
                            +"""
                                A simple server that can verify game results sent from clients by re-running the game simulation and checking its
                                outcome against that sent by the client. This can be done with full consistency even in areas such as
                                random number generation thanks to Kotlin Multiplatform.
                            """.trimIndent()
                        }
                        p {
                            +"""
                                It also stores summaries of game results sent by clients in-memory and can display a leaderboard of all game results
                                played using the vanilla game. Since we verify client game results, this leaderboard is totally resistant to
                                cheaters that try to submit fake results.
                            """.trimIndent()
                        }
                        p {
                            +"""
                                However, the server is currently only a very simple proof of concept due to time constraints.
                                For example, it does not have a pretty web UI nor store game results in a database.
                                Also, while the server can ensure that no fake game results are ever added to the scoreboard,
                                it does not prevent other attacks such as replay and DoS attacks that can, for example,
                                submit the same real game result multiple times or abuse the server's relatively high CPU cost for verification
                                to crash the server. Ideas that could prevent these attacks in real-world games are written in the project's README.
                            """.trimIndent()
                        }
                    }
                }
            }
        }
    }.start(wait = true)
}