/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.async;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
import com.ibm.wsspi.webcontainer.servlet.ITransferContextService;

/**
 * Class that provides access to declaritive service type funtionality to store/restore/reset thread ontextual data
 *
 */
@SuppressWarnings("deprecation")
public class ServiceWrapper {

  protected static final Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.wsspi.webcontainer.async");
  private static final String CLASS_NAME="com.ibm.wsspi.webcontainer.async.ServiceWrapper";

  private HashMap<String,Object> contextData = null;
  ClassLoader originalCL = null;
  ClassLoader newCL = null;
  AsyncContextImpl asyncContext;
  
  private static final String ComponentMetaData = "com-ibm-ws-runtime-metadata-ComponentMetaData-V1";
  
  /*
   * newFromPushedData - creating new SW for next dispatch/Runnable.  
   *    Xferred context data and classloaders to new SW.
   */
  static ServiceWrapper newFromPushedData (AsyncContextImpl asynContext) { // PM90834: added method
      ServiceWrapper orginalServiceWrapper = asynContext.serviceWrapper;
      ServiceWrapper sw = new ServiceWrapper(asynContext);
      sw.setContextData(orginalServiceWrapper.getContextData());
      sw.originalCL = orginalServiceWrapper.originalCL;
      sw.newCL= orginalServiceWrapper.newCL;             //PI92334 newCL is the current thread's classLoader

      if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {           
          logger.logp(Level.FINEST, CLASS_NAME, "newFromPushedData","created ServiceWrapper [" + sw +"] , original ServiceWrapper ["+orginalServiceWrapper+"]");
          logger.logp(Level.FINEST, CLASS_NAME, "newFromPushedData","transferred to new ServiceWrapper, originalCL [" + sw.originalCL +"] , newCL ["+ sw.newCL +"]");
      }

      return sw;
  }
  
  public ServiceWrapper(AsyncContextImpl asyncContext) {
      if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST))           
          logger.logp(Level.FINEST, CLASS_NAME, "constructor","creating with async context -> "+ asyncContext);
      
      this.asyncContext = asyncContext;
  }

  void setContextData (HashMap<String, Object> contextData) { // PM90834: added method
      this.contextData = contextData;
  }
  
  HashMap<String, Object> getContextData () { // PM90834: added method
      return this.contextData;
  }

  /*
   * pushContextData - save all the current context data and ClassLoader into the current servlet's ServiceWrapper.  
   * These will be transferred to the next dispatch/Runnable servlet's ServiceWrapper
   */
  @SuppressWarnings("deprecation")
  public void pushContextData() {
    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINEST)) { 
      logger.entering(CLASS_NAME,"pushContextData",this);
    }

    // will want to run on the calling thread's class loader
    this.newCL = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
        @Override
        public ClassLoader run() {
            ClassLoader old = Thread.currentThread().getContextClassLoader();
            return old;
        }
    });

    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
        logger.logp(Level.FINEST, CLASS_NAME, "pushContextData", "thread context class loader [" + this.newCL +"]");
      }
    
    this.contextData = new HashMap();
    
    // push data for other components that we already have hooks into
    ComponentMetaDataAccessorImpl cmdai = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor();
    ComponentMetaData cmd = cmdai.getComponentMetaData();
    if (cmd != null) {
      this.contextData.put(ComponentMetaData, cmd);
    }  
    
    // each producer service of the Transfer service is accessed in order to get the thread context data
    Iterator<ITransferContextService> TransferIterator = com.ibm.ws.webcontainer.osgi.WebContainer.getITransferContextServices();
    if (TransferIterator != null) { 
      while(TransferIterator.hasNext()){
        ITransferContextService tcs = TransferIterator.next();
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
          logger.logp(Level.FINEST, CLASS_NAME, "pushContextData", "calling storeState on: " + tcs);
        }
        tcs.storeState(this.contextData);
        asyncContext.storeStateCtxData = this.contextData;
      }
    } else {
      if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
        logger.logp(Level.FINEST, CLASS_NAME, "pushContextData", "no implmenting services found");
      }
    }
    
    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINEST)) { 
      logger.exiting(CLASS_NAME,"pushContextData",this);
    }
  }

  
  @SuppressWarnings("deprecation")
  protected void popContextData() {

    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINEST)) { 
      logger.entering(CLASS_NAME,"popContextData",this);
    }

    // save off this thread's class loader and use the class loader of the thread that launch this thread.
    this.originalCL = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
        @Override
        public ClassLoader run() {
            ClassLoader old = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(newCL);
            return old;
        }
    });
    
    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
        logger.logp(Level.FINEST, CLASS_NAME, "popContextData", "old context class loader [" + this.originalCL + "] , new context class loader ["+ newCL + "]");
    }

    if (this.contextData != null) {

        // pop data for other components that we already have hooks into
        ComponentMetaDataAccessorImpl cmdai = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor();
        ComponentMetaData cmd = (ComponentMetaData) this.contextData.get(ComponentMetaData);
        if (cmd != null) {
            cmdai.beginContext(cmd);
        }    
        
        // each producer service of the Transfer service is accessed in order to get the thread context data
      Iterator<ITransferContextService> TransferIterator = com.ibm.ws.webcontainer.osgi.WebContainer.getITransferContextServices();
      if (TransferIterator != null) { 
        while(TransferIterator.hasNext()){
          ITransferContextService tcs = TransferIterator.next();
          if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
            logger.logp(Level.FINEST, CLASS_NAME, "popContextData", "calling restoreState on: " + tcs);
          }
          tcs.restoreState(this.contextData);
        }  
      }
    }
  
    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINEST)) { 
      logger.exiting(CLASS_NAME,"popContextData",this);
    }
  }

  @SuppressWarnings("deprecation")
  protected void resetContextData() {

    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINEST)) { 
        logger.entering(CLASS_NAME,"resetContextData",this);
    }

    Iterator<ITransferContextService> TransferIterator = com.ibm.ws.webcontainer.osgi.WebContainer.getITransferContextServices();
    if (TransferIterator != null) { 
      while(TransferIterator.hasNext()){
        ITransferContextService tcs = TransferIterator.next();
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
          logger.logp(Level.FINEST, CLASS_NAME, "resetContextData", "calling resetState on: " + tcs);
        }
        tcs.resetState();
      }  
    }

    // reset data for other components that we already have hooks into
    ComponentMetaDataAccessorImpl cmdai = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor();
    cmdai.endContext();

    // go back to the new thread's original class loader
    AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
        @Override
        public ClassLoader run() {
            Thread.currentThread().setContextClassLoader(originalCL);
            return originalCL;
        }
    });

    this.contextData = null;


    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINEST)) { 
        logger.exiting(CLASS_NAME,"resetContextData",this);
    }
  }
  
  /**
   * Execute a runnable within some context. Other objects could call popContextData
   * and resetContextData on their own, but this ensures that they are called in the right order.
   * 
   * @param runnable
   */
  void wrapAndRun (Runnable runnable) { // PM90834 added method
      if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINEST)) { 
          logger.entering(CLASS_NAME,"wrapAndRun",runnable);
      }

      try {
          this.popContextData();

          if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
              logger.logp(Level.FINEST, CLASS_NAME, "wrapAndRun", "run with context class loader: " + Thread.currentThread().getContextClassLoader());
          }

          runnable.run();
      } finally {
          try {
              this.resetContextData();
          }finally {
              if (runnable instanceof CompleteRunnable) {
                  asyncContext.notifyITransferContextCompleteState();
              }

              if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINEST)) { 
                  logger.exiting(CLASS_NAME,"wrapAndRun",runnable);
              }
          }
      }
  }

}
