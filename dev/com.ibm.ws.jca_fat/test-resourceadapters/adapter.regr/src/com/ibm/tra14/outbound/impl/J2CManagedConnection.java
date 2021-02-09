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

package com.ibm.tra14.outbound.impl;

import com.ibm.tra14.outbound.base.ConnectionRequestInfoBase;
import com.ibm.tra14.outbound.base.ManagedConnectionBase;
import com.ibm.tra14.outbound.base.ManagedConnectionFactoryBase;
import com.ibm.tra14.trace.DebugTracer;

public class J2CManagedConnection extends ManagedConnectionBase {

    private static final String className = "J2CManagedConnection Ver 2";

    public J2CManagedConnection(ManagedConnectionFactoryBase mcf, ConnectionRequestInfoBase reqInfo) {
        super(mcf, reqInfo);
        DebugTracer.printClassLoaderInfo(className, this);
        DebugTracer.printStackDump(className, new Exception());
        /*
         * System.out.println("***!*** Printing debug information for J2CManagedConnection constructor ***!***");
         * System.out.println("* Debug Resource Adapter Version: 2 ");
         * System.out.println("* J2CManagedConnection extends ManagedConnectionImpl.java");
         * System.out.println("* Current ClassLoader: " + ManagedConnectionBase.class.getClassLoader().toString());
         * System.out.println("* Context ClassLoader: " + Thread.currentThread().getContextClassLoader().toString());
         * System.out.println("* Stack Dump: ");
         * Exception e = new Exception();
         * e.printStackTrace(System.out);
         * System.out.println("***!*** End debug information for J2CManagedConnection Constructor ***!***");
         */
    }
}