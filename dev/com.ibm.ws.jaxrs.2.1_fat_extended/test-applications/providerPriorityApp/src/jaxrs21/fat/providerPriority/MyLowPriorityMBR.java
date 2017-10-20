/**
 *
 */
package jaxrs21.fat.providerPriority;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.annotation.Priority;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;

@Provider
@Priority(1002)
public class MyLowPriorityMBR implements MessageBodyReader<MyObject> {

    private static final int mbrVersion = 1;

    @Context
    Providers providers;

    /*
     * (non-Javadoc)
     *
     * @see javax.ws.rs.ext.MessageBodyReader#isReadable(java.lang.Class, java.lang.reflect.Type, java.lang.annotation.Annotation[], javax.ws.rs.core.MediaType)
     */
    @Override
    public boolean isReadable(Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.ws.rs.ext.MessageBodyReader#readFrom(java.lang.Class, java.lang.reflect.Type, java.lang.annotation.Annotation[], javax.ws.rs.core.MediaType,
     * javax.ws.rs.core.MultivaluedMap, java.io.InputStream)
     */
    @Override
    public MyObject readFrom(Class<MyObject> clazz, Type type, Annotation[] annos, MediaType mt, MultivaluedMap<String, String> map,
                             InputStream is) throws IOException, WebApplicationException {

        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line = br.readLine();
        System.out.println("MyLowPriorityMBR received: " + line);
        String[] fields = line.split("\\|");

        MyObject myObject = new MyObject();
        myObject.setMyString(fields[0]);
        myObject.setMyInt(Integer.parseInt(fields[1]));
        myObject.setMbrVersion(mbrVersion);
        Version v = providers.getContextResolver(Version.class, mt).getContext(null);
        myObject.setContextResolverVersionFromReader(v.getVersion());

        return myObject;
    }

}
