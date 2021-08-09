/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity.config;

import java.lang.reflect.Field;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * This class holds all of the properties that can go in a nested properties.wasJms object. Note that these are often different PIDs but this class holds all of the properties for
 * all of the PIDs for convenience.
 */
// final as the toString impl will only work for this class
public final class WasJmsProperties extends ConfigElement {

    private String queueName;
    private String destinationRef;
    private String userName;
    private String clientID;
    private String nonPersistentMapping;
    private String persistentMapping;
    private String topicName;
    private String topicSpace;
    private String deliveryMode;
    private String timeToLive;
    private String priority;
    private String readAhead;
    private String temporaryQueueNamePrefix;
    private String remoteServerAddress;

    /**
     * @return the queueName
     */
    public String getQueueName() {
        return queueName;
    }

    /**
     * @return the destinationRef
     */
    public String getDestinationRef() {
        return destinationRef;
    }

    /**
     * @return the userName
     */
    public String getUserName() {
        return userName;
    }

    /**
     * @return the clientID
     */
    public String getClientID() {
        return clientID;
    }

    /**
     * @return the nonPersistentMapping
     */
    public String getNonPersistentMapping() {
        return nonPersistentMapping;
    }

    /**
     * @return the persistentMapping
     */
    public String getPersistentMapping() {
        return persistentMapping;
    }

    /**
     * @return the topicName
     */
    public String getTopicName() {
        return topicName;
    }

    /**
     * @return the topicSpace
     */
    public String getTopicSpace() {
        return topicSpace;
    }

    /**
     * @return the deliveryMode
     */
    public String getDeliveryMode() {
        return deliveryMode;
    }

    /**
     * @return the timeToLive
     */
    public String getTimeToLive() {
        return timeToLive;
    }

    /**
     * @return the priority
     */
    public String getPriority() {
        return priority;
    }

    /**
     * @return the readAhead
     */
    public String getReadAhead() {
        return readAhead;
    }

    /**
     * @return the temporaryQueueNamePrefix
     */
    public String getTemporaryQueueNamePrefix() {
        return temporaryQueueNamePrefix;
    }

    /**
     * @return the remoteServerAddress
     */
    public String getRemoteServerAddress() {
        return remoteServerAddress;
    }

    /**
     * @param queueName the queueName to set
     */
    @XmlAttribute
    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    /**
     * @param destinationRef the destinationRef to set
     */
    @XmlAttribute
    public void setDestinationRef(String destinationRef) {
        this.destinationRef = destinationRef;
    }

    /**
     * @param userName the userName to set
     */
    @XmlAttribute
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * @param clientID the clientID to set
     */
    @XmlAttribute
    public void setClientID(String clientID) {
        this.clientID = clientID;
    }

    /**
     * @param nonPersistentMapping the nonPersistentMapping to set
     */
    @XmlAttribute
    public void setNonPersistentMapping(String nonPersistentMapping) {
        this.nonPersistentMapping = nonPersistentMapping;
    }

    /**
     * @param persistentMapping the persistentMapping to set
     */
    @XmlAttribute
    public void setPersistentMapping(String persistentMapping) {
        this.persistentMapping = persistentMapping;
    }

    /**
     * @param topicName the topicName to set
     */
    @XmlAttribute
    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    /**
     * @param topicSpace the topicSpace to set
     */
    @XmlAttribute
    public void setTopicSpace(String topicSpace) {
        this.topicSpace = topicSpace;
    }

    /**
     * @param deliveryMode the deliveryMode to set
     */
    @XmlAttribute
    public void setDeliveryMode(String deliveryMode) {
        this.deliveryMode = deliveryMode;
    }

    /**
     * @param timeToLive the timeToLive to set
     */
    @XmlAttribute
    public void setTimeToLive(String timeToLive) {
        this.timeToLive = timeToLive;
    }

    /**
     * @param priority the priority to set
     */
    @XmlAttribute
    public void setPriority(String priority) {
        this.priority = priority;
    }

    /**
     * @param readAhead the readAhead to set
     */
    @XmlAttribute
    public void setReadAhead(String readAhead) {
        this.readAhead = readAhead;
    }

    /**
     * @param temporaryQueueNamePrefix the temporaryQueueNamePrefix to set
     */
    @XmlAttribute
    public void setTemporaryQueueNamePrefix(String temporaryQueueNamePrefix) {
        this.temporaryQueueNamePrefix = temporaryQueueNamePrefix;
    }

    /**
     * @param remoteServerAddress the remoteServerAddress to set
     */
    @XmlAttribute
    public void setRemoteServerAddress(String remoteServerAddress) {
        this.remoteServerAddress = remoteServerAddress;
    }

    @Override
    public String toString() {
        Class clazz = this.getClass();
        StringBuilder buf = new StringBuilder(clazz.getSimpleName())
                        .append('{');
        buf.append("id=\"" + (getId() == null ? "" : getId()) + "\" ");

        // Life is too short to type in all those fields!
        Field[] fields = clazz.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            Object value;
            try {
                value = field.get(this);
                if (value != null)
                    buf.append(field.getName() + "=\"" + value + "\" ");
            } catch (IllegalArgumentException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        buf.append("}");
        return buf.toString();
    }

}
