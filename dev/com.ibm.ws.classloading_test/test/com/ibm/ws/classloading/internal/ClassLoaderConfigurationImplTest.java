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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jmock.Mockery;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.classloading.ClassLoaderConfiguration;

public class ClassLoaderConfigurationImplTest {
    @Rule
    public final SharedOutputManager outputManager = SharedOutputManager.getInstance();

    private final Mockery mockery = new Mockery();

    @Test
    public void testNativeLibraryContainers() {
        ClassLoaderConfiguration clc = new ClassLoaderConfigurationImpl();
        Assert.assertEquals(Collections.emptyList(), clc.getNativeLibraryContainers());
        clc.setNativeLibraryContainers((Container[]) null);
        Assert.assertEquals(Collections.emptyList(), clc.getNativeLibraryContainers());
        clc.setNativeLibraryContainers((List<Container>) null);
        Assert.assertEquals(Collections.emptyList(), clc.getNativeLibraryContainers());

        Container container = mockery.mock(Container.class);

        clc = new ClassLoaderConfigurationImpl();
        clc.setNativeLibraryContainers(container);
        Assert.assertEquals(Arrays.asList(container), clc.getNativeLibraryContainers());
        clc.setNativeLibraryContainers(Arrays.asList(container));
        Assert.assertEquals(Arrays.asList(container), clc.getNativeLibraryContainers());
    }
}
