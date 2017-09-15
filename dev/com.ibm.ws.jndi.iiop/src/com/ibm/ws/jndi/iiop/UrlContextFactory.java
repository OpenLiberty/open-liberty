/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jndi.iiop;

import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.OperationNotSupportedException;
import javax.naming.spi.ObjectFactory;

import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.ws.transport.iiop.spi.ClientORBRef;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleComponent;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleContext;

public abstract class UrlContextFactory implements ObjectFactory, ApplicationRecycleComponent {

    static final TraceComponent tc = Tr.register(UrlContextFactory.class);
    
    private ClientORBRef orbRef;
    private final Set<String> appsToRecycle = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    
    @Reference
    protected void setIIOPClient(ClientORBRef orbRef) {
        this.orbRef = orbRef;
    }
    
    protected void unsetIIOPClient(ClientORBRef orbRef) {
        if(this.orbRef==orbRef) orbRef = null;
    }
    
    @Override
    public Object getObjectInstance(Object o, Name n, Context c, Hashtable<?, ?> env) throws Exception {
        final String methodName = "getObjectInstance(): ";
        // by OSGi JNDI spec Name and Context should be null
        // if they are not then this code is being called in
        // the wrong way
        if (n != null || c != null)
            return null;
        // Object is String, String[] or null
        // Hashtable contains any environment properties
        if (o == null) {
            if (tc.isDebugEnabled()) Tr.debug(tc, methodName + "object was null - returning new OrbContext.");
            registerCaller();
            return new OrbContext(orbRef.getORB(), env);
        }
        
        if (o instanceof String) {
            if (tc.isDebugEnabled()) Tr.debug(tc, methodName + "object was a string - performing a lookup on new OrbContext");
            registerCaller();
            return new OrbContext(orbRef.getORB(), env).lookup((String) o);
        }
        
        if (o instanceof String[]) {
            if (tc.isDebugEnabled()) Tr.debug(tc, methodName + "object was a string[] - ignoring");
        }

        throw new OperationNotSupportedException();
    }
    
    private void registerCaller() {
        ComponentMetaData cData = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        if (cData != null)
            appsToRecycle.add(cData.getJ2EEName().getApplication());
    }

    @Override
    public ApplicationRecycleContext getContext() {
        return null;
    }

    @Override
    public Set<String> getDependentApplications() {
        Set<String> members = new HashSet<String>(appsToRecycle);
        appsToRecycle.removeAll(members);
        return members;
    }

}
