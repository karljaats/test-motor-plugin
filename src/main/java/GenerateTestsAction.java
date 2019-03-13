import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.Version;


public class GenerateTestsAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(GenerateTestsAction.class.getName());

    @Override
    public void actionPerformed(AnActionEvent event) {

        String text;
        String projectPath;
        try {
            text = event.getData(LangDataKeys.EDITOR).getDocument().getText();
            projectPath = event.getProject().getBasePath();
        } catch (NullPointerException e){
            LOG.debug("Target document not found. Stopping");
            return;
        }


        process_template(text, projectPath);
    }

    private void process_template(String text, String projectPath){
        Configuration cfg = new Configuration(new Version("2.3.28"));
        cfg.setClassForTemplateLoading(GenerateTestsAction.class, "/");
        cfg.setDefaultEncoding("UTF-8");

        Template template=null;
        Map<String, Object> dataMap = new HashMap<String, Object>();

        try {
            template = cfg.getTemplate("test-template1.ftl");
        } catch (IOException e) {
            e.printStackTrace();
        }

        String packageName = extractPackageName(text);
        String className = extractClassName(text) + "Test";

        dataMap.put("package", packageName);
        dataMap.put("imports", extractImports(text));
        dataMap.put("class_name", className);
        dataMap.put("rest_of_code", extractRestOfCode(text));
        dataMap.put("tests", extractTests(text));

        Writer file=null;
        try {
            //TODO get the project internal folder structure from somewhere and don't assume it?
            //TODO test if this works if some folders are missing
            file = new FileWriter(new File(projectPath + "/src/test/java/" + packageName +
                    "/" + className + ".java"));
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
            } catch (NullPointerException e){
                LOG.debug("File to close is null");
            }
        }
    }


    private static String extractPackageName(String text){
        Pattern p = Pattern.compile("" +
                        "^package (?<packageName>.*);"
                , Pattern.MULTILINE);
        Matcher m = p.matcher(text);

        if(m.find()){
            return m.group("packageName");
        } else {
            return "";
        }
    }

    private static String extractImports(String text){
        //TODO doesn't consider comments
        Pattern p = Pattern.compile("" +
                        "(?<imports>import .*)\n.*? class"
                , Pattern.DOTALL | Pattern.MULTILINE);
        Matcher m = p.matcher(text);

        if(m.find()){
            return m.group("imports");
        } else {
            return "";
        }
    }

    private static String extractClassName(String text){
        //TODO doesn't consider comments
        Pattern p = Pattern.compile("" +
                        "class (?<className>.*?) \\{"
                , Pattern.MULTILINE);
        Matcher m = p.matcher(text);

        if(m.find()){
            return m.group("className");
        } else {
            return "";
        }
    }

    private static String extractRestOfCode(String text){
        //TODO currently all the rest of the code needs to be before data
        //TODO doesn't consider comments
        Pattern p = Pattern.compile("" +
                        "class .*? \\{(?<restOfCode>.*?)^[^\n]*? data[A-Za-z0-9$]*\\s*\\[]"
                , Pattern.DOTALL | Pattern.MULTILINE);
        Matcher m = p.matcher(text);

        if(m.find()){
            return m.group("restOfCode");
        } else {
            return "";
        }
    }

    private static List extractTests(String text){
        //TODO doesn't consider comments
        Pattern p = Pattern.compile("" +
                        "^[^\n]*? data(?<dataName>[A-Za-z0-9$]*)\\s*\\[]\\s*=\\s*\\{(?<data>.*?)}"
                , Pattern.DOTALL | Pattern.MULTILINE);
        Matcher m = p.matcher(text);

        List<Object> tests = new ArrayList<>();

        while(m.find()){
            String data = m.group("data");
            data = data.replaceAll("\\s+", "");
            String[] dataList = data.split(",");

            List<Object> test = new ArrayList<>();
            test.add("test" + m.group("dataName"));
            for (String input : dataList) {
                Map<String, String> assert1 = new HashMap<>();
                assert1.put("input", input);
                assert1.put("expected", input);
                test.add(assert1);
            }

            tests.add(test);
        }

        return tests;
    }
}
