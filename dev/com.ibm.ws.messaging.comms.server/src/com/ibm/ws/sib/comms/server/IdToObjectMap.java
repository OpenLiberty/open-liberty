/*******************************************************************************
 * Copyright (c) 2003, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.server;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * A id (integer) to object map.  Not synchronized for multi-threaded use.
 */
public class IdToObjectMap
{
   /** Trace */
   private static final TraceComponent tc = SibTr.register(IdToObjectMap.class,
                                                           CommsConstants.MSG_GROUP,
                                                           CommsConstants.MSG_BUNDLE);

   /** Our NLS reference object */
   private static final TraceNLS nls = TraceNLS.getTraceNLS(CommsConstants.MSG_BUNDLE);   // D256974

   /** The map used to provide the underlying mapping functionality */
   private HashMap map = new HashMap();

   /**
    * A "mutable integer" class that eliminates the requirement to generate lots of
    * throw away key objects when getting objects from and testing membership of the map.
    */
   private class IntegerKey
   {
      private int integer;

      public IntegerKey(int integer)
      {
         this.integer = integer;
      }

      public boolean equals(Object obj)
      {
         boolean returnValue = false;
         if (obj instanceof IntegerKey)
            returnValue = ((IntegerKey)obj).integer == integer;
         return returnValue;
      }

      public int hashCode()
      {
         return integer;
      }

      public void setValue(int value)
      {
         integer = value;
      }
   }

   /**
    * A "captive" instance of the key class.  Used for operations which only make
    * temporary use of a key.
    */
   private IntegerKey captiveComparitorKey = new IntegerKey(0);

   /**
    * Puts a key (integer), object pair into the map.
    * @param key
    * @param value
    * @throws SIErrorException if the key already exists in the map
    */
   public void put(int key, Object value) throws SIErrorException                         // D214655
   {
      if (tc.isEntryEnabled()) SibTr.entry(tc, "put");

      if (tc.isDebugEnabled()) SibTr.debug(tc, "key: "+key +"value: "+value);                   // f174317

      // Start D214655
      IntegerKey intKey = new IntegerKey(key);
      Object oldValue = map.put(intKey, value);
      if (oldValue != null)
      {
         // Something already exists in the map by this key. This is an error in all cases,
         // so first restore the old value and then throw an exception
         map.put(intKey, oldValue);

         throw new SIErrorException(
            nls.getFormattedMessage("KEY_IN_USE_SICO2058", new Object[] {""+key}, null)   // D256974
         );
      }
      // End D214655

      if (tc.isEntryEnabled()) SibTr.exit(tc, "put");
   }

   /**
    * Retrives the object associated with a particular key from the map.
    * @param key
    * @return Object
    * @throws SIErrorException if the element did not exist
    */
   public Object get(int key) throws SIErrorException                                     // D214655
   {
      if (tc.isEntryEnabled()) SibTr.entry(tc, "get");

      if (tc.isDebugEnabled()) SibTr.debug(tc, "key: "+key);                                    // f174137
      captiveComparitorKey.setValue(key);

      if (tc.isDebugEnabled()) SibTr.debug(tc, "captiveComparitorKey: "+captiveComparitorKey);  // f174317

      // Start D214655
      Object retObject = map.get(captiveComparitorKey);

      if (retObject == null)
      {
         // If no object existed this is always an error too

         throw new SIErrorException(
            nls.getFormattedMessage("NO_SUCH_KEY_SICO2059", new Object[] {""+key}, null)  // D256974
         );
      }

      if (tc.isEntryEnabled()) SibTr.exit(tc, "get");
      return retObject;
      // End D214655
   }

   /**
    * Removes an object from the map.
    * @param key
    * @return Object
    * @throws SIErrorException if the element did not exist
    */
   public Object remove(int key) throws SIErrorException                                  // D214655
   {
      if (tc.isEntryEnabled()) SibTr.entry(tc, "remove", ""+key);

      captiveComparitorKey.setValue(key);

      // Start D214655
      Object retObject = map.remove(captiveComparitorKey);

      if (retObject == null)
      {
         // If no object existed this is always an error too

         throw new SIErrorException(
            nls.getFormattedMessage("NO_SUCH_KEY_SICO2059", new Object[] {""+key}, null)  // D256974
         );
      }

      if (tc.isEntryEnabled()) SibTr.exit(tc, "remove");
      return retObject;
      // End D214655
   }

   // Start of f174317
   /**
    * Returns an iterator with which to browse the values
    *
    * @return Iterator
    */
   public Iterator iterator()
   {
      if (tc.isEntryEnabled()) SibTr.entry(tc, "iterator");
      if (tc.isEntryEnabled()) SibTr.exit(tc, "iterator");

      return map.values().iterator();
   }
   // End of f174317

   public String toString()
   {
      Collection values = (this.map).values();
      Iterator i = values.iterator();
      StringBuffer retValue = new StringBuffer("");

      while(i.hasNext()) {
         retValue.append(i.next().toString()+"\n");
      }

      return retValue.toString();
   }

   // begin D297060
   /**
    * Determines if the specified id is present in the IdToObjectMap
    * @param id the id to check
    * @return boolean true if (and only if) the id is present in the
    * map.
    */
   public boolean containsKey(int id)
   {
      if (tc.isEntryEnabled()) SibTr.entry(tc, "get", ""+id);

      captiveComparitorKey.setValue(id);
      final boolean result = map.containsKey(captiveComparitorKey);

      if (tc.isEntryEnabled()) SibTr.exit(tc, "get", ""+result);
      return result;
   }
   // end D297060
}
