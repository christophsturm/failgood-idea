package failgood.idea

import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.PsiElement

private val log = logger<RunTestLineMarkerContributor>()

class RunTestLineMarkerContributor : RunLineMarkerContributor() {
    override fun getInfo(e: PsiElement): Info? {
        val uniqueId = UniqueIdProducer.computeUniqueId(e) ?: return null
        //        log.warn("returning $uniqueId for ${e.text}")
        return Info(
            AllIcons.RunConfigurations.TestState.Run,
            { uniqueId.friendlyName },
            *ExecutorAction.getActions(1)
        )
    }
}
