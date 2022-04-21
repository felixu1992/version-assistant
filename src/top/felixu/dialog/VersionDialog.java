package top.felixu.dialog;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import top.felixu.model.Settings;
import top.felixu.util.NotificationUtils;
import top.felixu.util.SettingsUtils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author felixu
 * @since 2022.04.18
 */
public class VersionDialog extends JDialog {

    /**
     * 内容面板
     */
    private JPanel content;

    /**
     * 分支输入框
     */
    private JComboBox branch;

    /**
     * 版本号输入框
     */
    private JTextField version;

    /**
     * 确认按钮
     */
    private JButton ok;

    /**
     * 取消按钮
     */
    private JButton cancel;

    /**
     * IDEA 的项目信息
     */
    private final Project project;

    /**
     * 配置信息
     */
    private final Settings settings;

    private final NotificationUtils notification;

    public VersionDialog(Project project, NotificationUtils notification) {
        settings = SettingsUtils.get();
        this.notification = notification;
        this.project = project;

        // 初始化面板
        setContentPane(content);
        setModal(true);
        setTitle("版本助手");
        setLocationRelativeTo(null);
        getRootPane().setDefaultButton(ok);
        version.setText(SettingsUtils.getLatestVersion());

        // 初始化分支选择器
        try {
            for (String b : getAllBranch()) {
                b = b.trim();
                if (!b.startsWith("*")) {
                    branch.addItem(b);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                validateSelf();
                if (StringUtils.isEmpty(settings.getGitHome())) {
                    notification.error("请为版本助手配置 Git 的可执行文件");
                }
                if (StringUtils.isEmpty(settings.getMavenHome())) {
                    notification.error("请为版本助手配置 Maven 的可执行文件");
                }
            }
        });

        // 版本输入框校验器
        version.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                validateSelf();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                validateSelf();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                validateSelf();
            }
        });

        // 确认事件
        ok.addActionListener(e -> confirm());

        // 取消事件
        cancel.addActionListener(e -> cancel());

        // call cancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cancel();
            }
        });

        // call cancel() on ESCAPE
        content.registerKeyboardAction(e -> cancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void validateSelf() {
        if (StringUtils.isEmpty(version.getText())
                || StringUtils.isEmpty(settings.getGitHome())
                || StringUtils.isEmpty(settings.getMavenHome()))
            ok.setEnabled(false);
        else
            ok.setEnabled(true);
    }

    private void confirm() {
        // 异步执行，并开启执行进度条
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "版本助手") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    // 修改 Maven 版本
                    SettingsUtils.saveLatestVersion(version.getText());
                    List<String> logs = new ArrayList<>();
                    setMavenVersion(indicator, logs);
                    // Git 操作
                    commitVersion(indicator, logs);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        dispose();
    }

    private void cancel() {
        dispose();
    }

    private void setMavenVersion(ProgressIndicator indicator, List<String> logs) throws IOException {
        // /Users/felixu/application/apache-maven-3.8.1/bin/mvn
        // /Users/felixu/.m2/settings-isyscore.xml
        String[] cmd = new String[]{settings.getMavenHome(), "versions:set", "-DnewVersion=" + version.getText(), "-Dskip.test=true", "", ""};
        if (StringUtils.isNotEmpty(settings.getMavenConf())) {
            cmd[4] = "--settings";
            cmd[5] = settings.getMavenConf();
        }
        Process ps = Runtime.getRuntime().exec(cmd, null, new File(Objects.requireNonNull(project.getBasePath())));
        String result = getProcessResult(ps);
        logs.add("-------- Maven 修改版本号 -----------");
        logs.add(String.join(" ", cmd));
        indicator.setText("修改版本");
        setInfo(result, indicator, logs);
    }

    private void setInfo(String info, ProgressIndicator indicator, List<String> logs) {
        Arrays.stream(info.split("\n")).forEach(str -> {
            indicator.setText2(str);
            logs.add(str);
        });
        notification.infoLog(String.join("\n", logs));
        logs.clear();
    }

    private void commitVersion(ProgressIndicator indicator, List<String> logs) throws IOException {
        String git = settings.getGitHome();
        File dir = new File(Objects.requireNonNull(project.getBasePath()));

        // 提交信息
        logs.add("-------- 添加修改的文件 -----------");
        String[] gitAdd = {git, "add", "."};
        logs.add(String.join(" ", gitAdd));
        String addInfo = getProcessResult(Runtime.getRuntime().exec(gitAdd, null, dir));
        indicator.setText("在当前分支提交版本");
        setInfo(addInfo, indicator, logs);

        logs.add("-------- 提交当前修改 -----------");
        String[] gitCommit = {git, "commit", "-m", "build: " + version.getText()};
        logs.add(String.join(" ", gitCommit));
        String commitInfo = getProcessResult(Runtime.getRuntime().exec(gitCommit, null, dir));
        setInfo(commitInfo, indicator, logs);

        // 推送
        logs.add("-------- 推送到远程 -----------");
        String[] gitPush = {git, "push"};
        logs.add(String.join(" ", gitPush));
        String currentPushInfo = getProcessResult(Runtime.getRuntime().exec(gitPush, null, dir));
        indicator.setText("在当前分支推送版本");
        setInfo(currentPushInfo, indicator, logs);

        // 切分支
        String currentBranch = getCurrentBranch(indicator, logs);
        logs.add("-------- 切换到目标分支 -----------");
        String[] gitCheckout1 = {git, "checkout", (String) branch.getSelectedItem()};
        logs.add(String.join(" ", gitCheckout1));
        String checkoutInfo = getProcessResult(Runtime.getRuntime().exec(gitCheckout1, null, dir));
        indicator.setText("切换到目标分支");
        setInfo(checkoutInfo, indicator, logs);

        // merge
        logs.add("-------- 合并分支 -----------");
        String[] gitMerge = {git, "merge", currentBranch};
        logs.add(String.join(" ", gitMerge));
        String mergeInfo = getProcessResult(Runtime.getRuntime().exec(gitMerge, null, dir));
        indicator.setText("合并分支");
        setInfo(mergeInfo, indicator, logs);

        // 推送
        logs.add("-------- 推送到目标远程分支 -----------");
        String[] gitPushPush = {git, "push"};
        logs.add(String.join(" ", gitPushPush));
        String pushInfo = getProcessResult(Runtime.getRuntime().exec(gitPushPush, null, dir));
        indicator.setText("在目标分支推送版本");
        setInfo(pushInfo, indicator, logs);

        // 切回当前分支
        logs.add("-------- 回到当前分支 -----------");
        String[] gitCheckout2 = {git, "checkout", currentBranch};
        logs.add(String.join(" ", gitCheckout2));
        String backInfo = getProcessResult(Runtime.getRuntime().exec(gitCheckout2, null, dir));
        indicator.setText("返回初始分支");
        setInfo(backInfo, indicator, logs);
    }

    private List<String> getAllBranch() throws IOException {
        String[] cmd = new String[]{settings.getGitHome(), "branch", "-a"};
        Process ps = Runtime.getRuntime().exec(cmd, null, new File(Objects.requireNonNull(project.getBasePath())));
        String result = getProcessResult(ps);
        return Arrays.stream(result.split("\n")).collect(Collectors.toList());
    }

    private String getCurrentBranch(ProgressIndicator indicator, List<String> logs) throws IOException {
        String[] cmd = new String[]{settings.getGitHome(), "branch", "--show-current"};
        Process ps = Runtime.getRuntime().exec(cmd, null, new File(Objects.requireNonNull(project.getBasePath())));
        String result = getProcessResult(ps);
        logs.add("-------- 获取当前分支 -----------");
        logs.add(String.join(" ", cmd));
        setInfo(result, indicator, logs);
        return result;
    }

    private String getProcessResult(Process ps) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(ps.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line).append("\n");
        }
        String result = sb.toString();
        if (result.length() < 2)
            return result;
        return result.substring(0, result.length() - 1);
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        content = new JPanel();
        content.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        content.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        cancel = new JButton();
        cancel.setText("取消");
        panel2.add(cancel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        ok = new JButton();
        ok.setText("确认");
        panel2.add(ok, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        content.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel3.setBorder(BorderFactory.createTitledBorder(null, "提交版本", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JLabel label1 = new JLabel();
        label1.setText("目标分支：");
        panel3.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("目标版本：");
        panel3.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        version = new JTextField();
        version.setText("");
        panel3.add(version, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        branch = new JComboBox();
        panel3.add(branch, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return content;
    }

}
