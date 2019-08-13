
ext{
    version=20
}

buildscript{
    repositories{
        google()
        jcenter()
    }

    dependencies{
        classpath("com.android.tools.build:gradle:3.3.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.20")
    }

}

allprojects{
    repositories{
        google()
        jcenter()
    }
}

tasks.register("clean").configure{
    delete("build")
}