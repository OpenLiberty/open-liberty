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
package com.ibm.ws.sib.comms.server.clientsupport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.ws.sib.utils.DataSlice;
import com.ibm.wsspi.sib.core.DestinationType;
import com.ibm.wsspi.sib.core.OrderingContext;
import com.ibm.wsspi.sib.core.ProducerSession;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.SITransaction;

/**
 * Class that encapsulates a chunked message that is currently being assembled.
 * 
 * @author Gareth Matthews
 */
public class ChunkedMessageWrapper
{
   private ArrayList<DataSlice> slices = new ArrayList<DataSlice>();
   private SITransaction transaction = null;
   private ProducerSession producerSession = null;
   private SICoreConnection connection = null;
   private DestinationType destinationType = null;
   private SIDestinationAddress destinationAddress = null;
   private OrderingContext orderingContext = null;
   private String alternateUser = null;
   private int reason = 0;
   private String[] messageInserts = null;
   private enum Mode { PRODUCER_SEND, CONN_SEND, CONN_SEND_TO_EXCEPTION };
   private Mode state = null;
   
   /**
    * Constructor for when doing a ProducerSession.send()
    * 
    * @param transaction
    * @param producerSession
    */
   public ChunkedMessageWrapper(SITransaction transaction, ProducerSession producerSession)
   {
      this.transaction = transaction;
      this.producerSession = producerSession;
      this.state = Mode.PRODUCER_SEND;
   }
   
   /**
    * Constructor for when doing an SICoreConnection.send()
    * 
    * @param transaction
    * @param connection
    * @param destinationType
    * @param destinationAddress
    * @param orderingContext
    * @param alternateUser
    */
   public ChunkedMessageWrapper(SITransaction transaction, SICoreConnection connection, 
                                DestinationType destinationType, 
                                SIDestinationAddress destinationAddress, 
                                OrderingContext orderingContext, String alternateUser)
   {
      this.transaction = transaction;
      this.connection = connection;
      this.destinationType = destinationType;
      this.destinationAddress = destinationAddress;
      this.orderingContext = orderingContext;
      this.alternateUser = alternateUser;
      this.state = Mode.CONN_SEND;
   }
   
   /**
    * Constructor for when doing an SICoreConnection.sendToExceptionDestination()
    * 
    * @param transaction
    * @param connection
    * @param destinationAddress
    * @param reason
    * @param alternateUser
    * @param inserts
    */
   public ChunkedMessageWrapper(SITransaction transaction, SICoreConnection connection, 
                                SIDestinationAddress destinationAddress, 
                                int reason, String alternateUser, String[] inserts)
   {
      this.transaction = transaction;
      this.connection = connection;
      this.destinationAddress = destinationAddress;
      this.reason = reason;
      this.alternateUser = alternateUser;
      this.messageInserts = inserts;
      this.state = Mode.CONN_SEND_TO_EXCEPTION;
   }

   /**
    * @return Returns the producerSession.
    */
   public ProducerSession getProducerSession()
   {
      return producerSession;
   }

   /**
    * @return Returns the slices.
    */
   public List<DataSlice> getMessageData()
   {
      return slices;
   }

   /**
    * @param slice The slice to add.
    */
   public void addDataSlice(DataSlice slice)
   {
      slices.add(slice);
   }

   /**
    * @return Returns the transaction.
    */
   public SITransaction getTransaction()
   {
      return transaction;
   }
   
   /**
    * @return Returns the connection.
    */
   public SICoreConnection getConnection()
   {
      return connection;
   }

   /**
    * @return Returns the destinationAddress.
    */
   public SIDestinationAddress getDestinationAddress()
   {
      return destinationAddress;
   }

   /**
    * @return Returns the destinationType.
    */
   public DestinationType getDestinationType()
   {
      return destinationType;
   }

   /**
    * @return Returns the orderingContext.
    */
   public OrderingContext getOrderingContext()
   {
      return orderingContext;
   }

   /**
    * @return Returns the alternateUser.
    */
   public String getAlternateUser()
   {
      return alternateUser;
   }
   
   /**
    * @return Returns the messageInserts.
    */
   public String[] getMessageInserts()
   {
      return messageInserts;
   }

   /**
    * @return Returns the reason.
    */
   public int getReason()
   {
      return reason;
   }

   /**
    * @return Returns the total length of all slices.
    */
   public int getTotalMessageLength()
   {
      int msgLength = 0;
      for (DataSlice slice : slices)
      {
         msgLength += slice.getLength();
      }
      return msgLength;
   }
   
   /**
    * @return Returns information about this object.
    * 
    * @see java.lang.Object#toString()
    */
   public String toString()
   {
      String toString = "ChunkedMessageWrapper@" + Integer.toHexString(System.identityHashCode(this)) + 
                        " [Mode=" + state + "]: ";
      
      if (state == Mode.PRODUCER_SEND)
      {
         toString += "{ transaction=" + transaction + ", producerSession=" + producerSession + " }";
      }
      else if (state == Mode.CONN_SEND)
      {
         toString += "{ transaction=" + transaction + ", producerSession=" + producerSession + 
                     ", destinationType=" + destinationType + ", destinationAddress=" + destinationAddress +
                     ", orderingContext=" + orderingContext + ", alternateUser=" + alternateUser + " }";
      }
      else if (state == Mode.CONN_SEND_TO_EXCEPTION)
      {
         toString += "{ transaction=" + transaction + ", producerSession=" + producerSession + 
                     ", destinationAddress=" + destinationAddress + ", reason=" + reason +
                     ", alternateUser=" + alternateUser + ", inserts=" + Arrays.toString(messageInserts) + " }";
      }
      
      return toString;
   }
}
