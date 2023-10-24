package failgood.idea

import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclarationWithInitializer
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

object UniqueIdProducer {
    fun computeUniqueId(e: PsiElement): UniqueId? {
        if (e.firstChild != null)
            return null // "line markers should only be added to leaf elements"
        val callElement = e.getKtCallElement() ?: return null

        val containingClass = callElement.getStrictParentOfType<KtClassOrObject>() ?: return null
        if (!containingClass.isTestClass()) {
            return null
        }
        val className = containingClass.getQualifiedName()
        val path = getPathToTest(callElement) ?: return null

        return UniqueId("[engine:failgood]/[class:${path.first()}($className)]/" +
            path.drop(1).joinToString("/") { "[class:$it]" }, path.last())
    }

    // return a ktCallElement if it is the parent or grandparent of the psiElement
    private fun PsiElement.getKtCallElement(): KtCallElement? {
        return if (this is KtCallElement) this
        else
            parent.let {
                if (it is KtCallElement) it
                else it.parent.let { if (it is KtCallElement) it else null }
            }
    }

    val runnableNodeNames = setOf("it", "test", "describe", "context")

    private fun getPathToTest(declaration: KtCallElement): List<@NlsSafe String>? {
        val calleeName = getCalleeName(declaration) ?: return null
        if (!runnableNodeNames.contains(calleeName)) return null
        return buildList<String> {
                add(getFirstParameter(declaration) ?: return null)
                var nextDeclaration = declaration
                while (true) {
                    nextDeclaration =
                        nextDeclaration.getStrictParentOfType<KtCallElement>() ?: break
                    add(getFirstParameter(nextDeclaration) ?: return null)
                }
            }
            .reversed()
    }

    private fun getCalleeName(declaration: KtCallElement): String? {
        val calleeExpression = declaration.calleeExpression
        return (calleeExpression as? KtNameReferenceExpression)?.getReferencedName()
    }

    /** get the string value of the first argument (must be a string or a class) */
    private fun getFirstParameter(declaration: KtCallElement): @NlsSafe String? =
        when (
            val firstParameter =
                declaration.valueArgumentList?.children?.singleOrNull()?.children?.singleOrNull()
        ) {
            is KtStringTemplateExpression -> firstParameter.asString()
            is KtClassLiteralExpression -> firstParameter.receiverExpression?.text
            is KtSimpleNameExpression -> {
                val valDeclaration =
                    firstParameter.references
                        .first() // { it.resolve() is KtDeclarationWithInitializer }
                        ?.resolve() as? KtDeclarationWithInitializer
                val initializer = valDeclaration?.initializer as? KtStringTemplateExpression
                initializer?.asString()
            }
            else -> null
        }

    private fun KtStringTemplateExpression.asString() =
        entries.joinToString("") { it.text.replace("\\\"", "\"") }
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
data class UniqueId(val uniqueId: String, val friendlyName: String)
