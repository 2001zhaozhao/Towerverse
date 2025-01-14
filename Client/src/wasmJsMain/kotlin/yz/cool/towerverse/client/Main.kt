package yz.cool.towerverse.client

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.w3c.dom.HTMLCanvasElement
import org.w3c.fetch.Response
import yz.cool.towerverse.client.render.Renderer
import yz.cool.towerverse.client.render.WASMCanvasRenderer
import yz.cool.towerverse.simulation.ui.UI
import yz.cool.towerverse.mod.ModJson
import yz.cool.towerverse.official.loadOfficialContent
import yz.cool.towerverse.simulation.GameSimulation
import yz.cool.towerverse.simulation.verification.GameSimulationVerificationPayload
import kotlin.js.Promise
import kotlin.math.ceil

/**
 * Main game object.
 *
 * For now the code is specific to WASM, but it should be possible to generify this rather easily
 * to create a multiplatform game client.
 */
object Main {
    /**
     * The HTML canvas that renders the game.
     */
    lateinit var canvas: HTMLCanvasElement

    lateinit var renderer: Renderer

    var mods = ArrayList<ModJson>()

    /**
     * The game simulation on the client side.
     */
    lateinit var simulation: GameSimulation
    lateinit var ui: UI

    private val officialContent = loadOfficialContent()
    /**
     * Resets the game simulation, taking the new loaded mods list into account.
     */
    fun resetSimulation() {
        simulation = GameSimulation.createSimulation(
            officialGameObjects = officialContent,
            mods = mods,
            isVerificationMode = false,
            onVictory = {
                // If the player wins the game, give player the option to upload the game result to the server
                val uploadResult = window.confirm("You won the game! " +
                        "Would you like to upload your result to the game server?")

                if(uploadResult) {
                    val playerName: String = window.prompt("Please enter your player name to be shown on " +
                            "the server leaderboard (leave empty for Anonymous):").let{
                                if(it.isNullOrEmpty()) "Anonymous" else it
                            }
                    val simulationStateString = Json.encodeToString(simulation.state) // For debug in case failure

                    // Call the game server to verify the game result
                    val serverAddress = "http://localhost:8079"
                    customFetch(
                        "$serverAddress/result",
                        Json.encodeToString(
                            GameSimulationVerificationPayload.fromGameSimulation(mods, simulation, playerName)
                        )
                    ).then { response: Response ->
                        if(response.ok) {
                            val openLeaderboard = window.confirm(
                                "You game result was successfully validated by the server! " +
                                        "Would you like to open the leaderboard page?"
                            )
                            if(openLeaderboard) {
                                window.open(serverAddress, "_blank")
                            }
                        }
                        else {
                            response.text().then{
                                window.alert("The server has failed to validate your game result: ${
                                    response.status} ${response.statusText} $it")
                                println("Client end state:")
                                println(simulationStateString)
                                "".toJsString() // Make Kotlin/WASM compiler happy
                            }
                        }
                        response
                    }.catch {
                        window.alert(
                            "The network request to the server failed: $it. " +
                                    "Please make sure the server is running locally then refresh the page to try again."
                        )
                        "".toJsString() // Make Kotlin/WASM compiler happy
                    }
                }
            }
        )
        ui = UI(simulation)
        renderer = WASMCanvasRenderer(simulation, ui, canvas)
    }

    /**
     * Called every frame.
     *
     * @param dt The time in seconds since the last frame.
     */
    fun tick(dt: Double) {
        // Set the canvas size (note: this takes high DPI displays into account to always render at native resolution)
        canvas.width = (window.innerWidth * window.devicePixelRatio).toInt()
        canvas.height = (window.innerHeight * window.devicePixelRatio).toInt()

        renderer.render(dt,
            windowWidth = canvas.width.toDouble(),
            windowHeight = canvas.height.toDouble()
        )

        // Perform the game simulation every 50 ms scaled by game speed
        ui.tick(dt)
        // Number of game ticks that we allow to simulate per frame. This depends on the game speed
        // We're assuming the user uses at least a 60Hz monitor which is 3 times the game's tick rate at 1x speed,
        // hence the "gameSpeed / 3"
        var ticksRemaining = ceil(ui.gameSpeed / 3).toInt().coerceAtLeast(1)
        while(ui.timeSinceLastTick > 0.05 && ticksRemaining > 0) {
            simulation.gameTick()
            ui.timeSinceLastTick -= 0.05
            ticksRemaining--
        }
        // Don't let too much time flow over into the next frame
        ui.timeSinceLastTick = ui.timeSinceLastTick.coerceAtMost(0.075)
    }
}

/** Workaround for Kotlin/WASM fetch() straight-up not working */
@Suppress("UNUSED_PARAMETER")
fun customFetch(address: String, body: String): Promise<Response> = js("""
    fetch(address, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: body
    })
""")

fun main() {
    try {
        Main.canvas = document.getElementById("gameCanvas") as HTMLCanvasElement
        Main.resetSimulation() // Load the initial simulation

        // Start ticking the game
        window.requestAnimationFrame(::step)
    }
    catch (e: Exception) {
        // Make exception output more readable
        println("Exception on init: " + e::class.simpleName + "\n" + e.stackTraceToString())
        throw e
    }
}

// See requestAnimationFrame example at https://developer.mozilla.org/en-US/docs/Web/API/Window/requestAnimationFrame
private var lastTimestamp: Double = 0.0
private fun step(timestamp: Double) {
    try {
        val elapsed = timestamp - lastTimestamp
        lastTimestamp = timestamp
        Main.tick(elapsed / 1000)
    }
    catch (e: Exception) {
        // Make exception output more readable
        println("Exception on tick: " + e::class.simpleName + "\n" + e.stackTraceToString())
        throw e
    }

    window.requestAnimationFrame(::step)
}