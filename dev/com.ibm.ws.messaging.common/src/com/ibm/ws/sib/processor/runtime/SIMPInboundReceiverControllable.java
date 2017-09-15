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
package com.ibm.ws.sib.processor.runtime;

import com.ibm.ws.sib.processor.exceptions.SIMPControllableNotFoundException;
import com.ibm.ws.sib.processor.exceptions.SIMPRuntimeOperationFailedException;

/**
 *
 */
public interface SIMPInboundReceiverControllable extends SIMPDeliveryStreamSetReceiverControllable
{
  /**
   * If the target has been restored from a backup and does not want to risk 
   *  reprocessing a retransmitted message. There is a possibility that the request 
   *  will not reach the source quickly but we will not make any new requests on this 
   *  stream set. Invoking this causes the source to 
   *  execute clearMessagesAtSource(IindoubtAction).
   *  This is performed on all streams in the stream set.
   *  The request to do this is hardened and will complete after a restart if
   *  necessary.
   */
  public void requestFlushAtSource(boolean indoubtDiscard)
    throws SIMPRuntimeOperationFailedException, SIMPControllableNotFoundException;
}
