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
package com.ibm.ws.webcontainer31.async;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Iterator;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.ws.webcontainer31.osgi.osgi.WebContainerConstants;
import com.ibm.wsspi.webcontainer.servlet.ITransferContextService;

/**
 * 
 */
public class ThreadContextManager {
    
    private final static TraceComponent tc = Tr.register(ThreadContextManager.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);

    private static final String ComponentMetaData = "com-ibm-ws-runtime-metadata-ComponentMetaData-V1";

    //ClassLoader variables for getting and setting the current Context Class Loader
    //originalCL is the Context Class Loader from when the user started the action
    //currentCL is the Context Class Loader for the thread we are going on to
    private ClassLoader originalCL, currentCL;
    private HashMap<String,Object> originalCD, currentCD = null;
    
    public ThreadContextManager(){
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "ThreadContextManager created, getting the initial data");
        }
        //Grab the context ClassLoader off the starting thread
        this.originalCL = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            public ClassLoader run() {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                return cl;
            }
        });
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "ThreadContextManager saving the original thread's context data");
        }
        this.originalCD = saveContextData();
    }
    
    //Save off the context data for the current thread
    public HashMap<String,Object> saveContextData(){
        HashMap<String,Object> contextData = new HashMap<String, Object>();      
        //Save off the data from the other components we have hooks into
        ComponentMetaDataAccessorImpl cmdai = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor();
        ComponentMetaData cmd = cmdai.getComponentMetaData();
        if (cmd != null) {
          contextData.put(ComponentMetaData, cmd);
        }  
        
        //Each producer service of the Transfer service is accessed in order to get the thread context data
        //The context data is then stored off
        Iterator<ITransferContextService> TransferIterator = com.ibm.ws.webcontainer.osgi.WebContainer.getITransferContextServices();
        if (TransferIterator != null) {
            while(TransferIterator.hasNext()){
                ITransferContextService tcs = TransferIterator.next();
                
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Calling storeState on: " + tcs);
                }
                tcs.storeState(contextData);
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "No implmenting services found");
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Saving the context data : " + contextData);
        }
        return contextData;
    }
    
    /*
     * Push the original context data from the original thread onto the current thread
     * Pushes the context ClassLoader, ComponentMetaData, and ThreadContext data
     * Saves off some of the current thread's data to restore it later
     */
    public void pushContextData(){
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "pushContextData");
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Setting the original thread's class loader and saving off the current one");
        }
        //Set the original Context Class Loader
        this.currentCL = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            public ClassLoader run() {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(originalCL);
                return cl;
            }
        });
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Saving the current thread's context data");
        }
        //We are going to save off the current threads component metadata and context
        //We will restore this when we pop later on
        this.currentCD = saveContextData();
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Setting the original thread's context data : " + this.originalCD);
        }
        if (this.originalCD != null) {

            //Push the data onto the thread that we have hooks into
            ComponentMetaDataAccessorImpl cmdai = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor();
            ComponentMetaData cmd = (ComponentMetaData) this.originalCD.get(ComponentMetaData);
            if (cmd != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Calling begin on the ComponentMetaData : " + cmdai + ", with : " + cmd);
                }
                cmdai.beginContext(cmd);
            }
            
            //Each producer service of the Transfer service is accessed in order to get the thread context data
          Iterator<ITransferContextService> TransferIterator = com.ibm.ws.webcontainer.osgi.WebContainer.getITransferContextServices();
          if (TransferIterator != null) {
            while(TransferIterator.hasNext()){
              ITransferContextService tcs = TransferIterator.next();
              if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                  Tr.debug(tc, "Calling restoreState on: " + tcs);
              }
              tcs.restoreState(this.originalCD);
            }
          }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "pushContextData");
        }
    }
    
    /*
     * Pop off the current context data and revert back to what was originally there
     * Revert back to the previous state of the thread from before the pustContextData was called 
     */
    public void popContextData(){
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "popContextData");
        }
        
        // If we pushed thread context onto the thread, be sure to reset it before
        // taking it off.
        if (this.originalCD != null) {
          //Each producer service of the Transfer service is accessed in order to get the thread context data
          Iterator<ITransferContextService> TransferIterator = com.ibm.ws.webcontainer.osgi.WebContainer.getITransferContextServices();
          if (TransferIterator != null) {
            while(TransferIterator.hasNext()){
              ITransferContextService tcs = TransferIterator.next();
              if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                  Tr.debug(tc, "Calling resetState on: " + tcs);
              }
              tcs.resetState();
            }
          }
        }
        
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Restoring the current thread's class loader");
        }
        if(currentCL != null){
            //Revert back to the thread's current class loader
            AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                public ClassLoader run() {
                    Thread.currentThread().setContextClassLoader(currentCL);
                    return currentCL;
                }
            });
        }
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Restoring the current thread's context data");
        }
        //When popping we are going to reset the thread context to clear everything off, then we are going to set it back to the state when we first pushed
        //our stuff onto the thread
        if(currentCD != null){
            Iterator<ITransferContextService> TransferIterator = com.ibm.ws.webcontainer.osgi.WebContainer.getITransferContextServices();
            if (TransferIterator != null) { 
                while(TransferIterator.hasNext()){
                    ITransferContextService tcs = TransferIterator.next();
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "calling restoreState on: " + tcs);
                    }
                    tcs.restoreState(currentCD);
                }  
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Ending the current ComponentMetaData context");
        }
        //Reset data for other components that we already have hooks into
        ComponentMetaDataAccessorImpl cmdai = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor();
        cmdai.endContext();
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "popContextData");
        }
    }
}
