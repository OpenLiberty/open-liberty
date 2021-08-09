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
package com.ibm.ws.sib.processor.test;


import com.ibm.ws.sib.admin.JsEObject;
import com.ibm.ws.sib.admin.JsEngineComponent;
import com.ibm.ws.sib.admin.JsHealthMonitor;
import com.ibm.ws.sib.admin.JsMessagingEngine;
import com.ibm.ws.sib.msgstore.MessageStore;
import com.ibm.ws.sib.trm.TrmMeMain;
import com.ibm.ws.util.ThreadPool;

/**
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 * 
 * @author matrober
 */
public interface SIMPJsStandaloneEngine 
  extends JsMessagingEngine, JsHealthMonitor
{
  public abstract JsEngineComponent getTRM();
  public abstract void setTRM(TrmMeMain trm);

  public abstract void initialize(
    JsMessagingEngine engine,
    boolean clean,
    boolean reintTRM)
    throws Exception;
  public abstract void start(int mode);
  public abstract void stop(int mode);
  public abstract void destroy();
  public abstract void setCustomProperty(String name, String value);
  public abstract void setConfig(JsEObject config);

  public abstract MessageStore createMessageStoreOnly(boolean clean) throws Exception;
  
  /**
   * This method carries out the necessary actions to initialise the MessageProcessor
   * associated with this engine. By including this method here we avoid the need
   * to expose the MessageProcessor class as an interface. 
   *
   */
  public abstract void initializeMessageProcessor();
  
  
  /** Return the thread pool for mediations */
  public abstract ThreadPool getMediationThreadPool();
  
  /** Set the mediation threadpool */
  public abstract void setMediationThreadPool(ThreadPool pool);

}
