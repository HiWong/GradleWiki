plugins{
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-android-extensions")
}

android{
    compileSdkVersion(28)
    defaultConfig {
        applicationId="wang.imallen.blog.kotlindsl"
        minSdkVersion(15)  //这里就必须区分方法和属性赋值
        targetSdkVersion(28)
        versionCode =1
        versionName ="1.0"
        testInstrumentationRunner ="android.support.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled=false  //注意这里是isMinifyEnabled而不是minifyEnabled
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies{

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.3.20")
    implementation("com.android.support:appcompat-v7:28.0.0")
    implementation("com.android.support.constraint:constraint-layout:1.1.3")
    testImplementation("junit:junit:4.12")
    androidTestImplementation("com.android.support.test:runner:1.0.2")
    androidTestImplementation("com.android.support.test.espresso:espresso-core:3.0.2")

}