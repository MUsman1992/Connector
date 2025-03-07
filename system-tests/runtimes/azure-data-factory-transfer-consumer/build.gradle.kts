/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

plugins {
    `java-library`
    id("application")
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":core:control-plane:control-plane-core"))
    implementation(project(":core:data-plane-selector:data-plane-selector-core"))

    implementation(project(":extensions:common:api:api-observability"))

    implementation(project(":extensions:common:configuration:configuration-filesystem"))
    implementation(project(":extensions:common:iam:iam-mock"))

    implementation(project(":extensions:control-plane:api:management-api"))

    implementation(project(":data-protocols:dsp"))

    implementation(project(":extensions:control-plane:provision:provision-blob"))
    implementation(project(":extensions:common:vault:vault-azure"))
    implementation(project(":core:common:util"))

    api(libs.jakarta.rsApi)
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    mergeServiceFiles()
    archiveFileName.set("consumer.jar")
}

edcBuild {
    publish.set(false)
}
