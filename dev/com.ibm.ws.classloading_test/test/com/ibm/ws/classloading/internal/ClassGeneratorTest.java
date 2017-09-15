/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;

import junit.framework.Assert;

import org.jmock.Mockery;
import org.junit.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import test.common.ComponentContextMockery;

import com.ibm.ws.classloading.ClassGenerator;
import com.ibm.wsspi.adaptable.module.Container;

public class ClassGeneratorTest {
    public static class GeneratedClass {}

    @Test
    public void testClassGenerator() throws Exception {
        final ClassGenerator gen = new ClassGenerator() {
            @Override
            public byte[] generateClass(String name, ClassLoader loader) throws ClassNotFoundException {
                if (name.equals(GeneratedClass.class.getName())) {
                    try {
                        String fileName = GeneratedClass.class.getName().replace('.', '/') + ".class";
                        InputStream in = GeneratedClass.class.getClassLoader().getResourceAsStream(fileName);
                        if (in == null) {
                            throw new ClassNotFoundException(name + " " + fileName + " " + loader + " " + GeneratedClass.class.getClassLoader());
                        }

                        try {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            byte[] buf = new byte[4096];
                            for (int read; (read = in.read(buf)) != -1;) {
                                baos.write(buf, 0, read);
                            }
                            return baos.toByteArray();
                        } finally {
                            in.close();
                        }
                    } catch (IOException e) {
                        throw new ClassNotFoundException(name, e);
                    }
                }
                return null;
            }
        };
        @SuppressWarnings("unchecked")
        final ServiceReference<ClassGenerator>[] genRef = new ServiceReference[1];

        ClassLoadingServiceImpl service = TestUtil.getClassLoadingService(new URLClassLoader(new URL[0], null), new ComponentContextExpectationProvider() {
            @Override
            public void addExpectations(Mockery mockery, ComponentContext cc) {
                ComponentContextMockery ccMockery = new ComponentContextMockery(mockery);
                genRef[0] = ccMockery.mockService(cc, ClassLoadingServiceImpl.REFERENCE_GENERATORS, gen);
            }
        });

        AppClassLoader loader = service.createTopLevelClassLoader(Collections.<Container> emptyList(),
                                                                  service.createGatewayConfiguration().setDelegateToSystem(false),
                                                                  service.createClassLoaderConfiguration().setId(service.createIdentity("UnitTest", "0")));

        // ClassGenerator has not been added, so this load should fail.
        try {
            loader.loadClass(GeneratedClass.class.getName());
            Assert.fail("expected ClassNotFoundException");
        } catch (ClassNotFoundException e) {
            // expected.
        }

        service.addGenerator(genRef[0]);

        // ClassGenerator has been added, so this load should succeed.
        Assert.assertSame(loader, loader.loadClass(GeneratedClass.class.getName()).getClassLoader());

        service.removeGenerator(genRef[0]);

        // The class should have been defined by the loader, so this load should
        // still succeed.
        Assert.assertSame(loader, loader.loadClass(GeneratedClass.class.getName()).getClassLoader());
    }
}
