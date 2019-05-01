import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;

import java.io.File;


public class GenerateTestsAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent event) {

        String text;
        String projectPath;
        try {
            text = event.getData(LangDataKeys.EDITOR).getDocument().getText();
            projectPath = event.getProject().getBasePath();
        } catch (NullPointerException e){
            System.out.println("Target document not found. Stopping");
            return;
        }



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
        // Runs Gradle build but in a different thread so the tests usually get generated with the old .class files anyway
        String[] path = event.getProject().getBasePath().split("/");
        String configutationName = path[path.length-1] + " [build]";
        ProgramRunnerUtil.executeConfiguration(
                RunManager.getInstance(event.getProject()).findConfigurationByName(configutationName),
                DefaultRunExecutor.getRunExecutorInstance());
        */

        TestGenerator.process_template(text, projectPath);
    }


}
