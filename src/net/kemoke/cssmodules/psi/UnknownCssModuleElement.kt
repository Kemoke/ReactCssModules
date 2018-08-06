package net.kemoke.cssmodules.psi

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.css.StylesheetFile

class UnknownCssModuleElement(element: PsiElement, range: TextRange, val stylesheetFile: StylesheetFile?): PsiReferenceBase<PsiElement>(element, range) {
    override fun resolve() = element

    override fun getVariants(): Array<Any> = arrayOf()
}