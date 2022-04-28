package failgood.idea

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.pom.java.LanguageLevel
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import com.intellij.testFramework.fixtures.MavenDependencyUtil
import com.intellij.util.PsiErrorElementUtil
import org.jetbrains.kotlin.psi.KtFile

class LineMarkerContributorTest : LightJavaCodeInsightFixtureTestCase() {
    private val projectDescriptor = object : ProjectDescriptor(LanguageLevel.HIGHEST) {
        override fun configureModule(module: Module, model: ModifiableRootModel, contentEntry: ContentEntry) {
            super.configureModule(module, model, contentEntry)
/*
            PsiTestUtil.addProjectLibrary(model, "annotations", listOf(PathUtil.getJarPathForClass(ApiStatus.OverrideOnly::class.java)))
            PsiTestUtil.newLibrary("library")
                .classesRoot(testDataPath)
                .externalAnnotationsRoot("$testDataPath/since-2.0")
                .addTo(model)*/
            MavenDependencyUtil.addFromMaven(model, "d.failgood:failgood:0.6.1")
        }
    }

    override fun getProjectDescriptor(): ProjectDescriptor {
        return projectDescriptor
    }

    fun testContributeRunInfo() {
        val psiFile = myFixture.configureByFile("FailgoodTests.kt")
        assertInstanceOf(psiFile, KtFile::class.java)
        assertFalse(PsiErrorElementUtil.hasErrors(project, psiFile.virtualFile))
        val info = LineMarkerContributor().getInfo(psiFile.findElementAt(myFixture.caretOffset)!!)
        println(info)

    }

    override fun getTestDataPath() = "src/test/testData/"
}
