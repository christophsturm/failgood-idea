package failgood.idea

import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.PsiElement

private val log = logger<RunTestLineMarkerContributor>()

class RunTestLineMarkerContributor : RunLineMarkerContributor() {
    override fun getInfo(e: PsiElement): Info? {
        val uniqueId = UniqueIdProducer.getUniqueId(e) ?: return null

        // [engine:failgood]/[class:SingleTestExecutor(failgood.internal.SingleTestExecutorTest)]/[class:test execution]/[method:executes a single test]
        return Info(
            AllIcons.RunConfigurations.TestState.Run,
            { "run $uniqueId" },
            *ExecutorAction.getActions(1)
        )
    }
}
