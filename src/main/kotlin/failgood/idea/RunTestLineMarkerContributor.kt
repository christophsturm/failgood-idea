package failgood.idea

import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtModifierList
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.ValueArgument
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
        return Info(FailgoodTestFramework.icon, { "run test $path"  })
    }


    private fun getPathToTest(e: PsiElement): List<@NlsSafe String>? {
        val declaration = e.getStrictParentOfType<KtCallElement>()
        if (declaration is KtCallElement) {
            val calleeExpression = declaration.calleeExpression as? KtNameReferenceExpression ?: return null
            val calleeName = calleeExpression.getReferencedName()
            if (calleeName == "it" || calleeName == "test") {
                val testName = getFirstParameter(declaration)
                val parent = declaration.getStrictParentOfType<KtCallElement>()
                val contextName = getFirstParameter(parent!!)
                if (contextName == null || testName == null)
                    return null
                return listOf(contextName, testName)
            }
        }
        return null
    }

    private fun getFirstParameter(declaration: KtCallElement): @NlsSafe String? {
        val firstArgument: ValueArgument = declaration.valueArguments.first()
        val argumentExpression = firstArgument.getArgumentExpression()
        val references = argumentExpression?.references
        val singleReference = references?.singleOrNull()
        val testName = singleReference?.canonicalText
        return testName
    }

}
/**
 * checks if a class is a failgood test class (has a @Test Annotation)
 */
private fun KtClassOrObject.isTestClass(): Boolean {
    val modifierList: KtModifierList? = modifierList
    val annotationEntries = modifierList?.annotationEntries
    val isTestClass = (annotationEntries?.any { it.shortName?.asString() == "Test" }) == true
    return isTestClass
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
