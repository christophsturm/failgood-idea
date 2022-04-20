package com.github.christophsturm.failgoodidea

import com.intellij.psi.PsiElement

class TestFinder : com.intellij.testIntegration.TestFinder {
    override fun findSourceElement(from: PsiElement): PsiElement? = null

    override fun findTestsForClass(element: PsiElement): MutableCollection<PsiElement> = mutableListOf()

    override fun findClassesForTest(element: PsiElement): MutableCollection<PsiElement> = mutableListOf()

    override fun isTest(element: PsiElement): Boolean = false
}
