package de.achimonline.ansible_lint.annotator

import com.intellij.lang.annotation.AnnotationBuilder
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import de.achimonline.ansible_lint.annotator.AnsibleLintAnnotator.*
import de.achimonline.ansible_lint.annotator.actions.*
import de.achimonline.ansible_lint.parser.AnsibleLintItem
import de.achimonline.ansible_lint.parser.AnsibleLintItem.Location
import de.achimonline.ansible_lint.parser.AnsibleLintItem.Location.BeginAndEnd
import de.achimonline.ansible_lint.settings.AnsibleLintSettings
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.*

@RunWith(MockitoJUnitRunner::class)
class AnsibleLintAnnotatorTest {
    @Mock
    private lateinit var psiFile: PsiFile

    @Mock
    private lateinit var annotationHolder: AnnotationHolder

    @Mock
    private lateinit var annotationBuilder: AnnotationBuilder

    @Mock
    private lateinit var project: Project

    @Mock
    private lateinit var document: Document

    @Mock
    private lateinit var psiDocumentManager: PsiDocumentManager

    private lateinit var ansibleLintAnnotator: AnsibleLintAnnotator

    @Before
    fun setUp() {
        whenever(psiFile.project).doReturn(project)

        Mockito.mockStatic(PsiDocumentManager::class.java, Mockito.CALLS_REAL_METHODS).use {
            it.`when`<Any> { PsiDocumentManager.getInstance(project) }.thenReturn(psiDocumentManager)
        }

        whenever(psiDocumentManager.getDocument(psiFile)).doReturn(document)

        ansibleLintAnnotator = AnsibleLintAnnotator()
    }

    @Test
    fun apply() {
        val lintIgnores = setOf("testRule42", "testRule2", "testRule666")

        val lintItem1 = AnsibleLintItem(
            check_name = "testRule1",
            severity = "major",
            location = Location(lines = BeginAndEnd(begin = 1))
        )

        val lintItem2 = AnsibleLintItem( // ignored item; should become a "weak warning", instead of error
            check_name = "testRule2",
            severity = "blocker",
            location = Location(lines = BeginAndEnd(begin = 42))
        )

        val lintItem3 = AnsibleLintItem() // line info not set; item should be skipped

        val lintItem4 = AnsibleLintItem(
            check_name = "testRule4",
            severity = "minor",
            location = Location(lines = BeginAndEnd(begin = 666))
        )

        val lintItems = listOf(lintItem1, lintItem2, lintItem3, lintItem4)

        val applicableInformation = ApplicableInformation(
            AnsibleLintSettings(), lintItems, lintIgnores
        )

        whenever(document.getLineStartOffset(anyInt())).doReturn(1)
        whenever(document.getLineEndOffset(anyInt())).doReturn(999)
        whenever(document.getText(any())).doReturn("test")

        whenever(annotationHolder.newAnnotation(any(), any())).doReturn(annotationBuilder)
        whenever(annotationBuilder.range(any(TextRange::class.java))).doReturn(annotationBuilder)
        whenever(annotationBuilder.withFix(any())).doReturn(annotationBuilder)

        ansibleLintAnnotator.apply(psiFile, applicableInformation, annotationHolder)

        val severityCaptor = argumentCaptor<HighlightSeverity>()
        val messageCaptor = argumentCaptor<String>()

        argumentCaptor<HighlightSeverity, String>().apply {
            verify(annotationHolder, times(3)).newAnnotation(severityCaptor.capture(), messageCaptor.capture())
        }

        assertEquals(
            listOf(
                HighlightSeverity.ERROR,
                HighlightSeverity.WEAK_WARNING,
                HighlightSeverity.WARNING
            ), severityCaptor.allValues
        )

        assertEquals(
            listOf(
                ansibleLintAnnotator.getAnnotationMessage(lintItem1, false),
                ansibleLintAnnotator.getAnnotationMessage(lintItem2, true),
                ansibleLintAnnotator.getAnnotationMessage(lintItem4, false)
            ), messageCaptor.allValues
        )

        val possibleFixOptions = listOf(
            AnsibleLintAnnotatorClipboardAction::class,
            AnsibleLintAnnotatorIgnoreFileAction::class,
            AnsibleLintAnnotatorNoQAAction::class,
            AnsibleLintAnnotatorOpenUrlAction::class,
            AnsibleLintAnnotatorSkipListAction::class
        )

        val skippedItems = 1
        val ignoredItems = 1

        verify(
            annotationBuilder,
            times((lintItems.size - skippedItems) * possibleFixOptions.size - ignoredItems)
        ).withFix(any())
    }

    @Test
    fun apply_dontVisualizeIgnores() {
        val lintIgnores = setOf("testRule2")

        val lintItem1 = AnsibleLintItem(location = Location(lines = BeginAndEnd(begin = 1)))
        val lintItem2 = AnsibleLintItem(check_name = "testRule2", location = Location(lines = BeginAndEnd(begin = 42)))

        whenever(document.getLineStartOffset(anyInt())).doReturn(1)
        whenever(document.getLineEndOffset(anyInt())).doReturn(999)
        whenever(document.getText(any())).doReturn("test")

        whenever(annotationHolder.newAnnotation(any(), any())).doReturn(annotationBuilder)
        whenever(annotationBuilder.range(any(TextRange::class.java))).doReturn(annotationBuilder)
        whenever(annotationBuilder.withFix(any())).doReturn(annotationBuilder)

        ansibleLintAnnotator.apply(
            psiFile,
            ApplicableInformation(
                AnsibleLintSettings(visualizeIgnoredRules = false),
                listOf(lintItem1, lintItem2),
                lintIgnores
            ),
            annotationHolder
        )

        verify(annotationHolder, times(1)).newAnnotation(any(), any())
    }

    @Test
    fun getAnnotationHighlightSeverity() {
        val ansibleLintAnnotator = AnsibleLintAnnotator()

        assertEquals(HighlightSeverity.ERROR, ansibleLintAnnotator.getAnnotationHighlightSeverity(AnsibleLintItem(severity = "blocker")))
        assertEquals(HighlightSeverity.ERROR, ansibleLintAnnotator.getAnnotationHighlightSeverity(AnsibleLintItem(severity = "critical")))
        assertEquals(HighlightSeverity.ERROR, ansibleLintAnnotator.getAnnotationHighlightSeverity(AnsibleLintItem(severity = "major")))
        assertEquals(HighlightSeverity.WARNING, ansibleLintAnnotator.getAnnotationHighlightSeverity(AnsibleLintItem(severity = "minor")))
        assertEquals(HighlightSeverity.INFORMATION, ansibleLintAnnotator.getAnnotationHighlightSeverity(AnsibleLintItem(severity = "info")))

        assertEquals(HighlightSeverity.ERROR, ansibleLintAnnotator.getAnnotationHighlightSeverity(AnsibleLintItem(severity = " Blocker ")))
        assertEquals(HighlightSeverity.ERROR, ansibleLintAnnotator.getAnnotationHighlightSeverity(AnsibleLintItem(severity = " Critical ")))
        assertEquals(HighlightSeverity.ERROR, ansibleLintAnnotator.getAnnotationHighlightSeverity(AnsibleLintItem(severity = " Major ")))
        assertEquals(HighlightSeverity.WARNING, ansibleLintAnnotator.getAnnotationHighlightSeverity(AnsibleLintItem(severity = " Minor ")))
        assertEquals(HighlightSeverity.INFORMATION, ansibleLintAnnotator.getAnnotationHighlightSeverity(AnsibleLintItem(severity = " Info ")))

        assertEquals(HighlightSeverity.INFORMATION, ansibleLintAnnotator.getAnnotationHighlightSeverity(AnsibleLintItem(severity = " ")))
        assertEquals(HighlightSeverity.INFORMATION, ansibleLintAnnotator.getAnnotationHighlightSeverity(AnsibleLintItem(severity = "UNKNOWN")))
    }
}
