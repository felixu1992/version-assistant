package top.felixu.settings;

import com.intellij.openapi.options.SearchableConfigurable;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.felixu.model.Settings;
import top.felixu.util.SettingsUtils;

import javax.swing.*;

/**
 * @author felixu
 * @since 2022.04.18
 */
public class VersionSettings implements SearchableConfigurable {

    private VersionSettingsUI settingsUI = new VersionSettingsUI();

    @Override
    public @NotNull @NonNls String getId() {
        return "version-assistant";
    }

    @Override
    public @Nullable JComponent createComponent() {
        Settings settings = SettingsUtils.get();
        settingsUI.getMavenHome().setText(settings.getMavenHome());
        settingsUI.getMavenConf().setText(settings.getMavenConf());
        settingsUI.getGitHome().setText(settings.getGitHome());
        return settingsUI.$$$getRootComponent$$$();
    }

    @Override
    public boolean isModified() {
        return true;
    }

    @Override
    public void apply() {
        Settings settings = new Settings(settingsUI.getMavenHome().getText(), settingsUI.getMavenConf().getText(), settingsUI.getGitHome().getText());
        SettingsUtils.save(settings);
    }

    @Override
    public String getDisplayName() {
        return "版本助手";
    }
}
