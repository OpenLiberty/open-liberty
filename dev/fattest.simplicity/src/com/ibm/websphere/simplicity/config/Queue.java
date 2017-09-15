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
 *
 */
// final as the toString impl will only work for this class
public final class Queue extends ConfigElement {

    private String forceReliability;
    private String exceptionDestination;
    private String redeliveryInterval;
    private String maxRedeliveryCount;
    private String sendAllowed;
    private String receiveAllowed;
    private String receiveExclusive;
    private String maintainStrictOrder;
    private String maxQueueDepth;

    /**
     * @return the forceReliability
     */
    public String getForceReliability() {
        return forceReliability;
    }

    /**
     * @return the exceptionDestination
     */
    public String getExceptionDestination() {
        return exceptionDestination;
    }

    /**
     * @return the redeliveryInterval
     */
    public String getRedeliveryInterval() {
        return redeliveryInterval;
    }

    /**
     * @return the maxRedeliveryCount
     */
    public String getMaxRedeliveryCount() {
        return maxRedeliveryCount;
    }

    /**
     * @return the sendAllowed
     */
    public String getSendAllowed() {
        return sendAllowed;
    }

    /**
     * @return the receiveAllowed
     */
    public String getReceiveAllowed() {
        return receiveAllowed;
    }

    /**
     * @return the receiveExclusive
     */
    public String getReceiveExclusive() {
        return receiveExclusive;
    }

    /**
     * @return the maintainStrictOrder
     */
    public String getMaintainStrictOrder() {
        return maintainStrictOrder;
    }

    /**
     * @return the maxQueueDepth
     */
    public String getMaxQueueDepth() {
        return maxQueueDepth;
    }

    /**
     * @param forceReliability the forceReliability to set
     */
    @XmlAttribute
    public void setForceReliability(String forceReliability) {
        this.forceReliability = forceReliability;
    }

    /**
     * @param exceptionDestination the exceptionDestination to set
     */
    @XmlAttribute
    public void setExceptionDestination(String exceptionDestination) {
        this.exceptionDestination = exceptionDestination;
    }

    /**
     * @param redeliveryInterval the redeliveryInterval to set
     */
    @XmlAttribute
    public void setRedeliveryInterval(String redeliveryInterval) {
        this.redeliveryInterval = redeliveryInterval;
    }

    /**
     * @param maxRedeliveryCount the maxRedeliveryCount to set
     */
    @XmlAttribute
    public void setMaxRedeliveryCount(String maxRedeliveryCount) {
        this.maxRedeliveryCount = maxRedeliveryCount;
    }

    /**
     * @param sendAllowed the sendAllowed to set
     */
    @XmlAttribute
    public void setSendAllowed(String sendAllowed) {
        this.sendAllowed = sendAllowed;
    }

    /**
     * @param receiveAllowed the receiveAllowed to set
     */
    @XmlAttribute
    public void setReceiveAllowed(String receiveAllowed) {
        this.receiveAllowed = receiveAllowed;
    }

    /**
     * @param receiveExclusive the receiveExclusive to set
     */
    @XmlAttribute
    public void setReceiveExclusive(String receiveExclusive) {
        this.receiveExclusive = receiveExclusive;
    }

    /**
     * @param maintainStrictOrder the maintainStrictOrder to set
     */
    @XmlAttribute
    public void setMaintainStrictOrder(String maintainStrictOrder) {
        this.maintainStrictOrder = maintainStrictOrder;
    }

    /**
     * @param maxQueueDepth the maxQueueDepth to set
     */
    @XmlAttribute
    public void setMaxQueueDepth(String maxQueueDepth) {
        this.maxQueueDepth = maxQueueDepth;
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
