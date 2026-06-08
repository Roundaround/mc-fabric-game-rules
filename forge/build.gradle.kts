plugins {
  id("me.roundaround.allay")
}

repositories {
  exclusiveContent {
    forRepository { mavenLocal() }
    filter { includeModuleByRegex("me\\.roundaround", "trove(-.+)?") }
  }
}

dependencies {
  libBundle(platform(libs.trove.bom))
  libBundle(libs.trove.forge.core)
  libBundle(libs.trove.gui)
  libBundle(libs.trove.network)
}
