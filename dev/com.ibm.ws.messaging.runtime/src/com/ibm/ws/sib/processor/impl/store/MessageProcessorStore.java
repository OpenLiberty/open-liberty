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
package com.ibm.ws.sib.processor.impl.store;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.msgstore.MessageStore;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.store.itemstreams.SIMPItemStream;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.FormattedWriter;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * @author caseyj
 *
 * This class both acts as the ItemStream root of the persistent data storage 
 * structure and stores persistent state data for the MessageProcessor class.
 * <p>
 * Ideally, MessageProcessor would itself extend ItemStream and be retrieved
 * from the Message Store at warm restart.  But this is not possible since 
 * admin need to construct a MessageProcessor and call start() before anything
 * can be retrieved from the Store.  Therefore, MessageProcessorStore acts as
 * a proxy for storing persistent state data and the persistent storage
 * hierarchy.
 * <p>
 * It follows that it would also be ideal if this class were an inner 
 * class of MessageProcessor.  Unfortunately, the MessageStore is not capable of
 * reconstituting inner classes, so this has to be a top-level class.
 */
public final class MessageProcessorStore 
  extends SIMPItemStream
{
  private static final TraceComponent tc =
    SibTr.register(
      MessageProcessorStore.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
        
  /**
   * NLS for component
   */
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);  

    
  /**
   * Persistent data version number.
   */
  private static final int PERSISTENT_VERSION = 1;    
       
  /**
   * Uuid of the Message Processor to which this PersistentStore belongs
   */ 
  private SIBUuid8 messagingEngineUuid;  
              
  /**
   * Warm start constructor invoked from the Message Store.  Do not invoke me
   * directly.
   */
  public MessageProcessorStore()
  {
    super();
  }
  
  /**
   * Cold start constructor.  Adds itself to the given parent ItemStream
   * 
   * @param uuid              The cold started ME's uuid.
   * @param parentItemStream  The ItemStream to add this object to.
   * @param txManager         The transaction manager.
   * 
   * @throws MessageStoreException
   */
  public MessageProcessorStore(SIBUuid8 uuid, 
                               MessageStore parentItemStream,
                               SIMPTransactionManager txManager) 
    throws MessageStoreException
  {
    super();
    
    if (tc.isEntryEnabled()) 
      SibTr.entry(tc, "MessageProcessorStore", new Object[]{uuid, parentItemStream, txManager}); 
      
    setMessagingEngineUuid(uuid);
    
    parentItemStream.add(this, txManager.createAutoCommitTransaction());
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "MessageProcessorStore", this);
  }  
      
  /**
   * Returns the messagingEngineUuid.
   * 
   * @return SIBUuid
   */
  public SIBUuid8 getMessagingEngineUuid()
  {
    if (tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getMessagingEngineUuid");
      SibTr.exit(tc, "getMessagingEngineUuid", messagingEngineUuid);
    }

    return messagingEngineUuid;
  }  
 
  /**
   * Set the messagingEngineUuid.
   * 
   * @param uuid
   */  
  private void setMessagingEngineUuid(SIBUuid8 uuid)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "setMessagingEngineUuid", new Object[] { uuid });
      
    messagingEngineUuid = uuid;
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "setMessagingEngineUuid");
  } 
    
  /*
   * (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.store.itemstreams.SIMPReferenceStream#getVersion()
   */
  public int getPersistentVersion()
  {
    return PERSISTENT_VERSION;
  } 
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.store.itemstreams.SIMPItemStream#getPersistentData(java.io.ObjectOutputStream)
   */  
  public void getPersistentData(ObjectOutputStream oos)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "getPersistentData", oos);
    
    try 
    {
      oos.writeObject(messagingEngineUuid.toByteArray());
    }
    catch (java.io.IOException e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.store.MessageProcessorStore.getPersistentData",
        "1:210:1.42",
        this);

      SibTr.exception(tc, e);
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.impl.store.MessageProcessorStore",
          "1:217:1.42",
          e });
      
      if (tc.isEntryEnabled()) 
        SibTr.exit(tc, "getPersistentData", "SIErrorException");

      throw new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.impl.store.MessageProcessorStore",
            "1:228:1.42",
            e },
          null),
        e);
    }
  
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "getPersistentData");
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.store.itemstreams.SIMPItemStream#restore(java.io.ObjectInputStream, int)
   */  
  public void restore(ObjectInputStream ois, int dataVersion)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(
        tc, "restore", new Object[] { ois, new Integer(dataVersion) });
    
    checkPersistentVersionId(dataVersion);  
        
    try 
    {           
      messagingEngineUuid = new SIBUuid8((byte[])ois.readObject());
    }
    catch (Exception e) 
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.store.MessageProcessorStore.restore",
        "1:258:1.42",
        this);

      SibTr.exception(tc, e);
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.impl.store.MessageProcessorStore",
          "1:265:1.42",
          e });
      
      if (tc.isEntryEnabled()) SibTr.exit(tc, "restore", "SIErrorException");

      throw new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.impl.store.MessageProcessorStore",
            "1:275:1.42",
            e },
          null),
        e);
    }  
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "restore");  
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.msgstore.AbstractItem#xmlWriteOn(com.ibm.ws.sib.msgstore.FormattedWriter)
   */
  public void xmlWriteOn(FormattedWriter writer) throws IOException  
  {
    writer.newLine();
    writer.startTag("messagingEngineUuid");
    writer.write(messagingEngineUuid.toString());
    writer.endTag("messagingEngineUuid");
  }
}
