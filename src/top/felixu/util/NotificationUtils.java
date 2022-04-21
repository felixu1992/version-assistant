package top.felixu.util;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;

import java.util.concurrent.TimeUnit;

/**
 * @author felixu
 * @since 2022.04.18
 */
public class NotificationUtils {

    private final NotificationGroup balloon;

    private final NotificationGroup eventLog;

    private final Project project;

    public NotificationUtils(Project project) {
        this.balloon = NotificationGroupManager.getInstance().getNotificationGroup("version-assistant-notification");
        this.eventLog = NotificationGroupManager.getInstance().getNotificationGroup("version-assistant-event-log");
        this.project = project;
    }

    public void info(String msg) {
        Notifications.Bus.notifyAndHide(balloon.createNotification(msg, NotificationType.INFORMATION), project);
    }

    public void infoLog(String msg) {
        Notification notification = eventLog.createNotification(msg, NotificationType.INFORMATION);
        Notifications.Bus.notify(notification, project);
        AppExecutorUtil.getAppScheduledExecutorService().schedule(notification::expire, 0L, TimeUnit.SECONDS);
    }

    public void warning(String msg) {
        Notifications.Bus.notifyAndHide(balloon.createNotification(msg, NotificationType.WARNING), project);
    }

    public void warningLog(String msg) {
        Notification notification = eventLog.createNotification(msg, NotificationType.WARNING);
        Notifications.Bus.notify(notification, project);
        AppExecutorUtil.getAppScheduledExecutorService().schedule(notification::expire, 0L, TimeUnit.SECONDS);
    }

    public void error(String msg) {
        Notifications.Bus.notifyAndHide(balloon.createNotification(msg, NotificationType.ERROR), project);
    }

    public void errorLog(String msg) {
        Notification notification = eventLog.createNotification(msg, NotificationType.ERROR);
        Notifications.Bus.notify(notification, project);
        AppExecutorUtil.getAppScheduledExecutorService().schedule(notification::expire, 0L, TimeUnit.SECONDS);
    }
}
