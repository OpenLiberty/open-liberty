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

package com.ibm.ws.sib.processor;

import java.io.Serializable;

/**
 * @author rjnorris
 * CommandHandlers get invoked only by the non-tx flavour of invokeCommand.
 */
public interface CommandHandler 
{
  
  /**
   *   
   * @param commandName  The command to be invoked  
   * @param commandData  The data to pass on command invocation
   *
   * @return the return value of the invoked command. This may be a serialized exception.
   */
  public Serializable invoke( String commandName, Serializable commandData); 

}

