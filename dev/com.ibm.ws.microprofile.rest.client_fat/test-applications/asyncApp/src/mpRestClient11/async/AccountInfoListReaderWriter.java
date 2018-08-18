/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package mpRestClient11.async;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

@Provider
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AccountInfoListReaderWriter
        implements MessageBodyReader<List<AccountInfo>>, MessageBodyWriter<List<AccountInfo>> {

    @Override
    public long getSize(List<AccountInfo> arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean isWriteable(Class<?> clazz, Type type, Annotation[] annos, MediaType mt) {
        return clazz == List.class && mt.equals(MediaType.APPLICATION_JSON_TYPE);
    }

    @Override
    public void writeTo(List<AccountInfo> list, Class<?> clazz, Type type, Annotation[] annos, MediaType mt,
            MultivaluedMap<String, Object> headers, OutputStream os) throws IOException, WebApplicationException {
        Jsonb jsonb = JsonbBuilder.create();
        String result = jsonb.toJson(list);
        os.write(result.getBytes());
        os.flush();
    }

    @Override
    public boolean isReadable(Class<?> clazz, Type type, Annotation[] anno, MediaType mt) {
        return clazz == List.class && mt.equals(MediaType.APPLICATION_JSON_TYPE);
    }

    @SuppressWarnings("serial")
    @Override
    public List<AccountInfo> readFrom(Class<List<AccountInfo>> clazz, Type type, Annotation[] anno, MediaType mt,
            MultivaluedMap<String, String> headers, InputStream is) throws IOException, WebApplicationException {
        int size = Integer.parseInt( headers.getFirst(HttpHeaders.CONTENT_LENGTH) );
        byte[] buf = new byte[size];
        is.read(buf);
        String json = new String(buf);
        Jsonb jsonb = JsonbBuilder.create();
        return jsonb.fromJson(json, new ArrayList<AccountInfo>(){}.getClass().getGenericSuperclass());
    }

}
