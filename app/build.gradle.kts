import groovy.json.JsonSlurper

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.google.services)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.orderflow.autoresponder"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.orderflow.autoresponder"
        minSdk = 23
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.1"
        // Explicit rollout switch; default keeps the working legacy path.
        buildConfigField("boolean", "DEVICE_ENROLLMENT_ENABLED", (providers.gradleProperty("chatzyDeviceEnrollment").orNull == "true").toString())

        testInstrumentationRunner = if (providers.gradleProperty("chatzySupportProbe").orNull == "true")
            "com.orderflow.autoresponder.help.SupportProbeInstrumentation"
        else "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        ndk {
            abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a", "x86_64"))
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
        // Help tests read the editable bundled JSON directly; rerun when it changes.
        unitTests.all { it.inputs.file("src/main/res/raw/chatzy_help.json") }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Hilt
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Networking
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation("com.google.firebase:firebase-functions")
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)

    debugImplementation(libs.androidx.ui.tooling)

    // Unit Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
}

// Standard Google Services source-set precedence; never copy or replace credentials.
// Use -PexpectedFirebaseProjectId=<actual-project-id> in release CI to prevent cross-project builds.
listOf("debug", "release").forEach { buildType ->
    tasks.matching { it.name == "process${buildType.replaceFirstChar { c -> c.uppercaseChar() }}GoogleServices" }
        .configureEach {
            inputs.property("expectedFirebaseProjectId", providers.gradleProperty("expectedFirebaseProjectId").orElse(""))
            doFirst {
                val candidates = listOf(
                    file("src/$buildType/google-services.json"),
                    file("google-services.json")
                )
                val config = candidates.firstOrNull { it.isFile }
                    ?: throw GradleException("Missing genuine google-services.json for $buildType; package must be com.orderflow.autoresponder")
                @Suppress("UNCHECKED_CAST")
                val json = JsonSlurper().parse(config) as Map<String, Any?>
                val clients = json["client"] as? List<*> ?: emptyList<Any>()
                val packageMatches = clients.any { client ->
                    val info = (client as? Map<*, *>)?.get("client_info") as? Map<*, *>
                    val androidInfo = info?.get("android_client_info") as? Map<*, *>
                    androidInfo?.get("package_name") == "com.orderflow.autoresponder"
                }
                if (!packageMatches) throw GradleException("Firebase Android configuration package mismatch; expected com.orderflow.autoresponder")
                val actualProject = (json["project_info"] as? Map<*, *>)?.get("project_id") as? String
                if (actualProject.isNullOrBlank()) throw GradleException("Firebase configuration is missing project_id")
                val expectedProject = providers.gradleProperty("expectedFirebaseProjectId").orNull
                if (expectedProject != null && actualProject != expectedProject) {
                    throw GradleException("Firebase configuration does not match expectedFirebaseProjectId; refusing build")
                }
            }
        }
}
