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

package com.ibm.ws.sib.mfp.jmf.impl;

import com.ibm.ws.sib.mfp.jmf.JMFMessage;
import com.ibm.ws.sib.mfp.jmf.JMFMessageCorruptionException;

/** 
 * Each JSchemaInterpreter implementation provides the encoding/decoding logic for all
 * possible JSchemas, using a particular version of the JMF native encoding rules.  Given
 * a JSchema, the SchemaInterpreter returns a JMFMessage that does the encoding/decoding
 */

public interface JSchemaInterpreter {

  /** 
   * Decode an existing message that is provided in byte array form by producing a
   * JMFMessage for it.
   *
   * @param schema the JSchema describing the message
   * @param contents the byte array containing the message contents
   * @param offset the starting offset of the message contents within 'contents.'  This
   * excludes any framing material that is not the business of the schema interpreter.
   * @param length the length of the message contents within 'contents' that is being
   * provided.
   * @exception JMFMessageCorruptionException if the message contents byte array is found
   * to be corrupt. 
   */
  JMFMessage decode(JSchema schema, byte[] contents, int offset, int length)
    throws JMFMessageCorruptionException;

  /**
   * Check that all schemas are available to decode the given message
   * contents.
   * 
   * @param contents the byte array containing the message contents
   * @param offset the starting offset of the message contents within 'contents.'
   * @return an array of schema ids for any unknown schemas
   * @throws JMFMessageCorruptionException if the message contents byte array is found
   * to be corrupt.
   */
  long[] checkSchemata(byte[] contents, int offset)
    throws JMFMessageCorruptionException;

  /** 
   * Create a new Message to be initialized from scratch in the local process
   *
   * @param schema the JSchema to use for the new message
   */
  JMFMessage newMessage(JSchema schema);

  /** 
   * Re-encode a JMFMessage that was perhaps encoded by a different SchemaInterpreter.
   * This method will only re-encode to an older format, never a newer one.
   *
   * @param currMessage the JMFMessage that is to be re-encoded.
   * @return a JMFMessage containing the information that was in currMessage.  CurrMessage
   * itself is returned iff its SchemaInterpreter's ID is less than or equal to that of
   * the present SchemaInterpreter.  Otherwise, a handle is returned that uses the
   * encoding of the present SchemaInterpreter; this may or may not be the old handle,
   * depending on whether this reformatting can be accomplished "in place" or not.
   */
  JMFMessage reEncode(JMFMessage currMessage);
}
