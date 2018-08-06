package net.kemoke.cssmodules.psi

import com.intellij.lang.ecmascript6.psi.ES6ImportedBinding
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.css.CssClass
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.util.ProcessingContext

class ReferenceContributor: PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(Util.STYLE_NAME_PATTERN, object: PsiReferenceProvider() {
            override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
                val files = Util.getImportedStyleSheetFiles(element)
                val refList = ArrayList<PsiReference>()
                if (files.isNotEmpty()) {
                    val attributeValue = element as XmlAttributeValue
                    val value = attributeValue.value.toString()
                    if (value.startsWith('{')){
                        return PsiReference.EMPTY_ARRAY
                    }
                    var offset = attributeValue.valueTextRange.startOffset - attributeValue.textRange.startOffset
                    var classNameSize = 0
                    for (className in value.split(' ')) {
                        val bits = className.split('.')
                        var cssClass: CssClass? = null
                        var binding: ES6ImportedBinding? = null
                        for (filePair in files) {
                            binding = filePair.first
                            val file = filePair.second
                            if ((bits.size != 1 && binding == null) || (bits.size > 1 && (bits[0] != binding?.declaredName)) ){
                                continue
                            }
                            cssClass = if (bits.size == 2) {
                                Util.getCssClass(file, '.' + bits[1])
                            } else {
                                Util.getCssClass(file, ".$className")
                            }
                            classNameSize = if (bits.size == 2) {
                                bits[1].length
                            } else {
                                bits[0].length
                            }
                            if (binding != null) {
                                refList.add(object: PsiReferenceBase<PsiElement>(element, TextRange.from(offset, binding.declaredName?.length ?: className.length)) {
                                    override fun resolve() = binding
                                    override fun getVariants() = arrayOf<Any>()
                                })
                            }
                            break
                        }
                        if (cssClass != null){
                            refList.add(object: PsiReferenceBase<PsiElement>(element, TextRange.from(offset + (binding?.declaredName?.length?.plus(1) ?: 0), classNameSize)) {
                                override fun resolve() = cssClass
                                override fun getVariants() = arrayOf<Any>()
                            })
                        } else {
                            refList.add(UnknownCssModuleElement(element, TextRange.from(offset + (binding?.declaredName?.length?.plus(1) ?: 0), classNameSize), files.firstOrNull { (bits.size == 1 && binding == null) || (bits.size > 1 && (bits[0] == binding?.declaredName)) }?.second))
                        }
                        offset += className.length + 1
                    }
                }
                return refList.toTypedArray()
            }
        })
    }
}