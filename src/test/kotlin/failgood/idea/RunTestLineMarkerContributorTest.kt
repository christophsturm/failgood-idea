package failgood.idea

import com.intellij.jarRepository.JarRepositoryManager
import com.intellij.jarRepository.RemoteRepositoryDescription
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.pom.java.LanguageLevel
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import com.intellij.util.PsiErrorElementUtil
import java.io.File
import org.jetbrains.idea.maven.utils.library.RepositoryLibraryProperties
import org.jetbrains.kotlin.psi.KtFile

val CI = System.getenv("CI") != null

class RunTestLineMarkerContributorTest : LightJavaCodeInsightFixtureTestCase() {
    private val projectDescriptor =
        object : ProjectDescriptor(LanguageLevel.HIGHEST) {
            override fun configureModule(
                module: Module,
                model: ModifiableRootModel,
                contentEntry: ContentEntry
            ) {
                super.configureModule(module, model, contentEntry)
                /*
                PsiTestUtil.addProjectLibrary(model, "annotations", listOf(PathUtil.getJarPathForClass(ApiStatus.OverrideOnly::class.java)))
                PsiTestUtil.newLibrary("library")
                    .classesRoot(testDataPath)
                    .externalAnnotationsRoot("$testDataPath/since-2.0")
                    .addTo(model)*/

                addFromMaven(model, "dev.failgood:failgood:0.8.3", false, DependencyScope.COMPILE)
            }
        }

    override fun getProjectDescriptor(): ProjectDescriptor {
        return projectDescriptor
    }

    fun testContributeRunInfo() {
        val psiFile = myFixture.configureByFile("FailGoodTests.kt")
        assertInstanceOf(psiFile, KtFile::class.java)
        assertFalse(PsiErrorElementUtil.hasErrors(project, psiFile.virtualFile))
        val findElementAt = psiFile.findElementAt(myFixture.caretOffset)
        val info = RunTestLineMarkerContributor().getInfo(findElementAt!!)
        assert(info != null)
    }

    override fun getTestDataPath(): String = File("src/test/testData/").absolutePath

    fun addFromMaven(
        model: ModifiableRootModel,
        mavenCoordinates: String,
        includeTransitiveDependencies: Boolean,
        dependencyScope: DependencyScope?
    ) {
        val remoteRepositoryDescriptions = RemoteRepositoryDescription.DEFAULT_REPOSITORIES
        val libraryProperties =
            RepositoryLibraryProperties(mavenCoordinates, includeTransitiveDependencies)
        val roots =
            JarRepositoryManager.loadDependenciesModal(
                model.project,
                libraryProperties,
                false,
                false,
                null,
                remoteRepositoryDescriptions
            )
        val tableModel = model.moduleLibraryTable.modifiableModel
        val library = tableModel.createLibrary(mavenCoordinates)
        val libraryModel = library.modifiableModel
        check(!roots.isEmpty()) { String.format("No roots for '%s'", mavenCoordinates) }
        for (root in roots) {
            libraryModel.addRoot(root.file, root.type)
        }
        val libraryOrderEntry =
            model.findLibraryOrderEntry(library)
                ?: throw IllegalStateException(
                    "Unable to find registered library $mavenCoordinates"
                )
        libraryOrderEntry.scope = dependencyScope!!
        libraryModel.commit()
        tableModel.commit()
    }
}
