package top.felixu.model;

/**
 * @author felixu
 * @since 2022.04.18
 */
public class Settings {

    private String mavenHome;

    private String mavenConf;

    private String gitHome;


    public Settings() {
    }

    public Settings(String mavenHome, String mavenConf, String gitHome) {
        this.mavenHome = mavenHome;
        this.mavenConf = mavenConf;
        this.gitHome = gitHome;
    }

    public String getMavenHome() {
        return mavenHome;
    }

    public void setMavenHome(String mavenHome) {
        this.mavenHome = mavenHome;
    }

    public String getMavenConf() {
        return mavenConf;
    }

    public void setMavenConf(String mavenConf) {
        this.mavenConf = mavenConf;
    }

    public String getGitHome() {
        return gitHome;
    }

    public void setGitHome(String gitHome) {
        this.gitHome = gitHome;
    }

    @Override
    public String toString() {
        return "Settings{" +
                "mavenHome='" + mavenHome + '\'' +
                ", mavenConf='" + mavenConf + '\'' +
                ", gitHome='" + gitHome + '\'' +
                '}';
    }
}
