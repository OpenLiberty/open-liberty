/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.jaxrs.provider;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBElement;

@Produces({ "application/xml", "application/*+xml", "text/xml" })
@Consumes({ "application/xml", "application/*+xml", "text/xml" })
@Provider
public class JAXBElementSubProvider extends JAXBElementProvider<JAXBElement<?>> implements FakeInterface<JAXBElement<?>> {

    public JAXBElementSubProvider() {

    }

    private static boolean isSupported(Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            return (Class<?>) (pType.getRawType()) == JAXBElement.class;
        } else if (type instanceof Class<?>) {
            return (Class<?>) type == JAXBElement.class;
        }
        return false;
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] anns, MediaType mt) {
        if (!isSupported(genericType))
            return false;
        return super.isReadable(type, genericType, anns, mt);
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] anns, MediaType mt) {

        if (!isSupported(genericType))
            return false;
        return super.isWriteable(type, genericType, anns, mt);

    }
}
