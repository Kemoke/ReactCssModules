package net.kemoke.cssmodules.ide

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.ide.impl.DataManagerImpl
import com.intellij.lang.javascript.psi.JSLiteralExpression
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.css.CssElementFactory
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlToken
import com.intellij.util.IncorrectOperationException
import net.kemoke.cssmodules.psi.UnknownCssModuleElement
import org.jetbrains.annotations.Nls

class CreateCssClassIntention : PsiElementBaseIntentionAction(), HighPriorityAction {

    @Throws(IncorrectOperationException::class)
    override fun invoke(project: Project, editor: Editor, element: PsiElement) {
        val intentionElement = getIntentionElement(element)
        for (psiReference in intentionElement!!.references) {
            if (psiReference is UnknownCssModuleElement) {
                val className = psiReference.rangeInElement.substring(intentionElement.text)
                val stylesheetFile = psiReference.stylesheetFile ?: return
                stylesheetFile.navigate(true)
                var ruleset: PsiElement = CssElementFactory.getInstance(project).createRuleset(".$className {\n\n}", stylesheetFile.language)
                ruleset = stylesheetFile.add(ruleset)
                val newCaretOffset = ruleset.textOffset + ruleset.text.indexOf("{") + 2 // after '{\n'
                val editors = FileEditorManager.getInstance(project).getEditors(stylesheetFile.virtualFile)
                for (fileEditor in editors) {
                    if (fileEditor is TextEditor) {
                        val cssEditor = fileEditor.editor
                        cssEditor.caretModel.moveToOffset(newCaretOffset)
                        val editorLineEnd = ActionManager.getInstance().getAction("EditorLineEnd")
                        if (editorLineEnd != null) {
                            val actionEvent = AnActionEvent.createFromDataContext(
                                    ActionPlaces.UNKNOWN, null,
                                    DataManagerImpl.MyDataContext(cssEditor.component)
                            )
                            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(cssEditor.document)
                            editorLineEnd.actionPerformed(actionEvent)
                        }
                    }
                }
                return
            }
        }
    }

    override fun isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean {
        val intentionElement = getIntentionElement(element)
        if (intentionElement != null) {
            for (psiReference in intentionElement.references) {
                if (psiReference is UnknownCssModuleElement) {
                    return true
                }
            }
        }
        return false
    }

    override fun getText(): String {
        return "Create CSS Modules class"
    }

    @Nls
    override fun getFamilyName(): String {
        return text
    }


    private fun getIntentionElement(element: PsiElement): PsiElement? {
        var intentionElement: PsiElement?
        if (element is XmlToken) {
            intentionElement = PsiTreeUtil.getParentOfType(element, XmlAttributeValue::class.java)
        } else {
            intentionElement = PsiTreeUtil.getParentOfType(element, JSLiteralExpression::class.java)
            if (intentionElement == null) {
                intentionElement = PsiTreeUtil.getPrevSiblingOfType(element, JSLiteralExpression::class.java)
            }
        }
        return intentionElement
    }

}