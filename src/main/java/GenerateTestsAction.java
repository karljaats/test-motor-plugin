import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.Version;


public class GenerateTestsAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getProject();

        process_template();
    }

    private void process_template(){
        Configuration cfg = new Configuration(new Version("2.3.28"));
        //TODO make sure this actually works like this in an action
        cfg.setClassForTemplateLoading(GenerateTestsAction.class, "/");
        cfg.setDefaultEncoding("UTF-8");

        Template template=null;
        Map<String, Object> dataMap = new HashMap<String, Object>();

        try {
            template = cfg.getTemplate("test-template1.ftl");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // input mockup
        List<Object> tests = new ArrayList<>();
        List<Object> test1 = new ArrayList<>();
        test1.add("test01");
        Map<String, String> assert1 = new HashMap<>();
        assert1.put("expected", "11");
        assert1.put("input", "10");
        test1.add(assert1);
        tests.add(test1);

        String other_code = "" +
                "    private int f(int input) {\n" +
                "        Basic basic = new Basic();\n" +
                "        return basic.inc(input);\n" +
                "    }\n" +
                "\n" +
                "    private static int x = 10;\n" +
                "    private static int computeValue() {\n" +
                "        return 100;\n" +
                "    }";

        dataMap.put("package", "lihtne");
        dataMap.put("imports", "");
        dataMap.put("class_name", "Test1");
        dataMap.put("other_code", other_code);
        dataMap.put("tests", tests);

        Writer file=null;
        try {
            file = new FileWriter(new File("src/test/java/lihtne/Test1.java"));
            template.process(dataMap, file);
            file.flush();
            System.out.println("Success");
        }catch (Exception e) {
            e.printStackTrace();
        }
        finally
        {
            try {
                file.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
