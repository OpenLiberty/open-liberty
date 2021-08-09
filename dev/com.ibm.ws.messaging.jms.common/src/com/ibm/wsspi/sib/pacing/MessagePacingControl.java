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

package com.ibm.wsspi.sib.pacing;

/**
 * @author wallisgd
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public interface MessagePacingControl extends MessagePacing {

  // This interface wraps the MessagePacing interface (implemented by the 
  // external message pacer (e.g. XD) in a control interface that includes
  // the additional registration, callback and state checking methods below 
  
  public void registerMessagePacer(MessagePacing messagePacer) throws IllegalStateException;

  public void resumeAsynchDispatcher(Object dispatcherContext);

  boolean isActive();

}
