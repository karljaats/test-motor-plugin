import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import testmotor.GenerationException;
import testmotor.TestMotor;
import testmotor.tree.ExecNode;
import testmotor.tree.ValueNode;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class RunTestmotorAction extends AnAction {

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

       GradleRunner.compileClasses(projectPath);


        // create new TestMotor instance
        TestMotor tm = new TestMotor();
        // set seed for randomness
        tm.setSeed((int)System.currentTimeMillis());
        Method method = null;
        try {
            // find target method
            String classname = TestGenerator.extractPackageName(text) + "." + TestGenerator.extractClassName(text);

            Path p = Paths.get(projectPath, "build", "classes", "java", "test");
            Path p2 = Paths.get(projectPath, "build", "classes", "java", "main");

            URL[] urls = {p.toUri().toURL(), p2.toUri().toURL()};

            URLClassLoader classLoader = new URLClassLoader(urls);
            Class<?> clazz = classLoader.loadClass(classname);

            for (Method m : clazz.getDeclaredMethods()) {
                if (m.getName().equals("f")) method = m;
            }

        } catch (MalformedURLException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        int height = 1; // generated tree (test data) height
        boolean allowExceptions = false; // generated test data won't throw exceptions
        List<String> inputs = new ArrayList<>();
        try {
            for(int i=0; i<5; i++) {
                // generate single instance of test data
                ValueNode testData = tm.generateTestData(method, height, null, allowExceptions);

                inputs.add(((ExecNode) testData).getArguments().get(0).stringify());
            }
        } catch (GenerationException e) {
            //System.out.println(e.getMessage());
            //e.printStackTrace();
            System.out.println("Test data generation failed!");
        }

        for(String a: inputs){
            System.out.println(a);
        }

        int index = text.lastIndexOf("}");
        String text1 = text.substring(0, index);
        String text2 = text.substring(index);

        Type parameterType = method.getGenericParameterTypes()[0];

        String addedText = "\n";
        addedText += "\tpublic static " + parameterType.getTypeName() + " data1[] = {\n";
        for(String input: inputs){
            addedText = addedText.concat("\t\t\t" + input + ",\n");
        }
        addedText = addedText.substring(0, addedText.length()-2); // remove last coma
        addedText += "\n\t};";
        addedText += "\n";

        final String reassembledText = text1 + addedText + text2;
        WriteCommandAction.runWriteCommandAction(event.getProject(), () ->
                event.getData(LangDataKeys.EDITOR).getDocument().setText(reassembledText)
        );
    }
}
