package top.felixu;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import top.felixu.dialog.VersionDialog;
import top.felixu.util.NotificationUtils;

/**
 * @author felixu
 * @since 2022.04.15
 */
public class VersionAssistant extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        VersionDialog dialog = new VersionDialog(project, new NotificationUtils(project));
        dialog.pack();
        dialog.setVisible(true);
    }
}
