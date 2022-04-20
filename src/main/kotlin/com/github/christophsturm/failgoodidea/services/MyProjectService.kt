package com.github.christophsturm.failgoodidea.services

import com.intellij.openapi.project.Project
import com.github.christophsturm.failgoodidea.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
