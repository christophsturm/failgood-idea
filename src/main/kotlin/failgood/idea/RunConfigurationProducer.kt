package failgood.idea

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.junit.JUnitConfiguration
import com.intellij.execution.junit.JUnitConfigurationType
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement

private val log = logger<RunConfigurationProducer>()

internal class RunConfigurationProducer : LazyRunConfigurationProducer<JUnitConfiguration>() {
    override fun getConfigurationFactory(): ConfigurationFactory =
        JUnitConfigurationType.getInstance().configurationFactories.single()

    override fun setupConfigurationFromContext(
        configuration: JUnitConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        val uniqueId = UniqueIdProducer.computeUniqueId(sourceElement.get()) ?: return false
        val data = configuration.persistentData
        data.setUniqueIds(uniqueId.uniqueId)
        data.TEST_OBJECT = JUnitConfiguration.TEST_UNIQUE_ID
        configuration.name = "run ${uniqueId.friendlyName}"
        return true
    }

    override fun isConfigurationFromContext(
        configuration: JUnitConfiguration,
        context: ConfigurationContext
    ): Boolean {
        // we only care about uniqueid run configs
        if (configuration.testType != JUnitConfiguration.TEST_UNIQUE_ID) return false
        val uniqueId = context.psiLocation?.let { UniqueIdProducer.computeUniqueId(it) }?.uniqueId
        return uniqueId != null &&
            configuration.persistentData.uniqueIds.single<@NlsSafe String?>() == uniqueId
    }
}
