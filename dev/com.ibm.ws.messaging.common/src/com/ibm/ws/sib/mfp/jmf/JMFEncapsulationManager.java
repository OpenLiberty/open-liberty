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

package com.ibm.ws.sib.mfp.jmf;

/**
 * A JMFEncapsulationManager is responsible for encapsulating and de-encapsulating the
 * messages of a particular Message Model in JMF messages, and for transcribing to/from
 * JMF format for a particular Message Model.
 */

public interface JMFEncapsulationManager {

  /**
   * Produces an JMFEncapsulation by de-encapsulating the contents of a Dynamic field
   * within a JMF message
   *
   * @param frame the byte array within which the encapsulated JMFEncapsulation's
   * serialized bytes will be found
   * @param offset the offset within the frame argument where the JMFEncapsulation's
   * serialized bytes begin.
   * @param length the length of the area within the frame containing the
   * @param msg the JMF Message in which this JMFPart appears.  The JMFEncapsulationManager
   *   may use non-destructive JMFI calls to extract any needed meta-data from this message.
   *   The JMFEncapsulationManager should not mutate the message.
   * @return the JMFEncapsulation resulting from de-encapsulating the supplied bytes, using
   * the particular view implemented by this JMFEncapsulationManager.
   * @exception JMFMessageCorruptionException if the message could not be de-encapsulated
   * due to message corruption
   */
  public JMFEncapsulation deencapsulate(byte[] frame, int offset, int length, JMFMessageData msg)
                                                                  throws JMFMessageCorruptionException;
}
