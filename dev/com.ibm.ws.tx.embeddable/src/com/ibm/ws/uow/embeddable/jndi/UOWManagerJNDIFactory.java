/*******************************************************************************
 * Copyright (c) 1997, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.uow.embeddable.jndi;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.tx.TranConstants;
import com.ibm.wsspi.uow.UOWManagerFactory;

public class UOWManagerJNDIFactory implements ObjectFactory
{
    private static final TraceComponent tc = Tr.register(UOWManagerJNDIFactory.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    public Object getObjectInstance(Object referenceObject, Name name, Context context, Hashtable env) throws Exception
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) Tr.entry(tc, "getObjectInstance", new Object[]{referenceObject, name, context, env});

        final Object o = UOWManagerFactory.getUOWManager();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) Tr.exit(tc, "getObjectInstance", o);
        return o;
    }
}