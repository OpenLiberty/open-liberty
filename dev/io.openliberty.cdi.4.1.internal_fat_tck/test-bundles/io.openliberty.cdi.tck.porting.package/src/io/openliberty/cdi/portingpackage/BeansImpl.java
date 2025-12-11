/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.cdi.portingpackage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.jboss.cdi.tck.spi.Beans;

import com.ibm.ws.kernel.service.util.ServiceCaller;
import com.ibm.ws.serialization.SerializationService;

/**
 * Porting package Beans impl for OpenLiberty.
 *
 * Adapted from the Weld porting package to use liberty's SerializationService.
 */
public class BeansImpl implements Beans {

    private static final ServiceCaller<SerializationService> serializationService = new ServiceCaller<>(BeansImpl.class, SerializationService.class);

    @Override
    public boolean isProxy(Object instance) {
        return instance.getClass().getName().indexOf("_$$_Weld") > 0;
    }

    @Override
    public Object activate(byte[] bytes) throws IOException, ClassNotFoundException {
        // Use SerializationService to deserialize, use TCCL as classloader

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        SerializationService service = serializationService.current().orElseThrow(() -> new RuntimeException("SerializationService not available"));

        try (ByteArrayInputStream inStream = new ByteArrayInputStream(bytes);
                        ObjectInputStream objStream = service.createObjectInputStream(inStream, tccl)) {
            return objStream.readObject();
        }
    }

    @Override
    public byte[] passivate(Object object) throws IOException {
        SerializationService service = serializationService.current().orElseThrow(() -> new RuntimeException("SerializationService not available"));
        try (ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                        ObjectOutputStream objStream = service.createObjectOutputStream(outStream)) {
            objStream.writeObject(object);
            return outStream.toByteArray();
        }
    }

}
