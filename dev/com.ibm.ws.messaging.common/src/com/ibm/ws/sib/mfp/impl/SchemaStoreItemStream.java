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

import java.util.HashMap;
import java.util.Map;

import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.mfp.jmf.JMFSchema;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.Item;
import com.ibm.ws.sib.msgstore.ItemStream;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.NonLockingCursor;
import com.ibm.ws.sib.msgstore.NotInMessageStore;
import com.ibm.ws.sib.msgstore.OutOfCacheSpace;
import com.ibm.ws.sib.msgstore.PersistenceException;
import com.ibm.ws.sib.msgstore.ProtocolException;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.StreamIsFull;
import com.ibm.ws.sib.msgstore.TransactionException;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.utils.ras.SibTr;

import com.ibm.websphere.ras.TraceComponent;

/*
 * This class reperesents the SchemaStore within the message store.  It is
 * modelled on the 'IndexItemStream' example provided by the message store
 * component.
 *
 * The SchemaStoreItemStream contains instances of SchemaStoreItems.  To allow
 * a fast chcek of which items are in the store, and to locate items quickly
 * when needed we maintain an index of this stream in a private hash table.
 * The index is initially created when the stream is created or restored - we
 * don't expect too many schemas to be persisted, so this index creation should
 * not be a great overhead.
 */

public class SchemaStoreItemStream extends ItemStream {
  private static TraceComponent tc = SibTr.register(SchemaStoreItemStream.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);
 
  // This hash table serves two purposes.  It is keyed by JMFSchema IDs and its values are
  // the message store ids of any schemas saved within the message store.
  // This provides us with a fast mechanism for determining if a given JMFSchema exists
  // within the SchemaStore (just need to test for the existence of a key in this map).
  // And it provides an index for the SchemaStore, so we can quickly locate and restore
  // schema definitions when required.
  Map<Long, Long>  schemaIndex = new HashMap<Long, Long>();

  /*
   * The following methods are part of the contract necessary to be a store
   * in the message store.
   */

  // SchemaStore should always be persistent
  public int getStorageStrategy() {
    return STORE_ALWAYS;
  }

  // Override eventRestored() which is called when this Stream is first restored from
  // the message store.  We build the index of any currently stored items.
  public void eventRestored() throws SevereMessageStoreException {
    super.eventRestored();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "eventRestored");
    try {
      NonLockingCursor cursor = newNonLockingItemCursor(null);
      AbstractItem item = cursor.next();
      while (item != null) {
        if (item instanceof SchemaStoreItem) {
          addToIndex((SchemaStoreItem)item);
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "JSchema found in store: " + ((SchemaStoreItem)item).getSchema().getID());
        }
        item = cursor.next();
      }
    } catch (MessageStoreException e) {
      FFDCFilter.processException(e, "eventRestored", "108", this);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "eventRestored");
  }

  // Override addItem() to ensure we maintain our index
  public void addItem(Item item, Transaction tran) throws OutOfCacheSpace, StreamIsFull, ProtocolException, TransactionException, PersistenceException, SevereMessageStoreException {
    super.addItem(item, tran);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "addItem");
    if (item instanceof SchemaStoreItem) {
      addToIndex((SchemaStoreItem)item);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "JSchema added to store: " + ((SchemaStoreItem)item).getSchema().getID());
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "addItem");
  }


  /*
   * The following methods are specific to the SchemaStore implementation
   */

  // Returns true if a schema is already saved in the schema store
  boolean containsSchema(Long id) {
    return schemaIndex.containsKey(id);
  }

  // Add a new schema defintion to the store
  void addSchema(JMFSchema schema, Transaction tran) throws MessageStoreException {
    addItem(new SchemaStoreItem(schema), tran);
  }

  // Restore a schema definition from the store
  JMFSchema findSchema(long schemaId) throws MessageStoreException {
    Long storeId = schemaIndex.get(Long.valueOf(schemaId));
    if (storeId != null) {
      AbstractItem item = findById(storeId.longValue());
      return ((SchemaStoreItem)item).getSchema();
    } else
      throw new MessageStoreException("Schema not found in store: " + schemaId);
  }

  // Add an item to our index
  void addToIndex(SchemaStoreItem item) throws NotInMessageStore {
    schemaIndex.put(item.getSchema().getLongID(), Long.valueOf(item.getID()));
    item.setStream(this);
  }

  // Remove an item from our index
  void removeFromIndex(SchemaStoreItem item) {
    schemaIndex.remove(item.getSchema().getLongID());
    item.setStream(null);
  }
}
