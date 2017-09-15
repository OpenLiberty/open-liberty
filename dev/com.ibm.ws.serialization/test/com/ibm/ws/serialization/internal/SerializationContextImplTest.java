/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.serialization.internal;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.serialization.SerializationObjectReplacer;

public class SerializationContextImplTest {
    @Test
    public void testReplaceObject() {
        SerializationContextImpl context = new SerializationContextImpl(new SerializationServiceImpl());
        Assert.assertFalse(context.isReplaceObjectNeeded());
        Assert.assertSame(context, context.replaceObject(context));
    }

    @Test
    public void testReplaceObjectWithResolver() {
        SerializationContextImpl context = new SerializationContextImpl(new SerializationServiceImpl());
        context.addObjectReplacer(new SerializationObjectReplacer() {
            @Override
            public Object replaceObject(@Sensitive Object object) {
                if ((Integer) object < 2) {
                    return (Integer) object + 1;
                }
                return null;
            }
        });
        context.addObjectReplacer(new SerializationObjectReplacer() {
            @Override
            public Object replaceObject(@Sensitive Object object) {
                if ((Integer) object < 2) {
                    throw new IllegalStateException();
                }
                return null;
            }
        });
        Assert.assertTrue(context.isReplaceObjectNeeded());
        Assert.assertEquals(1, context.replaceObject(0));
        Assert.assertEquals(2, context.replaceObject(1));
        Assert.assertEquals(2, context.replaceObject(2));
    }
}
