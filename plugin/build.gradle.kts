plugins {
    id("com.github.johnrengelman.shadow")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":spigot"))
    implementation(project(":paper"))
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
}

tasks.shadowJar {
    archiveBaseName.set("UltimateShop")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")
    destinationDirectory.set(file("$rootDir/plugin/target"))

    relocate("org.bstats", "cn.superiormc.ultimateshop.bstats")
    relocate("com.zaxxer.hikari", "cn.superiormc.ultimateshop.libs.hikari")
    relocate("com.cronutils", "cn.superiormc.ultimateshop.libs.cronutils")
    relocate("redempt.crunch", "cn.superiormc.ultimateshop.libs.crunch")
    relocate("org.json", "cn.superiormc.ultimateshop.libs.json")
    relocate("org.slf4j", "cn.superiormc.ultimateshop.libs.slf4j")
    relocate("javax.el", "cn.superiormc.ultimateshop.libs.javax.el")
    relocate("com.sun.el", "cn.superiormc.ultimateshop.libs.com.sun.el")
    relocate("cn.gtemc.itembridge", "cn.superiormc.ultimateshop.libs.itembridge")

    mergeServiceFiles()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.processResources {
    filteringCharset = "UTF-8"
    expand(project.properties)
}
