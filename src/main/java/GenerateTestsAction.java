import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;


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

        TestGenerator.process_template(text, projectPath);
    }


}
