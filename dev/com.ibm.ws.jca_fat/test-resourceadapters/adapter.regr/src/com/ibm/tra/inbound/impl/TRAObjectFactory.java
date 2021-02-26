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

package com.ibm.tra.inbound.impl;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

public class TRAObjectFactory implements ObjectFactory {

    @Override
    @SuppressWarnings("unchecked")
    public Object getObjectInstance(Object arg0, Name arg1, Context arg2,
                                    Hashtable arg3) throws Exception {

        Object ret = null;

        java.io.PrintStream out = com.ibm.tra.trace.DebugTracer.getPrintStream();

        out.println("TRAObjectFactory.getObjectInstance() called: ");
        out.println("Object class name: " + ((arg0 == null) ? "null" : arg0.getClass().getName()));
        out.println("Object toString(): " + ((arg0 == null) ? "null" : arg0.toString()));
        out.println("Name:" + ((arg1 == null) ? "null" : arg1.toString()));
        out.println("Context: " + ((arg2 == null) ? "null" : arg2.toString()));
        out.println("Hashtable: " + ((arg3 == null) ? "null" : arg3.toString()));

        if (arg0 instanceof Reference) {
            Class cl = Class.forName(((Reference) arg0).getClassName());
            ret = cl.newInstance();
        } else {
            //...
        }
        return ret;
    }

}
