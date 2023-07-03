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
import de.achimonline.ansible_lint.bundle.AnsibleLintBundle.message
import de.achimonline.ansible_lint.parser.AnsibleLintItem
import de.achimonline.ansible_lint.settings.AnsibleLintSettings
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.mockStatic
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.*
import java.util.*

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

    private fun buildAnsibleLintItem(
        ruleId: String = "testRule-${UUID.randomUUID()}",
        description: String = "testDescription-${UUID.randomUUID()}",
        message: String = "testMessage-${UUID.randomUUID()}",
        startLine: Int = (1..666).random(),
        helpText: String = "helpText-${UUID.randomUUID()}",
        helpUri: String = "helpUri-${UUID.randomUUID()}",
        severity: HighlightSeverity = HighlightSeverity.ERROR,
        tags: Set<String> = setOf("testTag1", "testTag2")
    ): AnsibleLintItem {
        return AnsibleLintItem(
            ruleId = ruleId,
            description = description,
            message = message,
            startLine = startLine,
            helpText = helpText,
            helpUri = helpUri,
            severity = severity,
            tags = tags
        )
    }

    @Before
    fun setUp() {
        whenever(psiFile.project).doReturn(project)

        mockStatic(PsiDocumentManager::class.java, Mockito.CALLS_REAL_METHODS).use { mockedPsiDocumentManager ->
            mockedPsiDocumentManager.`when`<Any> { PsiDocumentManager.getInstance(project) }.doReturn(psiDocumentManager)
        }

        whenever(psiDocumentManager.getDocument(psiFile)).doReturn(document)

        ansibleLintAnnotator = AnsibleLintAnnotator()
    }

    @Test
    fun apply() {
        val lintItem1 = buildAnsibleLintItem(severity = HighlightSeverity.ERROR)
        val lintItem2 = buildAnsibleLintItem(severity = HighlightSeverity.ERROR)
        val lintItem3 = buildAnsibleLintItem(severity = HighlightSeverity.INFORMATION)
        val lintItem4 = buildAnsibleLintItem(severity = HighlightSeverity.WARNING)

        val lintItems = listOf(
            lintItem1,
            lintItem2,
            lintItem3,
            lintItem4
        )

        val lintIgnores = setOf(
            UUID.randomUUID().toString(),
            lintItem2.ruleId,
            UUID.randomUUID().toString()
        )
        val ignoredItems = 1

        val applicableInformation = ApplicableInformation(
            settings = AnsibleLintSettings(),
            lintItems = lintItems,
            lintIgnores = lintIgnores
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
            verify(annotationHolder, times(4)).newAnnotation(severityCaptor.capture(), messageCaptor.capture())
        }

        assertEquals(
            listOf(
                HighlightSeverity.ERROR,
                HighlightSeverity.WEAK_WARNING,
                HighlightSeverity.INFORMATION,
                HighlightSeverity.WARNING
            ), severityCaptor.allValues
        )

        assertEquals(
            listOf(
                ansibleLintAnnotator.getAnnotationMessage(lintItem1, false),
                ansibleLintAnnotator.getAnnotationMessage(lintItem2, true),
                ansibleLintAnnotator.getAnnotationMessage(lintItem3, false),
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

        verify(
            annotationBuilder,
            times(lintItems.size * possibleFixOptions.size - ignoredItems)
        ).withFix(any())
    }

    @Test
    fun apply_dontVisualizeIgnores() {
        val lintItem1 = buildAnsibleLintItem()
        val lintItem2 = buildAnsibleLintItem()

        val lintIgnores = setOf(lintItem2.ruleId)

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
    fun getAnnotationMessage() {
        var ansibleLintItem = buildAnsibleLintItem()
        assertEquals(
            "${ansibleLintItem.description} | ${ansibleLintItem.message} (${ansibleLintItem.helpText}) | ${message("annotation.rule-id-prefix")} ${ansibleLintItem.ruleId} <${ansibleLintItem.tags.joinToString(",")}>",
            ansibleLintAnnotator.getAnnotationMessage(ansibleLintItem, false)
        )

        ansibleLintItem = buildAnsibleLintItem(message = "")
        assertEquals(
            "${ansibleLintItem.description} (${ansibleLintItem.helpText}) | ${message("annotation.rule-id-prefix")} ${ansibleLintItem.ruleId} <${ansibleLintItem.tags.joinToString(",")}>",
            ansibleLintAnnotator.getAnnotationMessage(ansibleLintItem, false)
        )

        ansibleLintItem = buildAnsibleLintItem(helpText = "")
        assertEquals(
            "${ansibleLintItem.description} | ${ansibleLintItem.message} | ${message("annotation.rule-id-prefix")} ${ansibleLintItem.ruleId} <${ansibleLintItem.tags.joinToString(",")}>",
            ansibleLintAnnotator.getAnnotationMessage(ansibleLintItem, false)
        )

        ansibleLintItem = buildAnsibleLintItem(tags = emptySet())
        assertEquals(
            "${ansibleLintItem.description} | ${ansibleLintItem.message} (${ansibleLintItem.helpText}) | ${message("annotation.rule-id-prefix")} ${ansibleLintItem.ruleId}",
            ansibleLintAnnotator.getAnnotationMessage(ansibleLintItem, false)
        )

        ansibleLintItem = buildAnsibleLintItem(message = "", helpText = "")
        assertEquals(
            "${ansibleLintItem.description} | ${message("annotation.rule-id-prefix")} ${ansibleLintItem.ruleId} <${ansibleLintItem.tags.joinToString(",")}>",
            ansibleLintAnnotator.getAnnotationMessage(ansibleLintItem, false)
        )

        ansibleLintItem = buildAnsibleLintItem(message = "", helpText = "", tags = emptySet())
        assertEquals(
            "${ansibleLintItem.description} | ${message("annotation.rule-id-prefix")} ${ansibleLintItem.ruleId}",
            ansibleLintAnnotator.getAnnotationMessage(ansibleLintItem, false)
        )

        ansibleLintItem = buildAnsibleLintItem()
        assertEquals(
            "${message("annotation.ignored-prefix")} ${ansibleLintItem.description} | ${ansibleLintItem.message} (${ansibleLintItem.helpText}) | ${message("annotation.rule-id-prefix")} ${ansibleLintItem.ruleId} <${ansibleLintItem.tags.joinToString(",")}>",
            ansibleLintAnnotator.getAnnotationMessage(ansibleLintItem, true)
        )
    }
}
