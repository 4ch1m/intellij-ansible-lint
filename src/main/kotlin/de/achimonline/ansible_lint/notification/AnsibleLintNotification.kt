package de.achimonline.ansible_lint.notification

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project

class AnsibleLintNotification {
    private fun notify(project: Project?, notificationType: NotificationType, content: String?, action: AnAction?) {
        val notification = NotificationGroupManager
            .getInstance()
            .getNotificationGroup("ANSIBLE_LINT")
            .createNotification(content!!, notificationType)

            if (action != null) {
                notification.addAction(action)
            }

            notification.notify(project)
    }

    fun notifyError(project: Project?, content: String?, action: AnAction? = null) {
        notify(project, NotificationType.ERROR, content, action)
    }

    fun notifyWarning(project: Project?, content: String?, action: AnAction? = null) {
        notify(project, NotificationType.WARNING, content, action)
    }

    fun notifyInformation(project: Project?, content: String?, action: AnAction? = null) {
        notify(project, NotificationType.INFORMATION, content, action)
    }
}
