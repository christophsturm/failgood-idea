package failgood.idea.services

import com.intellij.openapi.project.Project
import failgood.idea.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
