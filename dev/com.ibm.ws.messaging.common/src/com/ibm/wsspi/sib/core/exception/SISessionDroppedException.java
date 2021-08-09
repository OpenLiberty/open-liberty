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
 package com.ibm.wsspi.sib.core.exception;

/**
 This subclass of SISessionUnavailableException is used to indicate that the 
 DestinationSession has been pre-emptively closed by the bus, for example 
 because the destination to which it was attached has been deleted, or following 
 a change of the SendAllowed or ReceiveAllowed value. The exception message 
 should indicate exactly what caused the session to be closed. The application 
 may try to recreate the session in the hope that such a state change has been 
 reversed.
 <p>
 This class has no security implications.
 */
public class SISessionDroppedException extends SISessionUnavailableException {

  private static final long serialVersionUID = 3517552638648582598L;
  public SISessionDroppedException(String msg) {
    super(msg);
  }

  public SISessionDroppedException(String msg, Throwable t) {
    super(msg, t);
  }

}
