/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.test;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import com.ibm.ws.classloading.exporting.test.TestInterface;

/**
 * Class SpiVisible is used to verify class visibility from within a
 * BELL service.
 */
public class SpiVisible implements TestInterface {

    @Override
    public String isThere(String name) {

        final String IBMSPI_CLASS_NAME = "com.ibm.wsspi.rest.handler.RESTHandler";

        String className = getSysPropPrivileged("className", IBMSPI_CLASS_NAME);
        String loadOp = getSysPropPrivileged("loadOp", "loadClass");

        // attempt to load a class using the library classloader
        System.out.println("SpiVisible.isThere: loading class " + className + " using " + loadOp);
        Class<?> clazz = null;
        try {
            if ("loadClass".equals(loadOp)) {
                clazz = this.getClass().getClassLoader().loadClass(className);
            }
            else if ("forName".equals(loadOp)) {
                // If this class load succeeds, the JVM records the library class loader as the
                // "initiating classloader".  That means all subsequent loads of the class will
                // be found by this library classloader when it invokes findLoadedClass() during
                // find().
                clazz = Class.forName(className);
            }
            else {
                throw new IllegalArgumentException("Invalid loadOp: " + loadOp);
            }
        } catch (Throwable t) {
            t.printStackTrace(System.err);
        }

        return name + " is there, SPI class " + className + (clazz==null ? " is not" : " is") + " visible to the BELL library classloader";
    }

    String getSysPropPrivileged(String key, String defaultValue) throws IllegalArgumentException {
        try {
            return (String)AccessController.doPrivileged(new PrivilegedExceptionAction() {
                @Override
                public String run() throws IllegalArgumentException {
                    return System.getProperty(key, defaultValue);
                }
            });
        } catch (PrivilegedActionException e) {
            throw (IllegalArgumentException)e.getException();
        }
    }
}
