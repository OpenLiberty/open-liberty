/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2013
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.onelocalinterface.provider;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.wink.common.utils.ProviderUtils;

@Stateless
@Local(OneLocalInterfaceMyOtherStuffMessageBodyWriter.class)
public class OneLocalInterfaceMyOtherStuffProvider {

    /*
     * note that the above DOES NOT directly implement the session bean interface
     */

    public long getSize(Object t,
                        Class<?> type,
                        Type genericType,
                        Annotation[] annotations,
                        MediaType mediaType) {
        return -1;
    }

    public boolean isWriteable(Class<?> type,
                               Type genericType,
                               Annotation[] annotations,
                               MediaType mediaType) {
        // OneLocalInterfaceMyStuffMessageBodyWriter has a
        // @Produces on it
        if (type == String.class) {
            return true;
        }
        return false;
    }

    public void writeTo(Object t,
                        Class<?> type,
                        Type genericType,
                        Annotation[] annotations,
                        MediaType mediaType,
                        MultivaluedMap<String, Object> httpHeaders,
                        OutputStream entityStream) throws IOException {
        ProviderUtils.writeToStream(this.getClass().getName() + " wrote this.",
                                    entityStream,
                                    mediaType);
    }
}
