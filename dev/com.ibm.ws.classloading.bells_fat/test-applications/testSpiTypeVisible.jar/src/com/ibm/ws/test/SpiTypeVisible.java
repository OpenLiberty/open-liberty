/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.test;

import com.ibm.ws.classloading.exporting.test.TestInterface;

public class SpiTypeVisible implements TestInterface {

    @Override
    public String isThere(String name) {
        final String spiClassName = "com.ibm.wsspi.rest.handler.RESTHandler";
        Class spiClass = null;
        try {
            // Try current classloader
            ClassLoader libCl = this.getClass().getClassLoader();
            System.out.println("SpiTypeVisible.isThere: loading spi class " + spiClassName);
            spiClass = libCl.loadClass(spiClassName);
        } catch (Exception e) {
            //
        }

        return name + " is there, SPI class " + spiClassName + (spiClass == null ? " is not visible" : " is visible") + " to the BELL library classloader";
    }

}
