import failgood.Test
@Test
class FailGoodTests {
    val context = describe("The test runner") {
        <caret>it("supports describe/it syntax") { assert(true) }
        describe("nested contexts") {
            it("can contain tests too") { assert(true) }
        }
    }
}
