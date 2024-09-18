package failgood.idea

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.junit.JUnitConfiguration
import com.intellij.execution.junit.JUnitConfigurationType
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement

private val log = logger<RunConfigurationProducer>()

internal class RunConfigurationProducer : LazyRunConfigurationProducer<JUnitConfiguration>() {
    override fun getConfigurationFactory(): ConfigurationFactory =
        JUnitConfigurationType.getInstance().configurationFactories.singleOrNull()
            ?: throw FailgoodPluginException(
                "JUnitConfigurationType.getInstance().configurationFactories is supposed to have only one entry but has:" +
                    JUnitConfigurationType.getInstance().configurationFactories.joinToString())

    override fun setupConfigurationFromContext(
        configuration: JUnitConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        // not sure if this actually happens, but for example the gradle configuration producer
        // checks for it, so we do too
        if (sourceElement.isNull) return false

        val uniqueId = UniqueIdProducer.computeUniqueId(sourceElement.get()) ?: return false
        val data = configuration.persistentData
        data.setUniqueIds(uniqueId.uniqueId)
        data.TEST_OBJECT = JUnitConfiguration.TEST_UNIQUE_ID
        configuration.name = uniqueId.name
        log.info("creating run config for $uniqueId")
        return true
    }

    override fun isConfigurationFromContext(
        configuration: JUnitConfiguration,
        context: ConfigurationContext
    ): Boolean {
        // we only care about uniqueid run configs that have one single unique id
        if (configuration.testType != JUnitConfiguration.TEST_UNIQUE_ID) return false
        val singleUniqueId = configuration.persistentData.uniqueIds.singleOrNull() ?: return false

        val uniqueId =
            context.psiLocation?.let { UniqueIdProducer.computeUniqueId(it) } ?: return false
        if (singleUniqueId == uniqueId.uniqueId) {
            if (configuration.name == uniqueId.name) {
                log.info("${configuration.name} is from us")
                return true
            }
            log.info("${configuration.name} is not from us but has same uniqueid")
        }
        return false
    }

    override fun shouldReplace(
        self: ConfigurationFromContext,
        other: ConfigurationFromContext
    ): Boolean {
        // our config should replace the other config if the other config is not a Junit
        // Configuration and if the other config is not for a unique id.
        // (because that means that the other config is probably more broad and ours is more
        // specific)

        //        val c = self.configuration as? JUnitConfiguration ?: return false
        val o = other.configuration as? JUnitConfiguration ?: return true
        return o.testType != JUnitConfiguration.TEST_UNIQUE_ID
    }
}
