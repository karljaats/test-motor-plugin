package ${package};

<#if imports != "">
    ${imports}

</#if>
import org.junit.Test;
import static org.junit.Assert.*;

public class ${class_name} {

${other_code}

<#list tests as test>
    @Test
    public void ${test[0]}() {
    <#list test[1..] as data>
        assertEquals(${data.expected}, f(${data.input}));
    </#list>
    }
</#list>
}