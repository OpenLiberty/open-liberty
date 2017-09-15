/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.sib;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

/**
 * The SIMessage interface is the public interface to an SIBus message for use
 * by Mediations as well as other SIBus components.
 * 
 * @ibm-was-base
 * @ibm-api
 */
public interface SIMessage extends Cloneable, Serializable {

    /* ************************************************************************* */
    /* Method for cloning the SIMessage */
    /* ************************************************************************* */

    /**
     * Obtain a new SIMessage which is a clone of this SIMessage.
     * 
     * @return Object A clone of the original SIMessage.
     * 
     * @exception CloneNotSupportedException The clone of the SIBusSdoMessage failed - see the exception text.
     */
    public Object clone() throws CloneNotSupportedException;

    /* ************************************************************************* */
    /* Methods for accessing the Message Properties */
    /* ************************************************************************* */

    /**
     * Return the Property stored in the Message under the given name.
     * <p>
     * Message Properties are stored as name-value pairs where the value may be any
     * Object which implements java.io.Serializable.
     * This method may be used to get the accessable system (SI_) properties, JMS
     * properties and user properties, however user property names must be
     * prefixed by "user." when passed into this method.
     * <p>
     * The reference returned is to a copy of the Object stored in the
     * Message, so any changes made to it will not be reflected in the Message.
     * 
     * @param name The name of the Property to be returned.
     *            For a user property, the name must include the "user." prefix.
     * 
     * @return Serializable A reference to the Message Property.
     *         Null is returned if there is no such item.
     * 
     * @exception IOException De-serialization of the Property failed.
     * @exception ClassNotFoundException De-serialization of the Property failed
     *                because a necessary Java Class could not be found.
     */
    public Serializable getMessageProperty(String name) throws IOException, ClassNotFoundException;

    /**
     * Add an item to the Message Properties under the given name.
     * <p>
     * Message Properties are stored as name-value pairs where the value may be any
     * Object which implements java.io.Serializable.
     * This method may be used to set the modifyable system (SI_) properties, JMS
     * properties and user properties, however user property names must be
     * prefixed by "user." when passed into this method.
     * <p>
     * If a Property with the given name already exists in the Message, it
     * will be replaced by the Object passed in to this method.
     * <p>
     * If the given name is null, no data will be stored. If the Object passed in
     * is null, this will have the effect of deleting the Property completely.
     * 
     * @param name A String value used to access the Property in future.
     *            For a user property, the name must include the "user." prefix.
     * @param item An Object which implements java.io.Serializable
     * 
     * @exception IOException Serialization of the Property failed.
     */
    public void setMessageProperty(String name, Serializable item) throws IOException;

    /**
     * Delete the Property with the given name from the Message.
     * <p>
     * This method may be used to delete the modifyable system (SI_) properties, JMS
     * properties and user properties, however user property names must be
     * prefixed by "user." when passed into this method.
     * <p>
     * If the given name is null, no data will be deleted.
     * 
     * @param name The String value of the Property to be deleted.
     *            For a user property, the name must include the "user." prefix.
     */
    public void deleteMessageProperty(String name);

    /**
     * Delete all the Properties from the Message.
     * <p>
     * This method deletes all the modifyable properties from the message. Note
     * that some of the system (SI_) and JMS properties are not modifyable
     * and therefore not clearable.
     */
    public void clearMessageProperties();

    /* ************************************************************************* */
    /* Methods for accessing the User Message Properties */
    /* ************************************************************************* */

    /**
     * Return the User Property stored in the Message under the given name.
     * <p>
     * Message Properties are stored as name-value pairs where the value may be any
     * Object which implements java.io.Serializable.
     * This method may only be used to retrieve user properties, and not to
     * retrieve system (SI_) or JMS properties. The property name passed into the
     * method should not include the "user." prefix.
     * <p>
     * The reference returned is to a copy of the Object stored in the
     * Message, so any changes made to it will not be reflected in the Message.
     * 
     * @param name The name of the Property to be returned.
     * 
     * @return Serializable A reference to the Message Property.
     *         Null is returned if there is no such item.
     * 
     * @exception IOException De-serialization of the Property failed.
     * @exception ClassNotFoundException De-serialization of the Property failed
     *                because a necessary Java Class could not be found.
     */
    public Serializable getUserProperty(String name) throws IOException, ClassNotFoundException;

    /**
     * Add User Property to the message under the given name.
     * <p>
     * Message Properties are stored as name-value pairs where the value may be any
     * Object which implements java.io.Serializable.
     * This method may only be used to set user properties, and not to
     * set system (SI_) or JMS properties. The property name passed into the
     * method should not include the "user." prefix.
     * <p>
     * If a Property with the given name already exists in the Message, it
     * will be replaced by the Object passed in to this method.
     * <p>
     * If the given name is null, no data will be stored. If the Object passed in
     * is null, this will have the effect of deleting the Property completely.
     * 
     * @param name A String value used to access the Property in future.
     * @param item An Object which implements java.io.Serializable
     * 
     * @exception IOException Serialization of the Property failed.
     */
    public void setUserProperty(String name, Serializable item) throws IOException;

    /**
     * Delete the User Property with the given name from the Message.
     * <p>
     * This method may only be used to delete user properties, and not to
     * delete system (SI_) or JMS properties. The property name passed into the
     * method should not include the "user." prefix.
     * <p>
     * If the given name is null, no data will be deleted.
     * 
     * @param name The String value of the Property to be deleted.
     */
    public void deleteUserProperty(String name);

    /**
     * Delete all the User Properties from the Message.
     * <p>
     * This method deletes all the user properties from the message.
     */
    public void clearUserProperties();

    /**
     * Get a list of the names of the User Properties in the message.
     * <p>
     * This method returns a list of the names of the User Properties in the
     * message. The names do not include the "user." prefix.
     * 
     * @return List A List containing the String names of the User Properties.
     *         An empty list is returned if there are no user properties.
     */
    public List getUserPropertyNames();

    /* ************************************************************************* */
    /* Methods for getting/setting Forward and Reverse Routing Paths */
    /* ************************************************************************* */

    /**
     * Get the contents of the ForwardRoutingPath for this SIMessage.
     * The List returned is a copy of the ForwardRoutingPath so changes to it
     * will not affect the SIMessage itself.
     * 
     * @return A List containing the ForwardRoutingPath which is a set of SIDestinationAddresses.
     *         Null is returned if the field was not set.
     */
    public List getForwardRoutingPath();

    /**
     * Set the ForwardRoutingPath for this SIMessage.
     * 
     * @param value A List containing the ForwardRoutingPath which is a set of SIDestinationAddresses.
     * 
     * @exception IllegalArgumentException The List contains one or more entries
     *                which are not SIDestinationAdddresses.
     * 
     * @exception NullPointerException The List contains one or more null entries.
     */
    public void setForwardRoutingPath(List value);

    /**
     * Get the contents of the ReverseRoutingPath for this SIMessage.
     * The List returned is a copy of the ReverseRoutingPath so changes to it
     * will not affect the SIMessage itself.
     * 
     * @return A List containing the ReverseRoutingPath which is a set of SIDestinationAddresses.
     *         Null is returned if the field was not set.
     */
    public List getReverseRoutingPath();

    /**
     * Set the ReverseRoutingPath for this SIMessage.
     * 
     * @param value A List containing the ReverseRoutingPath which is a set of SIDestinationAddresses.
     * 
     * @exception IllegalArgumentException The List contains one or more entries
     *                which are not SIDestinationAdddresses.
     * 
     * @exception NullPointerException The List contains one or more null entries.
     */
    public void setReverseRoutingPath(List value);

    /* ************************************************************************* */
    /* Methods for getting/setting the Priority, Reliability and TimeToLive */
    /* ************************************************************************* */

    /**
     * Get the value of the Priority field from the message header.
     * 
     * @return An int containing the Priority of the message.
     */
    public int getPriority();

    /**
     * Set the value of the Priority field in the message header.
     * 
     * @param value An int containing the Priority of the message.
     * 
     * @exception IllegalArgumentException The value given is outside the
     *                permitted range.
     */
    public void setPriority(int value);

    /**
     * Get the value of the Reliability field from the message header.
     * 
     * @return The Reliability instance representing the Reliability of the
     *         message (i.e. Express, Reliable or Assured).
     *         Reliability.UNKNOWN is returned if the field is not set.
     */
    public Reliability getReliability();

    /**
     * Set the value of the Reliability field in the message header.
     * 
     * @exception NullPointerException Null is not a valid Reliability.
     */
    public void setReliability(Reliability value);

    /**
     * Get the value of the TimeToLive field from the message header.
     * The value represents the time in milliseconds for the message to live
     * counting from when it was orginally sent.
     * A value of 0 indicates that the message will never expire.
     * 
     * @return A long containing the TimeToLive of the message.
     *         The default value of 0 is returned if the field was not set.
     */
    public long getTimeToLive();

    /**
     * Set the value of the TimeToLive field in the message header.
     * The value represents the time in milliseconds for the message to live
     * counting from when it was orginally sent.
     * A value of 0 indicates that the message should never expire.
     * 
     * @param value A long containing the TimeToLive of the message.
     * 
     * @exception IllegalArgumentException The value
     *                given is less than 0 or greater than 290,000,000 years.
     */
    public void setTimeToLive(long value);

    /**
     * Set the value of the DeliveryDelay field in the message header.
     * The value represents the time in milliseconds that must elapse after
     * a message is sent before the JMS provider may deliver the message to a consumer.
     * A value of 0 indicates that there is no delivery delay
     * 
     * @param value A long containing the TimeToLive of the message.
     * 
     * @exception IllegalArgumentException The value
     *                given is less than 0 or greater than 290,000,000 years.
     */
    public void setDeliveryDelay(long value);

    /**
     * Get the value of the DeliveryDelay field from the message header.
     * The value represents the time in milliseconds that must elapse after
     * a message is sent before the JMS provider may deliver the message to a consumer. * A value of 0 indicates that the message will never expire.
     * 
     * @return A long containing the TimeToLive of the message.
     *         The default value of 0 is returned if the field was not set.
     */
    public long getDeliveryDelay();

    /**
     * Get the remaining time in milliseconds before the message expires.
     * 
     * @return A long containing the remaining time, in milliseconds, before the
     *         message expires. A negative number indicates that the message
     *         will never expire.
     */
    public long getRemainingTimeToLive();

    /**
     * Set the remaining time in milliseconds before the message should expire.
     * 
     * @param value A long containing the remaining time, in milliseconds, before the
     *            message should expire. A negative number indicates that the message
     *            should never expire.
     * 
     * @exception IllegalArgumentException The resulting
     *                Time To Live is less than 0 or greater than 290,000,000 years.
     */
    public void setRemainingTimeToLive(long value);

    /**
     * Get the value of the ReplyPriority field from the message header.
     * 
     * @return An Integer containing the Priority for any reply.
     *         The default value of 4 is returned if the field was not set.
     */
    public int getReplyPriority();

    /**
     * Set the value of the ReplyPriority field in the message header.
     * 
     * @param value An int containing the Priority for any reply.
     * 
     * @exception IllegalArgumentException The value given is outside the
     *                permitted range.
     */
    public void setReplyPriority(int value);

    /**
     * Get the value of the ReplyReliability field from the message header.
     * 
     * @return The Reliability instance representing the Reliability for any reply
     *         message (i.e. Express, Reliable or Assured).
     *         Reliability.UNKNOWN is returned if the field is not set.
     */
    public Reliability getReplyReliability();

    /**
     * Set the value of the ReplyReliability field in the message header.
     * 
     * @exception NullPointerException Null is not a valid Reliability.
     */
    public void setReplyReliability(Reliability value);

    /**
     * Get the value of the ReplyTimeToLive field from the message header.
     * A value of 0 indicates that the reply message will never expire.
     * 
     * @return A Long containing the TimeToLive for any reply.
     *         The default value of 0 is returned if the field was not set.
     */
    public long getReplyTimeToLive();

    /**
     * Set the value of the ReplyTimeToLive field in the message header.
     * A value of 0 indicates that the reply message should never expire.
     * 
     * @param value A long containing the TimeToLive for any reply.
     * 
     * @exception IllegalArgumentException The value
     *                given is less than 0 or greater than 290,000,000 years.
     */
    public void setReplyTimeToLive(long value);

    /**
     * Clear the four ReplyXxxx fields in the message header.
     */
    public void clearReplyFields();

    /* ************************************************************************* */
    /* Methods for getting/setting the topic Discriminator fields */
    /* ************************************************************************* */

    /**
     * Get the contents of the topic Discriminator field from the message header.
     * 
     * @return A String containing the Discriminator for Pub/Sub
     *         Null is returned if the field was not set.
     */
    public String getDiscriminator();

    /**
     * Set the contents of the topic Discriminator field in the message header.
     * 
     * @param value A String containing the Discriminator for Pub/Sub
     *            The Discriminator is a simplified XPATH expression without wild cards.
     *            It is considered valid if the following expression is true:
     *            <code>
     *            java.util.regex.Pattern.matches("([^:./*][^:/*]*)(/[^:./*][^:/*]*)*",value);
     *            </code>
     * 
     * @exception IllegalArgumentException The value given contains wild cards
     *                or is otherwise invalid.
     */
    public void setDiscriminator(String value);

    /**
     * Get the contents of the ReplyDiscriminator field from the message header.
     * 
     * @return A String containing the Discriminator for any reply.
     *         Null is returned if the field was not set.
     */
    public String getReplyDiscriminator();

    /**
     * Set the contents of the ReplyDiscriminator field in the message header.
     * 
     * @param value A String containing the Discriminator for any reply.
     *            The Discriminator is a simplified XPATH expression without wild cards.
     *            It is considered valid if the following expression is true:
     *            <code>
     *            java.util.regex.Pattern.matches("([^:./*][^:/*]*)(/[^:./*][^:/*]*)*",value);
     *            </code>
     * 
     * @exception IllegalArgumentException The value given contains wild cards
     *                or is otherwise invalid.
     */
    public void setReplyDiscriminator(String value);

    /* ************************************************************************* */
    /* Methods for getting the Unique Id field */
    /* ************************************************************************* */

    /**
     * Get the unique System Message Id from the message header.
     * 
     * @return A String containing the system message id.
     *         Null is returned if the field has not yet been set.
     */
    public String getSystemMessageId();

    /* ************************************************************************* */
    /* Method for getting the RedeliveryCount field */
    /* ************************************************************************* */

    /**
     * Get the value of the RedeliveredCount field from the message header.
     * 
     * @return An int representation of the RedeliveredCount of the message.
     */
    public int getRedeliveredCount();

    /* ************************************************************************* */
    /* Methods for getting/setting the API Meta-Data */
    /* ************************************************************************* */

    /**
     * Get the contents of the ApiMessageId field from the message API Meta-Data.
     * 
     * @return A String containing the ApiMessageId.
     *         Null is returned if the field has not been set.
     */
    public String getApiMessageId();

    /**
     * Set the contents of the ApiMessageId field in the message API Meta-Data.
     * 
     * @param value A String containing the ApiMessageId.
     * 
     * @exception IllegalArgumentException The value given must be of the form
     *                ID:xxxx where xxxx represents an even number of hexadecimal digits.
     */
    public void setApiMessageId(String value);

    /**
     * Get the contents of the CorrelationId field from the message API Meta-Data.
     * 
     * @return A String containing the CorrelationId.
     *         Null is returned if the field has not been set.
     */
    public String getCorrelationId();

    /**
     * Set the contents of the CorrelationId field in the message API Meta-Data.
     * 
     * @param value A String containing the CorrelationId.
     * 
     * @exception IllegalArgumentException The value given must be either be
     *                of the form ID:xxxx, where xxxx represents an even number
     *                of hexadecimal digits,
     *                or must be an arbitrary String which does not start with ID:.
     */
    public void setCorrelationId(String value);

    /**
     * Get the contents of the UserId field from the message API Meta-Data.
     * 
     * @return A String containing the UserId.
     *         Null is returned if the field has not been set.
     */
    public String getUserId();

    /**
     * Set the contents of the UserId field in the message API Meta-Data.
     * 
     * @param value A String containing the UserId.
     */
    public void setUserId(String value);

    /**
     * Get the contents of the Format field from the message API Meta-Data.
     * 
     * @return A String containing the Format.
     *         Null is returned if the field has not been set.
     */
    public String getFormat();

}
