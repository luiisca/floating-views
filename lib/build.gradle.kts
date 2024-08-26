import com.vanniktech.maven.publish.SonatypeHost

plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.maven.publish)
}

android {
  namespace = "io.github.luiisca.floating.views"
  compileSdk = libs.versions.compileSdk.get().toInt()

  defaultConfig {
    minSdk = libs.versions.minSdk.get().toInt()

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    consumerProguardFiles("consumer-rules.pro")
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
  kotlinOptions {
    jvmTarget = "1.8"
  }
  buildFeatures {
    compose = true
  }
  composeOptions {
    kotlinCompilerExtensionVersion = "1.5.3"
  }
}

mavenPublishing {
  coordinates("io.github.luiisca", "floating.views", libs.versions.floating.views.get())
  publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
  signAllPublications()

  pom {
    name.set("Floating Views")
    description.set("Effortlessly create customizable floating UI elements.")
    inceptionYear.set("2024")
    url.set("https://github.com/luiisca/floating-views")
    licenses {
      license {
        name.set("MIT License")
        url.set("https://mit-license.org/license.txt")
        distribution.set("https://mit-license.org/license.txt")
      }
    }
    developers {
      developer {
        id.set("luiisca")
        name.set("Luis Cadillo")
        url.set("https://github.com/luiisca")
      }
    }
    scm {
      url.set("https://github.com/luiisca/floating-views")
      connection.set("scm:git:git://github.com/luiisca/floating-views.git")
      developerConnection.set("scm:git:ssh://git@github.com/luiisca/floating-views.git")
    }
  }
}

dependencies {
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.dynamic.animation)
  implementation(libs.androidx.saved.state)
  implementation(libs.androidx.compose.foundation)
}