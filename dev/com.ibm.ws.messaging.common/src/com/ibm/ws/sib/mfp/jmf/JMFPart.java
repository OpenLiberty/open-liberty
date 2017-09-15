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
 * A JMFPart is either an entire JMF format message, a JMF-format message part within a
 * message, or a non-JMF format encapsulated message part within a message.
 *
 * <p>The type of encoding employed by a JMFPart (JMF or something else) is characterized
 * by its <em>model id</em>.  Because model ids are assigned statically, are few in
 * number, and are highly stable, the complete set of model id constants is supplied as
 * part of this interface.
 */

public interface JMFPart {

  /**
   * The Model ID for JMF.  No JMFEncapsulationManager registers for this ID and no
   * JMFEncapsulationManager is needed for it.
   */
  public static final int MODEL_ID_JMF = 0;

  /**
   * The Model ID for SDO
   */
  public static final int MODEL_ID_SDO = 1;

  /**
   * Get the access JMFSchema associated with this JMFPart.  This is the JMFSchema that
   * accurately describes the logical structure of this JMFPart as viewed by
   * applications.
   *
   * <p>For JMF format messages, this is the same as the encoding JMFSchema (the JMFSchema
   * used to construct the underlying physical message) unless a schema compatibility
   * layer has been interposed.
   *
   * <p>For non-JMF-format message parts, this is JMFSchema that should be used to create
   * a JMFNativePart to pass to the transcribe() method and is the same JMFSchema as would
   * be returned by getJMFNativePart().getJMFSchema() but should be provided (if possible)
   * without the overhead of creating an explicit JMFI view of the message part.
   *
   * @return the access JMFSchema associated with this JMFPart
   */
  public JMFSchema getJMFSchema();

  /**
   * Get the message model ID implemented by this JMFPart.  A JMFNativePart always
   * returns MODEL_ID_JMF.
   *
   * @return the requested message model ID
   */
  public int getModelID();
}
