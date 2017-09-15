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

import com.ibm.websphere.sib.SIRCConstants;
import com.ibm.websphere.sib.exception.SIMessageException;

/**
 * Exception to be thrown when the object in a message payload can not be serialized.
 */
public class ObjectFailedToSerializeException extends SIMessageException {

  private static final long serialVersionUID = 1L;

  private final static int REASON = SIRCConstants.SIRC0200_OBJECT_FAILED_TO_SERIALIZE;
  private String[] inserts;

  public ObjectFailedToSerializeException() {
    super();
  }

  /**
   * Constructor for when the Exception is to be thrown because another
   * Exception has been caught.
   *
   * @param cause The original Throwable which has caused this to be thrown.
   * @param className The name of the class which can not be serialized,
   */
  public ObjectFailedToSerializeException(Throwable cause, String className) {
    super(cause);
    inserts = new String[1];
    inserts[0] = className;
  }

  /**
   * @see com.ibm.ws.sib.utils.Reasonable#getExceptionReason()
   * @return a reason code that can be used if this exception causes a message
   *         to be rerouted to the exception destination
   */
  public int getExceptionReason() {
    return REASON;
  }

  /**
   * @see com.ibm.ws.sib.utils.Reasonable#getExceptionInserts()
   * @return a set of inserts (that can be inserted into the message corresponding exception reason) if
   *         this exception causes a message to be rerouted to the exception destination
   */
  public String[] getExceptionInserts() {
    return inserts;
  }

}
