plugins {
  id("fabric-loom") version "1.10-SNAPSHOT"
  id("roundalib-gradle") version "1.0-SNAPSHOT"
}

roundalib {
  variants {
    create("hardcore", "technical")
    buildAndPublishAll()
  }
}
