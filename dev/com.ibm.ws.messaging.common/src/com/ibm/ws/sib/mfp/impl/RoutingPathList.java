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
package com.ibm.ws.sib.mfp.impl;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.ws.sib.mfp.JsDestinationAddress;
import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;

import com.ibm.websphere.ras.TraceComponent;

/*
 * This class provides a List of JsDestinationAddresses.  It does this by extending
 * the AbstractList class and implementing the methods therefore required to create
 * a modifiable List.
 *
 * In addition to the List methods that manipulate the list of JsDestinationAddresses,
 * we have a set of getters that return Lists (or a bit array in one case) of the
 * four seprate items within the JsDestinationAddress.  This is necessary because the
 * underlying JMF in which a message is ultimately stored operatates on four separate
 * lists of items, rather than a single list of an item with four values.
 *
 * The methods getNames(), getMEs(), getBusNames() and getLocalOnlys() return these
 * separate lists of the four items in a JsDestinationAddress.
 *
 * Instances of this class can exist in one of two main states:
 *
 * 1. They can contain a real List of JsDestinationAddresses originally constructed
 *    and provided by caller code.  In this state the modifiable List methods will
 *    operatate directly on the contained list and we will magic-up the four separate
 *    item lists when needed.  This is done using inner classes that pull their
 *    data from the contained JsDestinationAddress list, so no extra list copies
 *    need to be made.
 *
 * 2. It can contain four real Lists (actually three Lists and one bit array) initially
 *    obtained from the underyling JMF message.  In this state we can magic-up the complete
 *    JsDestinationAddress list using inner classes accessing data from the four separate
 *    lists.  Any read-only use of this list (for example just checking whether or not
 *    it is empty) will not require any copying to be done and will only pull real data
 *    out of JMF when needed.
 *
 *    However since JMF lists cannot (and in this case should not) be modified if attempts
 *    are made to modify the data in a list backed by JMF we will have to make a modifiable
 *    copy of the data.  This is done by constructing a new JsDestinationAddress list
 *    and discarding the references to the underyling JMF lists.
 */

public class RoutingPathList extends AbstractList<SIDestinationAddress> {
  private static TraceComponent tc = SibTr.register(RoutingPathList.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);
 
  // Constant empty lists to avoid the need to keep creating new ones
  private static final List<String> EMPTY_LIST_OF_STRINGS    = new ArrayList<String>();
  private static final List<byte[]> EMPTY_LIST_OF_BYTEARRAYS = new ArrayList<byte[]>();

  private List<SIDestinationAddress> addrList;

  private List<String> jmfNames;
  private List<byte[]> jmfMEs;
  private List<String> jmfBuses;
  private byte[] jmfLocalOnlys;

 /*
  * Construct a new instance based on a single user-supplied and modifiable List of
  * JsDestinationAddresses.
  */
  RoutingPathList(List<SIDestinationAddress> addrList) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "RoutingPathList", addrList);

    // The addrList must contain only non-null instances of SI/JsDestinationAddress
    // items, however we don't verify that here to improve performance.  It is verfifed
    // at the user API layer but internally we assume Jetstream code does the right thing.

    // The user's parameter can be null, which we interpret as wanting a new empty list.
    this.addrList = (addrList != null) ? addrList : new ArrayList<SIDestinationAddress>();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "RoutingPathList");
  }

  /*
   * Construct a new instance based on four separate Lists of the items that constitute
   * a JsDestinationAddress.  We expect these to have come from JMF and they should not
   * be changed.
   */
  RoutingPathList(List<String> names, byte[] los, List<byte[]> mes, List<String> buses) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "RoutingPathList", new Object[]{names, los, mes, buses});

    // The supplied parameters didn't ought to be null since they should have come from
    // JMF, but just in case they are we can use empty lists instread
    if (names != null) {
      jmfNames = names;
      jmfMEs = mes;
      jmfBuses = buses;
      jmfLocalOnlys = los;
    } else {
      jmfNames = EMPTY_LIST_OF_STRINGS;
      jmfMEs   = EMPTY_LIST_OF_BYTEARRAYS;
      jmfBuses = EMPTY_LIST_OF_STRINGS;
      jmfLocalOnlys = new byte[0];
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "RoutingPathList");
  }

  /*
   * Methods to return separate lists (or a byte[]) for the four separate items within
   * the JsDestinationAddress structure.  When backed by a single list of JsDestinationAddresses
   * we use inner classes to simulate these separate lists from the main list this avoids the
   * need to copy data.
   */

   // TODO: should we cache the single-list inner class instances?  I don't think we are
   // going to be asked for these repeatedly so I'm not currently doing so, but performance
   // measurements may show otherwise.

   List<String> getNames() {
     if (addrList == null)
       return jmfNames;

     // Create a readonly list that extracts the 'names' information from the main list
     return new AbstractList<String>() {
       public int size() {
         return addrList.size();
       }
       public String get(int index) {
         return ((JsDestinationAddress)addrList.get(index)).getDestinationName();
       }
     };
   }

   List<byte[]> getMEs() {
     if (addrList == null)
       return jmfMEs;

     // Create a readonly list that extracts the 'MEs' information from the main list
     return new AbstractList<byte[]>() {
       public int size() {
         return addrList.size();
       }
       public byte[] get(int index) {
         SIBUuid8 uid = ((JsDestinationAddress)addrList.get(index)).getME();
         return (uid != null) ? uid.toByteArray() : null;
       }
     };
   }

   List<String> getBusNames() {
     if (addrList == null)
       return jmfBuses;

     // Create a readonly list that extracts the 'bus names' information from the main list
     return new AbstractList<String>() {
       public int size() {
         return addrList.size();
       }
       public String get(int index) {
         return ((JsDestinationAddress)addrList.get(index)).getBusName();
       }
     };
   }

   byte[] getLocalOnlys() {
     if (addrList == null)
       return jmfLocalOnlys;

     // Create a byte[] containing a bitflag list of the main list's locals only information.
     int size = addrList.size();
     byte[] bitflags = new byte[(size+7)/8];
     for (int i = 0; i < size; i++)
       setFlag(bitflags, i, ((JsDestinationAddress)addrList.get(i)).isLocalOnly());
     return bitflags;
   }

  /*
   * Methods required by the AbstractList implementation to provide a modifiable List.
   * When backed by the four JMF lists we can implement read-only function by pulling data
   * out of the four lists.  If changes are to be made we have to first make a copy.
   */

  // Get() and size() don't change the list

  public int size() {
    if (addrList != null)
      return addrList.size();
    return jmfNames.size();
  }

  public SIDestinationAddress get(int index) {
    if (addrList != null)
      return addrList.get(index);

    // TODO: we could consider caching these constructed JsDestinationAddressImpl objects
    // if performance measurements show we are repeatedly asked for the same ones.

    String name = jmfNames.get(index);
    byte[] me =   jmfMEs.get(index);
    String bus =  jmfBuses.get(index);
    boolean localOnly = getFlag(jmfLocalOnlys, index);
    SIBUuid8 uid = (me == null) ? null : new SIBUuid8(me);

    return new JsDestinationAddressImpl(name, localOnly, uid, bus);
  }

  // For a modifiable List we must also provide an implementation of set(), add() and remove()

  public SIDestinationAddress set(int index, SIDestinationAddress value) {
    if (addrList == null)
      copyLists();
    return addrList.set(index, value);
  }

  public void add(int index, SIDestinationAddress value) {
    if (addrList == null)
      copyLists();
    addrList.add(index, value);
  }

  public SIDestinationAddress remove(int index) {
    if (addrList == null)
      copyLists();
    return addrList.remove(index);
  }

  /*
   * Helper method to copy the readonly JMF set of lists to a new modifiable JsDestinationAddress list
   */
  private void copyLists() {
    addrList = new ArrayList<SIDestinationAddress>();
    for (int i = 0; i < jmfNames.size(); i++) {
      String name = jmfNames.get(i);
      byte[] me =   jmfMEs.get(i);
      String bus =  jmfBuses.get(i);
      boolean localOnly = getFlag(jmfLocalOnlys, i);
      SIBUuid8 uid = (me == null) ? null : new SIBUuid8(me);
      addrList.add(new JsDestinationAddressImpl(name, localOnly, uid, bus));
    }

    // We should no longer reference the original JMF values
    jmfNames = null;
    jmfMEs = null;
    jmfBuses = null;
    jmfLocalOnlys = null;
  }

  /*
   * Helper methods to get and set the boolean flags from a bit array.
   */
  private static boolean getFlag(byte[] bitflags, int index) {
    byte flags = bitflags[index/8];
    byte bit = (byte)(1 << (index%8));
    return ((flags & bit) != 0);
  }

  private static void setFlag(byte[] bitflags, int index, boolean value) {
    byte flags = bitflags[index/8];
    byte bit = (byte)(1 << (index%8));
    flags = (value) ? (byte)(flags | bit) : (byte)(flags & ~bit);
    bitflags[index/8] = flags;
  }
}
