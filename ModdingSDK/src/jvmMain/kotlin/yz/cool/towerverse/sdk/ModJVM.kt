package yz.cool.towerverse.sdk

import yz.cool.towerverse.gameobject.Asset
import java.io.File
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Compiles this mod and then loads all mentioned assets from the JVM resources.
 */
@OptIn(ExperimentalEncodingApi::class)
fun Mod.compileWithAssets() {
    compile()
    for(modFile in modFiles) {
        for(asset in modFile.gameObjects.values.filterIsInstance<Asset>()) {
            if(asset.path != null) {
                // Load the asset from JVM resources
                asset.content =
                    Base64.encode(File(this::class.java.getResource(asset.path!!)!!.toURI()).readBytes())
            }
        }
    }
}

/**
 * Compiles this mod with assets, and then serializes it to JSON, saving the JSON to the given file path.
 */
fun Mod.compileAndSave(path: String) {
    compileWithAssets()
    File(path).writeText(serialize())
}