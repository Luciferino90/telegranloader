import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "2.6.6"
	id("io.spring.dependency-management") version "1.0.11.RELEASE"
	kotlin("jvm") version "1.5.31"
	kotlin("plugin.spring") version "1.5.31"
}

apply(plugin = "io.spring.dependency-management")
apply(plugin = "kotlin")
apply(plugin = "application")



group = "it.usuratonkachi"
version = "2.0.2"
java.sourceCompatibility = JavaVersion.VERSION_16

repositories {
	mavenCentral()
	maven {
		url = uri("https://jitpack.io")
	}
	maven {
		url = uri("https://mvn.mchv.eu/repository/mchv/")
	}
}

buildscript {
	repositories {
		mavenCentral()
	}
	dependencies {
		classpath("org.springframework.boot:spring-boot-gradle-plugin:2.6.6")
	}
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.springframework.boot:spring-boot-configuration-processor")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation("commons-io:commons-io:2.5")
	implementation("org.telegram:telegrambots:5.7.1")
	implementation("org.projectlombok:lombok")
	implementation("org.goots:jdownloader:1.1")
	implementation("com.turn:ttorrent-core:1.5")
	implementation("io.projectreactor:reactor-core")
	testImplementation("org.springframework.boot:spring-boot-starter-test") {
		exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
	}
	implementation(platform("it.tdlight:tdlight-java-bom:2.8.2.2"))
	implementation("it.tdlight:tdlight-java")
	implementation("it.tdlight:tdlight-natives-linux-amd64")
	implementation("it.tdlight:tdlight-natives-linux-armhf")
	implementation("it.tdlight:tdlight-natives-windows-amd64")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "16"
	}
}

tasks.getByName<Jar>("jar") {
	enabled = true
}

tasks.getByName<CreateStartScripts>("startScripts") {
	mainClassName = "it.usuratonkachi.telegranloader.TelegranloaderApplicationKt"
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
	mainClass.set("it.usuratonkachi.telegranloader.TelegranloaderApplicationKt")
}