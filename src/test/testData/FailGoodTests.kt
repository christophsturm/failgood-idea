import failgood.Test
@Test
class FailGoodTests {
    val context = describe("The test runner") {
        it<caret>("runs tests") { assert(true) }
    }
}
