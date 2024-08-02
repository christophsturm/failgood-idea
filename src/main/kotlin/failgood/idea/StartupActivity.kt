package failgood.idea

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import org.jetbrains.plugins.gradle.config.GradleSettingsListenerAdapter
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.settings.GradleSettingsListener
import org.jetbrains.plugins.gradle.settings.TestRunner
import org.jetbrains.plugins.gradle.util.GradleConstants

class StartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        val connection = project.messageBus.connect()
        connection.subscribe(GradleSettingsListener.TOPIC, GradleSettingsChangeListener(project))

        val gradleSettings: MutableCollection<GradleProjectSettings> =
            GradleSettings.getInstance(project).linkedProjectsSettings
        if (gradleSettings.any { it.testRunner != TestRunner.PLATFORM }) {
            showNotification(project)
        }
    }
}

class GradleSettingsChangeListener(private val project: Project) : GradleSettingsListenerAdapter() {
    override fun onTestRunnerChange(currentTestRunner: TestRunner, linkedProjectPath: String) {
        if (currentTestRunner != TestRunner.PLATFORM) showNotification(project)
    }
}

private fun showNotification(project: Project) {
    val notification =
        Notification(
            "failgood",
            "Failgood is incompatible with your Gradle settings",
            "Please set \"Run Tests Using\" to \"IntelliJ IDEA\"",
            NotificationType.ERROR
        )

    notification.addAction(
        NotificationAction.createSimple("Open Gradle settings") {
            notification.expire()
            ShowSettingsUtil.getInstance()
                .showSettingsDialog(project, GradleConstants.SYSTEM_ID.readableName)
        })

    Notifications.Bus.notify(notification, project)
}
