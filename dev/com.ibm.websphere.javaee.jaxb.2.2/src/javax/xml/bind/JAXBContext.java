/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package javax.xml.bind;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.Map;

import org.w3c.dom.Node;

public abstract class JAXBContext {

    public static final String JAXB_CONTEXT_FACTORY = "javax.xml.bind.context.factory";

    protected JAXBContext() {
    }

    public Binder<Node> createBinder() {
        throw new UnsupportedOperationException();
    }

    public <T> Binder<T> createBinder(Class<T> domType) {
        throw new UnsupportedOperationException();
    }

    public JAXBIntrospector createJAXBIntrospector() {
        throw new UnsupportedOperationException();
    }

    public abstract Marshaller createMarshaller() throws JAXBException;

    public abstract Unmarshaller createUnmarshaller() throws JAXBException;

    public abstract Validator createValidator() throws JAXBException;

    public void generateSchema(SchemaOutputResolver resolver) throws IOException {
        throw new UnsupportedOperationException();
    }

    public static JAXBContext newInstance(Class... classesToBeBound) throws JAXBException {
        return newInstance(classesToBeBound, Collections.<String, Object> emptyMap());
    }

    public static JAXBContext newInstance(Class[] classesToBeBound, Map<String, ?> properties) throws JAXBException {
        for (Class cl : classesToBeBound) {
            if (cl == null) {
                throw new IllegalArgumentException();
            }
        }
        // Liberty Change Start: Add doPriv
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<JAXBContext>() {
                @Override
                public JAXBContext run() throws JAXBException {
                    return ContextFinder.find(classesToBeBound, properties);
                }
            });
        } catch (PrivilegedActionException e) {
            throw (JAXBException) e.getException();
        }
        // Liberty Change End
    }

    public static JAXBContext newInstance(String contextPath) throws JAXBException {
        return newInstance(contextPath, Thread.currentThread().getContextClassLoader());
    }

    public static JAXBContext newInstance(String contextPath, ClassLoader classLoader) throws JAXBException {
        return newInstance(contextPath, classLoader, Collections.<String, Object> emptyMap());
    }

    public static JAXBContext newInstance(String contextPath, ClassLoader classLoader, Map<String, ?> properties) throws JAXBException {
        // Liberty Change Start: Add doPriv
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<JAXBContext>() {
                @Override
                public JAXBContext run() throws JAXBException {
                    return ContextFinder.find(contextPath, classLoader, properties);
                }
            });
        } catch (PrivilegedActionException e) {
            throw (JAXBException) e.getException();
        }
        // Liberty Change End
    }
}
