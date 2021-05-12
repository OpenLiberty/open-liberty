/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.tra.outbound.impl;

import javax.resource.spi.ConnectionManager;

import com.ibm.tra.outbound.base.ConnectionFactoryBase;
import com.ibm.tra.outbound.base.ManagedConnectionFactoryBase;
import com.ibm.tra.trace.DebugTracer;

public class J2CConnectionFactory extends ConnectionFactoryBase {

    private static final long serialVersionUID = -6672476191867883549L;
    private final String className = "J2CConnectionFactory Ver 2";

    public J2CConnectionFactory(ManagedConnectionFactoryBase cciMcf, ConnectionManager cm) {
        super(cciMcf, cm);
        DebugTracer.printClassLoaderInfo(className, this);
        DebugTracer.printStackDump(className, new Exception()); /*
                                                                 * System.out.println("***!*** Printing debug information for J2C ConnectionFactory constructor ***!***");
                                                                 * System.out.println("* Debug Resource Adapter Version: 2 ");
                                                                 * System.out.println("* Current ClassLoader: " + ConnectionFactoryBase.class.getClassLoader().toString());
                                                                 * System.out.println("* Context ClassLoader: " + Thread.currentThread().getContextClassLoader().toString());
                                                                 * System.out.println("* Stack Dump: ");
                                                                 * Exception e = new Exception();
                                                                 * e.printStackTrace(System.out);
                                                                 * System.out.println("***!*** End debug information for J2CConnectionFactory Constructor ***!***");
                                                                 */
    }
}