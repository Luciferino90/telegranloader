import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "2.2.5.RELEASE"
	id("io.spring.dependency-management") version "1.0.9.RELEASE"
	kotlin("jvm") version "1.3.61"
	kotlin("plugin.spring") version "1.3.61"
}

apply(plugin = "io.spring.dependency-management")
apply(plugin = "kotlin")
apply(plugin = "application")



group = "it.usuratonkachi"
version = "1.0"
java.sourceCompatibility = JavaVersion.VERSION_13

repositories {
	mavenCentral()
	maven {
		url = uri("https://jitpack.io")
	}
}

buildscript {
	repositories {
		mavenCentral()
	}
	dependencies {
		classpath("org.springframework.boot:spring-boot-gradle-plugin:2.2.5.RELEASE")
	}
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.springframework.boot:spring-boot-configuration-processor")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation("commons-io:commons-io:2.5")
	implementation("com.github.badoualy.kotlogram:api:1.0.0-RC3")
	implementation("org.telegram:telegramapi:66.2")
	implementation("org.telegram:telegrambots:4.6")
	implementation("org.projectlombok:lombok:1.18.12")
	implementation("org.goots:jdownloader:0.3")
	implementation("com.turn:ttorrent-core:1.5")
	implementation("io.projectreactor:reactor-core:3.3.3.RELEASE")
	testImplementation("org.springframework.boot:spring-boot-starter-test") {
		exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "12"
	}
}

tasks.getByName<Jar>("jar") {
	enabled = true
}

tasks.getByName<CreateStartScripts>("startScripts") {
	mainClassName = "it.usuratonkachi.telegranloader.TelegranloaderApplicationKt"
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
	mainClassName = "it.usuratonkachi.telegranloader.TelegranloaderApplicationKt"
}
