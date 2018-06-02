/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.managedobject.internal;

import org.jmock.Mockery;
import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.managedobject.ManagedObject;
import com.ibm.ws.managedobject.ManagedObjectException;
import com.ibm.ws.managedobject.ManagedObjectFactory;

public class ManagedObjectTest {
    private final Mockery mockery = new Mockery();

    public void testCreateManagedObjectFactory() throws Exception {
        new ManagedObjectServiceImpl().createManagedObjectFactory(null, Object.class, false);
    }

    @Test(expected = ManagedObjectException.class)
    public void testCreateManagedObjectFactoryNoDefaultConstructor() throws Exception {
        ManagedObjectFactory<NoDefaultConstructor> mof = new ManagedObjectServiceImpl().createManagedObjectFactory(null, NoDefaultConstructor.class, false);
        mof.createManagedObject();
    }

    public static class NoDefaultConstructor {
        public NoDefaultConstructor(boolean b) {}
    }

    @Test
    public void testManagedObjectFactoryImpl() throws Exception {
        ManagedObjectFactory<Object> factory = new ManagedObjectFactoryImpl<Object>(Object.class);
        Assert.assertFalse(factory.isManaged());
        Assert.assertEquals(Object.class, factory.getManagedObjectClass());
    }

    @Test
    public void testManagedObjectImpl() {
        Object object = new Object();
        ManagedObject<Object> mo = new ManagedObjectImpl<Object>(object);
        Assert.assertSame(object, mo.getObject());
        Assert.assertNull(mo.getContext());
        Assert.assertNull(mo.getContextData(Object.class));
        mo.release();
    }
}
