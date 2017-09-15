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
package com.ibm.ws.diagnostics.java;

import java.io.PrintWriter;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.ibm.ws.diagnostics.AbstractMBeanIntrospector;
import com.ibm.wsspi.logging.Introspector;

public class OperatingSystemMBeanIntrospector extends AbstractMBeanIntrospector implements Introspector {
    @Override
    public String getIntrospectorName() {
        return "OperatingSystemInfo";
    }

    @Override
    public String getIntrospectorDescription() {
        return "Data about the operating system from the OperatingSystem MXBean";
    }

    /**
     * Capture the JVM's knowledge about the operating system. This
     * implementation will introspect the {@code java.lang:type=OperatingSystem} platform
     * MBean for data. Introspection is used so we can capture VM specific
     * extensions that are not present on the SDK interface.
     * 
     * @param out the output stream to write the data to
     * @throws MalformedObjectNameException
     */
    @Override
    public void introspect(PrintWriter out) throws MalformedObjectNameException {
        introspect(new ObjectName("java.lang:type=OperatingSystem"), null, out);
    }
}
