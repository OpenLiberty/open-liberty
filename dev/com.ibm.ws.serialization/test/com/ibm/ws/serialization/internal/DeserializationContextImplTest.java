/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.serialization.internal;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.serialization.DeserializationObjectResolver;

public class DeserializationContextImplTest {
    @Test
    public void testResolveObject() throws Exception {
        DeserializationContextImpl context = new DeserializationContextImpl(new SerializationServiceImpl());
        Assert.assertFalse(context.isResolveObjectNeeded());
        Assert.assertSame(context, context.resolveObject(context));
    }

    @Test
    public void testResolveObjectWithResolver() throws Exception {
        DeserializationContextImpl context = new DeserializationContextImpl(new SerializationServiceImpl());
        context.addObjectResolver(new DeserializationObjectResolver() {
            @Override
            public Object resolveObject(@Sensitive Object object) {
                if ((Integer) object < 2) {
                    return (Integer) object + 1;
                }
                return null;
            }
        });
        context.addObjectResolver(new DeserializationObjectResolver() {
            @Override
            public Object resolveObject(@Sensitive Object object) throws IOException {
                if ((Integer) object < 2) {
                    throw new IllegalStateException();
                }
                if (((Integer) object).equals(3)) {
                    throw new IOException();
                }
                return null;
            }
        });
        Assert.assertTrue(context.isResolveObjectNeeded());
        Assert.assertEquals(1, context.resolveObject(0));
        Assert.assertEquals(2, context.resolveObject(1));
        Assert.assertEquals(2, context.resolveObject(2));
        try {
            context.resolveObject(3);
            Assert.fail("expected IOException");
        } catch (IOException e) {
        }
    }
}
