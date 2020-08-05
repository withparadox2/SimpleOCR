package com.withparadox2.simpleocr

import org.gradle.api.Plugin
import org.gradle.api.Project

public class TemplatePlugin implements Plugin<Project> {
  def isAlone = false

  @Override
  void apply(Project project) {
    def isBundleAll = isBundleAll(project)
    isAlone = project.rootProject.hasProperty('alone') || isBundleAll
    if (isAlone) {
      project.apply([plugin: 'com.android.application'])
    } else {
      project.apply([plugin: 'com.android.library'])
    }

    if (isAlone) {
      project.afterEvaluate {
        project.android.applicationVariants.all { variant ->
          def varName = variant.name
          def varNameCap = varName.capitalize()
          def assembleTask = project.tasks.findByName("assemble$varNameCap")
          def buildType = varNameCap.contains('Debug') ? 'debug' : 'release'

          if (assembleTask) {
            if (buildType == 'debug') {
              def bundleTask = project.rootProject.tasks.findByName('bundleAll')
              bundleTask.dependsOn(assembleTask)
            }
            assembleTask.doLast {
              project.copy {
                from "build/outputs/apk/${buildType}"
                into project.rootProject.file("app/src/main/assets")
                rename {
                  project.name.toLowerCase() + ".apk"
                }
                include "*${buildType}*.apk"
              }
            }
          }
        }
      }
    }
  }

  static boolean isBundleAll(Project project) {
    return project.gradle.startParameter.taskNames.any {
      return it.contains("bundleAll")
    }
  }
}