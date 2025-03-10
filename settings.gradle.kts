pluginManagement {
  repositories {
    gradlePluginPortal()
    maven("https://maven.fabricmc.net/") { name = "Fabric" }
    maven("https://repo.spongepowered.org/maven/") { name = "Mixin" }
//    maven("https://maven.rnda.dev/snapshots/")
    mavenLocal()
  }
}
