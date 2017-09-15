/*******************************************************************************
 * Copyright (c) 2003, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.recoverylog.spi;

//------------------------------------------------------------------------------
// Interface: LogCursorCallback
//------------------------------------------------------------------------------
/**
* <p>
* This interface defines callback methods that can be driven on an object when
* key events occur in the lifecycle of a LogCursor. 
* </p>
*
* <p>
* When a LogCursor is created, a callback object (implementing this interface)
* may be supplied on the constructor. The LogCursor object will then invoke
* methods on the callback when various events occur.
* </p>
*
* <p>
* At present, only the remove event will result in a callback.
* </p>
*/                                                                          
public interface LogCursorCallback
{
  //------------------------------------------------------------------------------
  // Interface: LogCursorCallback.removing
  //------------------------------------------------------------------------------
  /**
  * The associated LogCursors 'remove' method has been invoked. The remove operation
  * is about to be performed on the 'target' object.
  *
  * @param target The object that is being removed.
  *
  * @exception InternalLogException Thrown if an unexpected error has occured.
  */                                                          
  public void removing(Object target) throws InternalLogException;
}

