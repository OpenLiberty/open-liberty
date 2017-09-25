/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.threading.internal;

import java.io.PrintWriter;

import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.threading.PolicyExecutorProvider;
import com.ibm.wsspi.logging.Introspector;
import com.ibm.wsspi.threading.WSExecutorService;

/**
 *
 */
@Component(immediate = true,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = { Constants.SERVICE_VENDOR + "=" + "IBM" })
public class ThreadingIntrospector implements Introspector {

    private ExecutorServiceImpl impl;

    @Reference
    protected void setWSExecutorService(WSExecutorService wses) {
        if (wses instanceof ExecutorServiceImpl) {
            impl = (ExecutorServiceImpl) wses;
        }
    }

    protected void unsetWSExecutorService(WSExecutorService wses) {
        impl = null;
    }

    @Reference
    private PolicyExecutorProvider provider;

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.logging.Introspector#getIntrospectorName()
     */
    @Override
    public String getIntrospectorName() {
        return "ThreadingIntrospector";
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.logging.Introspector#getIntrospectorDescription()
     */
    @Override
    public String getIntrospectorDescription() {
        return "Liberty threading diagnostics";
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.logging.Introspector#introspect(java.io.PrintWriter)
     */
    @Override
    public void introspect(PrintWriter out) throws Exception {
        if (impl == null) {
            out.println("No ExecutorServiceImpl configured");
        } else {
            ThreadPoolController tpc = impl.threadPoolController;
            tpc.introspect(out);
        }
        out.println();
        provider.introspectPolicyExecutors(out);
    }

}
