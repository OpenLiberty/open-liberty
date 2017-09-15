/*******************************************************************************
 * Copyright (c) 1997, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.recoverylog.spi;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Collection;

import com.ibm.tx.util.logging.FFDCFilter;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;

//------------------------------------------------------------------------------
// Class: LogCursorImpl
//------------------------------------------------------------------------------
/**
* The LogCursorImpl class provides iterator like functions for a number of
* different objects in the recovery log service. For example, a LogCursorImpl is
* returned by the ServiceLog.recoverableUnits method and can be used to access
* each current RecoverableUnit in sequence. The standard hasNext() and next()
* methods are used to access the elements in the LogCursorImpl. Callers must cast
* the objects returned by next() into the appropriate type.
*/                                                                          
public class LogCursorImpl implements LogCursor
{
  /**
  * WebSphere RAS TraceComponent registration.
  */
  private static final TraceComponent tc = Tr.register(LogCursorImpl.class,
                                           TraceConstants.TRACE_GROUP, null);

  /**
  * Lock to control access to the data structures that a LogCursorImpl instance
  * is cycling through. Use of this lock is optional.
  */
  private Lock _controlLock;

  /**
  * The total number of elements that a LogCursorImpl instance will return.
  */
  private int _initialSize;

  /**
  * Boolean flag to indicate if a LogCursorImpl instance has no elements to
  * return.
  */
  private boolean  _empty = true;
                                    
  /**
  * A single LogCursorImpl instance can cycle through up to two internal 
  * iterators in series. This is the first of those iterators.
  */
  private Iterator _iterator1;

  /**
  * A single LogCursorImpl instance can cycle through up to two internal 
  * iterators in series. This is the second of those iterators.
  */
  private Iterator _iterator2;

  /**
  * The iterator (if any) from which the last element was returned by
  * the LogCursorImpl.next() method. The LogCursorImpl.remove method will
  * invoke the remove() method on this iterator.
  */
  private Iterator _removeIterator;

  /**
  * The object (if any) that was last returned on the last call
  * to the next() method. It is this object that will be removed
  * from the underlying interators if the remove method is invoked.
  */
  private Object _removeObject;

  /**
  * If the LogCursorImpl instance was constructed from a single object
  * reference (ie will return only that object) then rather than
  * use iterators we simply store the reference to the object.
  */
  private Object _singleObject;

  /**
  * Boolean flag to indicate if the LogCursorImpl.remove operation
  * is supported.
  */
  private boolean  _removeSupported;

  /**
  * Callback object provided by the creator of a LogCursorImpl to be
  * invoked when a remove operation occurs. This callback allows
  * the owner of the LogCursorImpl to perform extra processing
  * in response to a remove operation. Will be null if resume
  * is not supported and can be null if resume is supported but
  * no additional processing is required by the owner.
  */
  private LogCursorCallback _callback;

  /**
  * Constants to identify specific lock get/set pairs. These values must be
  * unique within the RLS code. Rather than use any form of allocation method
  * for these values components use statically defined constants to save
  * CPU cycles. All instances throughout the RLS source should be prefixed
  * with LOCK_REQUEST_ID to assist location and help ensure that no repeats
  * are introduced by mistake. This comment should also be included whereever
  * these values are defined.
  */
  private static final int LOCK_REQUEST_ID_LCI = 12;

  //------------------------------------------------------------------------------
  // Method: LogCursorImpl.LogCursorImpl
  //------------------------------------------------------------------------------
  /**
  * Constructor to build a LogCursorImpl that will iterate over a single object.
  * LogCursorImpl objects built with this constructor do not support the remove
  * method and will throw a NotSupportedException if its is called.
  *
  * @param controlLock Optional control lock to coordinate access to the data
  * @param object The object the LogCursorImpl should return on the first call
  *               to next()
  */
  public LogCursorImpl(Lock controlLock,Object object)
  {
    if (tc.isEntryEnabled()) Tr.entry(tc, "LogCursorImpl", new Object[] {controlLock, object});

    _controlLock = controlLock;
    _singleObject = object;
    _removeSupported = false;
    _callback = null;

    if (object!=null)
    {
      _initialSize = 1;
      _empty = false;
    }
    else
    {
      _initialSize = 0;
      _empty = true;
    }

    if (_controlLock != null)
    {
      _controlLock.getSharedLock(LOCK_REQUEST_ID_LCI);
    }

    if (tc.isEntryEnabled()) Tr.exit(tc, "LogCursorImpl", new Object[]{new Integer(_initialSize),this});
  }

  //------------------------------------------------------------------------------
  // Method: LogCursorImpl.LogCursorImpl
  //------------------------------------------------------------------------------
  /**
  * Constructor to build a LogCursorImpl that will iterate over a collection of
  * objects.
  *
  * @param controlLock Optional control lock to coordinate access to the data
  * @param collection The collection of objects the LogCursorImpl should return on
  *                   successive calls to next()
  * @param removeSupported Boolean flag to indicate if the LogCursorImpl instance
  *                        should allow the remove() method to be invoked. If
  *                        this flag is set to true the underlying collection
  *                        must be able to support element removal.
  * @param callback        Callback object to be invoked when a remove operation
  *                        occurs (may be null if no extra logic required in
  *                        this case)
  */
  public LogCursorImpl(Lock controlLock,Collection collection,boolean removeSupported,LogCursorCallback callback)
  {
      this(controlLock, collection, null, removeSupported, callback);
      
      if (tc.isEntryEnabled()) Tr.entry(tc, "LogCursorImpl", new Object[] {controlLock, collection, new Boolean(removeSupported), callback});
      if (tc.isEntryEnabled()) Tr.exit(tc, "LogCursorImpl", new Object[]{new Integer(_initialSize),this});
  }

  //------------------------------------------------------------------------------
  // Method: LogCursorImpl.LogCursorImpl
  //------------------------------------------------------------------------------
  /**
  * <p>
  * Constructor to build a LogCursorImpl that will iterate over two collections of
  * objects. Initially, the next() method will return objects from the first
  * collection until all objects in the collection have been returned. The
  * next() method will then return objects from the second collection until
  * there are no further object to return.
  * </p>
  *
  * <p>
  * When a LogCursorImpl object is no longer required, the user must invoke the
  * LogCursorImpl.close() method. This is required to allow any 'locking' rules
  * imposed by the providers of the LogCursorImpl (ie those methods that return
  * LogCursors) to be followed.
  * </p>
  *
  * @param controlLock Optional control lock to coordinate access to the data
  * @param collection1 The first collection of objects the LogCursorImpl should return on
  *                    successive calls to next()
  * @param collection2 The second collection of objects the LogCursorImpl should return on
  *                    successive calls to next()
  * @param removeSupported Boolean flag to indicate if the LogCursorImpl instance
  *                        should allow the remove() method to be invoked. If
  *                        this flag is set to true the underlying collections
  *                        must be able to support element removal.
  */
  public LogCursorImpl(Lock controlLock,Collection collection1,Collection collection2,boolean removeSupported,LogCursorCallback callback)
  {
    if (tc.isEntryEnabled()) Tr.entry(tc, "LogCursorImpl", new Object[] {controlLock, collection1, collection2, new Boolean(removeSupported), callback});
    
    int collection1Size = 0;
    int collection2Size = 0;

    if (collection1 != null)
    {
      collection1Size = collection1.size();
      
      if (tc.isDebugEnabled()) Tr.debug(tc, "Collection 1 size: " + collection1Size);
      
      _iterator1 = collection1.iterator();
      _empty = false;
    }

    if (collection2 != null)
    {
      collection2Size = collection2.size();
      
      if (tc.isDebugEnabled()) Tr.debug(tc, "Collection 2 size: " + collection2Size);
      
      _iterator2 = collection2.iterator();
      _empty = false;
    }
    
    _singleObject = null;    

    _controlLock = controlLock;
    _initialSize = collection1Size + collection2Size;    

    _removeSupported = removeSupported;

    // The callback will only be used if remove is a supported operation
    // so only cache the reference in this case.
    if (_removeSupported)
    {
      _callback = callback;
    }
    else
    {
      _callback = null;
    }

    if (_controlLock != null)
    {
      _controlLock.getSharedLock(LOCK_REQUEST_ID_LCI);
    }

    if (tc.isEntryEnabled()) Tr.exit(tc, "LogCursorImpl", new Object[]{new Integer(_initialSize),this});
  }

  //------------------------------------------------------------------------------
  // Method: LogCursorImpl.next
  //------------------------------------------------------------------------------
  /**
  * <p>
  * Returns the next object in the sequence of objects or null if there are no
  * further objects to return. Callers must cast the returned object into the
  * appropriate type. This type will vary depending  on which method in the 
  * recovery log service that returned the LogCursorImpl. See these methods for 
  * further details.
  * </p>
  *
  * <p>
  * Any ordering guarantees are imposed by the methods that return LogCursorImpl
  * objects. The LogCursorImpl itself does not specify any particular order
  * for the objects it returns. Each object will be returned at most once
  * from a given LogCursorImpl.
  * </p>
  *
  * @return Object The next object in the sequence of objects or null if there
  *                are no further objects to return.
  */
  public Object next()
  {
    if (tc.isEntryEnabled()) Tr.entry(tc, "next",this);

    Object nextObject = null;
    _removeIterator = null;

    if (!_empty)
    {
      if (_singleObject != null)
      {
        nextObject = _singleObject;
        _singleObject = null;
        _empty = true;
      }
      else
      {
        try
        {
          if ((_iterator1 != null) && (_iterator1.hasNext()))
          {
            nextObject = _iterator1.next();
            _removeIterator = _iterator1;
          }
          else if ((_iterator2 != null) && (_iterator2.hasNext()))
          {
            nextObject = _iterator2.next();
            _removeIterator = _iterator2;
          }
          else
          {
            _empty = true;
          }
        }
        catch(NoSuchElementException exc)
        {
          FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogCursorImpl.next", "328", this);
          _empty = true;
        }
      }
    }

    // Cache a reference to this object so that the remove method
    // can determine which object will be erased
    _removeObject = nextObject;
    
    final Object preprocessedResult = preprocessResult(nextObject);

    if (tc.isEntryEnabled()) Tr.exit(tc, "next", preprocessedResult);
    return preprocessedResult;
  }

  //------------------------------------------------------------------------------
  // Method: LogCursorImpl.hasNext
  //------------------------------------------------------------------------------
  /**
  * Returns a boolean flag to indicate if this LogCursorImpl has further objects to
  * return.
  *
  * @return boolean Flag indicating if this LogCursorImpl has further objects to
  *                 return.
  */
  public boolean hasNext()
  {
    if (tc.isEntryEnabled()) Tr.entry(tc, "hasNext",this);

    boolean hasNext = false;

    if ( (!_empty) && ( (_singleObject != null)                          ||
                        ((_iterator1 != null) && (_iterator1.hasNext())) ||
                        ((_iterator2 != null) && (_iterator2.hasNext()))  ) )
    {
      hasNext = true;
    }

    if (tc.isEntryEnabled()) Tr.exit(tc, "hasNext",new Boolean(hasNext));
    return hasNext;
  }

  //------------------------------------------------------------------------------
  // Method: LogCursorImpl.close
  //------------------------------------------------------------------------------
  /**
  * <p>
  * Closes the LogCursorImpl when it is no longer required. This is required to allow
  * any 'locking' rules imposed by the providers of the LogCursorImpl (ie those methods
  * that return LogCursors) to be followed.
  * </p>
  *
  * <p>
  * This method should be invoked once for each instance of LogCursorImpl. After this
  * call has been made, the LogCursorImpl is considered to be invalid and must not be
  * accessed again.
  * </p>
  */
  public void close()
  {
    if (tc.isEntryEnabled()) Tr.entry(tc, "close",this);

    if (_controlLock != null)
    {
      try
      {
        _controlLock.releaseSharedLock(LOCK_REQUEST_ID_LCI);
      }
      catch (NoSharedLockException exc)
      {
        // This should not happen as we get a shared lock in the constructor and this is the
        // only place we release it. Ignore this exception.
        FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogCursorImpl.close", "401", this);
      }
    }

    _iterator1 = null;
    _iterator2 = null;
    _removeIterator = null;
    _controlLock = null;
    _singleObject = null;
    _empty = true;

    if (tc.isEntryEnabled()) Tr.exit(tc, "close");
  }

  //------------------------------------------------------------------------------
  // Method: LogCursorImpl.initialSize
  //------------------------------------------------------------------------------
  /**
  * Returns the total number of elements that the LogCursorImpl will return as
  * calculated immediately after construction of the LogCursorImpl. In other words, this
  * value will not decrease after successive calls to next()
  *
  * @return int The initial size of the LogCursorImpl.
  */
  public int initialSize()
  {
    if (tc.isEntryEnabled()) Tr.entry(tc, "initialSize",this);
    if (tc.isEntryEnabled()) Tr.exit(tc, "initialSize",new Integer(_initialSize));
    return _initialSize;
  }

  //------------------------------------------------------------------------------
  // Method: LogCursorImpl.remove
  //------------------------------------------------------------------------------
  /**
  * Removes the object previously extracted on the last call to next() from the
  * underlying structure. For example a RecoverableUnit my be removed from the
  * list of active RecoverableUnits by first extracting it on a call to next()
  * and then calling remove(). This operation would be the logical equivalent
  * of calling ServiceLog.removeRecoverableUnit except that this method may be
  * invoked whilst processing the LogCursorImpl.
  *
  * @exception NotSupportedException This LogCursorImpl instance does not support the
  *            remove operation.
  */
  public void remove() throws NotSupportedException,InternalLogException
  {
    if (tc.isEntryEnabled()) Tr.entry(tc, "remove",this);

    if (!_removeSupported)
    {
      if (tc.isEntryEnabled()) Tr.exit(tc, "remove", "NotSupportedException");
      throw new NotSupportedException(null);      
    }

    if (_removeIterator != null)
    {
      if (_callback != null)
      {
        // Remove the object from the underlying map (via the iterator)
        _removeIterator.remove();

        try
        {
          _callback.removing(_removeObject);
        }
        catch (Exception exc)
        {
          FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogCursorImpl.remove", "469", this);
          if (tc.isEventEnabled()) Tr.event(tc, "An unexpected error occured whilst attempting to remove an object from the cursor");
          if (tc.isEntryEnabled()) Tr.exit(tc, "remove", "InternalLogException");
          throw new InternalLogException(exc);
        }
      }

      // Forget about the removed object so that no attempt can be made to issue
      // a duplicate remove operation.
      _removeIterator = null;
      _removeObject = null;
    }

    if (tc.isEntryEnabled()) Tr.exit(tc, "remove");
  }

  //------------------------------------------------------------------------------
  // Method: LogCursorImpl.last
  //------------------------------------------------------------------------------
  /**
  * <p>
  * Directly returns the last object in the LogCursorImpl or null if there are no further
  * objects to return. This operation is the logical equivalent of repeatedly
  * invoking next() until there are no further objects remaining and then returning
  * the last object obtained.
  * </p>
  *
  * <p>
  * Once this method has been called, no further objects will be returned from the
  * LogCursorImpl.
  * </p>
  * 
  * @return Object The last object in the LogCursorImpl.
  */
  public Object last()
  {
    if (tc.isEntryEnabled()) Tr.entry(tc, "last",this);

    Object lastObject = null;

    while (hasNext())
    {
      lastObject = next();
    }

    if (tc.isEntryEnabled()) Tr.exit(tc, "last",lastObject);

    return preprocessResult(lastObject);
  }

  //------------------------------------------------------------------------------
  // Method: LogCursorImpl.preprocessResult
  //------------------------------------------------------------------------------
  /**
  * <p>
  * This method is used to process objects that will be retured on next() or last()
  * method calls. If the object is actually a DataItem (this class stores byte 
  * data added to the recovery log) then its replaced with the encapsulated data
  * before return to the user. If the object is not a DataItem it is returned directly
  * It is used thus:
  *
  * objectToReturn = preprocessResult(objectToReturn);
  * </p>
  *
  * @param result The target object
  *
  * @return Object The target object or the encapsulated object, whichever applies.
  */
  private Object preprocessResult(Object result)
  {
    if (tc.isEntryEnabled()) Tr.entry(tc, "preprocessResult", new Object[] {result, this});
      
    Object processedResult = null;
      
    if (result instanceof DataItem)
    {
      processedResult = ((DataItem)result).getData();
    }
    else
    {
      processedResult = result;
    }
    
    if (tc.isEntryEnabled()) Tr.exit(tc, "preprocessResult", processedResult);
    return processedResult;
  }
}

