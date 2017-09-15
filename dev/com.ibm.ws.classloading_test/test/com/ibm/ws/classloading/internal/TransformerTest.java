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
package com.ibm.ws.classloading.internal;

import static com.ibm.ws.classloading.internal.TestUtil.createAppClassloader;
import static com.ibm.ws.classloading.internal.TestUtil.getTestJarURL;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.security.CodeSource;

import org.junit.Rule;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.ws.classloading.internal.ClassLoadingServiceImpl.ClassFileTransformerAdapter;
import com.ibm.wsspi.classloading.ClassTransformer;

/**
 * Test to make sure that transformers can be correctly added to/removed from an AppClassLoader
 */
public class TransformerTest {
    @Rule
    public SharedOutputManager outputManager = SharedOutputManager.getInstance();

    @Test
    public void testTransformerRegistration() throws Exception {
        ClassTransformer ct1 = new ClassTransformer() {
            @Override
            public byte[] transformClass(String name, byte[] bytes, CodeSource source, ClassLoader loader) {
                return bytes;
            }
        };
        ClassFileTransformerAdapter transformer1 = new ClassFileTransformerAdapter(ct1);
        AppClassLoader loader = createAppClassloader(this.getClass().getName() + ".jar-loader", getTestJarURL(), true);
        assertFalse("Should not be able to remove a transformer before it was even registered", loader.removeTransformer(transformer1));
        assertTrue("Should be able to add new transformer adapter", loader.addTransformer(transformer1));
        assertTrue("Should be able to remove newly added transformer adapter", loader.removeTransformer(transformer1));
        assertFalse("Should not be able to remove newly added transformer adapter twice", loader.removeTransformer(transformer1));
    }
}
