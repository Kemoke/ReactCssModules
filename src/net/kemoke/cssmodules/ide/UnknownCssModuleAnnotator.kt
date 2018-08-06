package net.kemoke.cssmodules.ide

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import net.kemoke.cssmodules.psi.UnknownCssModuleElement
import net.kemoke.cssmodules.psi.Util

class UnknownCssModuleAnnotator : Annotator {

    override fun annotate(psiElement: PsiElement, annotationHolder: AnnotationHolder) {
        var elementToAnnotate: PsiElement? = null
        if (Util.STYLE_NAME_FILTER.isAcceptable(psiElement, psiElement)) {
            elementToAnnotate = psiElement
        }
        if (elementToAnnotate != null) {
            for (psiReference in psiElement.references) {
                if (psiReference is UnknownCssModuleElement) {
                    val rangeInElement = psiReference.rangeInElement
                    if (rangeInElement.isEmpty) {
                        continue
                    }
                    val start = psiElement.textRange.startOffset + rangeInElement.startOffset
                    val length = rangeInElement.length
                    val textRange = TextRange.from(start, length)
                    if (!textRange.isEmpty) {
                        val message = "Unknown class name \"" + rangeInElement.substring(psiElement.text) + "\""
                        annotationHolder.createErrorAnnotation(textRange, message)
                    }
                }
            }
        }
    }
}