package failgood.idea

import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtModifierList
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

private val log = logger<LineMarkerContributor>()
class LineMarkerContributor : RunLineMarkerContributor() {
    override fun getInfo(e: PsiElement): Info? {
        val root = e.getStrictParentOfType<KtClassOrObject>() ?: return null
        val modifierList: KtModifierList? = root.modifierList
        val annotation = modifierList?.annotations
        val annotationEntries = modifierList?.annotationEntries
        val isTest = (annotationEntries?.any {it.shortName?.asString() == "Test"}) == true
        if (!isTest)
            return null
        isClassOrObject(e)
        isTestOrContext(e)
        return null
    }

    private fun isClassOrObject(e: PsiElement): PsiElement? {
        val declaration = e.getStrictParentOfType<KtNamedDeclaration>() ?: return null
        if (declaration.nameIdentifier != e) return null
        if (declaration !is KtClassOrObject) return null

        log.warn("found declaration:"+ declaration.name)
        return null
    }
    private fun isTestOrContext(e: PsiElement): PsiElement? {
        val declaration = e.getStrictParentOfType<KtCallElement>()
        if (declaration is KtCallElement) {

            val calleeExpression = declaration.calleeExpression as KtNameReferenceExpression
            val name = calleeExpression.getReferencedName()
            if (name == "it" || name == "test") {
                val testName: ValueArgument = declaration.valueArguments.first()
                log.warn(testName.toString())
            }
        }
            log.warn("call: ${e}")
        return null
    }
}
