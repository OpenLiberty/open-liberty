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

import java.util.Set;

/**
 * An JMFEncapsulation may implement any message model (it is anticipated that the
 * principal ones will be some combination of JROM, CMI, and WDO).  It has methods for
 * transcribing to/from JMF format and also for encapsulating as a byte array within a JMF
 * message.
 */

public interface JMFEncapsulation extends JMFPart {

  /**
   * Transcribe this JMFEncapsulation to a JMFNativePart for inclusion in a JMF format
   * message.
   *
   * @param target the JMFNativePart (provided in the empty state) into which the
   * transcribing is to occur.  It is assumed to have been created with the appropriate
   * schema.  The target should be updated to contain the same information as the
   * JMFEncapsulation, which is not changed by the operation.
   * @param deep true if any dependent parts of the message part matching
   * JMFDynamicType nodes in the schema should also be transcribed to JMF.
   * If false, then any such dependent parts of the message part should be represented as
   * JMFEncapsulations.  The deep flag is intended to apply recursively: if
   * true, the entire message part should be JMF encoded.
   */
  public void transcribe(JMFNativePart target, boolean deep)
    throws JMFSchemaViolationException,
           JMFModelNotImplementedException,
           JMFUninitializedAccessException,
           JMFMessageCorruptionException;

  /**
   * Gets a JMFNativePart ("indexed value") view of this JMFEncapsulation.  This is
   * different than transcribe in that it does not produce a new message with a JMF serial
   * format.  Rather, the JMFNativePart returned by this method may be implemented as part
   * of the JMFEncapsulation itself and may be as lazy as desired in translating the
   * JMFEncapsulation to JMFNativePart terms.  The value returned by this method need not
   * be capable of encoding itself directly into the JMF serial format.
   *
   * @return the requested JMFNativePart providing an indexed value view of the
   * JMFEncapsulation
   */
  public JMFNativePart getNativePart();

  /**
   * Get the length that this JMFEncapsulation needs in order to encapsulate itself.
   * Called by JMF preparatory to calling the encapsulate method
   *
   * @param msg the JMFMessage in which this JMFPart appears.  The JMFEncapsulation
   * may use non-destructive JMFI calls to extract any needed meta-data from this message.
   * The JMFEncapsulation should not mutate the message.
   * @return the length information requested
   * @exception JMFMessageCorruptionException if the message's encapsulated length cannot
   * be determined due to message corruption
   */
  public int getEncapsulatedLength(JMFMessageData msg)
    throws JMFMessageCorruptionException;

  /**
   * Store additional schemata, if any, associated with this JMFEncapsulation's contained
   * dynamic fields (if any).  Used to properly enumerate the schemata for a message that
   * is about to be encapsulated.
   *
   * @param accum the Set in which the schemata are being accumulated.  The
   * JMFEncapsulation should store its dependent JMFSchema objects, if any using
   * accum.add().
   */
  public void getSchemata(Set accum);

  /**
   * Encapsulate this JMFEncapsulation in a JMF format message
   *
   * @param frame the byte array into which encapsulation should occur
   * @param offset the offset within frame where encapsulation should occur
   * @param length the number of bytes available at offset.  This number will normally be
   * at least the number of bytes just returned by getEncapsulatedLength.
   * @param msg the JMFMessage in which this JMFPart appears.  The JMFEncapsulation
   * may use non-destructive JMFI calls to extract any needed meta-data from this message.
   * The JMFEncapsulation should not mutate the message.
   * @return the actual number of bytes used in the encapsulation
   * @exception JMFMessageCorruptionException if the message cannot be encapsulated due to
   * message corruption
   */
  public int encapsulate(byte[] frame, int offset, int length, JMFMessageData msg)
    throws JMFMessageCorruptionException;

  /**
   * Set the JMFMessageData segment within which this JMFEncapsulation is contained as a
   * JMFPart.  This method is called immediately after a new JMFEncapsulation is returned
   * by a JMFEncapsulationManager that is deserializing or transforming a JMFPart to
   * JMFEncapsulation form or when an JMFEncapsulation is set as a value in a
   * JMFNativePart.  The value should be remembered by the JMFEncapsulation, which should
   * call its unassemble() method when a mutation occurs that requires reserialization.
   *
   * @param msg the JMFMessageData segment within which the present JMFEncapsulation lives
   */
  public void setContainingMessageData(JMFMessageData msg);

  /**
   * Return the JMFMessageData segment within which this JMFEncapsulation is contained as
   * a JMFPart.  This method should simply return the value last provided by the corres-
   * ponding setContainingMessageData() method, or null if that method has not been called.
   *
   * @return the JMFMEssageData segment previously provided, or null if not set.
   */
  public JMFMessageData getContainingMessageData();

  /**
   * Returns a copy of this encapsulation of a JMFPart.  The copy is logically a complete
   * and independant copy of the original message; any changes made to the original will
   * not be visible in the copy and vice-versa.  For efficiency reasons (both time and
   * memory usage) any actual copying of data items within the message may be done lazily,
   * as and when (or if) updates are made.
   *
   * @return a copy of this encapsulation.
   */
  public JMFEncapsulation copy();
}
