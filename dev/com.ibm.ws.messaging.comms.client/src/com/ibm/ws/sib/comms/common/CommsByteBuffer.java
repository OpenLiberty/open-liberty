/*******************************************************************************
 * Copyright (c) 2003, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.common;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.exception.SIDataGraphException;
import com.ibm.websphere.sib.exception.SIDataGraphFormatMismatchException;
import com.ibm.websphere.sib.exception.SIDataGraphSchemaNotFoundException;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIMessageDomainNotSupportedException;
import com.ibm.websphere.sib.exception.SIMessageException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SINotSupportedException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.comms.CommsConnection;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.client.OptimizedTransaction;
import com.ibm.ws.sib.comms.client.SuspendableXAResource;
import com.ibm.ws.sib.comms.client.Transaction;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.HandshakeProperties;
import com.ibm.ws.sib.jfapchannel.JFapByteBuffer;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer;
import com.ibm.ws.sib.jfapchannel.impl.CommsClientServiceFacade;
import com.ibm.ws.sib.mfp.AbstractMessage;
import com.ibm.ws.sib.mfp.IncorrectMessageTypeException;
import com.ibm.ws.sib.mfp.JsDestinationAddress;
import com.ibm.ws.sib.mfp.JsDestinationAddressFactory;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.mfp.JsMessageHandle;
import com.ibm.ws.sib.mfp.MessageCopyFailedException;
import com.ibm.ws.sib.mfp.MessageEncodeFailedException;
import com.ibm.ws.sib.mfp.impl.JsMessageFactory;
import com.ibm.ws.sib.mfp.impl.JsMessageHandleFactory;
import com.ibm.ws.sib.utils.DataSlice;
import com.ibm.ws.sib.utils.Reasonable;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.wsspi.sib.core.SIMessageHandle;
import com.ibm.wsspi.sib.core.SITransaction;
import com.ibm.wsspi.sib.core.SIUncoordinatedTransaction;
import com.ibm.wsspi.sib.core.SIXAResource;
import com.ibm.wsspi.sib.core.SelectionCriteria;
import com.ibm.wsspi.sib.core.SelectorDomain;
import com.ibm.wsspi.sib.core.exception.SIAuthenticationException;
import com.ibm.wsspi.sib.core.exception.SICommandInvocationFailedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SIDestinationLockedException;
import com.ibm.wsspi.sib.core.exception.SIDiscriminatorSyntaxException;
import com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionAlreadyExistsException;
import com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionMismatchException;
import com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionNotFoundException;
import com.ibm.wsspi.sib.core.exception.SIInsufficientDataForFactoryTypeException;
import com.ibm.wsspi.sib.core.exception.SIInvalidDestinationPrefixException;
import com.ibm.wsspi.sib.core.exception.SILimitExceededException;
import com.ibm.wsspi.sib.core.exception.SIMessageNotLockedException;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;
import com.ibm.wsspi.sib.core.exception.SIRollbackException;
import com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;
import com.ibm.wsspi.sib.core.exception.SISessionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SITemporaryDestinationNotFoundException;

/**
 * This class extends the JFapByteBuffer to allow many useful methods that Comms requires for
 * putting data into buffers for transmission or reception.
 * <p>
 * This class can be used to put / get Strings, XId's, Transaction information, Messages of all
 * kinds, destination addresses and selection criterias.
 * <p>
 * For server-specific buffer creation see <code>CommsServerByteBuffer</code> which contains
 * methods for creating buffers for ME-ME messages and the retrieval of transaction information.
 */
public class CommsByteBuffer extends JFapByteBuffer
{
   /** Class name for FFDC's */
   private static String CLASS_NAME = CommsByteBuffer.class.getName();

   /** Register Class with Trace Component */
   private static final TraceComponent tc = SibTr.register(CommsByteBuffer.class,
                                                           CommsConstants.MSG_GROUP,
                                                           CommsConstants.MSG_BUNDLE);

   /** The poolManager the byte buffer was allocated from */
   private CommsByteBufferPool poolManager = null;


   // ******************************************************************************************* //
   // Construction


   /**
    * Constructor.
    *
    * @param pool
    */
   public CommsByteBuffer(CommsByteBufferPool pool)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "<init>", pool);
      this.poolManager = pool;
      reset();
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "<init>");
   }


   // ******************************************************************************************* //
   // Methods used when putting into the buffer


   /**
    * This helper method can be used to put a short into the buffer when the value passed in is
    * locally available as an int. Note that any values passed in that are > Short.MAX_SHORT will
    * wrap.
    *
    * @param item
    */
   public synchronized void putShort(int item)
   {
      super.putShort((short) item);
   }

   /**
    * Puts a String into the byte buffer encoded in UTF8.
    *
    * @param item
    */
   public synchronized void putString(String item)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "putString", item);

      checkValid();

      // A String is presented by a BIT16 denoting the length followed by encoded bytes. If the
      // String is null, then a length of 1, followed by a single null byte (0x00) is written into
      // the buffer.
      if (item == null)
      {
         WsByteBuffer currentBuffer = getCurrentByteBuffer(3);
         currentBuffer.putShort((short) 1);
         currentBuffer.put(new byte[] { (byte) 0 });
      }
      else
      {
         byte[] stringAsBytes = item.getBytes(StandardCharsets.UTF_8);
         WsByteBuffer currentBuffer = getCurrentByteBuffer(2 + stringAsBytes.length);
         currentBuffer.putShort((short) stringAsBytes.length);
         currentBuffer.put(stringAsBytes);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "putString");
   }

   /**
    * Puts an SIDestinationAddress into the byte buffer.
    *
    * @param destAddr
    * @param fapLevel the FAP level of this connection. Used to decide what information to flow down the wire.
    */
   public synchronized void putSIDestinationAddress(SIDestinationAddress destAddr, short fapLevel)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "putSIDestinationAddress", new Object[]{destAddr, Short.valueOf(fapLevel)});

      checkValid();

      String destName = null;
      String busName = null;
      byte[] uuid = new byte[0];
      boolean localOnly = false;

      if (destAddr != null)
      {
         destName = destAddr.getDestinationName();
         busName = destAddr.getBusName();

         // If the user has passed in something that we do not know how to serialize, do not even
         // try and do anything with the other parts of it.
         if (destAddr instanceof JsDestinationAddress)
         {
            JsDestinationAddress jsDestAddr = (JsDestinationAddress) destAddr;

            // If the isMediation() flag has been set, ensure we propagate this as a special UUId.
            // We can do this because a mediation destination only carries a name and the UUId
            // field is actually redundant.
            
            //lohith liberty change
       /*     if (jsDestAddr.isFromMediation())
            {
               uuid = new byte[1];
               uuid[0] = CommsConstants.DESTADDR_ISFROMMEDIATION;
            }
            else*/
            {
               if (jsDestAddr.getME() != null) uuid = jsDestAddr.getME().toByteArray();
               localOnly = jsDestAddr.isLocalOnly();
            }
         }
      }

      putShort((short) uuid.length);
      if (uuid.length != 0) put(uuid);
      putString(destName);
      putString(busName);

      //Only send localOnly field if fapLevel >= 9 so we don't break down-level servers/client.
      if(fapLevel >= JFapChannelConstants.FAP_VERSION_9)
      {
         put(localOnly ? CommsConstants.TRUE_BYTE : CommsConstants.FALSE_BYTE);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "putSIDestinationAddress");
   }

   /**
    * Puts a SelectionCriteria object into the byte buffer.
    *
    * @param criteria
    */
   public synchronized void putSelectionCriteria(SelectionCriteria criteria)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "putSelectionCriteria", criteria);

      checkValid();

      String discriminator = null;
      String selector = null;
      short selectorDomain = (short) SelectorDomain.SIMESSAGE.toInt();

      if (criteria != null)
      {
         discriminator = criteria.getDiscriminator();
         selector = criteria.getSelectorString();

         SelectorDomain selDomain = criteria.getSelectorDomain();
         if (selDomain != null)
         {
            selectorDomain = (short) selDomain.toInt();
         }
      }

      putShort(selectorDomain);
      putString(discriminator);
      putString(selector);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "putSelectionCriteria");
   }

   /**
    * Puts an Xid into the Byte Buffer.
    *
    * @param xid
    */
   public synchronized void putXid(Xid xid)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "putXid", xid);

      putInt(xid.getFormatId());
      putInt(xid.getGlobalTransactionId().length);
      put(xid.getGlobalTransactionId());
      putInt(xid.getBranchQualifier().length);
      put(xid.getBranchQualifier());

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "putXid");
   }

   /**
    * Puts an SITransaction into the buffer
    *
    * @param transaction transaction to "serialize"
    */
   public synchronized void putSITransaction(SITransaction transaction)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "putSITransaction", transaction);
      Transaction commsTx = (Transaction)transaction;

      int flags = -1;

      if (transaction == null)
      {
         // No transaction - add "no transaction" flags to buffer.
        putInt(0x00000000);
      }
      else
      {
         // First we must search for any optimized transactions as they hold much more information
         // than the old-style ones.
         OptimizedTransaction optTx = null;

         // First see if the transaction is a global one. This could be both optimized or
         // unoptimized but the result is buried within it
         if (transaction instanceof SuspendableXAResource)
         {
            SIXAResource suspendableXARes = ((SuspendableXAResource) transaction).getCurrentXAResource();
            if (suspendableXARes instanceof OptimizedTransaction)
            {
               // The current XA Resource is indeed optimized
               optTx = (OptimizedTransaction) suspendableXARes;
            }
         }
         // Otherwise the actual transaction itself may be an optimized one - this is in the case
         // of an optimized local transaction.
         else if (transaction instanceof OptimizedTransaction)
         {
            optTx = (OptimizedTransaction) transaction;
         }

         // If we are optimized...
         if (optTx != null)
         {
            // Optimized transaction
            flags = CommsConstants.OPTIMIZED_TX_FLAGS_TRANSACTED_BIT;

            boolean local = optTx instanceof SIUncoordinatedTransaction;
            boolean addXid = false;
            boolean endPreviousUow = false;
            if (local)
            {
               flags |= CommsConstants.OPTIMIZED_TX_FLAGS_LOCAL_BIT;
            }
            if (!optTx.isServerTransactionCreated())
            {
               flags |= CommsConstants.OPTIMIZED_TX_FLAGS_CREATE_BIT;
               if (local && optTx.areSubordinatesAllowed())
                  flags |= CommsConstants.OPTIMIZED_TX_FLAGS_SUBORDINATES_ALLOWED;
               optTx.setServerTransactionCreated();
               addXid = !local;
            }
            if (addXid && (optTx.isEndRequired()))
            {
               flags |= CommsConstants.OPTIMIZED_TX_END_PREVIOUS_BIT;
               endPreviousUow = true;
            }
            putInt(flags);
            putInt(optTx.getCreatingConversationId());
            putInt(commsTx.getTransactionId());
            if (addXid)
            {
               if (endPreviousUow)
               {
                  putInt(optTx.getEndFlags());
                  optTx.setEndNotRequired();
               }
               putXid(new XidProxy(optTx.getXidForCurrentUow()));
            }
         }
         else
         {
            // This is an un-optimized transaction - simply append transaction ID.
            putInt(commsTx.getTransactionId());
         }
      }

      if (TraceComponent.isAnyTracingEnabled()) {
        int commsId = -1;
        if (commsTx != null)  commsId = commsTx.getTransactionId();
        CommsLightTrace.traceTransaction(tc, "PutTxnTrace", commsTx, commsId, flags);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "putSITransaction");
   }

   /**
    * Puts a message into the byte buffer using the <code>encodeFast()</code> method of encoding
    * messages. This method takes account of any <i>capabilities</i> negotiated at the point the
    * connection was established.
    * <p>This method is used for putting an entire message into the buffer. There are more efficient
    * ways of doing this to aid the Java memory manager that were introduced in FAP9. These
    * methods should be used in favour of this one. @see putDataSlice().
    *
    * @param message The message to encode.
    * @param commsConnection The comms connection which is passed to MFP.
    * @param conversation The conversation over which the encoded message data is to be transferred.
    *
    * @return Returns the message length.
    *
    * @exception MessageCopyFailedException
    * @exception IncorrectMessageTypeException
    * @exception MessageEncodeFailedException
    * @exception UnsupportedEncodingException
    * @exception SIConnectionDroppedException
    */
   public synchronized int putMessage(JsMessage message, CommsConnection commsConnection, Conversation conversation)
      throws MessageCopyFailedException,
             IncorrectMessageTypeException,
             MessageEncodeFailedException,
             UnsupportedEncodingException,
             SIConnectionDroppedException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "putMessage",
                                           new Object[]{message, commsConnection, conversation});

      int messageLength = putMessgeWithoutEncode(encodeFast(message, commsConnection, conversation));

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "putMessage", messageLength);
      return messageLength;
   }

   /**
    * This method is used to put a message into the buffer but assumes that the encode has already
    * been completed. This may be in the case where we would like to send the message in chunks but
    * we decide that the message would be better sent as an entire message.
    *
    * @param messageParts
    * @return Returns the size of the message that was put into the buffer.
    */
   public int putMessgeWithoutEncode(List<DataSlice> messageParts)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "putMessgeWithoutEncode", messageParts);

      int messageLength = 0;
      // Now we have a list of MessagePart objects. First work out the overall length.
      for (int x = 0; x < messageParts.size(); x++)
      {
         messageLength += messageParts.get(x).getLength();
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Message is " + messageLength + "byte(s) in length");

      // Write the length
      putLong(messageLength);

      // Now take the message parts and wrap them into byte buffers using the offset's supplied
      for (int x = 0; x < messageParts.size(); x++)
      {
         DataSlice messPart = messageParts.get(x);
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "DataSlice[" + x + "]: " +
                                                  "Array: " + Arrays.toString(messPart.getBytes()) + ", " +
                                                  "Offset: " + messPart.getOffset() + ", " +
                                                  "Length: " + messPart.getLength());

         wrap(messPart.getBytes(), messPart.getOffset(), messPart.getLength());
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "putMessgeWithoutEncode", messageLength);
      return messageLength;
   }

   /**
    * This method performs the <strong>exact</strong> same function as the <code>putMessage</code>
    * method except that any odd exceptions that occur during message encoding are wrapped in an
    * SIResourceException so that they can be thrown directly to the client Core SPI app.
    *
    * @param message
    * @param commsConnection
    * @param conversation
    *
    * @return Returns the message length.
    *
    * @throws SIConnectionDroppedException if the connection is dropped.
    * @throws SIResourceException if the message fails to be encoded.
    */
   public synchronized int putClientMessage(SIBusMessage message, CommsConnection commsConnection, Conversation conversation)
      throws SIConnectionDroppedException, SIResourceException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "putClientMessage",
                                           new Object[]{message, commsConnection, conversation});

      int msgLength = 0;
      try
      {
         msgLength = putMessage((JsMessage) message, commsConnection, conversation);
      }
      catch (UnsupportedEncodingException e)
      {
         FFDCFilter.processException(e, CLASS_NAME + ".putClientMessage",
                                     CommsConstants.COMMSBYTEBUFFER_PUTCLIENTMSG_01,
                                     this);

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Caught unsupported encoding exception", e);
         throw new SIResourceException(e);
      }
      catch (MessageCopyFailedException e)
      {
         FFDCFilter.processException(e, CLASS_NAME + ".putClientMessage",
                                     CommsConstants.COMMSBYTEBUFFER_PUTCLIENTMSG_02,
                                     this);

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Caught message copy failed exception", e);
         throw new SIResourceException(e);
      }
      catch (IncorrectMessageTypeException e)
      {
         FFDCFilter.processException(e, CLASS_NAME + ".putClientMessage",
                                     CommsConstants.COMMSBYTEBUFFER_PUTCLIENTMSG_03,
                                     this);

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Caught incorrect message type exception", e);
         throw new SIResourceException(e);
      }
      catch (MessageEncodeFailedException e)
      {
         FFDCFilter.processException(e, CLASS_NAME + ".putClientMessage",
                                     CommsConstants.COMMSBYTEBUFFER_PUTCLIENTMSG_04,
                                     this);

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Caught message encoding failed exception", e);
         throw new SIResourceException(e);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "putClientMessage", msgLength);
      return msgLength;
   }

   /**
    * This method is used to put a data slice into the buffer. A data slice is usually given to us
    * by MFP as part of a message and this method can be used to add a single slice into the buffer
    * so that the message can be sent in multiple transmissions. This is preferable to sending the
    * message in one job lot because we can allocate the memory required in smaller chunks making
    * life easier on the Java memory manager.
    *
    * @param slice The slice to add.
    */
   public synchronized void putDataSlice(DataSlice slice)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "putDataSlice", slice);

      // First pump in the length
      putInt(slice.getLength());

      // Now add in the payload
      wrap(slice.getBytes(), slice.getOffset(), slice.getLength());

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "putDataSlice");
   }

   /**
    * Puts the array of message handles into the buffer.
    *
    * @param siMsgHandles
    */
   public synchronized void putSIMessageHandles(SIMessageHandle[] siMsgHandles)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "putSIMessageHandles", siMsgHandles);

      putInt(siMsgHandles.length);

      for (int handleIndex = 0; handleIndex < siMsgHandles.length; ++handleIndex)
      {
         JsMessageHandle jsHandle = (JsMessageHandle)siMsgHandles[handleIndex];
         putLong(jsHandle.getSystemMessageValue());
         put(jsHandle.getSystemMessageSourceUuid().toByteArray());
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "message handle: "+siMsgHandles[handleIndex]);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "putSIMessageHandles");
   }

   /**
    * This method will fill a buffer list with WsByteBuffer's that when put together form a
    * packet describing an exception and it's linked exceptions. It will traverse down the cause
    * exceptions until one of them is null.
    *
    * @param throwable The exception to examine.
    * @param probeId The probe id (if any) associated with the top level exception.
    * @param conversation The conversation that the exception data will be sent over.  This is
    *                     used to determine the level of FAP being used - and hence how to
    *                     encode certain exception information.
    */
   public synchronized void putException(Throwable throwable, String probeId, Conversation conversation)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "putException",
                                           new Object[]{throwable, probeId, conversation});

      Throwable currentException = throwable;

      // First we need to work out how many exceptions to send back
      short numberOfExceptions = 0;
      while(currentException != null)
      {
         currentException = currentException.getCause();
         numberOfExceptions++;
      }

      // Now add them to the buffer
      currentException = throwable;

      // First put in the buffer how many exceptions are being sent back
      putShort(numberOfExceptions);

      // Now iterate over the rest
      while (currentException != null)
      {
         short exceptionId = getExceptionId(currentException);

         addException(currentException,
                      exceptionId,
                      probeId);

         // Now get the next one in the chain
         currentException = currentException.getCause();
         // Ensure we null out the probe - this doesn't apply for any more exceptions
         probeId = null;
      }

      final HandshakeProperties handshakeProperties = conversation.getHandshakeProperties();
      if ((handshakeProperties != null) &&
          ((CATHandshakeProperties)handshakeProperties).isFapLevelKnown())
      {
         // Only do FAP level checking if we know the FAP level - otherwise, assume a pre-FAP
         // 9 format for the exception flow.  We can find ourselves in a situation where we
         // don't know the FAP level if, for example, an exception is thrown during handshaking.

         final int fapLevel = ((CATHandshakeProperties)handshakeProperties).getFapLevel();
         if (fapLevel >= JFapChannelConstants.FAP_VERSION_9)
         {
            // At FAP version 9 or greater we transport the reason and inserts
            // of any exception which implements the Reasonable interface, or
            // inherits from SIException or SIErrorException.
            int reason = Reasonable.DEFAULT_REASON;
            String inserts[] = Reasonable.DEFAULT_INSERTS;
            if (throwable instanceof Reasonable)
            {
               reason = ((Reasonable)throwable).getExceptionReason();
               inserts = ((Reasonable)throwable).getExceptionInserts();
            }
            else if (throwable instanceof SIException)
            {
               reason = ((SIException)throwable).getExceptionReason();
               inserts = ((SIException)throwable).getExceptionInserts();
            }
            else if (throwable instanceof SIErrorException)
            {
               reason = ((SIErrorException)throwable).getExceptionReason();
               inserts = ((SIErrorException)throwable).getExceptionInserts();
            }

            putInt(reason);
            putShort(inserts.length);
            for (int i=0; i < inserts.length; ++i)
            {
               putString(inserts[i]);
            }
         }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "putException");
   }

   // Put a <String,String> Map object into the transmission buffer

   public void putMap (Map<String,String> map) {                                                       //SIB0163.comms.1
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "putMap", "map="+map);

     if (map != null) {
       final Set<String> keys = map.keySet();
       putShort(keys.size()); // Number of entries
       for (String k: keys) {
         putString(k);
         putString(map.get(k));
       }
     } else {
       putShort(0);
     }

     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "putMap");
   }                                                                                                   //SIB0163.comms.1

   // ******************************************************************************************* //
   // Methods used when getting from the buffer


   /**
    * Gets the remaining bytes from this buffer and returns them as a byte[].
    *
    * @return Returns the byte[]
    */
   public synchronized byte[] getRemaining()
   {
      return get(receivedBuffer.remaining());
   }

   /**
    * Reads a String from the current position in the byte buffer.
    *
    * @return Returns the String
    */
   public synchronized String getString()
   {
      checkReleased();

      String returningString = null;

      // Read the length in
      short stringLength = receivedBuffer.getShort();
      // Allocate the right amount of space for it
      byte[] stringBytes = new byte[stringLength];
      // And copy the data in
      receivedBuffer.get(stringBytes);

      // If the length is 1, and the byte is 0x00, then this is null - so do nothing
      if (stringLength == 1 && stringBytes[0] == 0)
      {
         // String is null...
      }
      else
      {
         returningString = new String(stringBytes, StandardCharsets.UTF_8);
      }

      return returningString;
   }

   /**
    * Reads an SIDestinationAddress from the current position in the byte buffer.
    *
    * @param fapLevel the FAP level of this connection. Used to decide what information has been flowed down the wire.
    *
    * @return Returns an SIDestinationAddress
    */
   public synchronized SIDestinationAddress getSIDestinationAddress(short fapLevel)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getSIDestinationAddress", Short.valueOf(fapLevel)); //469395

      checkReleased();

      boolean isFromMediation = false;

      /**************************************************************/
      /* Uuid                                                       */
      /**************************************************************/
      short uuidLength = getShort();                            // BIT16 Uuid Length
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Uuid length:", ""+uuidLength);
      SIBUuid8 uuid = null;
      // Note: In all other cases, a length of -1 would normally indicate a null value.
      //       However, in this case we use a length 0 to inidicate a null value, mainly
      //       because the uuid can not be 0 in length, and using 0 makes the code neater.
      if (uuidLength != 0)
      {
         if (uuidLength == 1)
         {
            byte addressFlags = get();
            if (addressFlags == CommsConstants.DESTADDR_ISFROMMEDIATION)
            {
               isFromMediation = true;
            }
         }
         else
         {
            byte[] uuidBytes = get(uuidLength);                 // BYTE[] Uuid
            uuid = new SIBUuid8(uuidBytes);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Uuid:", uuid);
         }
      }
      else
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Uuid was null");
      }

      /**************************************************************/
      /* Destination                                                */
      /**************************************************************/
      String destinationName = getString();
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Destination name:", destinationName);

      /**************************************************************/
      /* Bus name                                                   */
      /**************************************************************/
      String busName = getString();
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Bus name:", busName);


      /**************************************************************/
      /* Local only                                                 */
      /**************************************************************/
      //Only read from buffer if fap 9 or greater
      //Default value if not flown is false for backwards compatibility.
      boolean localOnly = false;

      if(fapLevel >= JFapChannelConstants.FAP_VERSION_9)
      {
         final byte localOnlyByte = get();
         localOnly = (localOnlyByte == CommsConstants.TRUE_BYTE);
      }

      if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "localOnly: ", localOnly);

      // If we get a null UUID, a null name and a null bus name, return null
      JsDestinationAddress destAddress = null;
      if (uuid == null && destinationName == null && busName == null)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Both UUID, destination name and bus name were null");
      }
      else
      {
    	  //lohith liberty change
   /*      if (isFromMediation)
         {
            destAddress =
               ((JsDestinationAddressFactory) JsDestinationAddressFactory.getInstance()).
                                             createJsMediationdestinationAddress(destinationName);
         }
         else*/
         {
            destAddress =
               ((JsDestinationAddressFactory) JsDestinationAddressFactory.getInstance()).
                                                   createJsDestinationAddress(destinationName,
                                                                              localOnly,
                                                                              uuid,
                                                                              busName);
         }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getSIDestinationAddress",destAddress); //469395
      return destAddress;
   }

   /**
    * Reads a SelectionCriteria from the current position in the byte buffer.
    *
    * @return Returns a SelectionCriteria
    */
   public synchronized SelectionCriteria getSelectionCriteria()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getSelectionCriteria"); //469395

      checkReleased();

      SelectorDomain selectorDomain = SelectorDomain.getSelectorDomain(getShort());
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Selector domain", selectorDomain);

      /**************************************************************/
      /* Destination                                                */
      /**************************************************************/
      String discriminator = getString();
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Discriminator:", discriminator);

      /**************************************************************/
      /* Destination                                                */
      /**************************************************************/
      String selector = getString();
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Selector:", selector);

      SelectionCriteria selectionCriteria =
    		  CommsClientServiceFacade.getSelectionCriteriaFactory().createSelectionCriteria(discriminator,
                                                                        selector,
                                                                        selectorDomain);



      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getSelectionCriteria"); //469395
      return selectionCriteria;
   }

   /**
    * Reads an Xid from the current position in the buffer.
    *
    * @return Returns an Xid
    */
   public synchronized Xid getXid()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "getXid");

      checkReleased();

      int formatId = getInt();
      int glidLength = getInt();
      byte[] globalTransactionId = get(glidLength);
      int blqfLength = getInt();
      byte[] branchQualifier = get(blqfLength);

      XidProxy xidProxy = new XidProxy(formatId, globalTransactionId, branchQualifier);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getXid", xidProxy);
      return xidProxy;
   }

   /**
    * Reads an SIBusMessage from the current position in the buffer.
    *
    * @param commsConnection
    *
    * @return Returns the message, or null if the message length indicated no message.
    *
    * @throws SIResourceException
    */
   public synchronized SIBusMessage getMessage(CommsConnection commsConnection)
      throws SIResourceException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "getMessage");

      checkReleased();

      SIBusMessage mess = null;
      // Now build a JsMessage from the returned data. Note that a message length of -1 indicates
      // that no message has been returned and a null value should be returned to the caller.

      // Get length of JsMessage
      int messageLen = (int) getLong();
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Message length", messageLen);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Remaining in buffer", receivedBuffer.remaining());

      if (messageLen > -1)
      {
         // Build Message from byte array. If the buffer is backed by an array we should simply
         // pass that into MFP rather than copying into a new byte[]
         try
         {
            if (receivedBuffer.hasArray())
            {
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Received buffer has a backing array");

               mess = JsMessageFactory.getInstance().createInboundJsMessage(receivedBuffer.array(),
                                                                            receivedBuffer.position() + receivedBuffer.arrayOffset(),
                                                                            messageLen,
                                                                            commsConnection);

               // Move the position to the end of the message so that any data after the message
               // can be got
               receivedBuffer.position(receivedBuffer.position() + messageLen);
            }
            else
            {
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Received buffer does NOT have a backing array");

               byte[] messageArray = get(messageLen);
               mess = JsMessageFactory.getInstance().createInboundJsMessage(messageArray,
                                                                            0,
                                                                            messageLen,
                                                                            commsConnection);
            }
         }
         catch (Exception e)
         {
            FFDCFilter.processException(e, CLASS_NAME + ".getMessage", CommsConstants.COMMSBYTEBUFFER_GETMESSAGE_01, this,
              new Object[] {"messageLen="+messageLen+", position="+receivedBuffer.position()+", limit="+receivedBuffer.limit() + " " +
                (receivedBuffer.hasArray()?"arrayOffset="+receivedBuffer.arrayOffset()+" array.length="+receivedBuffer.array().length:"no backing array")});

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            {
               SibTr.debug(this, tc, "Unable to create message", e);
               dump(this, tc, 100);
            }

            throw new SIResourceException(e);
         }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getMessage", mess);
      return mess;
   }

   /**
    * Gets a data slice from the buffer. The returned slice will be backed by the original byte
    * array (i.e. not copied) if the backing array maps to real (malloc'd) storage. Otherwise a
    * copy is taken of the data before it is passed into MFP.
    *
    * @return Returns the data slice.
    * @see #putDataSlice(DataSlice)
    */
   public synchronized DataSlice getDataSlice()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getDataSlice");

      DataSlice slice = null;

      // Get the slice length
      int sliceLength = getInt();
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Slice length: " + sliceLength);

      // Now we can create a new DataSlice object from the bytes in the buffer
      if (receivedBuffer.hasArray())
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Received buffer has a backing array");

         slice = new DataSlice(receivedBuffer.array(),
                               receivedBuffer.position() + receivedBuffer.arrayOffset(),
                               sliceLength);

         // Move the position to the end of the message so that any data after the message
         // can be got
         receivedBuffer.position(receivedBuffer.position() + sliceLength);
      }
      else
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Received buffer does NOT have a backing array");

         byte[] sliceArray = get(sliceLength);
         slice = new DataSlice(sliceArray,
                               0,
                               sliceLength);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getDataSlice", slice);
      return slice;
   }

   /**
    * Reads some message handles from the buffer.
    *
    * @return Returns a array of SIMessageHandle objects
    */
   public synchronized SIMessageHandle[] getSIMessageHandles()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getSIMessageHandles");

      int arrayCount = getInt();              // BIT32 ArrayCount
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "arrayCount", arrayCount);

      SIMessageHandle[] msgHandles = new SIMessageHandle[arrayCount];

      JsMessageHandleFactory jsMsgHandleFactory = JsMessageHandleFactory.getInstance();

      // Get arrayCount SIMessageHandles
      for (int msgHandleIndex = 0; msgHandleIndex < msgHandles.length; ++msgHandleIndex)
      {
         long msgHandleValue = getLong();
         byte[] msgHandleUuid = get(8);

         msgHandles[msgHandleIndex] =
            jsMsgHandleFactory.createJsMessageHandle(new SIBUuid8(msgHandleUuid),
                                                     msgHandleValue);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getSIMessageHandles", msgHandles);
      return msgHandles;
   }

   /**
    * When this buffer is representing the result of an exchange call (i.e. wrapping a JFap
    * ReceivedData object) this method can be used to determine what the outcome of the call was.
    * <p>
    * There are three possible outcomes:
    * <ol>
    *   <li>The returned segment type matches expectations e.g a SEND would expect
    *       a SEND_R handshake return
    *   <li>The returned segment type indicates an exception. The exeception number
    *       is the retrieved and returned.
    *   <li>There is an unexpected segment type. This implies an internal error where
    *       the communication with the server is out of step or a coding error.
    * </ol>
    * <p>
    * <strong>This method should only be called when this buffer represents received data.</strong>
    *
    * @param expectedSegmentType The expected segment type
    *
    * @return Returns the exception Id or SI_NO_EXCEPTION.
    */
   public synchronized short getCommandCompletionCode(int expectedSegmentType)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getCommandCompletionCode",
                                           new Object[] {""+expectedSegmentType});

      checkReleased();

      short result = -1;
      if (receivedData != null)
      {
         // First see what the segment returned was
         int receivedDataSegmentType = getReceivedDataSegmentType();

         // If it matches what we expect, the there was no exception
         if (receivedDataSegmentType == expectedSegmentType)
         {
            result = CommsConstants.SI_NO_EXCEPTION;
         }
         else if (receivedDataSegmentType == JFapChannelConstants.SEG_EXCEPTION)
         {
            getShort();                     // Skip over the exception count
            result = getShort();

            // Ensure we rewind the buffer (as we are only peeking a look here)
            receivedBuffer.rewind();
         }
         else
         {
            // Get the segment types in decimal and hex for ease of use
            String expected = expectedSegmentType + " (0x" + Integer.toHexString(expectedSegmentType).toUpperCase() + ")";
            String actual = receivedDataSegmentType + " (0x" + Integer.toHexString(receivedDataSegmentType).toUpperCase() + ")";

            throw new SIErrorException(
               TraceNLS.getFormattedMessage(CommsConstants.MSG_BUNDLE,
                                            "JFAP_SEG_MISMATCH_EXCEPTION_SICO1006",
                                            new Object[] {expected, actual}, null)
            );
         }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getCommandCompletionCode", ""+result);
      return result;
   }

   /**
    * This method will check the return status of a remote XA call. An XA call can
    * return in two ways:
    * <ul>
    *   <li>It can return with SI_NOEXCEPTION. In which case the call was successful and the data
    *       buffer probably contains reply information. As such, if this occurs this method will
    *       return without looking further into the buffer.
    *   <li>It can return something else. In which case we assume the call has failed and the
    *       buffer only contains details about the exception. Here we will parse the buffer for
    *       the exception message, probe id and reason code (if those properties are there).
    *       Then, an XA exception will be thrown.
    * </ul>
    * <p>
    * This method also checks that the data we have received was what we are expecting. As such,
    * if the segment in the reply does not match what we expect, a comms exception will be thrown.
    * <p>
    * <strong>This method should only be called when this buffer represents received data.</strong>
    *
    * @param expected      The segment type that we are expecting in the reply.
    *
    * @param conversation  The conversation that the XA command completion data was received over.
    *                      In the case where a XAException is built from the data, this information
    *                      is used to determine the FAP version - and hence the encoding of the
    *                      exception.
    *
    * @throws XAException An exception.
    * @throws Exception   It is possible that something else went wrong on the server side and
    *                     another exception was sent back. In this case, we should throw this.
    */
   public synchronized void checkXACommandCompletionStatus(int expected, Conversation conversation)
      throws XAException, Exception
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "checkXACommandCompletionStatus", new Object[]{expected, conversation});

      checkReleased();

      if (receivedData != null)
      {
         // First get the completion code
         int exceptionType = getCommandCompletionCode(expected);

         // If this indicates an exception, then create it and throw it
         if (exceptionType != CommsConstants.SI_NO_EXCEPTION)
         {
            throw getException(conversation);
         }

         // Otherwise there was no exception
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "No exception");
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "checkXACommandCompletionStatus");
   }

   /**
    * This method is the main method for recreating an exception that has been received from our
    * peer, usually as a result of an exchange call. This method will determine the exception that
    * was sent and will recreate any chains of linked exceptions that were seen on the server.
    * <p>
    * This method expects that the exception data starts at the current buffer position.
    * <p>
    * <strong>This method should only be called when this buffer represents received data.</strong>
    *
    * @param conversation  the conversation that the data has been received over.  This is used
    *                      to determine the FAP level being used - and hence the encoding for
    *                      the exception.
    *
    * @return Returns the exception with any linked exceptions linked to it.
    */
   public synchronized Exception getException(Conversation conversation)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getException", conversation);

      checkReleased();

      // The top level exception we want to return at the end
      Exception topException = null;

      if (receivedBuffer != null)
      {
         // Read the number of exceptions returned
         short numberOfExceptions = getShort();

         // A handle on the last exception in the chain that new ones should link to
         Exception link = null;

         for (int x = 0; x < numberOfExceptions; x++)
         {
            short exceptionId = getShort();
            Exception e = parseSingleException(exceptionId);

            // Is this the first exception? If so, save it away in its proper place and save the
            // handle of the exception to place the link the next time (also the top level exception)
            if (topException == null)
            {
               topException = e;
               link = topException;
            }
            // Otherwise, link the exception we just got with the last one we just got
            else
            {
               link.initCause(e);
               link = e;
            }
         }
      }

      final HandshakeProperties handshakeProperties = conversation.getHandshakeProperties();
      if ((handshakeProperties != null) &&
          ((CATHandshakeProperties)handshakeProperties).isFapLevelKnown())
      {
         // If we don't know hte FAP level, at this point in time, then assume a pre-FAP 9
         // version.  This could happen if an exception is thrown during handshaking.

         final int fapLevel = ((CATHandshakeProperties)handshakeProperties).getFapLevel();
         if (fapLevel >= JFapChannelConstants.FAP_VERSION_9)
         {
            // At FAP version 9 or greater, exceptions are encoded to include a reason
            // and inserts.  Retrieve these.
            int reason = getInt();
            String[] inserts = Reasonable.DEFAULT_INSERTS;
            int numberOfInserts = getShort();
            if (numberOfInserts > 0)
            {
               inserts = new String[numberOfInserts];
               for (int i=0; i < numberOfInserts; ++i)
               {
                  inserts[i] = getString();
               }
            }

            // Stuff reason and inserts back into the chain of exceptions
            setReasonableInformation(topException, reason, inserts);
         }
      }

      if (TraceComponent.isAnyTracingEnabled() && topException != null) {
        CommsLightTrace.traceException(tc, topException);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getException", topException);
      return topException;
   }

   /**
    * Sets Reasonable information into a chain of exceptions.  This is done by scanning
    * down the chain of exceptions looking for exceptions that implement the Reasonable
    * interface (or have either the SIException or SIErrorException in their inheritence
    * hierarchy) and also have setters for reason or inserts.
    *
    * @param exception  the top level exception in a chain of one or more exceptions, to set
    *                   the Reasonable information into.
    *
    * @param reason  the reason information to set into the first exception which implements
    *                the Reasonable interface (or is one of a handful of other exception types)
    *                and has a setExceptionReason(int) method.  Note that if a value of
    *                Reasonable.DEFAULT_REASON is specified then no attempt will be made to set
    *                a reason value into any exception.
    *
    * @param inserts  the inserts information to set into the first exception which implements
    *                 the Reasonable interface (or is one of a handful of other exception types)
    *                 and has a setExceptionInserts(String[]) method.  Note that if a value of
    *                 Reasonable.DEFAULT_INSERTS is specified then no attempt will be made to set
    *                 a inserts value into any exception.
    */
   private void setReasonableInformation(Exception exception, int reason, String[] inserts)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setReasonableInformation", new Object[]{exception, reason, inserts});
      boolean injectedReason = reason == Reasonable.DEFAULT_REASON;
      boolean injectedInserts = inserts == Reasonable.DEFAULT_INSERTS;
      Throwable nextThrowable = exception;


      while ((nextThrowable != null) &&
             (!injectedReason || !injectedInserts))
      {
         if ((nextThrowable instanceof Reasonable) ||
             (nextThrowable instanceof SIException) ||
             (nextThrowable instanceof SIErrorException))
         {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Attempting to add reasonable information for exception of type: "+nextThrowable.getClass());
            if (!injectedReason)
            {
               try
               {
                  Method setReasonMethod =
                     nextThrowable.getClass().getMethod("setExceptionReason", new Class<?>[]{int.class});

                  setReasonMethod.invoke(nextThrowable, new Object[]{reason});

                  injectedReason = true;
               }
               catch(NoSuchMethodException e)
               {
                  // No FFDC code needed.
               }
               catch (Exception e)
               {
                  FFDCFilter.processException(e, CLASS_NAME + ".setReasonableInformation",
                                              CommsConstants.COMMSBYTEBUFFER_SETREASONABLE_01,
                                              this);
                  if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, e);
               }
            }

            if (!injectedInserts)
            {
               try
               {
                  Method setInsertsMethod =
                     nextThrowable.getClass().getMethod("setExceptionInserts", new Class<?>[]{String[].class});

                  setInsertsMethod.invoke(nextThrowable, new Object[]{inserts});

                  injectedInserts = true;
               }
               catch(NoSuchMethodException e)
               {
                  // No FFDC code needed.
               }
               catch (Exception e)
               {
                  FFDCFilter.processException(e, CLASS_NAME + ".setReasonableInformation",
                                              CommsConstants.COMMSBYTEBUFFER_SETREASONABLE_02,
                                              this);
                  if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, e);
               }
            }
         }
         else
         {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Exception ("+exception+") is not a type which can have a reason or inserts set on it.");
         }

         nextThrowable = nextThrowable.getCause();
      }
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setReasonableInformation");
   }

   /**
    * When this buffer is being used to wrapper a JFap ReceivedData object, this method will return
    * the segment type that was returned with the data.
    * <p>
    * This method will return -1 if this is called on a CommsByteBuffer instance that was not
    * created from a ReceivedData instance.
    *
    * @return Returns the segment type of any received data.
    */
   public synchronized int getReceivedDataSegmentType()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "getReceivedDataSegmentType");

      checkReleased();

      int segmentType = -1;
      if (receivedData != null)
      {
         segmentType = receivedData.getSegmentType();
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getReceivedDataSegmentType", segmentType);
      return segmentType;
   }

   /**
    * This method will peek at the next 4 bytes in the buffer and return the result as an int.
    * The buffer position will be unchanged by calling this method.
    *
    * @return Returns the next four bytes as an int.
    */
   public synchronized int peekInt()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "peekInt");

      checkReleased();

      int result = 0;
      if (receivedBuffer != null)
      {
         int currentPosition = receivedBuffer.position();
         result = receivedBuffer.getInt();
         receivedBuffer.position(currentPosition);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "peekInt", result);
      return result;
   }

   /**
    * This method will peek at the next 8 bytes in the buffer and return the result as a long.
    * The buffer position will be unchanged by calling this method.
    *
    * @return Returns the next eight bytes as a long.
    */
   public synchronized long peekLong()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "peekLong");

      checkReleased();

      long result = 0;
      if (receivedBuffer != null)
      {
         int currentPosition = receivedBuffer.position();
         result = receivedBuffer.getLong();
         receivedBuffer.position(currentPosition);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "peekLong", result);
      return result;
   }

   /**
    * Skips over the specified number of bytes in the received byte buffer.
    *
    * @param lengthToSkip
    */
   public synchronized void skip(int lengthToSkip)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "skip", lengthToSkip);

      checkReleased();

      if (receivedBuffer != null)
      {
         receivedBuffer.position(receivedBuffer.position() + lengthToSkip);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "skip");
   }

   /**
    * Rewinds the buffer to the beginning of the received data so that the data can be re-read.
    */
   public synchronized void rewind()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "rewind");

      checkReleased();

      if (receivedBuffer != null)
      {
         receivedBuffer.rewind();
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "rewind");
   }

   /**
    * This method will release any storage held by any of the byte buffers. It will also mark
    * itself as released so that any subsequent attempts to get from it will cause an
    * SIErrorException to be thrown.
    */
   public synchronized void release()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "release");

      super.release();

      if (poolManager != null)
      {
         poolManager.release(this);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "release");
   }

   /**
    * This method can be used to free the comms byte buffer but not free'ing the actual underlying
    * WsByteBuffer storage. This method should be used when this buffer is holding a message that
    * has been passed to the application or in the case where pooled storage was not used for data
    * received.
    *
    * @param freeBuffers Set to true is equivalent to calling <code>release()</code>. Set to false
    *                    will cause the underlying buffer not to get repooled (but everything else
    *                    will be).
    */
   public synchronized void release(boolean freeBuffers)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "release", freeBuffers);

      if (freeBuffers)
      {
         release();
      }
      else
      {
         // Otherwise, just release but preserve the buffers
         super.releasePreservingBuffers();

         if (poolManager != null)
         {
            poolManager.release(this);
         }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "release");
   }


   // ******************************************************************************************* //
   // Public helper methods


   /**
    * This method actually does the fast encode on a JsMessage message.
    *
    * @param message The message to encode
    * @param commsConnection
    * @param conversation The conversation that the encoded message will be sent
    *
    * @return Returns a list of MessagePart objects.
    *
    * @throws SIConnectionDroppedException if the connection has gone at the time of encode
    * @throws IncorrectMessageTypeException
    * @throws MessageCopyFailedException
    * @throws MessageEncodeFailedException
    * @throws UnsupportedEncodingException
    */
   public List<DataSlice> encodeFast(AbstractMessage message, CommsConnection commsConnection, Conversation conversation)
      throws MessageEncodeFailedException, SIConnectionDroppedException,
             IncorrectMessageTypeException, UnsupportedEncodingException, MessageCopyFailedException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "encodeFast",
                                           new Object[]{CommsLightTrace.msgToString(message), commsConnection, CommsLightTrace.minimalToString(conversation)});

      if(!message.isControlMessage())
      {
        // Determine capabilities negotiated at handshake time.
        short clientCapabilities = ((CATHandshakeProperties) conversation.getHandshakeProperties()).getCapabilites();
        boolean requiresJMF = (clientCapabilities & CommsConstants.CAPABILITIY_REQUIRES_JMF_ENCODING) != 0;
        boolean requiresJMS = (clientCapabilities & CommsConstants.CAPABILITIY_REQUIRES_JMS_MESSAGES) != 0;

        if (requiresJMF || requiresJMS)
        {
           // If the client requires that we only send it JMS messages, convert the message.
           // As we can only transcribe JMS messages to a JMF encoding - if the client requires
           // a JMF encoded message then also apply this conversion.
           message = ((JsMessage) message).makeInboundJmsMessage();
        }
        if (requiresJMF)
        {
           // If the client requires that we only send it JMF encoded messages, perform the
           // appropriate transcription.  Note: this assumes the message is a JMS message.
           message = ((JsMessage) message).transcribeToJmf();
        }
      }

      List<DataSlice> messageParts = null;
      try
      {
         messageParts = message.encodeFast(commsConnection);
      }
      catch (MessageEncodeFailedException e)
      {
         // No FFDC Code Needed
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Caught a MessageEncodeFailedException from MFP:", e);

         // We interrogate the MessageEncodeFailedException to see if it hides a problem which we want to reflect back to our caller
         // using a different exception

         if (e.getCause() != null) {
           if (e.getCause() instanceof SIConnectionDroppedException) { // Was the connection dropped under MFP?
             throw new SIConnectionDroppedException(TraceNLS.getFormattedMessage(CommsConstants.MSG_BUNDLE, "CONVERSATION_CLOSED_SICO0065", null, null));
           } else if (e.getCause() instanceof IllegalStateException) { // An IllegalStateException may indicate a dropped connection
             if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "The linked exception IS an IllegalStateException");

             if (conversation.isClosed()) {
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "The conversation was closed - rethrowing as SIConnectionDroppedException");
               throw new SIConnectionDroppedException(TraceNLS.getFormattedMessage(CommsConstants.MSG_BUNDLE, "CONVERSATION_CLOSED_SICO0065", null, null));
             }
           }
         }

         throw e;
      }

      if (TraceComponent.isAnyTracingEnabled())
        CommsLightTrace.traceMessageId(tc, "EncodeMsgTrace", message);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "encodeFast", messageParts);
      return messageParts;
   }


   // ******************************************************************************************* //
   // Private helper methods


   /**
    * This method will take a buffer with it's position pointing to the start of a single exception
    * and parse it. Once the exception has been parsed and recreated, it is returned (not thrown)
    * to the caller. Note this method only parses one exception.
    * <p>
    * An exception buffer can contain up to three types of information:
    * <ul>
    *   <li>An exception message.
    *   <li>A probe ID.
    *   <li>A reason code.
    * </ul>
    * <p>
    * The buffer may contain no information. In which case the appropriate exception will
    * be thrown with no exception message.
    * <p>
    * If a message is sent back then that will be used in the constructor of the appropriate
    * exception.
    * <p>
    * If a probe ID is sent back then the message is turned into a SICO8008 message that
    * describes the error and displays the probe ID. The probe ID is the same as the one that
    * is thrown in the Server FFDC record.
    * <p>
    * A reason code is generally only sent back with XA exceptions. If one is present for
    * an XAException it is set into the exception. Otherwise it is ignored.
    *
    * @param exceptionType The exception type.
    *
    * @return Returns an appropriate exception.
    */
   private Exception parseSingleException(int exceptionType)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "parseSingleException");

      short dataId = 0;

      // Now first, get all the info about the exception
      int reasonCode = 0;
      String probeId = null;
      String message = "";

      short numberOfDataItems = getShort();
      Exception returningException = null;

      for (int x = 0; x < numberOfDataItems; x++)
      {
         // Get the data ID
         dataId = getShort();

         switch (dataId)
         {
            case (CommsConstants.DATAID_EXCEPTION_MESSAGE):

               message = getString();

               break;
            case (CommsConstants.DATAID_EXCEPTION_PROBEID):

               probeId = getString();

               break;
            case (CommsConstants.DATAID_EXCEPTION_REASON):

               getShort();                  // Skip over the reason code length (will always be 4)
               reasonCode = getInt();

               break;
            default:

               break;
         }
      }

      // Add the probe Id to the message if there was one
      if (probeId != null)
      {
         message = TraceNLS.getFormattedMessage(CommsConstants.MSG_BUNDLE,
                                                "CORE_EXCEPTION_SICO8007",
                                                new Object[] { message, probeId }, null);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      {
         SibTr.debug(tc, "Exception Id, Message, ProbeID, Reason",
                     new Object[] {""+exceptionType, message, probeId, ""+reasonCode});
      }

      // Now throw the right exception
      switch (exceptionType)
      {
         case (CommsConstants.EXCEPTION_XAEXCEPTION):
            returningException = new XAException(message);
            ((XAException) returningException).errorCode = reasonCode;
            break;

         case (CommsConstants.SI_INCORRECT_CALL_EXCEPTION):
            returningException = new SIIncorrectCallException(message);
            break;

         case (CommsConstants.SI_INVALID_DESTINATION_PREFIX_EXCEPTION):
            returningException = new SIInvalidDestinationPrefixException(message);
            break;

         case (CommsConstants.SI_DISCRIMINATOR_SYNTAX_EXCEPTION):
            returningException =new SIDiscriminatorSyntaxException(message);
            break;

         case (CommsConstants.SI_SELECTOR_SYNTAX_EXCEPTION):
            returningException = new SISelectorSyntaxException(message);
            break;

         case (CommsConstants.SI_INSUFFICIENT_DATA_FOR_FACT_EXCEPTION):
            returningException = new SIInsufficientDataForFactoryTypeException(message);
            break;

         case (CommsConstants.SI_AUTHENTICATION_EXCEPTION):
            returningException = new SIAuthenticationException(message);
            break;

         case (CommsConstants.SI_NOT_POSSIBLE_IN_CUR_CONFIG_EXCEPTION):
            returningException = new SINotPossibleInCurrentConfigurationException(message);
            break;

         case (CommsConstants.SI_NOT_AUTHORISED_EXCEPTION):
            returningException = new SINotAuthorizedException(message);
            break;

         case (CommsConstants.SI_SESSION_UNAVAILABLE_EXCEPTION):
            returningException = new SISessionUnavailableException(message);
            break;

         case (CommsConstants.SI_SESSION_DROPPED_EXCEPTION):
            returningException = new SISessionDroppedException(message);
            break;

         case (CommsConstants.SI_DURSUB_ALREADY_EXISTS_EXCEPTION):
            returningException = new SIDurableSubscriptionAlreadyExistsException(message);
            break;

         case (CommsConstants.SI_DURSUB_MISMATCH_EXCEPTION):
            returningException = new SIDurableSubscriptionMismatchException(message);
            break;

         case (CommsConstants.SI_DURSUB_NOT_FOUND_EXCEPTION):
            returningException = new SIDurableSubscriptionNotFoundException(message);
            break;

         case (CommsConstants.SI_CONNECTION_UNAVAILABLE_EXCEPTION):
            returningException = new SIConnectionUnavailableException(message);
            break;

         case (CommsConstants.SI_CONNECTION_DROPPED_EXCEPTION):
            returningException = new SIConnectionDroppedException(message);
            break;

         case (CommsConstants.SI_DATAGRAPH_FORMAT_MISMATCH_EXCEPTION):
            returningException = new SIDataGraphFormatMismatchException(message);
            break;

         case (CommsConstants.SI_DATAGRAPH_SCHEMA_NOT_FOUND_EXCEPTION):
            returningException = new SIDataGraphSchemaNotFoundException(message);
            break;

         case (CommsConstants.SI_DESTINATION_LOCKED_EXCEPTION):
            returningException = new SIDestinationLockedException(message);
            break;

         case (CommsConstants.SI_TEMPORARY_DEST_NOT_FOUND_EXCEPTION):
            returningException = new SITemporaryDestinationNotFoundException(message);
            break;

         case (CommsConstants.SI_MESSAGE_EXCEPTION):
            returningException = new SIMessageException(message);
            break;

         case (CommsConstants.SI_RESOURCE_EXCEPTION):
            returningException = new SIResourceException(message);
            break;

         case (CommsConstants.SI_LIMIT_EXCEEDED_EXCEPTION):
            returningException = new SILimitExceededException(message);
            break;

         case (CommsConstants.SI_CONNECTION_LOST_EXCEPTION):
            returningException = new SIConnectionLostException(message);
            break;

         case (CommsConstants.SI_ROLLBACK_EXCEPTION):
            returningException = new SIRollbackException(message);
            break;

         case (CommsConstants.SI_NOT_SUPPORTED_EXCEPTION):
            returningException = new SINotSupportedException(message);
            break;

         case (CommsConstants.SI_MSG_DOMAIN_NOT_SUPPORTED_EXCEPTION):
            returningException = new SIMessageDomainNotSupportedException(message);
            break;

         case (CommsConstants.SI_DATAGRAPH_EXCEPTION):
            returningException = new SIDataGraphException(message);
            break;

         case (CommsConstants.SI_MESSAGE_NOT_LOCKED_EXCEPTION):
            returningException = new CommsSIMessageNotLockedException(message);
            break;

         case (CommsConstants.SI_ERROR_EXCEPTION):
            returningException = new SIErrorException(message);
            break;

         case (CommsConstants.SI_COMMAND_INVOCATION_FAILED_EXCEPTION):
            returningException = new SICommandInvocationFailedException(message);
            break;

         default:

            // Otherwise, we have no choice but to throw the runtime 'please raise me a PMR'
            // exception. Nice
            message = TraceNLS.getFormattedMessage(CommsConstants.MSG_BUNDLE,
                                                   "UNKNOWN_CORE_EXCP_SICO8002",
                                                   new Object[] {message}, null);
            returningException = new SIErrorException(message);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "parseSingleException");
      return returningException;
   }

   // TODO This needs to be fully implemented
   static class CommsSIMessageNotLockedException extends SIMessageNotLockedException
   {
      public static final long serialVersionUID = 1913042103164814385L;

      public CommsSIMessageNotLockedException(String msg)
      {
         super(msg);
      }
      public SIMessageHandle[] getUnlockedMessages()
      {
         // Return an empty array for now
         return new SIMessageHandle[0];
      }
   }

   /**
    * Adds a single exception to this buffer.
    * <p>
    * The exception information will contain the following information:
    * <ul>
    *   <li>Every exception will have an exception type and a text message that goes
    *       with it. The exception id is always flowed back. The exception message
    *       will be returned providing it is not null.
    *   <li>If the exception had an associated FFDC record generated, then we will also
    *       flow the probe id of the exception.
    *   <li>If the exception also has a return code such as an XAException, this will
    *       also be returned.
    * </ul>
    * <p>
    * The data returned will always consist of an exception id and then 0 to all of the other
    * parts.
    *
    * @param e             The exception to send back to the client.
    * @param exceptionId   The ID of the exception. This is so that we do not have to do an instanceof
    *                      to work out what the exception is every time.
    * @param probeId       FFDC Prode Id (may be null if no FFDC was created).
    */
   private void addException(Throwable e, short exceptionId, String probeId)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "addException",
                                           new Object[]{e, exceptionId, probeId});


      // First make the exception message if there was one. If it was an internal error, add the
      // toString() to the message as this will indicate what the exception was. Otherwise, just
      // add the message.
      String exceptionMessage = null;
      if (exceptionId == CommsConstants.EXCEPTION_INTERNAL_ERROR)
      {
         exceptionMessage = e.toString();
      }
      else
      {
         exceptionMessage = e.getMessage();
      }

      // Work out the data items that will be returned
      short noOfDataItemsReturned = 0;
      if (exceptionMessage != null) noOfDataItemsReturned++;
      if (probeId != null) noOfDataItemsReturned++;
      if (e instanceof XAException) noOfDataItemsReturned++;

      // Now add the exception Id and number of data items on top of the list of this load of
      // exceptions
      putShort(exceptionId);
      putShort(noOfDataItemsReturned);

      if (exceptionMessage != null)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Adding message: " + exceptionMessage);
         putShort(CommsConstants.DATAID_EXCEPTION_MESSAGE);
         putString(exceptionMessage);
      }

      // Now add the probe ID if there was one
      if (probeId != null)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Adding probe ID: " + probeId);
         putShort(CommsConstants.DATAID_EXCEPTION_PROBEID);
         putString(probeId);
      }

      // If the exception is an XAException we need to return the reason code to
      if (e instanceof XAException)
      {
         int reasonCode = ((XAException) e).errorCode;
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Adding reason code: " + reasonCode);

         putShort(CommsConstants.DATAID_EXCEPTION_REASON);
         putShort(4);
         putInt(reasonCode);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "addException");
   }

   /**
    * This method will work out what JFap exception Id we should flow across the wire when sending
    * an exception back. For all defined SI exceptions we will resolve them into their JFap constant
    * value. For any other exception we will return EXCEPTION_INTERNAL_ERROR.
    * <p>
    * Care should be taken in adding exceptions to this method. They need to be checked for in the
    * correct order as instanceof will match an instance to it's class and any of it's parents.
    * Therefore, check the lowest level first, then go up the tree.
    *
    * @param exception
    *
    * @return Returns the JFap exception Id for the exception passed in
    */
   private short getExceptionId(Throwable exception)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "getExceptionId");

      short exceptionId = CommsConstants.EXCEPTION_INTERNAL_ERROR;

      // ******** Do the SIIncorrectExcecption branch and all its subclasses
      if (exception instanceof SIInvalidDestinationPrefixException)
         exceptionId = CommsConstants.SI_INVALID_DESTINATION_PREFIX_EXCEPTION;

      else if (exception instanceof SIDiscriminatorSyntaxException)
         exceptionId = CommsConstants.SI_DISCRIMINATOR_SYNTAX_EXCEPTION;

      else if (exception instanceof SISelectorSyntaxException)
         exceptionId = CommsConstants.SI_SELECTOR_SYNTAX_EXCEPTION;

      else if (exception instanceof SIInsufficientDataForFactoryTypeException)
         exceptionId = CommsConstants.SI_INSUFFICIENT_DATA_FOR_FACT_EXCEPTION;

      else if (exception instanceof SIIncorrectCallException)
         exceptionId = CommsConstants.SI_INCORRECT_CALL_EXCEPTION;

      // ******** Now the SIAuthenticationException branch
      else if (exception instanceof SIAuthenticationException)
         exceptionId = CommsConstants.SI_AUTHENTICATION_EXCEPTION;

      // ******** Now the SINotPossibleInCurrentConfigurationException branch
      else if (exception instanceof SINotAuthorizedException)
         exceptionId = CommsConstants.SI_NOT_AUTHORISED_EXCEPTION;

      else if (exception instanceof SINotPossibleInCurrentConfigurationException)
         exceptionId = CommsConstants.SI_NOT_POSSIBLE_IN_CUR_CONFIG_EXCEPTION;

      // ******** Now the SINotPossibleInCurrentStateException branch
      else if (exception instanceof SISessionDroppedException)
         exceptionId = CommsConstants.SI_SESSION_DROPPED_EXCEPTION;

      else if (exception instanceof SISessionUnavailableException)
         exceptionId = CommsConstants.SI_SESSION_UNAVAILABLE_EXCEPTION;

      else if (exception instanceof SIDurableSubscriptionAlreadyExistsException)
         exceptionId = CommsConstants.SI_DURSUB_ALREADY_EXISTS_EXCEPTION;

      else if (exception instanceof SIDurableSubscriptionMismatchException)
         exceptionId = CommsConstants.SI_DURSUB_MISMATCH_EXCEPTION;

      else if (exception instanceof SIDurableSubscriptionNotFoundException)
         exceptionId = CommsConstants.SI_DURSUB_NOT_FOUND_EXCEPTION;

      else if (exception instanceof SIConnectionDroppedException)
         exceptionId = CommsConstants.SI_CONNECTION_DROPPED_EXCEPTION;

      else if (exception instanceof SIConnectionUnavailableException)
         exceptionId = CommsConstants.SI_CONNECTION_UNAVAILABLE_EXCEPTION;

      else if (exception instanceof SIDataGraphFormatMismatchException)
         exceptionId = CommsConstants.SI_DATAGRAPH_FORMAT_MISMATCH_EXCEPTION;

      else if (exception instanceof SIDataGraphSchemaNotFoundException)
         exceptionId = CommsConstants.SI_DATAGRAPH_SCHEMA_NOT_FOUND_EXCEPTION;

      else if (exception instanceof SIDestinationLockedException)
         exceptionId = CommsConstants.SI_DESTINATION_LOCKED_EXCEPTION;

      else if (exception instanceof SITemporaryDestinationNotFoundException)
         exceptionId = CommsConstants.SI_TEMPORARY_DEST_NOT_FOUND_EXCEPTION;

      // ******** Now the SIMessageException branch
      else if (exception instanceof SIMessageException)
         exceptionId = CommsConstants.SI_MESSAGE_EXCEPTION;

      // ******** Now the SIResourceException branch
      else if (exception instanceof SILimitExceededException)
         exceptionId = CommsConstants.SI_LIMIT_EXCEEDED_EXCEPTION;

      else if (exception instanceof SIConnectionLostException)
         exceptionId = CommsConstants.SI_CONNECTION_LOST_EXCEPTION;

      else if (exception instanceof SIRollbackException)
         exceptionId = CommsConstants.SI_ROLLBACK_EXCEPTION;

      else if (exception instanceof SIResourceException)
         exceptionId = CommsConstants.SI_RESOURCE_EXCEPTION;

      // ******** Now the SINotSupportedException branch
      else if (exception instanceof SIMessageDomainNotSupportedException)
         exceptionId = CommsConstants.SI_MSG_DOMAIN_NOT_SUPPORTED_EXCEPTION;

      else if (exception instanceof SINotSupportedException)
         exceptionId = CommsConstants.SI_NOT_SUPPORTED_EXCEPTION;

      // ******** Now the SIDataGraphException
      else if (exception instanceof SIDataGraphException)
         exceptionId = CommsConstants.SI_DATAGRAPH_EXCEPTION;

      // ******** Now the SICommandInvocationFailedException
      else if (exception instanceof SICommandInvocationFailedException)
         exceptionId = CommsConstants.SI_COMMAND_INVOCATION_FAILED_EXCEPTION;

      // ******** Now the SIMessageNotLockedException
      else if (exception instanceof SIMessageNotLockedException)
         exceptionId = CommsConstants.SI_MESSAGE_NOT_LOCKED_EXCEPTION;

      // ******** And the runtime one
      else if (exception instanceof SIErrorException)
         exceptionId = CommsConstants.SI_ERROR_EXCEPTION;

      // ******** Now the top level SIException
      else if (exception instanceof SIException)
         exceptionId = CommsConstants.SI_EXCEPTION;

      // Don't forget XA
      else if (exception instanceof XAException)
         exceptionId = CommsConstants.EXCEPTION_XAEXCEPTION;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getExceptionId", ""+exceptionId);
      return exceptionId;
   }

   // This method returns a <String,String> Map object built from the recived transmission buffer

   public Map<String,String> getMap () {                                                               //SIB0163.comms.1
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getMap");

     final short len = getShort();

     final Map<String,String> rc = new HashMap<String,String>();
     for (int i=0; i < len; i++) {
       rc.put(getString(),getString());
     }

     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getMap","rc="+rc);
     return rc;
   }                                                                                                   //SIB0163.comms.1

   /**
    * Calculates the length in bytes of a String when it is placed inside a CommsByteBuffer via the putString method.
    * A null String can be passed into this method.
    *
    * @param s the String to calculate the length of.
    * @return the number of bytes that the String will take up when encoded.
    */
   public static int calculateEncodedStringLength(String s)
   {
      if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "calculateEncodedStringLength", s);

      final int length;

      if(s == null)
      {
         length = 3;
      }
      else
      {
         final byte[] stringAsBytes = s.getBytes(StandardCharsets.UTF_8);
         length = stringAsBytes.length + 2;
      }

      if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "calculateEncodedStringLength", Integer.valueOf(length));
      return length;
   }
   
   /**
    * Write a boolean value to the buffer.
    * 
    * @param b the boolean value to write.
    */
   public synchronized void putBoolean(final boolean b)
   {
      put(b ? CommsConstants.TRUE_BYTE : CommsConstants.FALSE_BYTE);
   }
   
   /**
    * Returns the next value in the buffer interpreted as a boolean. Should only be called when
    * the next byte in the buffer was written by a call to putBoolean.
    * 
    * @return the next value in the buffer interpreted as a boolean.
    */
   public synchronized boolean getBoolean()
   {
      final byte value = get();
      if(value == CommsConstants.TRUE_BYTE) return true;
      else if(value == CommsConstants.FALSE_BYTE) return false;
      else throw new IllegalStateException("Unexpected byte: " + value);     
   }  
}
