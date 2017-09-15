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

public interface WebJsMessageEncoder {
  /**
   * Encode a JsJmsMessage into the simple text format supported by the
   * Web client.  Only JMS messages can be encoded this way.
   *
   * @return A String containing the simple message encoding
   * @exception MessageEncodeFailedException if the message could not be encoded
   */
  public String encodeForWebClient() throws MessageEncodeFailedException;
}
