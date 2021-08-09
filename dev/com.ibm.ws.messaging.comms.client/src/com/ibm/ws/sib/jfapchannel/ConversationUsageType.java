/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel;

/**
 * This enumeration represents all the particular different users of the JFAPChannel, and in particular how a particular conversation will be used. Each 
 * user has a different ConversationUsageType.  The notion of a ConversationUsageType was introduced because multiple users of the JFAPChannel have 
 * arisen on z/OS, via the TCP proxy channel, such as the WMQ resource adapter and message endpoint (MEP) support. Rather than repeatedly adjusting the code 
 * this enumeration has been introduced to describe any differences and similarities between usage types, hopefully minimizing future changes
 * and promoting reuse. When a ConversationUsageType instance is available the JFAPChannel code can either look at the instance variable 
 * directly or use one of the instance methods to decide how to behave.
 * <br>
 * ConversationUsageType can also be serialized between processes thanks to the way Java supports enumerations.
 * 
 */
public enum ConversationUsageType 
{
   /**
    * Normal usage of the JFAPChannel prior to WAS version 7. This is the default on distributed and when not using the TCP proxy channel.
    * This usage type doesn't specify a value of ConversationReceiveListener as several different ones are used depending on whether client <-> ME
    * or ME <-> ME comms is in use. As such calling the getConversationReceiveListenerClassName method will throw an UnsupportedOperationException.
    */
   JFAP(),
   
   /**
    * Indicates that this conversation will be used by the WMQ resource adapter. Driven in the SR to indicate that a message has been processed.
    * This information is then used by the CRA for message pacing. 
    * <br>
    * In use since WAS 7.
    */
   WMQRA("com.ibm.ws.wmqra.jfap.WMQRAConversationReceiveListener"), 
   
   /**
    * Indicates that this conversation will be used by MEP supprt. Driven in the SR to tell the CRA to activate/deactivate an endpoint + 
    * as a way of telling when a SR/CRA goes away. 
    * <br>
    * In use since WAS 7.0.0.11 
    */
   MEP("com.ibm.ws.sib.mep.jfap.MEPConversationReceiveListener");
   
   /**
    * The name of a class which implements the ConversationReceiveListener interface. May be set to null if more than one is supported, and the
    * one chosen is decided elsewhere (which is the case with normal Comms/JFAP).
    */
   private final String conversationReceiveListenerClassName;
   
   /**
    * Serialize this ConversationUsageType to the provided buffer. The serialized form of a ConversationUsageType is 4 bytes.
    * 
    * @param buffer
    */
   public final void serialize(final JFapByteBuffer buffer)
   {
      //Rely on the fact that as the CRA and SR use the same level of code the ordinal values must be the same.
      //If they aren't then this won't work.
      buffer.putInt(ordinal());
   }
   
   /**
    * Deserializes a previously serialized ConversationUsageType from the supplied buffer.
    * 
    * @param buffer
    * 
    * @return
    */
   public static final ConversationUsageType deserialize(final JFapByteBuffer buffer)
   {
      final int ordinalValue = buffer.getInt();
      
      if(ordinalValue < values().length)
      {
         return values()[ordinalValue];
      }
      
      throw new IllegalArgumentException("Invalid ordinal in buffer : " + ordinalValue);
   }
   
   /**
    * Create a ConversationUsageType specifying a particular conversation receive listener class.
    * <br>
    * @param receiveListenerClassName
    */
   private ConversationUsageType(final String conversationReceiveListenerClassName)
   {
      this.conversationReceiveListenerClassName = conversationReceiveListenerClassName;      
   }
   
   /**
    * Create a ConversationUsageType which doesn't specify a conversation receive listener class.
    * <br>
    * This should be used when more than one conversation receive listener class could be chosen depending
    * on the current environment. In this case calling getConversationReceiveListenerClassName() will result
    * in an UnsupportedOperationException being thrown.
    */
   private ConversationUsageType()
   {
      conversationReceiveListenerClassName = null;      
   }
   
   /**
    * @return the name of a class which implements the ConversationReceiveListener interface. 
    * @throws UnsupportedOperationException if this particular ConversationUsageType doesn't specify a particular conversation receive listener.
    */
   public final String getConversationReceiveListenerClassName() throws UnsupportedOperationException
   {
      //JFAP/Comms uses multiple different ConversationReceiveListeners so this call doesn't make sense.
      //In addition the decision about which one to use is elsewhere in the code.
      if(this == JFAP)
      {
         throw new UnsupportedOperationException("It is not valid to call ConversationUsageType.JFAP.getConversationReceiveListenerClassName.");
      }
      
      return conversationReceiveListenerClassName;
   }
   
   /**
    * Returns true if this ConversationUsageType requires normal handshaking functionality as per normal JFAP/Comms.
    * and false otherwise.
    * 
    * In this case 'normal' means that handshaking is performed on the first conversation over a connection, delaying
    * any other conversations until handshaking completed, and the results of the handshake is stored on the connection
    * for other conversations to access. 
    * <br>
    * Otherwise a handshake is performed for each conversation and the result is not shared between conversations.
    * <br> 
    * There is no reason why two conversations, one of which requires 'normal' handshake processing, and one which
    * doesn't, can't be multiplexed over the same connection.
    * 
    * @return   
    */
   public final boolean requiresNormalHandshakeProcessing()
   {
      return this == JFAP;
   }
}
