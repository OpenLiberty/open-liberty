/**
 *
 */
package jaxrs21.fat.providerPriority;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

@Consumes(MediaType.TEXT_PLAIN)
@Produces(MediaType.TEXT_PLAIN)
public class ClientSideMBRW implements MessageBodyReader<MyObject>, MessageBodyWriter<MyObject> {

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
        os.write(myObject.toString().getBytes());
        os.flush();
        os.close();
    }

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
    public MyObject readFrom(Class<MyObject> arg0, Type arg1, Annotation[] arg2, MediaType arg3, MultivaluedMap<String, String> arg4,
                             InputStream is) throws IOException, WebApplicationException {

        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line = br.readLine();
        System.out.println("ClientSideMBR received: " + line);
        String[] fields = line.split("\\|");

        MyObject myObject = new MyObject();
        myObject.setMyString(fields[0]);
        myObject.setMyInt(Integer.parseInt(fields[1]));
        myObject.setMbrVersion(Integer.parseInt(fields[2]));
        myObject.setContextResolverVersionFromReader(Integer.parseInt(fields[3]));
        myObject.setMbwVersion(Integer.parseInt(fields[4]));
        myObject.setContextResolverVersionFromWriter(Integer.parseInt(fields[5]));

        return myObject;
    }

}
