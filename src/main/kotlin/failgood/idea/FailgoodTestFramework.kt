package failgood.idea

import com.intellij.ide.fileTemplates.FileTemplateDescriptor
import com.intellij.lang.Language
import com.intellij.openapi.module.Module
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testIntegration.TestFramework
import org.jetbrains.kotlin.idea.KotlinLanguage
import javax.swing.Icon

class FailgoodTestFramework : TestFramework {
    companion object {
        val icon = EmptyIcon(0, 0)
    }
    override fun getName(): String = "Failgood"

    override fun getIcon(): Icon {
        return FailgoodTestFramework.icon
    }

    override fun isLibraryAttached(module: Module): Boolean {
        val scope = GlobalSearchScope.allScope(module.project)
        return JavaPsiFacade.getInstance(module.project).findClass("failgood.Test", scope) != null
    }

    override fun getLibraryPath(): String? = null

    override fun getDefaultSuperClass(): String? = null

    override fun isTestClass(clazz: PsiElement) = false

    override fun isPotentialTestClass(clazz: PsiElement) = false

    override fun findSetUpMethod(clazz: PsiElement): PsiElement? = null

    override fun findTearDownMethod(clazz: PsiElement): PsiElement? = null

    override fun findOrCreateSetUpMethod(clazz: PsiElement): PsiElement? = null

    override fun getSetUpMethodFileTemplateDescriptor(): FileTemplateDescriptor? = null

    override fun getTearDownMethodFileTemplateDescriptor(): FileTemplateDescriptor? = null

    override fun getTestMethodFileTemplateDescriptor(): FileTemplateDescriptor = FileTemplateDescriptor("meh")

    override fun isIgnoredMethod(element: PsiElement?) = false

    override fun isTestMethod(element: PsiElement?) = false

    override fun getLanguage(): Language = KotlinLanguage.INSTANCE
}
