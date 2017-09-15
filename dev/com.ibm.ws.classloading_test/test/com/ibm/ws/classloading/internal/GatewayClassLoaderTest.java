/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.internal;

import static com.ibm.ws.classloading.internal.TestUtil.getLoaderFor;
import static com.ibm.ws.classloading.internal.TestUtil.ClassSource.A;
import static com.ibm.ws.classloading.internal.TestUtil.ClassSource.B;

import org.junit.Rule;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.wsspi.classloading.GatewayConfiguration;

/** Test the gateway class loader delegates to parent, then to system */
public class GatewayClassLoaderTest {
    @Rule
    public final SharedOutputManager outputManager = SharedOutputManager.getInstance();

    private static final String OUR_CHOSEN_SYSTEM_CLASS = "javax.rmi.CORBA.Util";

    private ClassLoader createGatewayToParent(ClassLoader parentLoader, GatewayConfiguration config) throws Exception {
        return GatewayClassLoader.createGatewayClassLoader(null, config, parentLoader, new CompositeResourceProvider());
    }

    ClassLoader createGatewayToParent(ClassLoader parentLoader) throws Exception {
        GatewayConfigurationImpl config = new GatewayConfigurationImpl();
        return createGatewayToParent(parentLoader, config);
    }

    /** Check we can load a class from the parent class loader */
    @Test
    public void testLoadingClassesFromParent() throws Exception {
        createGatewayToParent(getLoaderFor(A))
                        .loadClass("test.OuterClass");
    }

    /** Check we can load a class from the system class loader */
    @Test(expected = NoSuchFieldException.class)
    public void testLoadingClassesFromSystemClassLoader() throws Exception {
        createGatewayToParent(getLoaderFor(A))
                        .loadClass(OUR_CHOSEN_SYSTEM_CLASS)
                        .getField("IMPOSTER"); // ensure it is not our fake version by expecting an exception here
    }

    @Test(expected = ClassNotFoundException.class)
    public void testNotLoadingClassesFromSystemClassLoader() throws Exception {
        GatewayConfigurationImpl config = new GatewayConfigurationImpl();
        config.setDelegateToSystem(false);
        TestUtilClassLoader parentLoader = getLoaderFor(A);
        parentLoader.doNotLoad(OUR_CHOSEN_SYSTEM_CLASS);
        createGatewayToParent(parentLoader, config)
                        .loadClass(OUR_CHOSEN_SYSTEM_CLASS);
    }

    /**
     * Check we can override a system class with a class from the parent classloader.
     * This is THE MAIN FUNCTIONAL TEST for the {@link GatewayClassLoader}.
     */
    @Test
    public void testOverridingClassesFromSystemClassLoader() throws Exception {
        createGatewayToParent(getLoaderFor(B))
                        .loadClass(OUR_CHOSEN_SYSTEM_CLASS)
                        .getField("IMPOSTER");
    }

    /**
     * Sometimes, the OSGi class loader will hide a system class.
     * Test that our gateway classloader handles this appropriately.
     */
    @Test(expected = NoSuchFieldException.class)
    public void testLoadingHiddenClassesDirectlyFromSystemClassLoader() throws Exception {
        createGatewayToParent(getLoaderFor(B).doNotLoad(OUR_CHOSEN_SYSTEM_CLASS))
                        .loadClass(OUR_CHOSEN_SYSTEM_CLASS)
                        .getField("IMPOSTER"); // ensure it is not our fake version by expecting an exception here
    }
}
