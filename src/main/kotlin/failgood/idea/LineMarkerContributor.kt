package failgood.idea

import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtModifierList
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

private val log = logger<LineMarkerContributor>()

class LineMarkerContributor : RunLineMarkerContributor() {
    override fun getInfo(e: PsiElement): Info? {
        val root = e.getStrictParentOfType<KtClassOrObject>() ?: return null
        val modifierList: KtModifierList? = root.modifierList
        modifierList?.annotations
        val annotationEntries = modifierList?.annotationEntries
        val isTest = (annotationEntries?.any { it.shortName?.asString() == "Test" }) == true
        if (!isTest) {
            return null
        }
        isTestOrContext(e)
        return Info(FailgoodTestFramework.icon, { "run test" })
    }

    private fun isTestOrContext(e: PsiElement): PsiElement? {
        val declaration = e.getStrictParentOfType<KtCallElement>()
        if (declaration is KtCallElement) {
            val calleeExpression = declaration.calleeExpression as KtNameReferenceExpression
            val name = calleeExpression.getReferencedName()
            if (name == "it" || name == "test") {
                val testName: ValueArgument = declaration.valueArguments.first()
                val argumentExpression = testName.getArgumentExpression()
                val references = argumentExpression?.references
                val psiReference = references?.find { it is KtSimpleNameReference }
                println(psiReference?.resolve())
//                println(testName.toString())
            }
        }
        log.warn("call: $e")
        return null
    }
}
