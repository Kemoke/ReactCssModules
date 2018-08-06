package net.kemoke.cssmodules.ide

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.css.CssClass
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.util.ProcessingContext
import net.kemoke.cssmodules.psi.Util
import java.util.*

class CompletionContributor : CompletionContributor() {
    init {
        val completionProvider = object: CompletionProvider<CompletionParameters>() {
            override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext?, resultSet: CompletionResultSet) {
                val psiElement = Optional.ofNullable(parameters.originalPosition).orElse(parameters.position)
                val added = ArrayList<String?>()
                if (psiElement.parent is XmlAttributeValue) {
                    if (Util.STYLE_NAME_FILTER.isAcceptable(psiElement.parent, psiElement)) {
                        for ((binding, file) in Util.getImportedStyleSheetFiles(psiElement)) {
                            for (cssClass in PsiTreeUtil.findChildrenOfType(file, CssClass::class.java)) {
                                val declaredName = (binding?.declaredName?.plus(".") ?: "") + cssClass.name
                                if (!Util.isCssModuleClass(cssClass) || added.contains(declaredName)){
                                    continue
                                }
                                added.add(declaredName)
                                var element = LookupElementBuilder.create(declaredName)
                                        .withIcon(cssClass.getIcon(0))
                                if (cssClass.presentation != null) {
                                    element = element.withTypeText(cssClass.presentation?.locationString, true)
                                }
                                resultSet.addElement(element)
                            }
                        }
                    }
                }
            }
        }
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), completionProvider)
    }
}