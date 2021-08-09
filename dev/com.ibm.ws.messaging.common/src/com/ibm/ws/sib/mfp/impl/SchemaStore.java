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
import com.ibm.ws.sib.mfp.MessageEncodeFailedException;
import com.ibm.ws.sib.mfp.MessageRestoreFailedException;
import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.mfp.jmf.JMFException;
import com.ibm.ws.sib.mfp.jmf.JMFRegistry;
import com.ibm.ws.sib.mfp.jmf.JMFSchema;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.Filter;
import com.ibm.ws.sib.msgstore.MessageStore;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.utils.ras.SibTr;

import com.ibm.websphere.ras.TraceComponent;

/*
 * This class provides the methods used by the rest of MFP to interface to and make use
 * of the SchemaStore section of the message store for persisting schema definitions
 * when messages are themselves to be persisted.
 *
 * Note: this class (despite its name) is not the schema store itself, that is the
 * SchemaStoreItemStream class.  This class just provides the MFP access to the
 * schema store.
 */

// IF YOU RENAME THIS CLASS YOU MUST CHANGE THE VALUE OF com.ibm.ws.sib.mfp.SchemaStoreNotifier#CLASS_NAME
public class SchemaStore {
  private static TraceComponent tc = SibTr.register(SchemaStore.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);
 
  // Provide a mapping between ME MessageStore instances and the SchemaStore within
  // that message store.  This avoids the need to repeatedly search the MessageStore
  // for the corresponding SchemaStore.
  static Map<MessageStore, SchemaStoreItemStream> schemaStores = new HashMap<MessageStore, SchemaStoreItemStream>();

  // Called when persisting a message in the current ME.  Any schema defintions not
  // already held in the SchemaStore for that ME are added.
  static void saveSchemas(MessageStore msgStore, JMFSchema[] schemas) throws MessageEncodeFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "saveSchemas");
    try {
      // Get the SchemaStore reference, creating a new one if necessary
      SchemaStoreItemStream schemaStore = getSchemaStore(msgStore);

      // Check if any new schemas need to be persisted to the SchemaStore
      for (int i = 0; i < schemas.length; i++) {
        if (!schemaStore.containsSchema(schemas[i].getLongID())) {
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Saving schema:" + schemas[i].getID());
          schemaStore.addSchema(schemas[i], msgStore.getTransactionFactory().createAutoCommitTransaction());
        }
      }
    } catch (MessageStoreException e) {
      FFDCFilter.processException(e, "saveSchemas", "83");
      throw new MessageEncodeFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "saveSchemas");
  }

  // Called when recovering a message from the message store.  Any schemas needed to
  // decode the message that are not already in the JMF registry must be reloaded
  static void loadSchemas(MessageStore msgStore, long[] schemaIds) throws MessageRestoreFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "loadSchemas");
    try {
      // Get the SchemaStore reference, creating a new one if necessary
      SchemaStoreItemStream schemaStore = getSchemaStore(msgStore);

      // Check for any unknown schemas
      for (int i = 0; i < schemaIds.length; i++) {
        if (JMFRegistry.instance.retrieve(schemaIds[i]) == null) {
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Restoring schema:" + schemaIds[i]);
          JMFRegistry.instance.register(schemaStore.findSchema(schemaIds[i]));
        }
      }
    } catch (MessageStoreException e) {
      FFDCFilter.processException(e, "loadSchemas", "105");
      throw new MessageRestoreFailedException(e);
    } catch (JMFException e) {
      FFDCFilter.processException(e, "loadSchemas", "108");
      throw new MessageRestoreFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "loadSchemas");
  }

  // This static method is used to locate (and create if necessary) an instance of a
  // SchemaStore for a particular ME.
  private static SchemaStoreItemStream getSchemaStore(MessageStore msgStore) throws MessageStoreException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "getSchemaStore");

    // Check the local cache first.
    SchemaStoreItemStream store = schemaStores.get(msgStore);

    // Not in the local cache, so try to find it in the MessageStore
    if (store == null) {
      Filter filter = new Filter() {
        public boolean filterMatches(AbstractItem item) {
          return (item instanceof SchemaStoreItemStream);
        }
      };
      store = (SchemaStoreItemStream)msgStore.findFirstMatching(filter);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() && store != null) SibTr.debug(tc, "Schema store found in message store");

      // Not in the MessageStore, so try to create a new one
      if (store == null) {
        store = new SchemaStoreItemStream();
        msgStore.add(store, msgStore.getTransactionFactory().createAutoCommitTransaction());
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "New schema store created");
      }

      schemaStores.put(msgStore, store);

    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getSchemaStore");
    return store;
  }

  /**
   * This method should be called when a MessageStore is stopping.  The
   * underlying schema store will release any references to the store.
   *
   * @param msgStore the MessageStore that is stopping
   */
  // IF YOU RENAME OR CHANGE THE PARAMETERS OF THIS METHOD YOU MUST CHANGE THE VALUE OF
  // com.ibm.ws.sib.mfp.SchemaStoreNotifier#METHOD_NAME AND/OR
  // com.ibm.ws.sib.mfp.SchemaStoreNotifier#PARAMETER_TYPES
  public static void messageStoreStoppingNotify(MessageStore msgStore) {
    schemaStores.remove(msgStore);
  }
}
