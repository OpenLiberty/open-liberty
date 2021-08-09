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

import com.ibm.ws.sib.utils.DataSlice;

/**
 * A JMFMessage encapsulates a complete message.  In addition to the methods inherited
 * from JMFNativePart (which permit manipulation of the data in the message), it has
 * methods to turn the message into a byte array.
 */

public interface JMFMessage extends JMFNativePart {

  /**
   * Get the JMF encoding version that was used to encode this message
   *
   * @return the requested version id
   */
  short getJMFEncodingVersion();

  /**
   * Gets all the JMFSchemas needed to decode the message.  The list will include the
   * encoding schema for the message plus that for any embedded JMFDynamicTypes.  While
   * this list often includes the result of getJMFSchema(), this may not be the case if
   * the access schema and the encoding schema is different; getJMFSchema() will return
   * the access schema, which may not be strictly necessary for decoding.
   *
   * @return the requested array of JMFSchemas (which will always have at least one
   * element).
   */
  JMFSchema[] getSchemata()
    throws JMFUninitializedAccessException, JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException;

  /**
   * Says whether the message is still in the original "frame" in which it was provided
   * to this JMFMessage via the JMFRegistry.decode method.  If so, returns the current
   * length of the message starting at the original offset.  If not, returns -1 and the
   * getEncodedLength and toByteArray methods must be called to copy or re-encode the
   * message.  Note that a message can be in its original frame even if changed if those
   * changes could be performed in "write-through" fashion.
   */
  int originalFrame();

  /**
   * Gets the number of bytes it takes to encode this message.  Normally, this method is
   * not called if original frame is still in use as indicated by a positive return from
   * the originalFrame method.  However, this method must always return an accurate
   * answer.
   *
   * @exception JMFUninitializedAccessException if the message is not sufficiently
   * initialized to have a definite encoded length
   */
  int getEncodedLength()
    throws JMFUninitializedAccessException, JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException;

  /**
   * Write this message into a byte array.
   *
   * @param buffer the place where the byte encoding is to be deposited
   * @param offset the starting location in buffer where the byte encoding is to be
   *               deposited.
   * @param length the number of bytes available at offset.  This will always be the value
   *               returned by a previous call to getEncodedLength.
   * @return The number of bytes written into the array.
   * @exception JMFUninitializedAccessException if the message is not sufficiently
   *                                            initialized to be turned into a byte array.
   */
  int toByteArray(byte[] buffer, int offset, int length)
    throws JMFUninitializedAccessException, JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException;

  /**
   * Turn this message into a byte array.
   *
   * @param length the number of bytes expected to be in the array.  This will always be the value
   * returned by a previous call to getEncodedLength.
   * @return  a new byte array into which the message has been encoded.
   * @exception JMFUninitializedAccessException if the message is not sufficiently
   * initialized to be turned into a byte array.
   */
  byte[] toByteArray(int length)
    throws JMFUninitializedAccessException, JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException;

  /**
   * Returns a copy of this JMFMessage.  The copy is logically a complete and independant
   * copy of the original message; any changes made to the original will not be visible
   * in the copy and vice-versa.  For efficiency reasons (both time and memory usage) any
   * actual copying of data items within the message may be done lazily, as and when (or
   * if) updates are made.
   *
   * @return a copy of this message.
   */
  JMFMessage copy();

  /**
   *  Return the 'lock artefact' for the JMF message:
   *    For a top-level message, the value returned is the message instance itself.
   *    For a lower-level message (i.e. one representing a Dynamic field) the value
   *    returned is the 'master' message instance.
   *  The method is provided on the interface so that a caller can make state
   *  transition assumptions across calls.
   *  Added for d364050, specifically for use across  getEncodedLength() & toByteArray()
   *
   *  @return a reference to the instance against which this message should be locked
   */
  Object getMessageLockArtefact();

  /**
   * Return a DataSlice containing the assembled message content, or null if the
   * message is not assembled.
   * d348294
   *
   * @return The message content, if assembled, otherwise null.
   */
  public DataSlice getAssembledContent();

  /**
   * isEMPTYlist
   * Return true if the value of the given field is one of the singleton
   * EMPTY lists, otherwise false.
   *
   * @param accessor   The aceesor value of the field of interest.
   *
   * @return boolean Description of returned value
   */
  public boolean isEMPTYlist(int accessor);

}
