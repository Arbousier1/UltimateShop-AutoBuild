rootProject.name = "UltimateShop"

include("core")
include("spigot")
include("paper")
include("plugin")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
