/*******************************************************************************
 * Copyright (c) 1997, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;

/**
 * This class implements a set whose membership is determined by
 * the value of its elements.
 * If the equals method of two objects indicate they are equal and both
 * are added to a ValueSet, only the first will actually be added.
 * The second is considered a duplicate. 
 * Like a Hashtable, a ValueSet has random access performance via hashing.  
 * A linear search only happens within a single collision chain.
 */
public class ValueSet extends HashSet {
   
   private static final long serialVersionUID = -4824866462347967426L;
    
   public ValueSet(int capacity) {
      super(capacity);
   }

   public ValueSet(Enumeration enumeration) {
      this(4);
      while (enumeration.hasMoreElements()) {
         add(enumeration.nextElement());
      }
   }

   public ValueSet(Iterator it) {
      this(4);
      while (it.hasNext()) {
         add(it.next());
      }

   }

   public Object getOne() {
      Iterator it = iterator();
      if (it.hasNext())
         return it.next();
      return null;
   }

   /**
    * Returns an enumeration of the values in this ValueSet.
    * Use the Enumeration methods on the returned object to fetch the elements
    * sequentially.
    * @return An enumeration of the values in this ValueSet.
    * @see     java.util.Enumeration
    * @note   Use iterator() instead!
    */
   public Enumeration elements() {
      return new ValueSetEnumeration();
   }

   class ValueSetEnumeration implements Enumeration {
      Iterator it;
      ValueSetEnumeration() {
         it = iterator();
      }
      public boolean hasMoreElements() {
         return it.hasNext();
      }
      public Object nextElement() {
         return it.next();
      }
   }

   /**
    * Adds all elements of set into this.
    * Values are not duplicated. 
    * @param set The ValueSet to be unioned into this one.
    **/
   public void union(ValueSet set) {
      addAll(set);
   }

   public void union(Enumeration enumeration) {
      while (enumeration.hasMoreElements()) {
         add(enumeration.nextElement());
      }
   }

   public void union(Iterator it) {
      while (it.hasNext()) {
         add(it.next());
      }
   }

   /**
    * Gets the current value in this ValueSet having the same 
    * value as the value parameter.  If no value was in this
    * ValueSet equal to the value parameter, return null.
    * This method does not replace the current value.
    * @param value The value to be added.
    * @return An Object already in the ValueSet equal to the value
    * parameter.
    */

   /**
    * Gets a current value in this ValueSet - the first one it can find. 
    * If no value was in this ValueSet, return null.
    * This method does not change the state of the ValueSet.
    * @return An Object in the queue.
    */

}
