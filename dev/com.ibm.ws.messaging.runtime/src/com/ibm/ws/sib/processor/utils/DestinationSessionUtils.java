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
package com.ibm.ws.sib.processor.utils;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.mfp.JsDestinationAddress;
import com.ibm.ws.sib.mfp.JsDestinationAddressFactory;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * @author jroots
 *
 * Provides common utility methods used by the DestinationSession 
 * implementations, i.e. BrowserSessionImpl, ConsumerSessionImpl 
 * and ProducerSessionImpl.
 */
public class DestinationSessionUtils 
{
  private static TraceComponent tc =
    SibTr.register(
      DestinationSessionUtils.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  private static JsDestinationAddressFactory addrFactory = null;
   
  public static JsDestinationAddress createJsDestinationAddress(
    DestinationHandler dest)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "createJsDestinationAddress",
        dest);
    if(addrFactory == null)
      addrFactory = (JsDestinationAddressFactory) MessageProcessor.getSingletonInstance(SIMPConstants.JS_DESTINATION_ADDRESS_FACTORY);
    JsDestinationAddress destAddr = addrFactory.createJsDestinationAddress(
      dest.getName(), true, dest.getMessageProcessor().getMessagingEngineUuid());
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "createJsDestinationAddress", destAddr);
    return destAddr;
  }

}
