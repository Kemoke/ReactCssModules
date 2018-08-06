package net.kemoke.cssmodules.psi

import com.intellij.lang.ecmascript6.psi.ES6FromClause
import com.intellij.lang.ecmascript6.psi.ES6ImportDeclaration
import com.intellij.lang.ecmascript6.psi.ES6ImportedBinding
import com.intellij.lang.javascript.psi.JSFile
import com.intellij.openapi.util.Ref
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.psi.css.CssClass
import com.intellij.psi.css.CssFunction
import com.intellij.psi.css.StylesheetFile
import com.intellij.psi.filters.ElementFilter
import com.intellij.psi.filters.position.FilterPattern
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlAttributeValue

object Util {
    val STYLE_NAME_FILTER: ElementFilter = object : ElementFilter {
        override fun isAcceptable(element: Any, context: PsiElement?): Boolean {
            if (element is XmlAttributeValue && context != null && context.containingFile is JSFile) {
                val xmlAttribute = PsiTreeUtil.getParentOfType(element, XmlAttribute::class.java)
                if (xmlAttribute != null) {
                    return xmlAttribute.name == "styleName"
                }
            }
            return false
        }

        override fun isClassAcceptable(hintClass: Class<*>): Boolean {
            return XmlAttributeValue::class.java.isAssignableFrom(hintClass)
        }
    }

    /**
     * PSI Pattern for matching "styleName" React attributes.
     */
    val STYLE_NAME_PATTERN: PsiElementPattern.Capture<XmlAttributeValue> = PlatformPatterns
            .psiElement(XmlAttributeValue::class.java)
            .and(FilterPattern(STYLE_NAME_FILTER))

    fun getImportedStyleSheetFiles(cssReferencingElement: PsiElement): List<Pair<ES6ImportedBinding?, StylesheetFile>> {
        val files = ArrayList<Pair<ES6ImportedBinding?, StylesheetFile>>()
        cssReferencingElement.containingFile.accept(object : PsiRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element is ES6FromClause) {
                    val file = resolveStyleSheetFile(element)
                    val binding = PsiTreeUtil.findChildOfType(element.parent, ES6ImportedBinding::class.java)
                    if (file != null){
                        files.add(Pair(binding, file))
                    }
                    return
                }
                if (element is ES6ImportDeclaration) {
                    val file = resolveStyleSheetFile(element)
                    if (file != null){
                        files.add(Pair(null, file))
                    }
                }
                super.visitElement(element)
            }
        })
        return files
    }

    fun resolveStyleSheetFile(element: PsiElement) = element.references.firstOrNull { it.resolve() is StylesheetFile }?.resolve() as StylesheetFile?

    fun getCssClass(stylesheetFile: StylesheetFile, cssClass: String): CssClass? {
        val cssClassRef = Ref<CssClass>()
        stylesheetFile.accept(object : PsiRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (cssClassRef.get() != null) {
                    return
                }
                if (element is CssClass) {
                    if (cssClass == element.getText() && isCssModuleClass(element)) {
                        cssClassRef.set(element)
                        return
                    }
                }
                super.visitElement(element)
            }
        })
        return cssClassRef.get()
    }

    fun isCssModuleClass(cssClass: CssClass): Boolean {
        val parentFunction = PsiTreeUtil.getParentOfType(cssClass, CssFunction::class.java)
        if (parentFunction != null) {
            if ("global" == parentFunction.name) {
                // not a generated CSS Modules class
                return false
            }
        }
        return true
    }
}