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

package com.ibm.ws.sib.processor;

import java.util.Map;

import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.mfp.JsDestinationAddress;
import com.ibm.ws.sib.msgstore.ItemStream;
import com.ibm.ws.sib.transactions.mpspecific.MSSIXAResourceProvider;
import com.ibm.wsspi.sib.core.BrowserSession;
import com.ibm.wsspi.sib.core.ConsumerSession;
import com.ibm.wsspi.sib.core.DestinationType;
import com.ibm.wsspi.sib.core.OrderingContext;
import com.ibm.wsspi.sib.core.ProducerSession;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.SITransaction;
import com.ibm.wsspi.sib.core.SelectionCriteria;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SIDestinationLockedException;
import com.ibm.wsspi.sib.core.exception.SIDiscriminatorSyntaxException;
import com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionAlreadyExistsException;
import com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionMismatchException;
import com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionNotFoundException;
import com.ibm.wsspi.sib.core.exception.SILimitExceededException;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;
import com.ibm.wsspi.sib.core.exception.SITemporaryDestinationNotFoundException;
import com.ibm.wsspi.sib.core.ConsumerSetChangeCallback;

/**
 * Internal extended SICoreConnection interface
 */
public interface MPCoreConnection extends SICoreConnection, MSSIXAResourceProvider {
  

  /**
   * This method accepts the same arguments as the Core SPI equivalent
   * and returns the same class objects. The difference is that this
   * method will only work against system destinations. If a non-system
   * destination is supplied an SIDestinationWrongTypeException is thrown.
   * 
   * @see com.ibm.wsspi.sib.core.SICoreConnection#createBrowserSession(
   *        SIDestinationAddress,
   *        DestinationType,
   *        String,
   *        String)
   * 
   * @param destAddress
   * @param destType
   * @param criteria
   * @return A new BrowserSession
   *
   * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
   * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
   * @throws com.ibm.websphere.sib.exception.SIResourceException
   * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
   * @throws com.ibm.wsspi.sib.core.exception.SILimitExceededException
   * @throws com.ibm.websphere.sib.exception.SIErrorException
   * @throws com.ibm.wsspi.sib.core.exception.SINotAuthorizedException
   * @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
   * @throws com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException
   */
  public BrowserSession createSystemBrowserSession(
      SIDestinationAddress destAddress,
      DestinationType destType,
      SelectionCriteria criteria)
  throws SIConnectionUnavailableException, SIConnectionDroppedException,
         SIResourceException, SIConnectionLostException, SILimitExceededException, 
         SIErrorException,
         SINotAuthorizedException,
         SIIncorrectCallException,
         SINotPossibleInCurrentConfigurationException;

  /**
   * This method accepts the same arguments as the Core SPI equivalent
   * and returns the same class objects. The difference is that this
   * method will only work against system destinations. If a non-system
   * destination is supplied an SIDestinationWrongTypeException is thrown.
   * 
   * @see com.ibm.wsspi.sib.core.SICoreConnection#createConsumerSession(
   *        SIDestinationAddress,
   *        DestinationType,
   *        String,
   *        String,
   *        Reliability,
   *        boolean,
   *        boolean,
   *        Reliability)
   * 
   * @param destAddress
   * @param destType
   * @param criteria
   * @param reliability
   * @param enableReadAhead
   * @param nolocal
   * @param unrecoverableReliability
   * @return  A new ConsumerSession
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
   @throws com.ibm.websphere.sib.exception.SIResourceException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
   @throws com.ibm.wsspi.sib.core.exception.SILimitExceededException
   @throws com.ibm.websphere.sib.exception.SIErrorException
   @throws com.ibm.wsspi.sib.core.exception.SINotAuthorizedException
   @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
   @throws com.ibm.wsspi.sib.core.exception.SIDestinationLockedException
   @throws com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException
   */
  public ConsumerSession createSystemConsumerSession( 
      SIDestinationAddress destAddress,
      DestinationType destType,
      SelectionCriteria criteria,
      Reliability reliability,
      boolean enableReadAhead,
      boolean nolocal,
      Reliability unrecoverableReliability,
      boolean bifurcatable)
  throws SIConnectionUnavailableException, SIConnectionDroppedException,
         SIResourceException, SIConnectionLostException, SILimitExceededException, 
         SIErrorException,
         SINotAuthorizedException,
         SIIncorrectCallException,
         SIDestinationLockedException,
         SINotPossibleInCurrentConfigurationException;

	/**
	 * This method accepts the same arguments as the Core SPI equivalent
	 * and returns the same class objects. The difference is that this
	 * method will only work against system destinations. If a non-system
	 * destination is supplied an SIDestinationWrongTypeException is thrown.
	 * 
	 * @see com.ibm.wsspi.sib.core.SICoreConnection#createProducerSession( 
	 *      SIDestinationAddress,
	 *      String,
	 *      DestinationType,
	 *      OrderingContext,
	 *      boolean)
	 * 
	 * @param destAddr
	 * @param discriminator
	 * @param destType
	 * @param context
	 * @return A new ProducerSession to the given destinationAddress
	 * 
	 * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
	 * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
	 * @throws com.ibm.websphere.sib.exception.SIResourceException
	 * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
	 * @throws com.ibm.wsspi.sib.core.exception.SILimitExceededException
	 * @throws com.ibm.websphere.sib.exception.SIErrorException
	 * @throws com.ibm.wsspi.sib.core.exception.SINotAuthorizedException
	 * @throws com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException
	 * @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
	 */
	public ProducerSession createSystemProducerSession(
	  SIDestinationAddress destAddress,
	  String discriminator, 
	  DestinationType destFilter, 
	  OrderingContext context, 
          String alternateUser)   
   throws SIConnectionUnavailableException, SIConnectionDroppedException,
		   SIResourceException, SIConnectionLostException, SILimitExceededException, 
		   SIErrorException,
		   SINotAuthorizedException,
		   SINotPossibleInCurrentConfigurationException,
		   SIIncorrectCallException;
		                    
        /**
         * This method overloads the above method with the ability to specify
         * if PubSub fingerprints should be stripped from any sent messages or
         * not.
         * 
         * If in doubt, set to true!
         * 
         * See overloaded Core SPI method for further details on remaining options
         */
        public ProducerSession createSystemProducerSession(
          SIDestinationAddress destAddress,
          String discriminator, 
          DestinationType destFilter, 
          OrderingContext context, 
          String alternateUser,
          boolean clearPubSubFingerprints)   
   throws SIConnectionUnavailableException, SIConnectionDroppedException,
                   SIResourceException, SIConnectionLostException, SILimitExceededException, 
                   SIErrorException,
                   SINotAuthorizedException,
                   SINotPossibleInCurrentConfigurationException,
                   SIIncorrectCallException;
                                    
        /** 
         * An extended version of SICoreConnection.createProducerSession
         * This method overloads that method with the ability to specify
         * if PubSub fingerprints should be stripped from any sent messages or
         * not.
         * 
         * If in doubt, set to true!
         * 
         * See overloaded Core SPI method for further details on remaining options
         */
        public ProducerSession createProducerSession(
            SIDestinationAddress destAddr,
            String discriminator,
            DestinationType destType,
            OrderingContext extendedMessageOrderingContext,
            String alternateUser,
            boolean fixedMessagePoint,
            boolean preferLocalMessagePoint,
            boolean clearPubSubFingerprints)
      throws SIConnectionUnavailableException, SIConnectionDroppedException,
            SIResourceException, SIConnectionLostException, SILimitExceededException,
            SINotAuthorizedException,
            SINotPossibleInCurrentConfigurationException,
            SITemporaryDestinationNotFoundException,
            SIIncorrectCallException, SIDiscriminatorSyntaxException;
        
  /**
   * This method accepts the same arguments as the Core SPI equivalent
   * and returns the same class objects. The difference is that this
   * method will only work against system destinations. If a non-system
   * destination is supplied an SIDestinationWrongTypeException is thrown.
   * 
   * @see com.ibm.wsspi.sib.core.SICoreConnection#receiveNoWait(
   *      SITransaction,
   *      Reliability,
   *      SIDestinationAddress,
   *      DestinationType,
   *      String,
   *      String,
   *      Reliability)
   * 
   * @param tran
   * @param unrecoverableReliability
   * @param destAddress
   * @param destType
   * @param criteria
   * @param reliability
   * @return null if no message received
   * 
   * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
   * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
   * @throws com.ibm.websphere.sib.exception.SIResourceException
   * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
   * @throws com.ibm.wsspi.sib.core.exception.SILimitExceededException
   * @throws com.ibm.websphere.sib.exception.SIErrorException
   * @throws com.ibm.wsspi.sib.core.exception.SINotAuthorizedException
   * @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
   * @throws com.ibm.wsspi.sib.core.exception.SIDestinationLockedException
   * @throws com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException
   */
  public SIBusMessage systemReceiveNoWait(
      SITransaction tran,
      Reliability unrecoverableReliability,
      SIDestinationAddress destAddress,
      DestinationType destType,
      SelectionCriteria criteria,
      Reliability reliability)
  throws SIConnectionUnavailableException, SIConnectionDroppedException,
         SIResourceException, SIConnectionLostException, SILimitExceededException, 
         SIErrorException,
         SINotAuthorizedException,
         SIIncorrectCallException,
         SIDestinationLockedException,
         SINotPossibleInCurrentConfigurationException;
           
  /**
   * This method accepts the same arguments as the Core SPI equivalent
   * and returns the same class objects. The difference is that this
   * method will only work against system destinations. If a non-system
   * destination is supplied an SIDestinationWrongTypeException is thrown.
   * 
   * @see com.ibm.wsspi.sib.core.SICoreConnection#receiveWithWait(
   *      SITransaction,
   *      Reliability,
   *      SIDestinationAddress,
   *      DestinationType,
   *      String,
   *      String,
   *      Reliability,
   *      long)
   * 
   * @param tran
   * @param unrecoverableReliability
   * @param destAddress
   * @param destType
   * @param criteria
   * @param reliability
   * @param timeout
   * @return null if no message available.
   * 
   * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
   * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
   * @throws com.ibm.websphere.sib.exception.SIResourceException
   * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
   * @throws com.ibm.wsspi.sib.core.exception.SILimitExceededException
   * @throws com.ibm.websphere.sib.exception.SIErrorException
   * @throws com.ibm.wsspi.sib.core.exception.SINotAuthorizedException
   * @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
   * @throws com.ibm.wsspi.sib.core.exception.SIDestinationLockedException
   * @throws com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException
   */
  public SIBusMessage systemReceiveWithWait(
      SITransaction tran,
      Reliability unrecoverableReliability,
      SIDestinationAddress destAddress,
      DestinationType destType,
      SelectionCriteria criteria,
      Reliability reliability,
      long timeout)
  throws SIConnectionUnavailableException, SIConnectionDroppedException,
         SIResourceException, SIConnectionLostException, SILimitExceededException, 
         SIErrorException,
         SINotAuthorizedException,
         SIIncorrectCallException,
         SIDestinationLockedException,
         SINotPossibleInCurrentConfigurationException;
  
  
  /**
   * This method accepts the same arguments as the Core SPI equivalent
   * and returns the same class objects. The difference is that this
   * method will only work against system destinations. If a non-system
   * destination is supplied an SIDestinationWrongTypeException is thrown.
   * 
   * @see com.ibm.wsspi.sib.core.SICoreConnection#send(
   *      SIBusMessage,
   *      SITransaction,
   *      SIDestinationAddress,
   *      DestinationType,
   *      OrderingContext,
   *      boolean)
   * 
   * @param msg
   * @param tran
   * @param destAddr
   * @param destType
   * 
   * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
   * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
   * @throws com.ibm.websphere.sib.exception.SIResourceException
   * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
   * @throws com.ibm.wsspi.sib.core.exception.SILimitExceededException
   * @throws com.ibm.websphere.sib.exception.SIErrorException
   * @throws com.ibm.wsspi.sib.core.exception.SINotAuthorizedException
   * @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
   * @throws com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException
   */
  public void systemSend(
  	SIBusMessage msg,
  	SITransaction tran,
  	SIDestinationAddress destAddr,
    DestinationType destType,
    OrderingContext context,
    boolean keepSecurityContext)
  throws SIConnectionUnavailableException, SIConnectionDroppedException,
         SIResourceException, SIConnectionLostException, SILimitExceededException, 
         SIErrorException,
         SINotAuthorizedException,
         SIIncorrectCallException,
         SINotPossibleInCurrentConfigurationException;


  /**
   * Creates a System Destination using the given prefix.
   * 
   * The Prefix is limited to a maximum of 24 characters (facilitates MQ
   * interoperability by keeping the name less than 48 characters) and
   * must be unique within the ME, otherwise an
   * SIDestinationAlreadyExistsException is returned.
   * The prefix can contain the characters a-z, A-Z, 0-9, ., /, and %.
   * 
   * @param prefix
   * @return A DestinationAddress representing this system destination
   */
  public JsDestinationAddress createSystemDestination(String prefix)
          throws SIException;

  /**
   * Deletes a System Destination. Any messages on the destination at this
   * time are discarded.
   * 
   * @param destAddr
   * @throws SINotPossibleInCurrentConfigurationException
   * @throws SIException
   */
  public void deleteSystemDestination(JsDestinationAddress destAddr)
      throws SINotPossibleInCurrentConfigurationException,
        SIException;
        
  /**
   * Deletes a System Destination. Any messages on the destination at this
   * time are discarded.
   * 
   * @param prefix
   * @throws SINotPossibleInCurrentConfigurationException
   * @throws SICoreException
   */
  public void deleteSystemDestination(String prefix)
      throws SINotPossibleInCurrentConfigurationException,
        SIException;        

  /**
   * Creates a ConsumerSession for use by MQ InterOperation. There are 2 unique 
   * features required by MQInterOp: 
   * (1) Ability for create consumer with a forward scanning cursor. This consumer 
   * will only see message once. It is possible that some messages are missed by this
   * consumer if the messages are not available (locked/deleted) at the time that
   * the consumer searches for a message.
   * (2) Create a consumer that can consume messages from a temporary destination
   * that was created under another Connection. This is required for compatibility
   * with MA88 where the transaction scope is per connection and multiple connections
   * may be used.
   * 
   * @see com.ibm.wsspi.sib.core.SICoreConnection#createConsumerSession(
   *       SIDestinationAddress,
   *       DestinationType,
   *       String,
   *       String,
   *       Reliability,
   *       boolean,
   *       boolean,
   *       Reliability)
   * 
   * @param destAddress
   * @param destType
   * @param criteria
   * @param reliability
   * @param enableReadAhead
   * @param nolocal
   * @param unrecoverableReliability
   * @param bifurcatable
   * @param alternateUser
   * @param forwardScanning
   * @param system true if the creating application knows that it's attaching to a system destination.
   * @return A new ConsumerSession
   * 
   * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
   * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
   * @throws com.ibm.websphere.sib.exception.SIResourceException
   * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
   * @throws com.ibm.wsspi.sib.core.exception.SILimitExceededException
   * @throws com.ibm.websphere.sib.exception.SIErrorException
   * @throws com.ibm.wsspi.sib.core.exception.SINotAuthorizedException
   * @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
   * @throws com.ibm.wsspi.sib.core.exception.SIDestinationLockedException
   * @throws com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException
   */
	public ConsumerSession createMQInterOpConsumerSession(
	    SIDestinationAddress destAddress,
		DestinationType destinationType, 
		SelectionCriteria criteria, 
		Reliability reliability, 
		boolean enableReadAhead, 
		boolean nolocal, 
		Reliability unrecoverableReliability,
    boolean bifurcatable, 
    String alternateUser,
    boolean forwardScanning,
    boolean system) 
  	throws SIConnectionDroppedException, 
		   SIConnectionUnavailableException, 
		   SIConnectionLostException, 
		   SILimitExceededException, 
		   SINotAuthorizedException, 
		   SIDestinationLockedException, 
		   SITemporaryDestinationNotFoundException, 
		   SIResourceException, 
		   SIErrorException, 
		   SIIncorrectCallException, 
		   SINotPossibleInCurrentConfigurationException;
  
  /**
   * @see com.ibm.wsspi.sib.core.SICoreConnection#createConsumerSessionForDurableSubscription()
   * 
   * (1) Ability for create consumer with a forward scanning cursor. This consumer 
   * will only see message once. It is possible that some messages are missed by this
   * consumer if the messages are not available (locked/deleted) at the time that
   * the consumer searches for a message.
   */     
  public ConsumerSession createMQInterOpConsumerSessionForDurableSubscription(String subscriptionName,
                                                              String durableSubscriptionHome,
                                                              SIDestinationAddress destinationAddress,
                                                              SelectionCriteria criteria,
                                                              boolean supportsMultipleConsumers,
                                                              boolean nolocal,
                                                              Reliability reliability,
                                                              boolean enableReadAhead,
                                                              Reliability unrecoverableReliability,
                                                              boolean bifurcatable,
                                                              String alternateUser,
                                                              boolean forwardScanning)
  throws SIConnectionUnavailableException, SIConnectionDroppedException,
         SIResourceException, SIConnectionLostException, SILimitExceededException, 
         SINotAuthorizedException,
         SIIncorrectCallException,
         SIDurableSubscriptionNotFoundException, SIDurableSubscriptionMismatchException,
         SIDestinationLockedException;
		   
  /**
   * Indicates if messages should be copied when they are produced using
   * this connection. Default is true.
   * 
   * @param copied true if messages should be copied
   */
  public void setMessageCopiedWhenSent(boolean copied);  

  /**
   * Indicates if messages should be copied when they consumed using
   * this connection. Default is true.
   * 
   * @param copied true if messages should be copied
   */
  public void setMessageCopiedWhenReceived(boolean copied);  
  
  /**
   * Indicates if messages should have there waitTime set before
   * consumption. 
   * 
   * @param setWaitTIme true if messages should have waitTIme set
   */
  public void setSetWaitTimeInMessage(boolean setWaitTime);  
  
  /**
   * This method accepts the same arguments as the Core SPI equivalent
   * and returns the same class objects. The difference is that this
   * method will only work against MQLink destinations. If a non-mqlink
   * destination is supplied an SIDestinationWrongTypeException is thrown.
   * 
   * @param mqLinkUuid
   * @param criteria
   * @param unrecoverableReliability
   * @return A new ConsumerSession
   * 
   * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
   * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
   * @throws com.ibm.websphere.sib.exception.SIResourceException
   * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
   * @throws com.ibm.wsspi.sib.core.exception.SILimitExceededException
   * @throws com.ibm.websphere.sib.exception.SIErrorException
   * @throws com.ibm.wsspi.sib.core.exception.SINotAuthorizedException
   * @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
   * @throws com.ibm.wsspi.sib.core.exception.SIDestinationLockedException
   * @throws com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException
   */
  public ConsumerSession createMQLinkConsumerSession(   
      String mqLinkUuid,
      SelectionCriteria criteria,
      Reliability unrecoverableReliability)
  throws SIConnectionUnavailableException, SIConnectionDroppedException,
         SIResourceException, SIConnectionLostException, SILimitExceededException, 
         SIErrorException,
         SINotAuthorizedException,
         SIIncorrectCallException,
         SIDestinationLockedException,
         SINotPossibleInCurrentConfigurationException;
           
  /**
   * Retrieves the MQLink's PubSubBridge ItemStream
   * 
   * @param mqLinkUuid of the MQLink 
   */
  public ItemStream getMQLinkPubSubBridgeItemStream(String mqLinkUuid)
      throws SIException;
     
  /**
   * Method that returns true if the bus is secure
   */
  public boolean isBusSecure();
  
  /**
   * Returns true if Multicast is enabled on this Messaging Engine
   */
  public boolean isMulticastEnabled();
  
  /**
   * Returns the MulticastProperties for this messaging engine.
   * null is returned when multicast is not enabled.
   */
  public MulticastProperties getMulticastProperties();
  
  /**
   * An extended version of SICoreConnection.createDurableSubscription which includes
   * a list of selection criteria and a map for user data (these abilities are also available
   * independently from the create, see below)
   *
   * Use of either of the above two parameters is only valid for locally homed subscriptions
   */
  public void createDurableSubscription( 
    String subscriptionName,
    String durableSubscriptionHome,
    SIDestinationAddress destinationAddress,
    SelectionCriteria[] criteriaList,
    boolean supportsMultipleConsumers,
    boolean nolocal,
    String alternateUser,
    Map userData)
    throws SIConnectionUnavailableException, SIConnectionDroppedException,
           SIResourceException, SIConnectionLostException, SILimitExceededException, 
           SIErrorException,
           SINotAuthorizedException,
           SIIncorrectCallException,
           SINotPossibleInCurrentConfigurationException,
           SIDurableSubscriptionAlreadyExistsException;

  /** 
   * An extended version of SICoreConnection.createConsumerSessionForDurableSubscription
   * which allows the ConsumerSession to be created for the DurableSubscription identified
   * by the subscriptionName. The DurableSubscription must be local for this method to be used
   * otherwise an SIDurableSubscriptionNotFoundException will be returned
   */
  public ConsumerSession createConsumerSessionForDurableSubscription(String subscriptionName, 
                                                                     boolean enableReadAhead, 
                                                                     Reliability unrecoverableReliability, 
                                                                     boolean bifurcatable )
 
  throws SIConnectionUnavailableException, SIConnectionDroppedException,
         SIErrorException,
         SIDurableSubscriptionNotFoundException, SIDurableSubscriptionMismatchException,
         SIDestinationLockedException, SIIncorrectCallException, SIResourceException;
 
  /** 
   * Retrieve the MPSubscription object that represents the named durable subscription
   *
   * This function is only available on locally homed subscriptions
  **/
  public MPSubscription getSubscription(String subscriptionName)
  throws SIDurableSubscriptionNotFoundException;
         
  /**
   * Register a CommandHandler to be used on subsequent calls to
   * invokeCommand() with the specified key
   * 
   *
   * @param key            A String identifying the requestor
   * @param commandHandler The commandHandler which will process the commands    
   *
   */
  public void registerCommandHandler( String key, CommandHandler handler);
  
  /**
   Deregisters a previously registered callback.
  */ 
  public void deregisterConsumerSetMonitor(
    ConsumerSetChangeCallback callback)
  throws SINotPossibleInCurrentConfigurationException;  

  /**
   Deletes a durable subscription that was created using 
   createDurableSubscription. This method should replace the current SICoreConnection
   createDurableSubscription call as it correctly takes an alternateUser
   parameter and therefore mirrors the equivalent create call.
     
   @param subscriptionName the name of the durable subscription to be deleted
   @param durableSubscriptionHome the name of the ME on which the durable
   subscription is located
   @param alternateUser the name of the user used to create the durable 
   subscription (may be null)

   @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
   @throws com.ibm.websphere.sib.exception.SIResourceException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
   @throws com.ibm.wsspi.sib.core.exception.SINotAuthorizedException
   @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
   @throws com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionNotFoundException
   @throws com.ibm.wsspi.sib.core.exception.SIDestinationLockedException
  */
  public void deleteDurableSubscription(String subscriptionName,
                    String durableSubscriptionHome,
                    String alternateUser)
    throws SIConnectionUnavailableException, SIConnectionDroppedException,
           SIResourceException, SIConnectionLostException,  
           SINotAuthorizedException,
           SIIncorrectCallException,
           SIDurableSubscriptionNotFoundException,
           SIDestinationLockedException;
  
}
