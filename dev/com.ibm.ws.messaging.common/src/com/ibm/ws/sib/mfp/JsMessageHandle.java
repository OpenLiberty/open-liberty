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
package com.ibm.ws.sib.mfp;

import java.io.Serializable;

import com.ibm.wsspi.sib.core.SIMessageHandle;
import com.ibm.ws.sib.utils.SIBUuid8;

/**
 * JsMessageHandle is the internal interface for Jetstream components to
 * access an SIMessageHandle.
 */
public interface JsMessageHandle extends SIMessageHandle, Serializable {

  /**
   *  Get the value of the SystemMessageSourceUuid field for the message
   *  represented by the JsMessageHandle.
   *
   *  @return A SIBUuid8 containing the source UUID of the message.
   */
  public SIBUuid8 getSystemMessageSourceUuid();

  /**
   *  Get the value of the SystemMessageValue field for the message
   *  represented by the JsMessageHandle.
   *
   *  @return A long containing the identifier value of the message.
   */
  public long getSystemMessageValue();

}
