import failgood.Test
@Test
class FailGoodTests {
    val context = describe("The test runner") {
        <caret>it("runs tests") { assert(true) }
    }
}
