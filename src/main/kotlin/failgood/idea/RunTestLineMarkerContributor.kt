package failgood.idea

import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

private val log = logger<RunTestLineMarkerContributor>()

class RunTestLineMarkerContributor : RunLineMarkerContributor() {
    override fun getInfo(e: PsiElement): Info? {
        if (e.firstChild != null)
            return null // "line markers should only be added to leaf elements"

        val containingClass = e.getStrictParentOfType<KtClassOrObject>() ?: return null
        if (!containingClass.isTestClass()) {
            return null
        }
        val className = containingClass.getQualifiedName()
        val path = getPathToTest(e) ?: return null

        val uniqueId="[engine:failgood]/[class:${path.first()}($className)]:/[class:${path[1]}]"
        //[engine:failgood]/[class:SingleTestExecutor(failgood.internal.SingleTestExecutorTest)]/[class:test execution]/[method:executes a single test]
        return Info(AllIcons.RunConfigurations.TestState.Run, { "run test $path"  }, *ExecutorAction.getActions())
    }


    private fun getPathToTest(e: PsiElement): List<@NlsSafe String>? {
        val declaration = e.getStrictParentOfType<KtCallElement>()
        if (declaration is KtCallElement) {
            val calleeExpression = declaration.calleeExpression as? KtNameReferenceExpression ?: return null
            val calleeName = calleeExpression.getReferencedName()
            if (calleeName == "it" || calleeName == "test") {
                val testName = getFirstParameter(declaration) ?: return null
                val parent = declaration.getStrictParentOfType<KtCallElement>()
                val contextName = getFirstParameter(parent!!) ?: return null
                return listOf(contextName, testName)
            }
        }
        return null
    }

    private fun getFirstParameter(declaration: KtCallElement): @NlsSafe String? {
        val ste = declaration.valueArgumentList?.children?.singleOrNull()?.children?.singleOrNull() ?: return null
        val ste2 = ste as? KtStringTemplateExpression ?: return null
        return ste2.entries.joinToString("") { it.text.replace("\\\"", "\"") }
    }

}

/**
 * checks if a class is a failgood test class (has a @Test Annotation)
 */
private fun KtClassOrObject.isTestClass(): Boolean {
    val annotationEntries = this.modifierList?.annotationEntries
    return (annotationEntries?.any { it.shortName?.asString() == "Test" }) == true
}
// protected method copied from KtClassOrObject.kt
fun KtClassOrObject.getQualifiedName(): String? {
    val stub = stub
    if (stub != null) {
        val fqName = stub.getFqName()
        return fqName?.asString()
    }

    val parts = mutableListOf<String>()
    var current: KtClassOrObject? = this
    while (current != null) {
        val name = current.name ?: return null
        parts.add(name)
        current = PsiTreeUtil.getParentOfType(current, KtClassOrObject::class.java)
    }
    val file = containingFile as? KtFile ?: return null
    val fileQualifiedName = file.packageFqName.asString()
    if (fileQualifiedName.isNotEmpty()) {
        parts.add(fileQualifiedName)
    }
    parts.reverse()
    return parts.joinToString(separator = ".")
}
