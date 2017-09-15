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
package com.ibm.ws.sib.utils;

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.List;

/* ************************************************************************** */
/**
 * A Thread Local implementation of a stack (and hence one that does not
 * need synchronization)
 * @param <E> The type of the elements in the stack
 */
/* ************************************************************************** */
public final class ThreadLocalStack<E>
{
  /** WSThreadLocal holding the list for the current thread */
  private ThreadLocal<List<E>> _elements
    = new ThreadLocal<List<E>>()
    {
      protected List<E> initialValue()
      {
        return new ArrayList<E>(); // For small arrays, ArrayList is better than LinkedList (less GC)
      }
    }; 
   
  /* -------------------------------------------------------------------------- */
  /* isEmpty method
  /* -------------------------------------------------------------------------- */
  /**
   * @return true if the stack for this thread is currently empty
   */
  public boolean isEmpty()
   {
     return _elements.get().isEmpty();
   }
   
  /* -------------------------------------------------------------------------- */
  /* peek method
  /* -------------------------------------------------------------------------- */
  /**
   * @return The top element on the stack for this thread
   * @throws EmptyStackException is thrown if the stack for this thread is empty
   */
  public E peek() throws EmptyStackException
  {
    if (isEmpty())
    {
      throw new EmptyStackException();
    }
    
    return _elements.get().get(_elements.get().size()-1);
  } 
   
  /* -------------------------------------------------------------------------- */
  /* push method
  /* -------------------------------------------------------------------------- */
  /**
   * Push a new element onto the top of the stack for this thread
   * 
   * @param item The new element to be added to the top of the stack
   * @return The new element
   */
  public E push(E item)
   {
     _elements.get().add(item);
     return item;
   }
   
  /* -------------------------------------------------------------------------- */
  /* pop method
  /* -------------------------------------------------------------------------- */
  /**
   * Pop and return the top element of the stack for this thread
   * 
   * @return The previously top element of the stack for this thread
   * @throws EmptyStackException is thrown if the stack for this thread is empty
   */
  public E pop() throws EmptyStackException
   {
     E answer = peek(); // Throws EmptyStackException if there's nothing there
     _elements.get().remove(_elements.get().size()-1);
     return answer;     
   }
}