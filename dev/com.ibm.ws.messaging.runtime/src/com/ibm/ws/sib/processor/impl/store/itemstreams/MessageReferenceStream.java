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
package com.ibm.ws.sib.processor.impl.store.itemstreams;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * @author tevans
 */
public abstract class MessageReferenceStream extends SIMPReferenceStream
{
  /**
   * Trace.
   */
  private static TraceComponent tc =
    SibTr.register(
      MessageReferenceStream.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
   
   
  /**
   * Warm start constructor invoked by the Message Store.
   */
  public MessageReferenceStream()
  {
    super();

    // This space intentionally blank
  }

  /**
   * Method registerListeners.
   * <p>Register any message event listeners</p>
   * @param msg
   */
  public abstract void registerListeners(SIMPMessage msg);

}
