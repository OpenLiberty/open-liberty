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

import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.mfp.jmf.JMFRegistry;
import com.ibm.ws.sib.mfp.jmf.JMFSchema;
import com.ibm.ws.sib.msgstore.Item;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.utils.DataSlice;
import com.ibm.ws.sib.utils.ras.SibTr;

import com.ibm.websphere.ras.TraceComponent;

/*
 * This class represents an item that can be stored in our SchemaStore part of the
 * message store.  It is modelled on the 'IndexableItem' expample provided by
 * the message store component.
 *
 * Each JMF schema saved in the SchemaStore will be represented by an instance of
 * this class.
 */

public class SchemaStoreItem extends Item {
  private static TraceComponent tc = SibTr.register(SchemaStoreItem.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);


  private JMFSchema schema;
  private SchemaStoreItemStream stream;

  /*
   * Constructors.  We must have a no-args constructor for the message store.
   */
  public SchemaStoreItem() {
  }

  SchemaStoreItem(JMFSchema schema) {
    this.schema = schema;
  }

  /*
   * The following methods are part of the contract necessary to be an item that
   * can be stored in the message store.
   */

  // Schemas should always be persistent
  public int getStorageStrategy() {
    return STORE_ALWAYS;
  }

  // If a commit fails this item won't end up in the store, so we need to remove it
  // from its parent's stream's index.
  public void eventPostCommitRemove(Transaction tran) throws SevereMessageStoreException {
    super.eventPostCommitRemove(tran);
    if (stream != null)
        stream.removeFromIndex(this);
  }

  // Return the serialized form of the schema for persisting in a database.
  // The serialized form is wrapped in a DataSlice and returned in a single-entry List. SIB0112b.mfp.1
  public List<DataSlice> getPersistentData() {
    List<DataSlice> slices = new ArrayList<DataSlice>(1);
    slices.add(new DataSlice(schema.toByteArray()));
    return slices;
  }

  // Return the size of the data.  The JMFSchema.toByteArray() result
  // always already exists, so this is fast & will do.
  public int getInMemorySize() {
    return schema.toByteArray().length;
  }

  // Restore this item from serialized form
  public void restore(List<DataSlice> slices) {
    schema = JMFRegistry.instance.createJMFSchema(slices.get(0).getBytes());
  }

  /*
   * The following methods are specific to the SchemaStore implementation
   */

  JMFSchema getSchema() {
    return schema;
  }

  void setStream(SchemaStoreItemStream stream) {
    this.stream = stream;
  }
}
