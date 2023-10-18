package failgood.idea

import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.JavaTestFixtureFactory
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl
import failgood.describe

//@Test
class FailgoodLineMarkerContributorTest {
    val context = describe("testing the failgood plugin with failgood") {
        it("works") {
            val factory = IdeaTestFixtureFactory.getFixtureFactory()
            val fixtureBuilder = factory.createLightFixtureBuilder(
                DefaultLightProjectDescriptor(), "failgood-test"
            )
            val fixture = fixtureBuilder.fixture
            val myFixture = JavaTestFixtureFactory.getFixtureFactory()
                .createCodeInsightFixture(fixture, LightTempDirTestFixtureImpl(true))
            myFixture.setUp()
        }
    }
}
