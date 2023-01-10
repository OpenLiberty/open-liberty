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
package com.ibm.ws.sib.api.jmsra.impl;
//Sanjay Liberty Changes
//import javax.resource.ResourceException;
import javax.resource.ResourceException;


public class JmsJcaCloneConnectionException extends ResourceException {

  public JmsJcaCloneConnectionException (String s) {
    super (s);
  }
  
  public JmsJcaCloneConnectionException (Throwable th) {
    super (th);
  }
  
  public JmsJcaCloneConnectionException (String s, Throwable th) {
    super (s, th);
  }
  
  public JmsJcaCloneConnectionException (String s1, String s2) {
    super (s1, s2);
  }
  
}
