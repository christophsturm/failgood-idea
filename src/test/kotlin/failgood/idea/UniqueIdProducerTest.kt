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
import org.intellij.lang.annotations.Language
import org.jetbrains.idea.maven.utils.library.RepositoryLibraryProperties
import org.jetbrains.kotlin.psi.KtFile

// val CI = System.getenv("CI") != null

class UniqueIdProducerTest : LightJavaCodeInsightFixtureTestCase() {
    private val projectDescriptor =
        object : ProjectDescriptor(LanguageLevel.HIGHEST) {
            override fun configureModule(
                module: Module,
                model: ModifiableRootModel,
                contentEntry: ContentEntry
            ) {
                super.configureModule(module, model, contentEntry)
                addFromMaven(model, "dev.failgood:failgood:0.8.3", false, DependencyScope.COMPILE)
            }
        }

    override fun getProjectDescriptor(): ProjectDescriptor = projectDescriptor

    fun testComputesUniqueIdForNestedTest() {
        test(
            """
    val context =
        describe("level 1") { describe("level 2") { i<caret>t("test") { assert(true) } } }
""",
            "[engine:failgood]/[class:level 1(FailGoodTests)]/[class:level 2]/[class:test]"
        )
    }

    fun testComputesUniqueIdForNestedTestWithMinimalWhitespace() {
        test(
            """val context = describe("level 1") { describe("level 2") { i<caret>t("test") { assert(true) } } }""",
            "[engine:failgood]/[class:level 1(FailGoodTests)]/[class:level 2]/[class:test]"
        )
    }

    fun testWorksForTestsDefinedInObject() {
        testFull(
            """import failgood.Test

@Test
object FailGoodTests {
    val context =
        describe("level 1") { describe("level 2") { i<caret>t ("test") { assert(true) } } }
}
""",
            "[engine:failgood]/[class:level 1(FailGoodTests)]/[class:level 2]/[class:test]"
        )
    }

    fun testReturnsNullForOpenBracket() {
        // to make sure that we produce only one marker per runnable node
        test(
            """val context = describe("level 1") { describe("level 2") { it<caret>("test") { assert(true) } } }
""",
            null
        )
    }

    fun testReturnsNullForString() {
        test(
            """
    val context =
        describe("level 1") { describe("level 2") { it("t<caret>est") { assert(true) } } }
""",
            null
        )
    }

    fun _testWorksForNestedTestWithGenericClassAsRootDescription() {
        test(
            """
    val context = describe<Test> { describe("level 2") { i<caret>t("test") { assert(true) } } }
""",
            "[engine:failgood]/[class:Test(FailGoodTests)]/[class:level 2]/[class:test]"
        )
    }

    fun testWorksForThirdPartyDescribe() {
        // support third party describe methods as long as their first parameter is the name of the
        // context that they create
        testFull(
            """import failgood.Test
import failgood.dsl.ContextLambda
fun describeOther(name: String, otherParameter: String, ContextLambda: lambda) = describe(name, function = lambda)

@Test
class FailGoodTests {
    val context = describeOther("Test", "blah") { describe("level 2") { i<caret>t("test") { assert(true) } } } }
""",
            "[engine:failgood]/[class:Test(FailGoodTests)]/[class:level 2]/[class:test]"
        )
    }

    fun testWorksForClassAsRootDescription() {
        test(
            """
    val context = describe(Test::class) { describe("level 2") { i<caret>t("test") { assert(true) } } }
""",
            "[engine:failgood]/[class:Test(FailGoodTests)]/[class:level 2]/[class:test]"
        )
    }

    fun testComputesUniqueIdForNestedTestWithAsRootDescriptionDefinedInAVal() {
        test(
            """
    @Suppress("MayBeConstant")
    val rootContextValName="rootContext"
    val context = describe(rootContextValName) { describe("level 2") { i<caret>t("test") { assert(true) } } }
""",
            "[engine:failgood]/[class:rootContext(FailGoodTests)]/[class:level 2]/[class:test]"
        )
    }

    fun testComputesUniqueIdForNestedTestWithAsRootDescriptionDefinedInAConstVal() {
        test(
            """
    const val rootContextValName="rootContext"
    val context = describe(rootContextValName) { describe("level 2") { i<caret>t("test") { assert(true) } } }
""",
            "[engine:failgood]/[class:rootContext(FailGoodTests)]/[class:level 2]/[class:test]"
        )
    }

    fun testComputesUniqueIdForContext() {
        test(
            """
    val context = describe("level 1") { describ<caret>e("level 2") { it("test") { assert(true) } } }
""",
            "[engine:failgood]/[class:level 1(FailGoodTests)]/[class:level 2]"
        )
    }

    private fun test(@Language("kotlin") source: String, expected: String?) {
        @Language("kotlin")
        val completeSource = """import failgood.Test
@Test
class FailGoodTests {
$source
}"""
        testFull(completeSource, expected)
    }

    private fun testFull(@Language("kotlin") source: String, expected: String?) {
        val psiFile = myFixture.configureByText("FailGoodTests.kt", source)
        // health checks of the testing environment
        assertInstanceOf(psiFile, KtFile::class.java)
        assertFalse(PsiErrorElementUtil.hasErrors(project, psiFile.virtualFile))
        val element = psiFile.findElementAt(myFixture.caretOffset)!!
        val uniqueId = UniqueIdProducer.computeUniqueId(element)?.uniqueId
        assertEquals(expected, uniqueId)
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
