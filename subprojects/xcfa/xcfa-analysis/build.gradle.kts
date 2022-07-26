plugins {
    id("kotlin-common")
}

dependencies {
    implementation(project(":theta-common"))
    implementation(project(":theta-core"))
    implementation(project(":theta-analysis"))
    implementation(project(":theta-xcfa"))
    testImplementation(project(":theta-c2xcfa"))
    testImplementation(project(":theta-solver-z3"))
    testImplementation(project(":theta-solver"))
}
