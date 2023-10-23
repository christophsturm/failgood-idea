package failgood.idea

import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

object UniqueIdProducer {
    fun getUniqueId(e: PsiElement): String? {
        if (e.firstChild != null)
            return null // "line markers should only be added to leaf elements"

        val callElement =
            if (e is KtCallElement) e
            else
                e.parent.let {
                    if (it is KtCallElement) it
                    else it.parent.let { if (it is KtCallElement) it else return null }
                }

        val containingClass = callElement.getStrictParentOfType<KtClassOrObject>() ?: return null
        if (!containingClass.isTestClass()) {
            return null
        }
        val className = containingClass.getQualifiedName()
        val path = getPathToTest(callElement) ?: return null

        return "[engine:failgood]/[class:${path.first()}($className)]/[class:${path[1]}]"
    }

    private fun getPathToTest(declaration: KtCallElement): List<@NlsSafe String>? {
        val calleeName = getCalleeName(declaration) ?: return null
        if (calleeName != "it" && calleeName != "test") return null
        val testName = getFirstParameter(declaration) ?: return null
        val parent = declaration.getStrictParentOfType<KtCallElement>()
        val contextName = getFirstParameter(parent!!) ?: return null
        return listOf(contextName, testName)
    }

    private fun getCalleeName(declaration: KtCallElement): String? {
        val calleeExpression = declaration.calleeExpression
        return (calleeExpression as? KtNameReferenceExpression)?.getReferencedName()
    }

    private fun getFirstParameter(declaration: KtCallElement): @NlsSafe String? {
        val ste =
            declaration.valueArgumentList?.children?.singleOrNull()?.children?.singleOrNull()
                ?: return null
        val ste2 = ste as? KtStringTemplateExpression ?: return null
        return ste2.entries.joinToString("") { it.text.replace("\\\"", "\"") }
    }
}
/** checks if a class is a failgood test class (has a @Test Annotation) */
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
