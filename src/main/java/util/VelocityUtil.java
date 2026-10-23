package util;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeInstance;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

public class VelocityUtil {
    private static final RuntimeInstance runtime = new RuntimeInstance();
    static { runtime.init(); }

    public static String getResult(Template template, Map<String, Object> values) {
        StringWriter out = new StringWriter();
        template.merge(new VelocityContext(values),out);
        return out.toString();
    }

    public static Template getTemplate(String source, String id) {
        try {
            Template template = new Template();
            template.setName(id);
            template.setRuntimeServices(runtime);
            template.setData(runtime.parse(new StringReader(source),template));
            template.initDocument();
            return template;
        } catch (Exception e) { throw new IllegalArgumentException("Cannot parse template " + id,e); }
    }
}
