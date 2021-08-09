package com.ibm.ws.objectManager;

/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import com.ibm.ws.objectManager.utils.Trace;
import com.ibm.ws.objectManager.utils.Tracing;

/**
 * A synchronized HashMap with greater parallelism.
 */
class ConcurrentHashMap extends java.util.AbstractMap
{
    private static final Class cclass = ConcurrentHashMap.class;
    private static Trace trace = ObjectManager.traceFactory.getTrace(ConcurrentHashMap.class,
                                                                     ObjectManagerConstants.MSG_GROUP_MAPS);
    private static int numberOfProcessors = 1;
    static {
        // We cannot always invoke availableProcessors, because OSGImin does not support this.
        // We should rediscover the number of processors occasionally as it can change.
        try {
            java.lang.reflect.Method availableProcessorsMethod = Runtime.class.getMethod("availableProcessors",
                                                                                         new Class[] {});
            Integer integer = (Integer) availableProcessorsMethod.invoke(Runtime.getRuntime(),
                                                                         new Object[] {});
            numberOfProcessors = integer.intValue();
        } catch (Exception exception) {
            // No FFDC Code Needed.
            if (Tracing.isAnyTracingEnabled() && trace.isEventEnabled())
                trace.event(cclass
                            , "<init>"
                            , exception
                                );
        }
    } // static initialiser.

    private java.util.Map[] subMaps;

    public static final boolean gatherStatistics = false; // Built for statistics if true.
    // For gatherStatistics.
    private int[] subMapAccessFrequency;

    /**
     * Build a set of HashMaps, the level of concurrency is equal to the number of processors available.
     */
    protected ConcurrentHashMap()
    {
        this(numberOfProcessors);
    } // ConcurrentHashMap().

    /**
     * Build a set of HashMaps.
     * 
     * @param subMapCount the number of subMaps to be used.
     */
    public ConcurrentHashMap(int subMapCount)
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass,
                        "<init>",
                        new Object[] { new Integer(subMapCount) });

        subMaps = new java.util.Map[subMapCount];
        for (int i = 0; i < subMaps.length; i++) {
            subMaps[i] = makeSubMap();
        } // for subMaps.

        if (gatherStatistics)
            subMapAccessFrequency = new int[subMaps.length];

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass,
                       "<init>");
    } // ConcurrentHashMap().

    java.util.Map makeSubMap() {
        return new java.util.HashMap();
    }

    /*
     * Compute the subMap containing a key.
     * 
     * @param key for which the subMap to be found.
     * 
     * @return java.util.Map which could contain the key.
     */
    private final java.util.Map getSubMap(final Object key)
    {
        // Defend against systematically changing hashcodes, for examples where consecutive
        // Integers are presented as keys. If we dont shuffle the hashcode then we may get a
        // bunch of requests that hit the same submap.
        int index = key.hashCode();
        index += ~(index << 8);
        index ^= (index >>> 9);
        // Index must be positive.
        index &= 0X7FFFFFFF;
        index = index % subMaps.length;
        if (gatherStatistics)
            subMapAccessFrequency[index]++;
        return subMaps[index];
        // return subMaps[new java.util.Random(key.hashCode()).nextInt(subMaps.length)];
    } // getSubMap().

    // --------------------------------------------------------------------------
    // Extends AbstractMap.
    // --------------------------------------------------------------------------  

    /**
     * Retrieve an object from the map based on its key.
     * 
     * @param key for the Object to be removed.
     * @return Object matching the key.
     */
    public final Object get(Object key)
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass,
                        "get",
                        "key=" + key + "Object");

        java.util.Map subMap = getSubMap(key);
        Object returnValue;
        synchronized (subMap) {
            returnValue = subMap.get(key);
        } // synchronized (subMap).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass,
                       "get",
                       "returns retuenVaue=" + returnValue + "(Object)");
        return returnValue;
    } // end of get().

    /**
     * Add an object to the subset of the Map.
     * 
     * @param key of the Object being inserted.
     * @param value the Object being inserted.
     * @return Object currently occupying the location at the key.
     */
    public final Object put(Object key,
                            Object value)
    {
        Object foundObject = null;
        java.util.Map subMap = getSubMap(key);
        synchronized (subMap) {
            foundObject = subMap.put(key,
                                     value);
        } // synchronized (subMap).
        return foundObject;
    } // put().

    /**
     * Add an object to the subset of the Map, if there is no Object
     * represented by the key.
     * 
     * @param key of the Object to be inserted into the map.
     * @param value to b einserted if there is no Object alreay matching the key.
     * 
     * @return the Object occupying the key or null if there is none.
     * @see java.util.concurrent.ConcurrentHashMap#putIfabsent(K, V)
     */
    public final Object putIfAbsent(Object key,
                                    Object value)
    {
        Object foundObject = null;
        java.util.Map subMap = getSubMap(key);
        synchronized (subMap) {
            foundObject = subMap.get(key);
            if (foundObject == null)
                subMap.put(key,
                           value);
//      // Assume that most of the time the key is vacant, but be prepared to put back the original 
//      // if it is not.
//      foundObject = subMap.put(key,
//                               value);
//      // If there was already something there put back the original.
//      if (foundObject != null && foundObject != value)
//        subMap.put(key,
//                   foundObject);
        } // synchronized (subMap).
        return foundObject;
    } // putIfVacant().

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Map#remove(java.lang.Object)
     */
    public final Object remove(Object key)
    {
        java.util.Map subMap = getSubMap(key);
        synchronized (subMap) {
            return subMap.remove(key);
        } // synchronized (subMap).
    }

    /**
     * Remove any singe element from the map.
     * 
     * @return Object the removed Object or null.
     */
    public final Object removeOne()
    {
        // Avoid locking the whole Map by looking at the subMaps indivdually starting at a
        // random point in the sequence.
        int index = new java.util.Random(Thread.currentThread().hashCode()).nextInt(subMaps.length);
        int firstIndex = index;
        Object removedObject = null;
        do {
            java.util.Map subMap = subMaps[index];
            synchronized (subMap) {
                if (!subMap.isEmpty()) {
                    java.util.Iterator iterator = subMap.values()
                                    .iterator();
                    removedObject = iterator.next();
                    iterator.remove();

                    break;
                } // if (freeTransactions.isEmpty()).
            } // synchronized (subMap).

            index++;
            if (index == subMaps.length)
                index = 0;
        } while (index != firstIndex);

        return removedObject;
    } // removeOne().

    public final void clear()
    {
        for (int i = 0; i < subMaps.length; i++) {
            synchronized (subMaps[i]) {
                subMaps[i].clear();
            } // synchronized (subMaps[i]).
        }
    } // clear().

    /**
     * Clear up to a specified number of elements from the the map.
     * 
     * @param numberToClear the count of elements to clear.
     * @return long the actual number cleared which is the
     *         lesser of size() or numberToClear.
     */
    public final long clear(long numberToClear)
    {
        long numberRemainingToClear = numberToClear;
        // Avoid locking the Map by looking at the subMaps indivdually starting at a
        // random point in the sequence, so as not to disturb the distribution.
        int index = new java.util.Random(Thread.currentThread().hashCode()).nextInt(subMaps.length);
        int numberEmpty = 0;
        while (numberRemainingToClear > 0 && numberEmpty < subMaps.length) {
            java.util.Map subMap = subMaps[index];
            synchronized (subMap) {
                if (!subMap.isEmpty()) {
                    java.util.Iterator iterator = subMap.entrySet().iterator();
                    iterator.next();
                    iterator.remove();
                    numberRemainingToClear--;
                    numberEmpty = 0;
                } else {
                    numberEmpty++;
                } // if (subMap.isEmpty()).
            } // synchronized (subMap).

            index++;
            if (index == subMaps.length)
                index = 0;
        } // while (numberRemainingToClear > 0).

        return numberToClear - numberRemainingToClear;
    } // clear().

    // Defect 306607
    /**
     * This is a more performant implementation of this method than that
     * found in AbstractMap. It simply iterates over the array of subMaps
     * and adds together their sizes
     * 
     * @return The number of elements in this ConcurrentHashMap
     */
    public int size()
    {
        int count = 0;

        if (subMaps != null)
        {
            for (int i = 0; i < subMaps.length; i++)
            {
                count += subMaps[i].size();
            }
        }
        return count;
    }

    /*
     * (non-Javadoc)
     * This implementation is thread safe because it makes a copy of the set under synchronized locks under the subMaps.
     * 
     * @see java.util.Map#entrySet()
     */
    public java.util.Set entrySet()
    {
        java.util.Set combinedSet = new java.util.HashSet();
        if (subMaps != null) {
            for (int i = 0; i < subMaps.length; i++) {
                synchronized (subMaps[i]) {
                    combinedSet.addAll(subMaps[i].entrySet());
                } // synchronized (subMap).
            }
        }
        return combinedSet;

    } // entrySet().

    //  // For one time initialisation.
//  private java.util.Set entries = null; 
//  public java.util.Set entrySet()
//  {    
//    if (entries == null) {
//      entries = new java.util.AbstractSet()
//      {
//        public java.util.Iterator iterator()
//        {
//          return new java.util.Iterator()
//          {
//            private int subMapIndex = 0;
//            private java.util.Iterator subMapIterator = subMaps[subMapIndex].entrySet().iterator();
//
//            public boolean hasNext()
//            {
//              if (subMapIterator.hasNext())
//                return true;
//              
//              subMapIndex++;
//              while (subMapIndex < subMaps.length){
//                subMapIterator = subMaps[subMapIndex].entrySet().iterator();
//                if (subMapIterator.hasNext())
//                  return true;
//                subMapIndex++;
//              } // for subMaps.
//             
//              subMapIndex--;
//              return false;
//            } // hasNext();
//
//            public Object next()
//            {
//              hasNext();
//              return (java.util.Map.Entry) subMapIterator.next();
//            }
//
//            public void remove()
//            {
//              subMapIterator.remove();
//            }
//          };
//        }
//
//        public int size()
//        {
//          return ConcurrentHashMap.this.size();
//        }
//      };
//    }
//    return entries;
//   
//  } // entrySet().

    /*
     * Builds a set of properties containing the current statistics.
     * 
     * @return java.util.Map the statistics.
     */
    protected java.util.Map captureStatistics()
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "captureStatistics"
                            );

        java.util.Map statistics = new java.util.HashMap();

        String histogram = " ";
        for (int n = 0; n < subMapAccessFrequency.length; n++) {
            histogram += subMapAccessFrequency[n] + " ";
            subMapAccessFrequency[n] = 0;
        }
        statistics.put("subMapAccessFrequency", histogram);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "captureStatistics"
                       , new Object[] { statistics }
                            );
        return statistics;
    } // captureStatistics().

    /**
     * Short description of the object.
     * Overrides toString in AbstractMap, which conyains the whole map.
     */
    public String toString()
    {
        return new String(cclass.getName()
                          + "/" + (subMaps == null ? 0 : subMaps.length)
                          + "/" + Integer.toHexString(hashCode()));
    } // of method toString().

} // Of class ConcurrentHashMap.
