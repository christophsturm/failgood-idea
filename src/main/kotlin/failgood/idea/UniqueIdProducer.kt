package failgood.idea

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
    fun computeUniqueId(e: PsiElement): FriendlyUniqueId? {
        if (e.firstChild != null)
            return null // "line markers should only be added to leaf elements"

        // show the run marker only on the identifier of the call
        // (for example the "it" or "describe") to avoid duplicate markers
        if (e.elementType.toString() != "IDENTIFIER") return null
        val callElement = e.getKtCallElement() ?: return null

        val containingClass = callElement.getStrictParentOfType<KtClassOrObject>() ?: return null
        if (!containingClass.isTestClass()) {
            return null
        }
        val path = getPathToTest(callElement) ?: return null
        // only root may be unnamed
        if (path.drop(1).any { it == "unnamed" }) return null
        // if root is unnamed its named like the class
        val testCollectionName =
            path.first().let { if (it == "unnamed") containingClass.name else it }

        return FriendlyUniqueId(
            "[engine:failgood]/[class:$testCollectionName(${containingClass.getQualifiedName()})]/" +
                path.drop(1).joinToString("/") { "[class:$it]" },
            path.last()
        )
    }

    // return a ktCallElement if it is the parent or grandparent of the psiElement
    private fun PsiElement.getKtCallElement(): KtCallElement? {
        return if (this is KtCallElement) this
        else
            parent.let { parent ->
                if (parent is KtCallElement) parent
                else
                    parent.parent.let { grandParent ->
                        if (grandParent is KtCallElement) grandParent else null
                    }
            }
    }

    private val runnableNodeNames = setOf("it", "test", "describe", "context")

    private fun getPathToTest(declaration: KtCallElement): List<String>? {
        val calleeName = getCalleeName(declaration) ?: return null
        if (!runnableNodeNames.contains(calleeName)) return null
        return buildList {
                add(getContextOrTestName(declaration) ?: return null)
                var nextDeclaration = declaration
                while (true) {
                    nextDeclaration =
                        nextDeclaration.getStrictParentOfType<KtCallElement>() ?: break
                    add(getContextOrTestName(nextDeclaration) ?: return null)
                }
            }
            .reversed()
    }

    private fun getCalleeName(declaration: KtCallElement): String? {
        val calleeExpression = declaration.calleeExpression
        return (calleeExpression as? KtNameReferenceExpression)?.getReferencedName()
    }

    /** get the string value of the first argument (must be a string or a class) */
    private fun getContextOrTestName(declaration: KtCallElement): String? =
        when (
            val firstParameter =
                declaration.valueArgumentList?.children?.firstOrNull()?.children?.singleOrNull()
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
            null -> {
                if (getCalleeName(declaration) == "tests") "unnamed" else null
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

data class FriendlyUniqueId(val uniqueId: String, val friendlyName: String)
