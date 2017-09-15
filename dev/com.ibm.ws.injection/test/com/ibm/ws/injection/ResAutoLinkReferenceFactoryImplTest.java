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
package com.ibm.ws.injection;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.injectionengine.osgi.internal.ResAutoLinkReferenceFactoryImpl;

public class ResAutoLinkReferenceFactoryImplTest {
    @Test
    public void testGetBindingName() {
        Assert.assertEquals("x", ResAutoLinkReferenceFactoryImpl.getBindingName("x"));
        Assert.assertEquals("x/y", ResAutoLinkReferenceFactoryImpl.getBindingName("x/y"));
        Assert.assertEquals("", ResAutoLinkReferenceFactoryImpl.getBindingName("java:"));
        Assert.assertEquals("x", ResAutoLinkReferenceFactoryImpl.getBindingName("java:x"));
        Assert.assertEquals("", ResAutoLinkReferenceFactoryImpl.getBindingName("java:x/"));
        Assert.assertEquals("y", ResAutoLinkReferenceFactoryImpl.getBindingName("java:x/y"));
        Assert.assertEquals("y/z", ResAutoLinkReferenceFactoryImpl.getBindingName("java:x/y/z"));
        Assert.assertEquals("env", ResAutoLinkReferenceFactoryImpl.getBindingName("java:x/env"));
        Assert.assertEquals("", ResAutoLinkReferenceFactoryImpl.getBindingName("java:x/env/"));
        Assert.assertEquals("y", ResAutoLinkReferenceFactoryImpl.getBindingName("java:x/env/y"));
        Assert.assertEquals("y/z", ResAutoLinkReferenceFactoryImpl.getBindingName("java:x/env/y/z"));
    }
}
