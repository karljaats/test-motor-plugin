import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;

import java.io.File;

public class GradleRunner {

    public static void compileClasses(String projectPath){
        // Compiles the code using Gradle before building the template but doesn't notify the user of it
        GradleConnector connector = GradleConnector.newConnector();
        connector.forProjectDirectory(new File(projectPath));
        ProjectConnection connection = connector.connect();

        BuildLauncher build = connection.newBuild();
        build.forTasks("compileJava", "compileTestJava");
        build.setStandardOutput(System.out);
        build.run();

        connection.close();

        /*
        // Alternate way to run Gradle but, as it gets executed separately, the Action often finishes before it does
        String[] path = event.getProject().getBasePath().split("/");
        String configutationName = path[path.length-1] + " [build]";
        ProgramRunnerUtil.executeConfiguration(
                RunManager.getInstance(event.getProject()).findConfigurationByName(configutationName),
                DefaultRunExecutor.getRunExecutorInstance());
        */
    }
}
