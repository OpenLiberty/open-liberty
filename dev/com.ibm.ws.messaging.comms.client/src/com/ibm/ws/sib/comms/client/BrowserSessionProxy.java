/*******************************************************************************
 * Copyright (c) 2004, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
 
package com.ibm.ws.sib.comms.client;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.client.proxyqueue.BrowserProxyQueue;
import com.ibm.ws.sib.comms.common.CommsByteBuffer;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.mfp.MessageDecodeFailedException;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.BrowserSession;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;
import com.ibm.wsspi.sib.core.exception.SISessionUnavailableException;


/**
 * A client implementation of the Core API Browser Session.  This
 * object implements the BrowserSession interface with network
 * aware code.  The implementation "knows" how to provide browser
 * session functionality in a client environment.
 */
public class BrowserSessionProxy extends DestinationSessionProxy implements BrowserSession      // f179339.4
{
   /** Class name for FFDC's */
   private static String CLASS_NAME = BrowserSessionProxy.class.getName();
   
   /** Trace */
   private static final TraceComponent tc = SibTr.register(BrowserSessionProxy.class, 
                                                           CommsConstants.MSG_GROUP, 
                                                           CommsConstants.MSG_BUNDLE);

   // The proxy queue which backs this browser session.   
   private BrowserProxyQueue proxyQueue;

   // Reader/writer lock used to provide mutual exclusion for the close operation.
   // Close itself must obtain this lock as a writer.  Most other methods from
   // the BrowserSession interface need to obtain this as a reader.   
   private ReentrantReadWriteLock closeLock = null;
   
   // Lock used to provide synchronization for session methods.  We cannot
   // synchronize on this as it might cause "unexpected" behaviour for the user.
   private Object lock = new Object();
   
   /**
    * Constructor.
    * 
    * @param con
    * @param cp
    * @param data
    * @param proxy
    * @param destAddr
    */
   protected BrowserSessionProxy(Conversation con, ConnectionProxy cp, CommsByteBuffer data,
                                 BrowserProxyQueue proxy, SIDestinationAddress destAddr)
   {
      super(con, cp);
      
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", 
                                           new Object[]{con, cp, data, proxy, destAddr});
      
      setDestinationAddress(destAddr);
      inflateData(data);
      closeLock = cp.closeLock;
      proxyQueue = proxy;

      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
   }

   /**
    * Browses the next message. Aside from locking concerns, this simply delegates to the proxy
    * queue "next" implementation.
    * 
    * @see BrowserProxyQueue#next()
    * 
    * @throws com.ibm.wsspi.sib.core.exception.SISessionUnavailableException
    * @throws com.ibm.wsspi.sib.core.exception.SISessionDroppedException
    * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
    * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
    * @throws com.ibm.websphere.sib.exception.SIResourceException
    * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
    * @throws com.ibm.websphere.sib.exception.SIErrorException
    * @throws com.ibm.wsspi.sib.core.exception.SINotAuthorizedException
    */
   // f169897.2 changed throws   
   public SIBusMessage next() 
      throws SISessionUnavailableException, SISessionDroppedException,
             SIConnectionUnavailableException, SIConnectionDroppedException,
             SIResourceException, SIConnectionLostException, 
             SIErrorException,
             SINotAuthorizedException
	{
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "next");
      
      checkAlreadyClosed();
      
      SIBusMessage message = null;
      synchronized(lock)
      {
         try
         {
            closeLock.readLock().lockInterruptibly();
            try
            {
               message = proxyQueue.next();
            }
            catch(MessageDecodeFailedException mde)
            {
               FFDCFilter.processException(mde, CLASS_NAME + ".next", 
                                           CommsConstants.BROWSERSESSION_NEXT_01, this);
               SIResourceException coreException = new SIResourceException(mde);
               throw coreException;
            }
            finally
            {
               closeLock.readLock().unlock();
            }
         }
         catch(InterruptedException e)
         {
            // No FFDC code needed
            if (tc.isDebugEnabled()) SibTr.debug(this, tc, "interrupted exception");
         }
      }
      
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "next", message);
      return message;
	}

   /**
    * Closes the browser session.  This involves some synchronization, flowing a
    * close request and also marking the appropriate objects as closed.
    * 
    * @throws com.ibm.websphere.sib.exception.SIResourceException
    * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
    * @throws com.ibm.websphere.sib.exception.SIErrorException
    * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
    */
   public void close() 
      throws SIResourceException, SIConnectionLostException,
             SIErrorException, SIConnectionDroppedException
	{
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "close");
      synchronized(lock)
      {
         if (!isClosed())
         {
            try
            {
               closeLock.writeLock().lockInterruptibly();
               try
               {
                  // Close the proxy queue
                  proxyQueue.close();
                
                  // Mark this session as closed.
                  setClosed();  
               }
               finally
               {
                  closeLock.writeLock().unlock();
               }
            }
            catch(InterruptedException e)
            {
               // No FFDC code needed
               if (tc.isDebugEnabled()) SibTr.debug(this, tc, "interrupted exception");
            }
         }
      }      
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "close");
	}

   /**
    * Resets the browse cursor.  Here, we simply delegate to the proxy
    * queue which backs the browser session.
    * 
    * @throws com.ibm.wsspi.sib.core.exception.SISessionUnavailableException
    * @throws com.ibm.wsspi.sib.core.exception.SISessionDroppedException
    * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
    * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
    * @throws com.ibm.websphere.sib.exception.SIResourceException
    * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
    * @throws com.ibm.websphere.sib.exception.SIErrorException
    */
	//f169897.2 added
   public void reset()
      throws SISessionUnavailableException, SISessionDroppedException,
             SIConnectionUnavailableException, SIConnectionDroppedException,
             SIResourceException, SIConnectionLostException, 
             SIErrorException
	{
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "reset");
      
      checkAlreadyClosed();
      
      synchronized(lock)
      {
         try
         {
            closeLock.readLock().lockInterruptibly();
            try
            {
               proxyQueue.reset();
            }
            finally
            {
               closeLock.readLock().unlock();
            }

         }
         catch(InterruptedException e)
         {
            // No FFDC code needed
            if (tc.isDebugEnabled()) SibTr.debug(this, tc, "interrupted exception");
         }
      }
      
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "reset");
	}
}
