/**
 *
 */
package jaxb.web.utils;

import java.io.IOException;
import java.io.StringWriter;

import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;

import javax.xml.bind.SchemaOutputResolver;
/**
 *
 */
public class StringSchemaOutputResolver extends SchemaOutputResolver {

    private StringWriter stringWriter = new StringWriter();

    public Result createOutput(String namespaceURI, String suggestedFileName) throws IOException {
        StreamResult result = new StreamResult(stringWriter);
        result.setSystemId(suggestedFileName);
        return result;
    }

    public String getSchema() {
        return stringWriter.toString();
    }
}
