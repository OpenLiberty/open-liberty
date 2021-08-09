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
package com.ibm.ws.kernel.service.util;

import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.xml.stream.XMLInputFactory;

/**
 * This class provides a way of obtaining the system default XMLInputFactory.
 * The default {@link XMLInputFactory} newInstance or newFactory methods use the
 * thread context class loader to obtain the XMLInputFactory. This can lead to
 * the use of different XML parsers at different times. This class should be used
 * to obtain the XMLInputFactory as it is designated to return the system default.
 */
public class DesignatedXMLInputFactory {

    private static final PrivilegedAction<XMLInputFactory> XML_INPUT_FACTORY_ACTION = new PrivilegedAction<XMLInputFactory>() {
        @Override
        public XMLInputFactory run() {
            Thread currentThread = Thread.currentThread();
            ClassLoader loader = currentThread.getContextClassLoader();
            if (loader != null) {
                try {
                    currentThread.setContextClassLoader(null);
                    return XMLInputFactory.newInstance();
                } finally {
                    //put it back again
                    currentThread.setContextClassLoader(loader);
                }
            }
            else {
                return XMLInputFactory.newInstance();
            }
        }
    };

    /**
     * 
     * @return an instance of the system default {@link XMLInputFactory}
     */
    public static XMLInputFactory newInstance() {
        //to ensure consistency in the selection of XMLInputFactory we need to use the system default
        //this means removing any TCCL for the duration of obtaining the XMLInputFactory instance
        return AccessController.doPrivileged(XML_INPUT_FACTORY_ACTION);
    }
}
