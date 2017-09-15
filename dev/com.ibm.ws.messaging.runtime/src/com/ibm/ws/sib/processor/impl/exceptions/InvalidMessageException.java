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
package com.ibm.ws.sib.processor.impl.exceptions;

/**
 * @author gatfora
 *
 * Used in the SourceStream class (writeAckPrefix) to indicate that
 * an unexpected message was received.
 */
public class InvalidMessageException extends Exception
{
  /** The serial version UID, for version to version compatability */
  private static final long serialVersionUID = -2845187083546468027L;

}
