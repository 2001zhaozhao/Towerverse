package yz.cool.towerverse.client.render

import yz.cool.towerverse.gameobject.*
import yz.cool.towerverse.simulation.GameSimulation
import yz.cool.towerverse.simulation.ui.*
import yz.cool.towerverse.types.Color
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.min

/**
 * Platform-independent base class of the rendering logic.
 * Renders game state based on the [GameSimulation] and [UI] objects provided.
 * 
 * This class also handles translating platform-specific user input (such as clicks on the canvas) into UI events.
 *
 * Note that Towerverse currently only has one client platform (WASM Canvas),
 * but using this class should allow easily adding more rendering platforms in the future.
 */
abstract class Renderer(
    val gameSimulation: GameSimulation,
    val ui: UI
) {
    /**
     * Called each tick to render the game simulation.
     *
     * @param dt The time in seconds since the last tick.
     * @param windowWidth The width of the window in pixels.
     * @param windowHeight The height of the window in pixels.
     */
    fun render(
        dt: Double,
        windowWidth: Double,
        windowHeight: Double
    ) {
        // Update bottom bar scaling to determine how much of the screen is taken up by the bottom bar
        calculateBottomBarScaling(windowWidth, windowHeight)
        val bottomBarHeight = bottomBarScale * 100.0 // Base bottom bar height is 100 pixels

        // Update map scaling with the screen space we have left above the bottom bar
        updateBaseScaleAndOffset(windowWidth, windowHeight - bottomBarHeight)
        resetCanvas()

        // Render the map
        renderMap()

        // Render the bottom bar on top of the map
        renderBottomBar(windowWidth)
    }

    // ========== Map scaling

    /** The width of the game map in tiles, used to calculate the base scale and offset. */
    val mapWidth: Int = gameSimulation.map.width
    /** The height of the game map in tiles, used to calculate the base scale and offset. */
    val mapHeight: Int = gameSimulation.map.height

    /** The base scale in pixels per tile */
    var baseScale: Double = 40.0
    /** The base X offset in pixels for the top left corner of the map relative to the top left corner of the screen */
    var baseOffsetX: Double = 0.0
    /** The base Y offset in pixels for the top left corner of the map relative to the top left corner of the screen */
    var baseOffsetY: Double = 0.0

    fun updateBaseScaleAndOffset(windowWidth: Double, windowHeight: Double) {
        // Calculate map scale
        val baseScaleIfXConstrained = windowWidth / mapWidth
        val baseScaleIfYConstrained = windowHeight / mapHeight
        baseScale = min(baseScaleIfXConstrained, baseScaleIfYConstrained)

        // Calculate map offset so that the map is centered
        val extraXPixels = windowWidth - baseScale * mapWidth
        val extraYPixels = windowHeight - baseScale * mapHeight
        baseOffsetX = extraXPixels / 2
        baseOffsetY = extraYPixels / 2
    }

    // User-defined scale values, note that these don't affect the game simulation and therefore
    // don't follow the game's usual "actions" system
    /** User-defined scale compared to the base scale, used when zooming in */
    var scale: Double = 1.0
    /** User-defined X offset in **tiles** used when zooming in & panning when zoomed in */
    var offsetX: Double = 0.0
    /** User-defined Y offset in **tiles** used when zooming in & panning when zoomed in */
    var offsetY: Double = 0.0

    // Input values that the actual scale/offset will slowly converge towards
    var scaleInput: Double = 1.0
    var offsetXInput: Double = 0.0
    var offsetYInput: Double = 0.0

    fun convergeScaleAndOffset(dt: Double) {
        scale = scale * (1 - dt) + scaleInput * dt
        offsetX = offsetX * (1 - dt) + offsetXInput * dt
        offsetY = offsetY * (1 - dt) + offsetYInput * dt

        // If the input values are close enough to the actual values, set them to the actual values
        if(abs(scale - scaleInput) < 0.001) scale = scaleInput
        if(abs(offsetX - offsetXInput) < 0.01) offsetX = offsetXInput
        if(abs(offsetY - offsetYInput) < 0.01) offsetY = offsetYInput
    }

    /** Zoom in by a given amount about the given screen coordinates */
    fun zoomIn(amount: Double, screenX: Double, screenY: Double) {
        val oldTileX = screenXToInputTileX(screenX)
        val oldTileY = screenYToInputTileY(screenY)
        scaleInput *= 1 + amount
        if(scaleInput > 10) scaleInput = 10.0 // Max scale

        val tileX = screenXToInputTileX(screenX)
        val tileY = screenYToInputTileY(screenY)
        // Compensate for the change in offset at the given screen coordinates
        // (this will keep the mouse pointed at the same tile when zooming in)
        offsetXInput -= tileX - oldTileX
        offsetYInput -= tileY - oldTileY
    }
    /** Zoom out by a given amount about the given screen coordinates */
    fun zoomOut(amount: Double, screenX: Double, screenY: Double) {
        val oldTileX = screenXToInputTileX(screenX)
        val oldTileY = screenYToInputTileY(screenY)
        scaleInput /= 1 + amount

        val tileX = screenXToInputTileX(screenX)
        val tileY = screenYToInputTileY(screenY)
        // Compensate for the change in offset at the given screen coordinates
        // (this will keep the mouse pointed at the same tile when zooming in)
        offsetXInput -= tileX - oldTileX
        offsetYInput -= tileY - oldTileY

        if(scaleInput <= 1) { // Min scale, this will also cancel user-defined offsets
            scaleInput = 1.0
            offsetXInput = 0.0
            offsetYInput = 0.0
        }
    }

    /** Move the board by given number of pixels divided by current zoom */
    fun moveBoardByPixels(x: Double, y: Double) {
        offsetXInput += x / (baseScale * scaleInput)
        offsetYInput += y / (baseScale * scaleInput)
    }

    /** Mouse drag which cancels current velocity and causes slowdown over time */
    fun moveBoardMouseDrag(x: Double, y: Double, dt: Double) {
        val dx = x / (baseScale * scaleInput)
        val dy = y / (baseScale * scaleInput)
        val vx = dx / dt
        val vy = dy / dt

        offsetX += dx
        offsetY += dy
        // Use current velocity to determine the new resting place for the game map
        offsetXInput = offsetX + vx
        offsetYInput = offsetY + vy
    }

    /** Convert from tile distance to screen distance */
    fun tileToScreen(amount: Double): Double {
        return amount * baseScale * scale
    }

    /** Convert from tile coordinates to screen coordinates */
    fun tileXToScreenX(x: Double): Double {
        return (x + offsetX) * baseScale * scale + baseOffsetX
    }

    /** Convert from tile coordinates to screen coordinates */
    fun tileYToScreenY(y: Double): Double {
        return (y + offsetY) * baseScale * scale + baseOffsetY
    }

    /** Convert from screen coordinates to tile distance */
    fun screenToTile(amount: Double): Double {
        return amount / (baseScale * scale)
    }

    /** Convert from screen coordinates to tile coordinates */
    fun screenXToTileX(x: Double): Double {
        return (x - baseOffsetX) / (baseScale * scale) - offsetX
    }

    /** Convert from screen coordinates to tile coordinates */
    fun screenYToTileY(y: Double): Double {
        return (y - baseOffsetY) / (baseScale * scale) - offsetY
    }

    // helper for zoom-in / zoom-out logic
    private fun screenXToInputTileX(x: Double): Double {
        return (x - baseOffsetX) / (baseScale * scale) - offsetX
    }

    private fun screenYToInputTileY(y: Double): Double {
        return (y - baseOffsetY) / (baseScale * scale) - offsetY
    }

    // ========== Map rendering

    fun renderMap() {
        // Render tiles
        for(x in 0 ..< gameSimulation.map.width) {
            for(y in 0 ..< gameSimulation.map.height) {
                val tile = gameSimulation.tiles[x][y]
                // Make size slightly bigger than 1 tile to cover up any gaps
                renderAppearance(tile.type.appearance, x + 0.5, y + 0.5, 1.015, CoordinateScaling.Tile)
            }
        }

        // Render exits
        for(exit in gameSimulation.map.exits) {
            val (x, y) = exit
            renderText("🏠", x + 0.05, y + 0.5, 0.7, Color.WHITE, CoordinateScaling.Tile)
        }

        // Render spawnpoints
        for(spawnpoint in gameSimulation.map.spawnpoints) {
            val (x, y) = spawnpoint.location
            renderText("⚠️", x + 0.05, y + 0.5, 0.7, Color.WHITE, CoordinateScaling.Tile)
        }

        // Render towers
        for(x in 0 ..< gameSimulation.map.width) {
            for(y in 0 ..< gameSimulation.map.height) {
                val tower = gameSimulation.tiles[x][y].tower
                if(tower != null) {
                    renderAppearance(tower.type.appearance, x + 0.5, y + 0.5, 0.96,
                        CoordinateScaling.Tile)
                    for(turret in tower.turrets) {
                        renderAppearance(turret.type.appearance,
                            x + 0.5 + turret.type.offsetX, y + 0.5 + turret.type.offsetY, 0.96,
                            CoordinateScaling.Tile, angle = turret.angle)
                    }
                    renderText(tower.level.toString(), x + 0.15, y + 0.16, 0.32, Color.WHITE,
                        CoordinateScaling.Tile)
                }
            }
        }

        // Render enemies
        val timeSinceLastTick = ui.timeSinceLastTick
        for(enemy in gameSimulation.enemies.sortedBy{it.y}) {
            // Interpolate position
            val x = (enemy.x + enemy.vx * timeSinceLastTick)
                .coerceIn(0.0..(gameSimulation.map.width - 0.0000001))
            val y = (enemy.y + enemy.vy * timeSinceLastTick)
                .coerceIn(0.0..(gameSimulation.map.width - 0.0000001))

            renderAppearance(enemy.type.appearance, x, y, enemy.type.size, CoordinateScaling.Tile,
                angle = atan2(enemy.vy, enemy.vx))

            // Level and health bar
            val topLeftX = x - enemy.type.size * 0.5
            val topLeftY = y - enemy.type.size * 0.5
            renderText("${enemy.level}", topLeftX, topLeftY - 0.12, 0.16, Color.WHITE,
                CoordinateScaling.Tile)
            renderRectangle(topLeftX, topLeftY - 0.04, enemy.type.size, 0.04, Color.BLACK,
                CoordinateScaling.Tile)
            renderRectangle(topLeftX, topLeftY - 0.04, enemy.health / enemy.maxHealth * enemy.type.size,
                0.04, Color.RED, CoordinateScaling.Tile)
        }

        // Render projectiles
        for(projectile in gameSimulation.projectiles) {
            // Interpolate position
            val x = projectile.x + projectile.vx * timeSinceLastTick
            val y = projectile.y + projectile.vy * timeSinceLastTick
            // Size varies depending on z height and damage multiplier
            val size = projectile.type.size * (1.0 + (projectile.z - 0.5) * 0.2) * projectile.renderSizeMultiplier

            renderAppearance(projectile.type.appearance, x, y, size, CoordinateScaling.Tile,
                angle = atan2(projectile.vy, projectile.vx))
        }

        // Render selected tile
        if(ui.selectedTile != null) {
            val (x, y) = ui.selectedTile!!
            renderRectangle(x.toDouble(), y.toDouble(), 1.0, 1.0,
                Color.WHITE.multiplyAlpha(0.4), CoordinateScaling.Tile)
            val tile = gameSimulation.tiles[x][y]
            val tower = tile.tower
            if(tower != null) {
                // Render turret range
                for(turret in tower.turrets) {
                    renderTargetingShape(
                        x + 0.5 + turret.type.offsetX, y + 0.5 + turret.type.offsetY,
                        turret.type.shape, tower.type.baseRangeModifier, tower.levelRangeModifier
                    )
                }
                for(turret in tower.turrets) {
                    // Render targets
                    for(target in turret.targets) {
                        if(target.isDead) continue
                        renderText("💥", target.x - 0.3, target.y, 0.4, Color.WHITE, CoordinateScaling.Tile)
                    }
                }

                if(tower.isAtMaxLevel) {
                    renderText("Tower is at max level",
                        x + 0.5, if(y == gameSimulation.map.height - 1) y - 0.34 else y + 1.1, 0.2, Color.RED,
                        CoordinateScaling.Tile, bold = true, centered = true)
                }
                else {
                    renderText("[L-Click] Upgrade for $${tower.upgradeCost}",
                        x + 0.5, if(y == gameSimulation.map.height - 1) y - 0.34 else y + 1.1, 0.2,
                        if(gameSimulation.money >= tower.upgradeCost) Color.YELLOW else Color.RED,
                        CoordinateScaling.Tile, bold = true, centered = true)
                }
                renderText("[R-Click] Remove to regain $${tower.refundCost}",
                    x + 0.5, if(y == gameSimulation.map.height - 1) y - 0.1 else y + 1.34, 0.2, Color.YELLOW, CoordinateScaling.Tile, bold = true, centered = true)
            }
            else if(tile.type.isTowerPlaceable) {
                renderText("[L-Click] Place ${ui.selectedTower.name} for $${ui.selectedTower.cost}",
                    x + 0.5, if(y == gameSimulation.map.height - 1) y - 0.34 else y + 1.1, 0.2,
                    if(gameSimulation.money >= ui.selectedTower.cost) Color.YELLOW else Color.RED,
                    CoordinateScaling.Tile, bold = true, centered = true)
            }
        }

        // Render particles
        for(particle in ui.particles) {
            renderParticle(particle)
        }

        // Victory/defeat text
        if(gameSimulation.hasAlreadyWon) {
            renderText("VICTORY", gameSimulation.map.width * 0.5, gameSimulation.map.height * 0.5,
                gameSimulation.map.height * 0.1, Color.GREEN, CoordinateScaling.Tile, centered = true)
        }
        if(gameSimulation.hasAlreadyLost) {
            renderText("DEFEAT", gameSimulation.map.width * 0.5, gameSimulation.map.height * 0.5,
                gameSimulation.map.height * 0.1, Color.RED, CoordinateScaling.Tile, centered = true)
        }
    }

    fun renderTargetingShape(
        x: Double, y: Double, targetingShape: TowerTargetingShape,
        baseRangeModifier: Double, levelRangeModifier: Double
    ) {
        when(targetingShape) {
            is TowerTargetingShapeCompound -> {
                for(shape in targetingShape.shapes) {
                    renderTargetingShape(x, y, shape, baseRangeModifier, levelRangeModifier)
                }
            }
            is TowerTargetingShapeCircle -> {
                val range = targetingShape.radius * baseRangeModifier *
                        ((levelRangeModifier - 1.0) * targetingShape.radiusRangeModifierEffect + 1.0)
                renderCircle(x, y, range, Color.CYAN.multiplyAlpha(0.25), CoordinateScaling.Tile)
            }
            is TowerTargetingShapeRectangle -> {
                val width = (targetingShape.width * baseRangeModifier *
                        (((levelRangeModifier - 1.0) * targetingShape.widthRangeModifierEffect) + 1.0))
                val height = targetingShape.height * baseRangeModifier *
                        ((levelRangeModifier - 1.0) * targetingShape.heightRangeModifierEffect + 1.0)
                val offsetX = targetingShape.offsetX *
                        ((levelRangeModifier - 1.0) * targetingShape.offsetXRangeModifierEffect + 1.0)
                val offsetY = targetingShape.offsetY *
                        ((levelRangeModifier - 1.0) * targetingShape.offsetYRangeModifierEffect + 1.0)
                renderRectangle(x + offsetX - width / 2, y + offsetY - height / 2, width, height,
                    Color.CYAN.multiplyAlpha(0.25), CoordinateScaling.Tile)
            }
            else -> {}
        }
    }

    // ========== Bottom bar scaling & rendering

    /** Scale of the bottom bar from the default 800x100 size */
    var bottomBarScale: Double = 1.0
    /** X offset of the top left position of the bottom bar.
     * Note that the bottom bar background ignores this and paints the whole row */
    var bottomBarOffsetX: Double = 0.0
    /** Y offset of the top left position of the bottom bar */
    var bottomBarOffsetY: Double = 0.0

    fun calculateBottomBarScaling(windowWidth: Double, windowHeight: Double) {
        // Determine bottom bar size
        val bottomBarHeightPx = min(windowHeight * 0.2, windowWidth * 0.125)
        bottomBarScale = bottomBarHeightPx / 100.0
        val bottomBarWidthPx = bottomBarHeightPx * 8.0 // 8:1 ratio
        bottomBarOffsetX = (windowWidth - bottomBarWidthPx) / 2
        bottomBarOffsetY = windowHeight - bottomBarHeightPx
    }

    fun bottomBarToScreen(amount: Double): Double {
        return amount * bottomBarScale
    }

    fun bottomBarXToScreenX(x: Double): Double {
        return x * bottomBarScale + bottomBarOffsetX
    }

    fun bottomBarYToScreenY(y: Double): Double {
        return y * bottomBarScale + bottomBarOffsetY
    }

    /**
     * Returns if the given screen Y coordinate is in the bottom bar,
     * meaning that an input should be handled by the bottom bar
     */
    fun isScreenYInBottomBar(y: Double): Boolean {
        return y >= bottomBarOffsetY
    }

    fun screenToBottomBar(amount: Double): Double {
        return amount / bottomBarScale
    }

    fun screenXToBottomBarX(x: Double): Double {
        return (x - bottomBarOffsetX) / bottomBarScale
    }

    fun screenYToBottomBarY(y: Double): Double {
        return (y - bottomBarOffsetY) / bottomBarScale
    }

    fun renderBottomBar(windowWidth: Double) {
        // Background
        renderRectangle(0.0, bottomBarOffsetY, windowWidth, bottomBarOffsetY, Color(80))

        // Selected tower background
        renderRectangle(ui.selectedTowerIndex * 36.0 + 8.0, 20.0, 40.0, 40.0,
            Color.WHITE.multiplyAlpha(0.4), CoordinateScaling.BottomBar)
        renderText(ui.selectedTower.name, 15.0, 74.0, 10.0,
            Color.YELLOW, CoordinateScaling.BottomBar, bold = true)
        if(ui.selectedTower.description != null) {
            renderText(ui.selectedTower.description!!, 15.0, 86.0, 10.0,
                Color.YELLOW, CoordinateScaling.BottomBar)
        }

        // Towers, sorted by cost
        for(i in ui.bottomBarTowerTypes.indices) {
            val towerType = ui.bottomBarTowerTypes[i]
            // Render the tower icon
            val towerX = i * 36.0 + 28.0
            val towerY = 40.0
            val towerSize = if(i == ui.selectedTowerIndex) 40.0 else 36.0
            renderAppearance(towerType.appearance, towerX, towerY, towerSize, CoordinateScaling.BottomBar)
            for(turret in towerType.turrets) {
                renderAppearance(turret.appearance,
                    towerX + turret.offsetX * towerSize, towerY + turret.offsetY * towerSize, towerSize,
                    CoordinateScaling.BottomBar)
            }
            // Render the cost
            renderText("LVL 1", i * 36.0 + 10.0, 46.0, 9.0,
                towerType.rarity.color, CoordinateScaling.BottomBar, bold = true)
            renderText("$${towerType.cost}", i * 36.0 + 10.0, 57.0, 9.0,
                Color.WHITE, CoordinateScaling.BottomBar)
        }

        // Base Health
        renderText("Base Health: ${gameSimulation.playerHealth.toLong()} HP", 15.0, 9.0, 12.0,
            Color.GREEN, CoordinateScaling.BottomBar)

        // Wave Counter, Money, Game Speed, Game Tick
        val waveNumber = gameSimulation.enemySpawner.waveNumber
        val finalWave = gameSimulation.map.waveDefinition.finalWave
        renderText(if(finalWave > 0) "WAVE $waveNumber / $finalWave" else "WAVE $waveNumber", 680.0, 12.0, 18.0,
            Color.YELLOW, CoordinateScaling.BottomBar, bold = true)
        renderText(
            if(gameSimulation.isFinalWave) "Final Wave!" else
                "${ceil(gameSimulation.enemySpawner.currentWaveDuration -
                    gameSimulation.enemySpawner.timeSinceWaveStart).toInt()} seconds left", 680.0, 33.0, 10.0,
            Color.GREEN, CoordinateScaling.BottomBar, bold = true)
        renderText("$${gameSimulation.money}", 680.0, 54.0, 18.0,
            Color.GREEN, CoordinateScaling.BottomBar, bold = true)

        renderText("[Add Mod]", 580.0, 32.0, 14.0,
            Color.YELLOW, CoordinateScaling.BottomBar, bold = true)

        renderText(if(ui.gameSpeed == 0.0) "Paused" else "${ui.gameSpeed}X", 680.0, 82.0, 18.0,
            Color.YELLOW, CoordinateScaling.BottomBar, bold = true)
        if(waveNumber == 0 && gameSimulation.tick % 20 <= 10)
            renderText("Click to change game speed ➡️", 490.0, 82.0, 12.0,
                Color.YELLOW, CoordinateScaling.BottomBar, bold = true)
        renderText("Tick ${gameSimulation.tick}", 735.0, 74.0, 10.0,
            Color.YELLOW, CoordinateScaling.BottomBar)
        renderText("Score ${gameSimulation.score}",
            755.0 - 5 * (gameSimulation.score.toString().length.coerceAtLeast(4)), 86.0, 10.0,
            Color.YELLOW, CoordinateScaling.BottomBar)
    }

    // ========== Common coordinate scaling class

    sealed class CoordinateScaling {
        abstract fun toScreen(renderer: Renderer, amount: Double): Double
        abstract fun toScreenX(renderer: Renderer, x: Double): Double
        abstract fun toScreenY(renderer: Renderer, y: Double): Double

        data object Screen : CoordinateScaling() {
            override fun toScreen(renderer: Renderer, amount: Double): Double {
                return amount
            }
            override fun toScreenX(renderer: Renderer, x: Double): Double {
                return x
            }
            override fun toScreenY(renderer: Renderer, y: Double): Double {
                return y
            }
        }

        data object Tile : CoordinateScaling() {
            override fun toScreen(renderer: Renderer, amount: Double): Double {
                return renderer.tileToScreen(amount)
            }
            override fun toScreenX(renderer: Renderer, x: Double): Double {
                return renderer.tileXToScreenX(x)
            }
            override fun toScreenY(renderer: Renderer, y: Double): Double {
                return renderer.tileYToScreenY(y)
            }
        }

        data object BottomBar : CoordinateScaling() {
            override fun toScreen(renderer: Renderer, amount: Double): Double {
                return renderer.bottomBarToScreen(amount)
            }
            override fun toScreenX(renderer: Renderer, x: Double): Double {
                return renderer.bottomBarXToScreenX(x)
            }
            override fun toScreenY(renderer: Renderer, y: Double): Double {
                return renderer.bottomBarYToScreenY(y)
            }
        }
    }

    // ========== Input Handling

    /**
     * Handles click event at the given screen coordinates.
     */
    fun onClick(x: Double, y: Double, isRightClick: Boolean) {
        if(isScreenYInBottomBar(y)) {
            // Deselect map tile
            ui.onClickOutsideMap()
            // Bottom bar click
            val scaledX = screenXToBottomBarX(x)
            val scaledY = screenYToBottomBarY(y)
            if(scaledY in 20.0..65.0) {
                // Clicked on a tower in the bottom bar
                val towerIndex = ((scaledX - 10.0) / 36.0).toInt()
                if(towerIndex in 0 ..< ui.bottomBarTowerTypes.size) {
                    ui.onTowerSelect(towerIndex)
                }
            }

            if(scaledY in 70.0..95.0 && scaledX in 670.0..750.0) {
                // Clicked on game speed
                ui.onGameSpeedClick(isRightClick)
            }

            if(scaledY in 20.0..45.0 && scaledX in 570.0..650.0) {
                // Clicked on upload mod button
                onUploadModButtonClick()
            }
        }
        else {
            // Map click
            val tileX = screenXToTileX(x).toInt()
            val tileY = screenYToTileY(y).toInt()
            if(tileX in 0 ..< gameSimulation.map.width && tileY in 0 ..< gameSimulation.map.height) {
                ui.onTileClick(tileX, tileY, isRightClick)
            }
            else {
                // Deselect map tile
                ui.onClickOutsideMap()
            }
        }
    }

    // ========== Abstract rendering

    abstract fun resetCanvas()

    /**
     * Renders a rectangle with the given screen coordinates.
     */
    abstract fun renderRectangle(
        x: Double, y: Double, width: Double, height: Double, color: Color,
        coordinateScaling: CoordinateScaling = CoordinateScaling.Screen, isSolid: Boolean = true
    )

    /**
     * Renders a circle with the given screen coordinates.
     */
    abstract fun renderCircle(
        x: Double, y: Double, radius: Double, color: Color,
        coordinateScaling: CoordinateScaling = CoordinateScaling.Screen, isSolid: Boolean = true
    )

    /**
     * Renders the given appearance at the given x, y **center** coordinates.
     */
    abstract fun renderAppearance(
        appearance: Appearance?, x: Double, y: Double, baseSize: Double,
        coordinateScaling: CoordinateScaling = CoordinateScaling.Screen, alpha: Double = 1.0, angle: Double = 0.0
    )

    /**
     * Renders text at the specific screen coordinates. Defaults to a size of 15px.
     * The text will be vertically centered at the given y coordinates.
     */
    abstract fun renderText(
        text: String, x: Double, y: Double, size: Double = 15.0, color: Color = Color.WHITE,
        coordinateScaling: CoordinateScaling = CoordinateScaling.Screen,
        bold: Boolean = false, italic: Boolean = false, centered: Boolean = false
    )

    /**
     * Renders a line between two points.
     */
    abstract fun renderLine(
        x1: Double, y1: Double, x2: Double, y2: Double,
        color: Color, lineWidth: Double, coordinateScaling: CoordinateScaling = CoordinateScaling.Screen
    )

    // ========== Special (upload mod button)

    abstract fun onUploadModButtonClick()

    // ========== Particle Rendering

    private fun renderParticle(particle: Particle) = with(particle) {
        when(this) {
            is FloatingTextParticle -> {
                val alpha = 1 - (1.0 - remainingLifetime / lifetime) * (1.0 - remainingLifetime / lifetime)
                val yOffset = -1.0 + 1.0 * (remainingLifetime / lifetime) * (remainingLifetime / lifetime)
                renderText(text, x, y + yOffset, size,
                    color.multiplyAlpha(alpha), CoordinateScaling.Tile, bold = true, centered = true)
            }
            is CircleParticle -> {
                renderCircle(x, y, radius, color, CoordinateScaling.Tile, isSolid)
            }
            is LineParticle -> {
                renderLine(x1, y1, x2, y2, color, lineWidth, CoordinateScaling.Tile)
            }
            is SplashParticle -> {
                val alpha = remainingLifetime / lifetime * 0.3
                val sizeMultiplier = 1.2 - 0.2 * (remainingLifetime / lifetime) * (remainingLifetime / lifetime)
                renderCircle(x, y, radius * sizeMultiplier, color.multiplyAlpha(alpha), CoordinateScaling.Tile)
            }
        }
    }
}