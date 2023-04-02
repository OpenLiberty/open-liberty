/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.processor.impl.exceptions;

/**
 * Invalid stream Exception is called from the PersistentStore class.
 */
public final class InvalidStreamException extends Exception
{
  /** The serial version UID, for version to version compatability */
  private static final long serialVersionUID = 617046342844616334L;

  /**
   * InvalidStreamException is thrown when selecting an item 
   * stream from the persistent store that is not valid.
   */
  public InvalidStreamException()
  {
    super();
  }

  /**
   * InvalidStreamException is thrown when selecting an item 
   * stream from the persistent store that is not valid.
   *
   * @param arg0  Exception text
   */
  public InvalidStreamException(String arg0)
  {
    super(arg0);
  }

}
