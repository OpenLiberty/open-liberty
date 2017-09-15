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

package com.ibm.ws.sib.processor.impl;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.mfp.MessageCopyFailedException;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.NonLockingCursor;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.interfaces.BrowseCursor;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Uses a MessageStore NonLockingCursor to browse messages
 * on a MessageStore ItemStream.
 * 
 * The cursor makes a copy of each message returned for
 * saftey reasons.
 */
public class JSBrowseCursor implements BrowseCursor
{
  
  // NLS for component
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);
    
  private static final TraceComponent tc =
    SibTr.register(
      JSBrowseCursor.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

 

  private NonLockingCursor msgStoreNonLockingCursor;
  
    
  public JSBrowseCursor(NonLockingCursor msgStoreNonLockingCursor)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "JSBrowseCursor", 
                      msgStoreNonLockingCursor);
      
    this.msgStoreNonLockingCursor = msgStoreNonLockingCursor;
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "JSBrowseCursor", this);
  }

  /*
   *  (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.BrowseCursor#next()
   */
  public JsMessage next() throws SIResourceException
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "next");
      
    AbstractItem msg = null;
  
     if (msgStoreNonLockingCursor != null)
     {
       try
       {
         //Get the next item from the browseCursor and cast it
         //to a SIMPMessage
         msg = msgStoreNonLockingCursor.next();
       }
       catch (MessageStoreException e)
       {
         // MessageStoreException shouldn't occur so FFDC.
         FFDCFilter.processException(
           e,
           "com.ibm.ws.sib.processor.impl.JSBrowseCursor.next",
           "1:106:1.1",
           this);
            
         SibTr.exception(tc, e);
         SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
           new Object[] {
             "com.ibm.ws.sib.processor.impl.JSBrowseCursor",
             "1:113:1.1",
             e });

         if (tc.isEntryEnabled())
           SibTr.exit(tc, "next", e);

         throw new SIResourceException(
           nls.getFormattedMessage(
             "INTERNAL_MESSAGING_ERROR_CWSIP0002",
             new Object[] {
               "com.ibm.ws.sib.processor.impl.JSBrowseCursor",
               "1:124:1.1",
               e },
             null),
           e);
       }

       if (msg != null)
       {
         //Set the redelivered count on the SIBusMessage
         JsMessage jsMsg = ((SIMPMessage) msg).getMessage();
         jsMsg.setRedeliveredCount(msg.guessUnlockCount());
         try
         {
           //Get a safe copy of the message
           jsMsg = jsMsg.getReceived();
         }
         catch (MessageCopyFailedException e)
         {
           // A MessageCopyFailedException is bad, log a FFDC
           FFDCFilter.processException(
             e,
             "com.ibm.ws.sib.processor.impl.JSBrowseCursor.next",
             "1:146:1.1",
             this);
            
           SibTr.exception(tc, e);
           SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
             new Object[] {
               "com.ibm.ws.sib.processor.impl.JSBrowseCursor",
               "1:153:1.1",
               e });

           if (tc.isEntryEnabled())
             SibTr.exit(tc, "next", e);

           throw new SIResourceException(
             nls.getFormattedMessage(
               "INTERNAL_MESSAGING_ERROR_CWSIP0002",
               new Object[] {
                 "com.ibm.ws.sib.processor.impl.JSBrowseCursor",
                 "1:164:1.1",
                 e },
               null),
             e);
         }//end catch
         
         if (tc.isEntryEnabled())
           SibTr.exit(tc, "next", jsMsg);
         //return the copy
         return jsMsg;
         
       }//end if msg!=null
     }//end if msgStoreNonLockingCursor != null
    
   if (tc.isEntryEnabled())
     SibTr.exit(tc, "next", null);
    return null;  
  }
  
  /*
   *  (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.BrowseCursor#finished()
   */
  public void finished()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "finished");
      
    if(msgStoreNonLockingCursor!=null)
    {
      msgStoreNonLockingCursor.finished();
    }
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "finished");
  }
}
