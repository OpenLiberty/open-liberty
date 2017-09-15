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

public class JavaHeapMBeanIntrospector extends AbstractMBeanIntrospector implements Introspector {
    @Override
    public String getIntrospectorName() {
        return "JavaHeapInfo";
    }

    @Override
    public String getIntrospectorDescription() {
        return "Information about the heap from the Memory related MXBeans";
    }

    @Override
    public void introspect(PrintWriter out) throws MalformedObjectNameException {
        introspect(new ObjectName("java.lang:type=Memory"), null, out);
        introspect(new ObjectName("java.lang:type=MemoryManager,*"), null, out);
        introspect(new ObjectName("java.lang:type=MemoryPool,*"), null, out);
    }
}
