plugins {
  id("fabric-loom") version "1.10-SNAPSHOT"
  id("roundalib-gradle") version "1.0.0"
}

roundalib {
  variants {
    create("hardcore", "technical")
    buildAndPublishAll()
  }
}
