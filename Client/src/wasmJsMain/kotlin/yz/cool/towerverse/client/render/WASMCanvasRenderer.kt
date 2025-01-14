package yz.cool.towerverse.client.render

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.serialization.json.Json
import org.khronos.webgl.Uint8ClampedArray
import org.khronos.webgl.set
import org.khronos.webgl.toInt8Array
import org.w3c.dom.*
import org.w3c.dom.events.MouseEvent
import org.w3c.files.FileReader
import org.w3c.files.get
import yz.cool.towerverse.client.Main
import yz.cool.towerverse.simulation.ui.UI
import yz.cool.towerverse.gameobject.*
import yz.cool.towerverse.mod.ModJson
import yz.cool.towerverse.simulation.GameSimulation
import yz.cool.towerverse.types.Color
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * The WASM Canvas renderer.
 */
class WASMCanvasRenderer(
    gameSimulation: GameSimulation,
    ui: UI,
    val canvas: HTMLCanvasElement
) : Renderer(gameSimulation, ui) {
    init {
        // Register mouse click handler to the canvas
        canvas.addEventListener("click", { event ->
            event as MouseEvent
            if(event.button.toInt() == 0) {
                onClick(
                    event.offsetX * window.devicePixelRatio, event.offsetY * window.devicePixelRatio,
                    isRightClick = false
                )
            }
            else if(event.button.toInt() == 2) {
                onClick(
                    event.offsetX * window.devicePixelRatio, event.offsetY * window.devicePixelRatio,
                    isRightClick = true
                )
            }
        })
        canvas.oncontextmenu = { event -> // Disable context menu so right click can be used
            event.preventDefault()
            onClick(
                event.offsetX * window.devicePixelRatio, event.offsetY * window.devicePixelRatio,
                isRightClick = true
            )
        }
    }

    val ctx = canvas.getContext("2d") as CanvasRenderingContext2D

    override fun resetCanvas() {
        ctx.clearRect(0.0, 0.0, canvas.width.toDouble(), canvas.height.toDouble())
    }

    override fun renderRectangle(
        x: Double,
        y: Double,
        width: Double,
        height: Double,
        color: Color,
        coordinateScaling: CoordinateScaling,
        isSolid: Boolean
    ) {
        ctx.globalAlpha = color.a / 255.0
        if(isSolid) {
            ctx.fillStyle = color.toCssString().toJsString()
            ctx.fillRect(
                coordinateScaling.toScreenX(this, x),
                coordinateScaling.toScreenY(this, y),
                coordinateScaling.toScreen(this, width),
                coordinateScaling.toScreen(this, height)
            )
        }
        else {
            ctx.strokeStyle = color.toCssString().toJsString()
            ctx.strokeRect(
                coordinateScaling.toScreenX(this, x),
                coordinateScaling.toScreenY(this, y),
                coordinateScaling.toScreen(this, width),
                coordinateScaling.toScreen(this, height)
            )
        }
        ctx.globalAlpha = 1.0
    }

    override fun renderCircle(
        x: Double,
        y: Double,
        radius: Double,
        color: Color,
        coordinateScaling: CoordinateScaling,
        isSolid: Boolean
    ) {
        ctx.globalAlpha = color.a / 255.0
        if(isSolid) {
            ctx.fillStyle = color.toCssString().toJsString()
            ctx.beginPath()
            ctx.arc(
                coordinateScaling.toScreenX(this, x),
                coordinateScaling.toScreenY(this, y),
                coordinateScaling.toScreen(this, radius),
                0.0, 2.0 * PI
            )
            ctx.fill()
        }
        else {
            ctx.strokeStyle = color.toCssString().toJsString()
            ctx.beginPath()
            ctx.arc(
                coordinateScaling.toScreenX(this, x),
                coordinateScaling.toScreenY(this, y),
                coordinateScaling.toScreen(this, radius),
                0.0, 2.0 * PI
            )
            ctx.stroke()
        }
        ctx.globalAlpha = 1.0
    }

    private val imageCache = HashMap<ImageAsset, Image>()
    private val loadedImages = HashSet<Image>()
    override fun renderAppearance(
        appearance: Appearance?,
        x: Double,
        y: Double,
        baseSize: Double,
        coordinateScaling: CoordinateScaling,
        alpha: Double,
        angle: Double
    ) {
        when(appearance) {
            null -> {}
            is AppearanceCompound -> {
                for(child in appearance.children) {
                    renderAppearance(child, x, y, baseSize, coordinateScaling)
                }
            }
            is AppearanceImage -> {
                val image = imageCache.getOrPut(appearance.image) {
                    val image = Image()
                    image.onload = {
                        loadedImages += image
                    }
                    image.src = "data:image/png;base64,${appearance.image.content}"
                    image
                }
                if(image in loadedImages) {
                    val minWH = min(image.width, image.height)
                    val width = baseSize * appearance.scale * image.width / minWH
                    val height = baseSize * appearance.scale * image.height / minWH
                    ctx.save()
                    ctx.translate(
                        coordinateScaling.toScreenX(this, x),
                        coordinateScaling.toScreenY(this, y)
                    )
                    ctx.rotate(angle)
                    ctx.drawImage(
                        image,
                        coordinateScaling.toScreen(this, -width / 2),
                        coordinateScaling.toScreen(this, -height / 2),
                        coordinateScaling.toScreen(this, width),
                        coordinateScaling.toScreen(this, height)
                    )
                    ctx.restore()
                }
            }
            is AppearanceCircle -> {
                // Get screen space center and size/diameter in pixels
                val centerX = coordinateScaling.toScreenX(this, x +
                        sin(angle) * appearance.offset.x + cos(angle) * appearance.offset.y)
                val centerY = coordinateScaling.toScreenY(this, y +
                        cos(angle) * appearance.offset.x - sin(angle) * appearance.offset.y)
                val size = coordinateScaling.toScreen(this, baseSize * appearance.scale)

                ctx.beginPath()
                ctx.arc(centerX, centerY, size / 2, 0.0, 2.0 * PI)
                ctx.fillStyle = appearance.color.multiplyAlpha(alpha).toCssString().toJsString()
                ctx.globalAlpha = appearance.color.a / 255.0
                ctx.fill()
                ctx.globalAlpha = 1.0
            }
            is AppearanceSquare -> {
                // Get screen space center and size/diameter in pixels
                val centerX = coordinateScaling.toScreenX(this, x +
                        sin(angle) * appearance.offset.x + cos(angle) * appearance.offset.y)
                val centerY = coordinateScaling.toScreenY(this, y +
                        cos(angle) * appearance.offset.x - sin(angle) * appearance.offset.y)
                val size = coordinateScaling.toScreen(this, baseSize * appearance.scale)

                // Note: the square itself isn't rotated by angle right now
                ctx.fillStyle = appearance.color.multiplyAlpha(alpha).toCssString().toJsString()
                ctx.globalAlpha = appearance.color.a / 255.0
                ctx.fillRect(centerX - size / 2, centerY - size / 2, size, size)
                ctx.globalAlpha = 1.0
            }
            else -> error("Unsupported appearance: $appearance")
        }
    }

    override fun renderText(
        text: String,
        x: Double,
        y: Double,
        size: Double,
        color: Color,
        coordinateScaling: CoordinateScaling,
        bold: Boolean,
        italic: Boolean,
        centered: Boolean
    ) {
        ctx.font = "${if(bold) "bold " else ""}${if(italic) "italic " else ""}${
            coordinateScaling.toScreen(this, size)}px sans-serif"
        ctx.fillStyle = color.toCssString().toJsString()
        ctx.textAlign = if(centered) CanvasTextAlign.CENTER else CanvasTextAlign.LEFT
        ctx.globalAlpha = color.a / 255.0
        ctx.fillText(text,
            coordinateScaling.toScreenX(this, x),
            // Y coordinate shifted by size * 0.5 to make the text vertically centered
            coordinateScaling.toScreenY(this, y + size * 0.4)
        )
        ctx.globalAlpha = 1.0
    }

    override fun renderLine(
        x1: Double,
        y1: Double,
        x2: Double,
        y2: Double,
        color: Color,
        lineWidth: Double,
        coordinateScaling: CoordinateScaling
    ) {
        ctx.strokeStyle = color.toCssString().toJsString()
        val previousWidth = ctx.lineWidth
        ctx.lineWidth = coordinateScaling.toScreen(this, lineWidth)
        ctx.beginPath()
        ctx.moveTo(coordinateScaling.toScreenX(this, x1), coordinateScaling.toScreenY(this, y1))
        ctx.lineTo(coordinateScaling.toScreenX(this, x2), coordinateScaling.toScreenY(this, y2))
        ctx.stroke()
        ctx.lineWidth = previousWidth
    }

    override fun onUploadModButtonClick() {
        val fileSelector = document.createElement("input") as HTMLInputElement
        fileSelector.setAttribute("type", "file")
        fileSelector.setAttribute("accept", ".json")
        fileSelector.click()
        fileSelector.onchange = {
            val file = fileSelector.files?.get(0)
            if(file != null) {
                val reader = FileReader()
                reader.onload = {
                    val json = (reader.result as JsString).toString()
                    try {
                        val mod = Json.decodeFromString<ModJson>(json)
                        Main.mods += mod
                        Main.resetSimulation() // Restart the game simulation
                        window.alert("Successfully loaded mod: " + mod.modInformation.name)
                    }
                    catch (e: Exception) {
                        println("Exception on load mod: " + e::class.simpleName + "\n" + e.stackTraceToString())
                        window.alert("Failed to load mod: " + e::class.simpleName)
                        throw e
                    }

                    fileSelector.remove() // Remove file selector
                }
                reader.readAsText(file)
            }
        }
    }
}