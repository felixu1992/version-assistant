package top.felixu.util;

import com.intellij.ide.util.PropertiesComponent;
import top.felixu.model.Settings;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author felixu
 * @since 2022.04.18
 */
public class SettingsUtils {

    private static final PropertiesComponent PROPERTIES_COMPONENT = PropertiesComponent.getInstance();

    private static final List<Field> FIELDS = Arrays.stream(Settings.class.getDeclaredFields()).collect(Collectors.toList());

    public static void save(Settings settings) {
        FIELDS.forEach(field -> {
            try {
                field.setAccessible(true);
                PROPERTIES_COMPONENT.setValue(field.getName(), (String) field.get(settings));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        });
    }

    public static Settings get() {
        Settings settings = new Settings();
        FIELDS.forEach(field -> {
            try {
                field.setAccessible(true);
                field.set(settings, PROPERTIES_COMPONENT.getValue(field.getName()));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        });
        return settings;
    }

    public static void saveLatestVersion(String version) {
        PROPERTIES_COMPONENT.setValue("LATEST", version);
    }

    public static String getLatestVersion() {
        return PROPERTIES_COMPONENT.getValue("LATEST");
    }
}
