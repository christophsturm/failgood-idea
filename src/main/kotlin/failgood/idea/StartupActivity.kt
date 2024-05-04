package failgood.idea

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.settings.TestRunner
import org.jetbrains.plugins.gradle.util.GradleConstants

class StartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        val gradleSettings: MutableCollection<GradleProjectSettings> =
            GradleSettings.getInstance(project).linkedProjectsSettings
        if (gradleSettings.any { it.testRunner != TestRunner.PLATFORM }) {
            val notification =
                Notification(
                    "failgood",
                    "Incompatible Gradle settings",
                    "Failgood works best when tests run via IDEA and not via Gradle",
                    NotificationType.ERROR
                )

            notification.addAction(
                NotificationAction.createSimple("Open Gradle settings") {
                    notification.expire()
                    ShowSettingsUtil.getInstance()
                        .showSettingsDialog(project, GradleConstants.SYSTEM_ID.readableName)
                }
            )

            Notifications.Bus.notify(notification, project)
        }
    }
}
