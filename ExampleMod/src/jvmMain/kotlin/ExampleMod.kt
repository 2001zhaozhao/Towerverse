import yz.cool.towerverse.mod.ModInformation
import yz.cool.towerverse.sdk.Mod
import yz.cool.towerverse.sdk.ModFile
import yz.cool.towerverse.sdk.compileAndSave

object ExampleMod : Mod(
    ModInformation(
        modId = "example",
        name = "Example Mod",
        author = "Towerverse",
    )
) {
    override val modFiles: List<ModFile> = listOf(
        ExampleModFile
    )
}

// Main function that compiles and saves the mod file.
// Find the output file under "Towerverse/ExampleMod/ExampleMod.json".
fun main() {
    ExampleMod.compileAndSave("ExampleMod.json")
}