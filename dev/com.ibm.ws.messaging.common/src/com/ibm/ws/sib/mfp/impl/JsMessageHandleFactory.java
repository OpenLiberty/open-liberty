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

package com.ibm.ws.sib.mfp.impl;

import com.ibm.ws.sib.mfp.JsMessageHandle;
import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.*;

/**
 * A singleton JsMessageHandleFactory is created at static initialization
 * and is subsequently used for the creation of all JsMessageHandles.
 * This factory is for use by Jetstream components only.
 */
public abstract class JsMessageHandleFactory {

  private static TraceComponent tc = SibTr.register(JsMessageHandleFactory.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  private final static String JS_MESSAGE_HANDLE_FACTORY_CLASS     = "com.ibm.ws.sib.mfp.impl.JsMessageHandleFactoryImpl";

  private static JsMessageHandleFactory instance = null;
  private static NoClassDefFoundError createException = null;

  static {
  
    /* Create the singleton factory instance                                  */
    try {
      createFactoryInstance();
    }
    catch (NoClassDefFoundError e) {
      FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.JsMessageHandleFactory.<clinit>", "56");
      createException = e;
    }
  }

  /**
   *  Get the singleton JsMessageHandleFactory which is to be used for
   *  creating JsMessageHandle instances.
   *
   *  @return The JsMessageHandleFactory
   */
  public static JsMessageHandleFactory getInstance() {

    /* If instance creation failed, throw on the NoClassDefFoundError         */
    if (instance == null) {
      throw createException;
    }

    /* Otherwise, return the singleton                                        */
    return instance;
  }


  /**
   *  Create a new JsMessageHandle to represent an SIBusMessage.
   *
   *  @param destinationName  The name of the SIBus Destination
   *  @param localOnly        Indicates that the Destination should be localized
   *                          to the local Messaging Engine.
   *
   *  @return JsMessageHandle The new JsMessageHandle.
   *
   *  @exception NullPointerException Thrown if either parameter is null.
   */
  public abstract JsMessageHandle createJsMessageHandle(SIBUuid8 uuid
                                                       ,long     value
                                                       )
                                                       throws NullPointerException;


  /**
   *  Create the singleton Factory instance.
   */
  private static void createFactoryInstance() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createFactoryInstance");
    try {
      Class cls = Class.forName(JS_MESSAGE_HANDLE_FACTORY_CLASS);
      instance = (JsMessageHandleFactory) cls.newInstance();
    }
    catch (Exception e) {
      FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.JsMessageHandleFactory.createFactoryInstance", "133");
      SibTr.error(tc,"UNABLE_TO_CREATE_HANDLEFACTORY_CWSIF0031",e);
      NoClassDefFoundError ncdfe = new NoClassDefFoundError(e.getMessage());
      ncdfe.initCause(e);
      throw ncdfe;
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createFactoryInstance");
  }

}
