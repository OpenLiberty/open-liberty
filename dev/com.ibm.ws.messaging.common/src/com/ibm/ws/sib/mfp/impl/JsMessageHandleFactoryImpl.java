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

import com.ibm.ws.sib.mfp.impl.JsMessageHandleFactory;
import com.ibm.ws.sib.mfp.JsMessageHandle;
import com.ibm.ws.sib.mfp.MfpConstants;

import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;

import com.ibm.websphere.ras.TraceComponent;

/**
 *  This class extends the abstract com.ibm.ws.sib.mfp.JsMessageHandleFactory
 *  class and provides the concrete implementations of the methods for
 *  creating SIMessageHandles and JsMessageHandles.
 *  <p>
 *  The class must be public so that the abstract class static
 *  initialization can create an instance of it at runtime.
 *
 */
public final class JsMessageHandleFactoryImpl extends JsMessageHandleFactory {

  private static TraceComponent tc = SibTr.register(JsMessageHandleFactoryImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

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
  public final JsMessageHandle createJsMessageHandle(SIBUuid8 uuid
                                                    ,long     value
                                                    )
                                                    throws NullPointerException {
    if (uuid == null)  {
      throw new NullPointerException("uuid");
    }
    return new JsMessageHandleImpl(uuid, Long.valueOf(value));

  }

}
