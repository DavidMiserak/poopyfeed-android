plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    id("org.jetbrains.kotlinx.kover") version "0.9.7"
    id("com.google.gms.google-services")
}

android {
    namespace = "net.poopyfeed.pf"
    compileSdk = 36

    defaultConfig {
        applicationId = "net.poopyfeed.pf"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "net.poopyfeed.pf.HiltTestRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8000/api/v1/\"")
            buildConfigField("String", "WEB_BASE_URL", "\"http://10.0.2.2:4200/\"")
            // E2E critical path tests skip when empty (no backend). Set via -PE2E_TEST_EMAIL=... in CI.
            buildConfigField("String", "E2E_TEST_EMAIL", "\"${project.findProperty("E2E_TEST_EMAIL") ?: ""}\"")
            buildConfigField("String", "E2E_TEST_PASSWORD", "\"${project.findProperty("E2E_TEST_PASSWORD") ?: ""}\"")
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "API_BASE_URL", "\"https://poopyfeed.net/api/v1/\"")
            buildConfigField("String", "WEB_BASE_URL", "\"https://poopyfeed.net/\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    lint {
        baseline = file("lint-baseline.xml")
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.activity.ktx)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Retrofit + OkHttp
    implementation(libs.retrofit)
    implementation(libs.okhttp.logging)

    // Kotlinx
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit.kotlinx.serialization)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // UI Components
    implementation(libs.androidx.swiperefreshlayout)

    // Testing
    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.fragment.testing)
    testImplementation(libs.hilt.android.testing)
    testImplementation(libs.androidx.navigation.testing)
    kspTest(libs.hilt.compiler)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.androidx.espresso.contrib)
    androidTestImplementation(libs.androidx.espresso.intents)
    androidTestImplementation(libs.androidx.test.rules)
    kspAndroidTest(libs.hilt.compiler)

    // Google services (e.g. Firebase)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.messaging)
}

kover {
    reports {
        // Filters for all report types of all build variants
        filters {
            excludes {
                // Exclude Android-generated classes like view/data binding, R, etc.
                androidGeneratedClasses()

                // Exclude Hilt aggregated wiring
                packages("hilt_aggregated_deps")

                // Exclude dependency injection and network wiring (matches JaCoCo setup)
                packages(
                    "net.poopyfeed.pf.di",
                    "net.poopyfeed.pf.data.api",
                    "net.poopyfeed.pf.data.models",
                )

                // Exclude UI shell classes
                classes(
                    "net.poopyfeed.pf.MainActivity",
                    "net.poopyfeed.pf.PoopyFeedApplication",
                    // Hilt/Dagger generated wiring in app package
                    "net.poopyfeed.pf.*_Factory",
                    "net.poopyfeed.pf.*_HiltModules*",
                    "net.poopyfeed.pf.*_GeneratedInjector",
                )

                // Exclude Room database wiring (but not entities)
                classes(
                    "net.poopyfeed.pf.data.db.PoopyFeedDatabase*",
                    "net.poopyfeed.pf.data.db.*Dao*",
                )

                // Exclude RecyclerView adapters (UI binding; key logic covered by ViewModels/fragment tests)
                classes("net.poopyfeed.pf.*Adapter")
                // Exclude Fragment classes (UI lifecycle; logic in ViewModels, covered by fragment tests)
                classes(
                    "net.poopyfeed.pf.accounts.LoginFragment",
                    "net.poopyfeed.pf.accounts.SignupFragment",
                    "net.poopyfeed.pf.accounts.AccountSettingsFragment",
                    "net.poopyfeed.pf.HomeFragment",
                    "net.poopyfeed.pf.children.ChildrenListFragment",
                    "net.poopyfeed.pf.children.ChildDetailFragment",
                    "net.poopyfeed.pf.children.EditChildFragment",
                    "net.poopyfeed.pf.children.CreateChildBottomSheetFragment",
                    "net.poopyfeed.pf.children.ChildrenListFabBottomSheetFragment",
                    "net.poopyfeed.pf.children.ChildDetailQuickLogBottomSheetFragment",
                    "net.poopyfeed.pf.sharing.SharingFragment",
                    "net.poopyfeed.pf.sharing.PendingInvitesFragment",
                    "net.poopyfeed.pf.sharing.CreateInviteBottomSheetFragment",
                    "net.poopyfeed.pf.notifications.NotificationsFragment",
                    "net.poopyfeed.pf.notifications.PoopyFeedMessagingService",
                    "net.poopyfeed.pf.naps.NapsListFragment",
                    "net.poopyfeed.pf.naps.CreateNapBottomSheetFragment",
                    "net.poopyfeed.pf.naps.EditNapBottomSheetFragment",
                    "net.poopyfeed.pf.diapers.DiapersListFragment",
                    "net.poopyfeed.pf.diapers.CreateDiaperBottomSheetFragment",
                    "net.poopyfeed.pf.diapers.EditDiaperBottomSheetFragment",
                    "net.poopyfeed.pf.feedings.FeedingsListFragment",
                    "net.poopyfeed.pf.feedings.CreateFeedingBottomSheetFragment",
                    "net.poopyfeed.pf.feedings.EditFeedingBottomSheetFragment",
                )
            }
        }

        // LINE coverage: target 90%.
        verify {
            rule {
                minBound(90)
            }
        }
    }
}
