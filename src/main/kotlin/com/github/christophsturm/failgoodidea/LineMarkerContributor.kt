package com.github.christophsturm.failgoodidea

import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

private val log = logger<LineMarkerContributor>()
class LineMarkerContributor : RunLineMarkerContributor() {
    override fun getInfo(e: PsiElement): Info? {
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
        if (e is KtCallElement)
            log.warn("call: ${e}")
        return null
    }
}
