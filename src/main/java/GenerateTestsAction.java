import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import freemarker.template.Configuration;
import freemarker.template.Template;
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
        String oldclassName = extractClassName(text);
        String newclassName = oldclassName + "Test";

        dataMap.put("package", packageName);
        dataMap.put("imports", extractImports(text));
        dataMap.put("class_name", newclassName);
        dataMap.put("rest_of_code", extractRestOfCode(text));
        dataMap.put("tests", extractTests(text, packageName + "." + oldclassName, projectPath));

        Writer file=null;
        try {
            //TODO get the project internal folder structure from somewhere and don't assume it?
            //TODO test if this works if some folders are missing
            file = new FileWriter(new File(projectPath + "/src/test/java/" + packageName +
                    "/" + newclassName + ".java"));
            template.process(dataMap, file);
            file.flush();
            System.out.println("File created");
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

    private static List extractTests(String text, String classname, String projectpath){
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
            String dataFieldName = "data" + m.group("dataName");

            List<Object> expectedResults;
            try {
                expectedResults = getExpectedResults(projectpath, classname, dataFieldName);
            } catch (Exception e){
                System.out.println("Couldn't generate expected results for " + dataFieldName);
                e.printStackTrace();
                continue;
            }

            List<Object> test = new ArrayList<>();
            test.add("test" + m.group("dataName"));

            for (int i=0; i < dataList.length; i++) {
                String input = dataList[i];
                Map<String, Object> assert1 = new HashMap<>();
                assert1.put("input", input);
                try {
                    Object expected = expectedResults.get(i);
                    if(expected == null){
                        assert1.put("expected", "null");
                    }
                    else if(expected instanceof String){
                        assert1.put("expected", "\"" + expected + "\"");
                    } else {
                        assert1.put("expected", expected.toString());
                    }
                } catch (Exception e){
                    e.printStackTrace();
                    assert1.put("expected", "?");
                }
                test.add(assert1);
            }

            tests.add(test);
        }

        return tests;
    }

    private static List<Object> getExpectedResults(String projectpath, String className, String dataFieldName)
            throws IOException, ClassNotFoundException, IllegalAccessException, InvocationTargetException{
        //TODO maybe add some try catch blocks here instead and write some debug logs for them

        //TODO get the path for this from somewhere
        //TODO and/or check that the class actually exists there

        //Path p = Paths.get(projectpath, "out", "test", "classes");
        //Path p2 = Paths.get(projectpath, "out", "production", "classes");
        Path p = Paths.get(projectpath, "build", "classes", "java", "test");
        Path p2 = Paths.get(projectpath, "build", "classes", "java", "main");

        URL[] urls = {p.toUri().toURL(), p2.toUri().toURL()};

        try (URLClassLoader classLoader = new URLClassLoader(urls)) {
            Class<?> clazz = classLoader.loadClass(className);

            Method method = null;
            for (Method m : clazz.getDeclaredMethods()) {
                if (m.getName().equals("f")) method = m;
            }

            Object[] dataArray = null;
            for(Field field: clazz.getDeclaredFields()){
                if(field.getName().equals(dataFieldName)){
                    field.setAccessible(true);
                    Object array = field.get(null);

                    dataArray = new Object[Array.getLength(array)];
                    for(int i = 0; i < Array.getLength(array); i++){
                        dataArray[i] = Array.get(array, i);
                    }

                    break;
                }
            }

            List<Object> expectedResults = new ArrayList<>();

            // Can access even if private
            method.setAccessible(true);

            for(Object input: dataArray){
                expectedResults.add(method.invoke(null, input));
            }

            return expectedResults;
        }
    }
}
