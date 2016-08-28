package util;

import org.apache.commons.io.IOUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.runtime.parser.node.SimpleNode;

import javax.sql.rowset.serial.SerialBlob;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Blob;
import java.util.Map;

public class VelocityUtil {


    private static RuntimeServices rs = RuntimeSingleton.getRuntimeServices();


    public static String getResult(Template t, Map<String, Object> map) {
        try {
            VelocityContext vc = new VelocityContext(map);
            StringWriter sw = new StringWriter();
            t.merge(vc, sw);
            return sw.toString();
        } catch (Exception e) {
            return "";
        }
    }


    public static Template getTemplate(String template, String templateId) {
        try {
            Blob blob = new SerialBlob(template.getBytes());
            StringReader sr = new StringReader(IOUtils.toString(blob.getBinaryStream()));
            SimpleNode sn = rs.parse(sr, templateId);
            Template t = new Template();
            t.setRuntimeServices(rs);
            t.setData(sn);
            t.initDocument();
            return t;
        } catch (Exception e) {
            return null;
        }
    }

}
