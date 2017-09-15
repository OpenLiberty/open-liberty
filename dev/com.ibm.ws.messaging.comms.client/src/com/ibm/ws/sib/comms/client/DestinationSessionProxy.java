/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.client;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.common.CommsByteBuffer;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.DestinationSession;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;
import com.ibm.wsspi.sib.core.exception.SISessionUnavailableException;

/**
 * This class sits at the top of the hierachy of consumers and producers. It is declared
 * abstract as it is never used on its own but is declared to expose the common functionality
 * between consumers and producers.
 * 
 * @author Gareth Matthews
 */
public abstract class DestinationSessionProxy extends Proxy implements DestinationSession
{
   /** Register Class with Trace Component */
   private static final TraceComponent tc = SibTr.register(DestinationSessionProxy.class, 
                                                           CommsConstants.MSG_GROUP, 
                                                           CommsConstants.MSG_BUNDLE);
   
   /** The NLS reference */
   private static final TraceNLS nls = TraceNLS.getTraceNLS(CommsConstants.MSG_BUNDLE);
   
   /** The destination address of this session */
   protected SIDestinationAddress destinationAddress = null;                  // F195720.3
      
   /** Log Source code level on static load of class */
   static 
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Source info: @(#)SIB/ws/code/sib.comms.client.impl/src/com/ibm/ws/sib/comms/client/DestinationSessionProxy.java, SIB.comms, WASX.SIB, uu1215.01 1.19");
   }

   /**
    * Constructor.
    * 
    * @param con
    * @param cp
    */
   public DestinationSessionProxy(Conversation con, ConnectionProxy cp)
   {
      super(con, cp);
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
   }
   
   /**
    * This method should be implemented by any subclasses to perform any consumer
    * specific closing.
    * 
    * @throws com.ibm.websphere.sib.exception.SIResourceException
    * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
    * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
    * @throws com.ibm.websphere.sib.exception.SIErrorException
    */
   public abstract void close() throws SIResourceException, 
                                       SIConnectionLostException,
                                       SIConnectionDroppedException, 
                                       SIErrorException;


   /**
    * Returns the SICoreConnection which created this Session.
    * 
    * @return SICoreConnection.
    * 
    * @throws com.ibm.wsspi.sib.core.exception.SISessionUnavailableException
    * @throws com.ibm.wsspi.sib.core.exception.SISessionDroppedException
    * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
    * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
    */
   public SICoreConnection getConnection() 
      throws SISessionUnavailableException, SISessionDroppedException,
             SIConnectionUnavailableException, SIConnectionDroppedException
 
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getConnection");
      
      checkAlreadyClosed();
      ConnectionProxy conn = getConnectionProxy();
      conn.checkAlreadyClosed();
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getConnection", conn);
      return conn;
   }
   
   /**
    * Helper method to check if this session is closed and throws the appropriate
    * exception if it is.
    * 
    * @throws SISessionUnavailableException if the session has been closed.
    */
   protected void checkAlreadyClosed() throws SISessionUnavailableException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "checkAlreadyClosed");
      
      if (isClosed())
         throw new SISessionUnavailableException(
            nls.getFormattedMessage("SESSION_CLOSED_SICO1013", null, null)
         );
         
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "checkAlreadyClosed");
   }
   
   /**
    * This method will return the destination address of the destination that this
    * session is currently attached to.
    * 
    * @return SIDestinationAddress
    */
   public SIDestinationAddress getDestinationAddress()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getDestinationAddress");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getDestinationAddress", destinationAddress);
      return destinationAddress;
   }

   /**
    * A protected method that is used to set the destination address of this session.
    * This is generally called by subclasses who will discover the address when they
    * create the consumer session.
    * 
    * @param destinationAddress
    */
   protected void setDestinationAddress(SIDestinationAddress destinationAddress)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setDestinationAddress", destinationAddress);
      
      this.destinationAddress = destinationAddress;
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setDestinationAddress");
   }
   
   /**
    * This method is used to take the data the is sent back on reply to a session
    * create call and stash the useful data away. The data that is sent back consists
    * of the session id and the destination address we are attached to.
    * <p>
    * The buffer passed in must be positioned at the front of the data that refers
    * to this session.
    * 
    * @param buf
    */
   protected void inflateData(CommsByteBuffer buf)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "inflateData", buf);
      
      // f196076 removed buf.flip()
      this.setProxyID(buf.getShort());
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Session ID", ""+getProxyID());
      
      // Get the destination address and save it away with the session
      
      // Did they send back an address?
      if (buf.hasRemaining())
      {
         setDestinationAddress(buf.getSIDestinationAddress(getConversation().getHandshakeProperties().getFapLevel()));
      }
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "inflateData");
   }   
}
