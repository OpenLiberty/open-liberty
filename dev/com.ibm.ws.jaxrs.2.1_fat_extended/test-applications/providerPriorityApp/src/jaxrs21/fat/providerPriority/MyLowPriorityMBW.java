/**
 *
 */
package jaxrs21.fat.providerPriority;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.annotation.Priority;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;

@Provider
@Priority(1015)
public class MyLowPriorityMBW implements MessageBodyWriter<MyObject> {

    private static final int mbwVersion = 1;

    @Context
    Providers providers;

    /*
     * (non-Javadoc)
     *
     * @see javax.ws.rs.ext.MessageBodyWriter#isWriteable(java.lang.Class, java.lang.reflect.Type, java.lang.annotation.Annotation[], javax.ws.rs.core.MediaType)
     */
    @Override
    public boolean isWriteable(Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.ws.rs.ext.MessageBodyWriter#writeTo(java.lang.Object, java.lang.Class, java.lang.reflect.Type, java.lang.annotation.Annotation[], javax.ws.rs.core.MediaType,
     * javax.ws.rs.core.MultivaluedMap, java.io.OutputStream)
     */
    @Override
    public void writeTo(MyObject myObject, Class<?> clazz, Type type, Annotation[] annos, MediaType mt, MultivaluedMap<String, Object> map,
                        OutputStream os) throws IOException, WebApplicationException {
        myObject.setMbwVersion(mbwVersion);
        Version v = providers.getContextResolver(Version.class, MediaType.TEXT_PLAIN_TYPE).getContext(null);
        myObject.setContextResolverVersionFromWriter(v.getVersion());

        System.out.println("MyLowPriorityMBW sending: " + myObject);
        os.write(myObject.toString().getBytes());
        os.flush();
        os.close();
    }

}
