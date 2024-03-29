import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "3.0.0-RC1"
	id("io.spring.dependency-management") version "1.1.0"
	kotlin("jvm") version "1.7.20"
	kotlin("plugin.spring") version "1.7.20"
}

apply(plugin = "io.spring.dependency-management")
apply(plugin = "kotlin")
apply(plugin = "application")

group = "it.usuratonkachi"
version = "2.0.4"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
	mavenCentral()
	maven {
		url = uri("https://jitpack.io")
	}
	maven {
		url = uri("https://mvn.mchv.eu/repository/mchv/")
	}
	maven {
		url = uri("https://mvn.mchv.eu/repository/mchv/")
	}
	maven {
		url = uri("https://repo.spring.io/milestone")
	}
}

buildscript {
	repositories {
		mavenCentral()
	}
	dependencies {
		classpath("org.springframework.boot:spring-boot-gradle-plugin:3.0.0-RC1")
	}
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.springframework.boot:spring-boot-configuration-processor")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation("commons-io:commons-io:2.11.0")
	implementation("org.jsoup:jsoup:1.15.3")
	implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.13.4")
	implementation("org.telegram:telegrambots:6.1.0")
	implementation("org.projectlombok:lombok")
	implementation("org.goots:jdownloader:1.1")
	implementation("com.turn:ttorrent-core:1.5")
	implementation("io.projectreactor:reactor-core")
	testImplementation("org.springframework.boot:spring-boot-starter-test") {
		exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
	}
	implementation(platform("it.tdlight:tdlight-java-bom:2.8.5.8"))
	implementation("it.tdlight:tdlight-java:2.8.5.8")
	implementation("it.tdlight:tdlight-natives-linux-amd64:4.0.274")
	implementation("it.tdlight:tdlight-natives-linux-armhf:4.0.274")
	implementation("it.tdlight:tdlight-natives-windows-amd64:4.0.274")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "17"
	}
}

tasks.getByName<Jar>("jar") {
	enabled = true
}

springBoot {
	mainClass.set("it.usuratonkachi.telegranloader.TelegranloaderApplicationKt")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
	mainClass.set("it.usuratonkachi.telegranloader.TelegranloaderApplicationKt")
}