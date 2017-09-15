/*******************************************************************************
 * Copyright (c) 2002, 2003 IBM Corporation and others.
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
// interface: LogCursor
//------------------------------------------------------------------------------
/**
* The LogCursor class provides iterator like functions for a number of
* different objects in the recovery log service. For example, a LogCursor is
* returned by the ServiceLog.recoverableUnits method and can be used to access
* each current RecoverableUnit in sequence. The standard hasNext() and next()
* methods are used to access the elements in the LogCursor. Callers must cast
* the objects returned by next() into the appropriate type.
*/                                                                          
public interface LogCursor
{
  //------------------------------------------------------------------------------
  // Method: LogCursor.next
  //------------------------------------------------------------------------------
  /**
  * <p>
  * Returns the next object in the sequence of objects or null if there are no
  * further objects to return. Callers must cast the returned object into the
  * appropriate type. This type will vary depending  on which method in the 
  * recovery log service that returned the LogCursor. See these methods for 
  * further details.
  * </p>
  *
  * <p>
  * Any ordering guarantees are imposed by the methods that return LogCursor
  * objects. The LogCursor itself does not specify any particular order
  * for the objects it returns. Each object will be returned at most once
  * from a given LogCursor.
  * </p>
  *
  * @return Object The next object in the sequence of objects or null if there
  *                are no further objects to return.
  */
  public Object next();

  //------------------------------------------------------------------------------
  // Method: LogCursor.hasNext
  //------------------------------------------------------------------------------
  /**
  * Returns a boolean flag to indicate if this LogCursor has further objects to
  * return.
  *
  * @return boolean Flag indicating if this LogCursor has further objects to
  *                 return.
  */
  public boolean hasNext();

  //------------------------------------------------------------------------------
  // Method: LogCursor.close
  //------------------------------------------------------------------------------
  /**
  * <p>
  * Closes the LogCursor when it is no longer required. This is required to allow
  * any 'locking' rules imposed by the providers of the LogCursor (ie those methods
  * that return LogCursors) to be followed.
  * </p>
  *
  * <p>
  * This method should be invoked once for each instance of LogCursor. After this
  * call has been made, the LogCursor is considered to be invalid and must not be
  * accessed again.
  * </p>
  */
  public void close();

  //------------------------------------------------------------------------------
  // Method: LogCursor.initialSize
  //------------------------------------------------------------------------------
  /**
  * Returns the total number of elements that the LogCursor will return as
  * calculated immediately after construction of the LogCursor. In other words, this
  * value will not decrease after successive calls to next()
  *
  * @return int The initial size of the LogCursor.
  */
  public int initialSize();

  //------------------------------------------------------------------------------
  // Method: LogCursor.remove
  //------------------------------------------------------------------------------
  /**
  * Removes the object previously extracted on the last call to next() from the
  * underlying structure. For example a RecoverableUnit my be removed from the
  * list of active RecoverableUnits by first extracting it on a call to next()
  * and then calling remove(). This operation would be the logical equivalent
  * of calling ServiceLog.removeRecoverableUnit except that this method may be
  * invoked whilst processing the LogCursor.
  *
  * @exception NotSupported This LogCursor instance does not support the
  *            remove operation.
  */
  public void remove() throws NotSupportedException,InternalLogException;

  //------------------------------------------------------------------------------
  // Method: LogCursor.last
  //------------------------------------------------------------------------------
  /**
  * <p>
  * Directly returns the last object in the LogCursor or null if there are no further
  * objects to return. This operation is the logical equivalent of repeatedly
  * invoking next() until there are no further objects remaining and then returning
  * the last object obtained.
  * </p>
  *
  * <p>
  * Once this method has been called, no further objects will be returned from the
  * LogCursor.
  * </p>
  * 
  * @return Object The last object in the LogCursor.
  */
  public Object last();
}

