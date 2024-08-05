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
                addFromMaven(model, "dev.failgood:failgood:0.9.1", false, DependencyScope.COMPILE)
            }
        }

    override fun getProjectDescriptor(): ProjectDescriptor = projectDescriptor

    fun testComputesUniqueIdForNestedTestWithDescribe() {
        val friendlyUniqueId =
            simpleGetUniqueId(
                """
    val context =
        describe("level 1") { describe("level 2") { i<caret>t("test") { assert(true) } } }
"""
            )
        assertEquals(
            "[engine:failgood]/[class:level 1(com.pkg.FailGoodTests)]/[class:level 2]/[class:test]",
            friendlyUniqueId?.uniqueId
        )
        assertEquals("test", friendlyUniqueId?.name)
    }

    fun testComputesUniqueIdForNestedTest() {
        val friendlyUniqueId =
            simpleGetUniqueId(
                """
    val context =
        testsFor("level 1") { describe("level 2") { i<caret>t("test") { assert(true) } } }
"""
            )
        assertEquals(
            "[engine:failgood]/[class:level 1(com.pkg.FailGoodTests)]/[class:level 2]/[class:test]",
            friendlyUniqueId?.uniqueId
        )
    }

    fun testComputesUniqueIdForNestedTestInUnnamedTestCollection() {
        val friendlyUniqueId =
            simpleGetUniqueId(
                """
    val context =
        tests { describe("level 2") { i<caret>t("test") { assert(true) } } }
"""
            )
        assertEquals(
            "[engine:failgood]/[class:FailGoodTests(com.pkg.FailGoodTests)]/[class:level 2]/[class:test]",
            friendlyUniqueId?.uniqueId
        )
    }

    fun testComputesUniqueIdForNestedTestWithMinimalWhitespace() {
        val friendlyUniqueId =
            simpleGetUniqueId(
                """val context = describe("level 1") { describe("level 2") { i<caret>t("test") { assert(true) } } }"""
            )
        assertEquals(
            "[engine:failgood]/[class:level 1(com.pkg.FailGoodTests)]/[class:level 2]/[class:test]",
            friendlyUniqueId?.uniqueId
        )
    }

    fun testWorksForTestsDefinedInObject() {
        val friendlyUniqueId =
            getUniqueId(
                """package com.pkg
import failgood.Test

@Test
object FailGoodTests {
    val context =
        describe("level 1") { describe("level 2") { i<caret>t ("test") { assert(true) } } }
}
"""
            )
        assertEquals(
            "[engine:failgood]/[class:level 1(com.pkg.FailGoodTests)]/[class:level 2]/[class:test]",
            friendlyUniqueId?.uniqueId
        )
    }

    fun testReturnsNullForOpenBracket() {
        // to make sure that we produce only one marker per runnable node
        val friendlyUniqueId =
            simpleGetUniqueId(
                """val context = describe("level 1") { describe("level 2") { it<caret>("test") { assert(true) } } }
"""
            )
        assertEquals(null, friendlyUniqueId?.uniqueId)
    }

    fun testReturnsNullForString() {
        val friendlyUniqueId =
            simpleGetUniqueId(
                """
    val context =
        describe("level 1") { describe("level 2") { it("t<caret>est") { assert(true) } } }
"""
            )
        assertEquals(null, friendlyUniqueId?.uniqueId)
    }

    fun testReturnsNullForTestBody() {
        val friendlyUniqueId =
            simpleGetUniqueId(
                """
    val context =
        describe("level 1") { describe("level 2") { it("test") { <caret>assert(true) } } }
"""
            )
        assertEquals(null, friendlyUniqueId?.uniqueId)
    }

    fun testReturnsNullForCallInsideTestBody() {
        val friendlyUniqueId =
            simpleGetUniqueId(
                """
    val context =
        describe("level 1") { describe("level 2") { it("test") { as<caret>sert(true) } } }
"""
            )
        assertEquals(null, friendlyUniqueId?.uniqueId)
    }

    fun _testWorksForNestedTestWithGenericClassAsRootDescription() {
        val friendlyUniqueId =
            simpleGetUniqueId(
                """
    val context = describe<Test> { describe("level 2") { i<caret>t("test") { assert(true) } } }
"""
            )
        assertEquals(
            "[engine:failgood]/[class:Test(com.pkg.FailGoodTests)]/[class:level 2]/[class:test]",
            friendlyUniqueId?.uniqueId
        )
    }

    fun testWorksForThirdPartyDescribe() {
        // support third party describe methods as long as their first parameter is the name of the
        // context that they create
        val friendlyUniqueId =
            getUniqueId(
                """package com.pkg
import failgood.Test
import failgood.dsl.ContextLambda
fun describeOther(name: String, otherParameter: String, ContextLambda: lambda) = describe(name, function = lambda)

@Test
class FailGoodTests {
    val context = describeOther("Test", "blah") { describe("level 2") { i<caret>t("test") { assert(true) } } } }
"""
            )
        assertEquals(
            "[engine:failgood]/[class:Test(com.pkg.FailGoodTests)]/[class:level 2]/[class:test]",
            friendlyUniqueId?.uniqueId
        )
    }

    fun testWorksForClassAsRootDescription() {
        val friendlyUniqueId =
            simpleGetUniqueId(
                """
    val context = describe(Test::class) { describe("level 2") { i<caret>t("test") { assert(true) } } }
"""
            )
        assertEquals(
            "[engine:failgood]/[class:Test(com.pkg.FailGoodTests)]/[class:level 2]/[class:test]",
            friendlyUniqueId?.uniqueId
        )
    }

    fun testComputesUniqueIdForNestedTestWithAsRootDescriptionDefinedInAVal() {
        val friendlyUniqueId =
            simpleGetUniqueId(
                """
    @Suppress("MayBeConstant")
    val rootContextValName="rootContext"
    val context = describe(rootContextValName) { describe("level 2") { i<caret>t("test") { assert(true) } } }
"""
            )
        assertEquals(
            "[engine:failgood]/[class:rootContext(com.pkg.FailGoodTests)]/[class:level 2]/[class:test]",
            friendlyUniqueId?.uniqueId
        )
    }

    fun testComputesUniqueIdForNestedTestWithAsRootDescriptionDefinedInAConstVal() {
        val friendlyUniqueId =
            simpleGetUniqueId(
                """
    const val rootContextValName="rootContext"
    val context = describe(rootContextValName) { describe("level 2") { i<caret>t("test") { assert(true) } } }
"""
            )
        assertEquals(
            "[engine:failgood]/[class:rootContext(com.pkg.FailGoodTests)]/[class:level 2]/[class:test]",
            friendlyUniqueId?.uniqueId
        )
    }

    fun testComputesUniqueIdForContext() {
        val friendlyUniqueId =
            simpleGetUniqueId(
                """
    val context = describe("level 1") { describ<caret>e("level 2") { it("test") { assert(true) } } }
"""
            )
        assertEquals(
            "[engine:failgood]/[class:level 1(com.pkg.FailGoodTests)]/[class:level 2]",
            friendlyUniqueId?.uniqueId
        )
    }

    private fun simpleGetUniqueId(source: String): FriendlyUniqueId? {
        @Language("kotlin")
        val completeSource =
            """package com.pkg
    import failgood.Test
    @Test
    class FailGoodTests {
    $source
    }"""
        return getUniqueId(completeSource)
    }

    private fun getUniqueId(source: String): FriendlyUniqueId? {
        val psiFile = myFixture.configureByText("FailGoodTests.kt", source)
        // health checks of the testing environment
        assertInstanceOf(psiFile, KtFile::class.java)
        assertFalse(PsiErrorElementUtil.hasErrors(project, psiFile.virtualFile))
        val element = psiFile.findElementAt(myFixture.caretOffset)!!
        return UniqueIdProducer.computeUniqueId(element)
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
