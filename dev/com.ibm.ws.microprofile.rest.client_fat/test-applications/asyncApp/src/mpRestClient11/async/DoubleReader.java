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
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

@Provider
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DoubleReader implements MessageBodyReader<Double> {
    private final static Logger _log = Logger.getLogger(DoubleReader.class.getName());

    @Context
    UriInfo uriInfo;
    
    @Override
    public boolean isReadable(Class<?> clazz, Type type, Annotation[] anno, MediaType mt) {
        return clazz == Double.class && mt.equals(MediaType.APPLICATION_JSON_TYPE);
    }

    @Override
    public Double readFrom(Class<Double> clazz, Type type, Annotation[] anno, MediaType mt,
            MultivaluedMap<String, String> headers, InputStream is) throws IOException, WebApplicationException {

        byte[] buf = new byte[4096];
        int bytesRead = is.read(buf);
        if (bytesRead < 0) {
            _log.log(Level.INFO, "!!! No response body from URI " + uriInfo.getAbsolutePath(), new Throwable());
            return 0.0;
        }
        String json = new String(Arrays.copyOf(buf, bytesRead));
        
        Double d;
        try {
            d = Double.parseDouble(json);
        } catch (NumberFormatException ex) {
            Jsonb jsonb = JsonbBuilder.create();
            d = jsonb.fromJson(json, Double.class);
        }
        _log.info("Response body from URI " + uriInfo.getAbsolutePath() + " = " + json + " ; parsed value is: " + d);
        return d;
    }

}
