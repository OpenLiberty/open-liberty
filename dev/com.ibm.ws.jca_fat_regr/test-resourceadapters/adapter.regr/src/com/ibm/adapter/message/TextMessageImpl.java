/*******************************************************************************
 * Copyright (c) 2003, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.adapter.message;

import java.util.Enumeration;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.TextMessage;

/**
 * <p>This class implements javax.jms.TextMessage This is used for TRA to create a TextMessage.</p>
 */
public class TextMessageImpl implements TextMessage {

    String text;

    /**
     * Constructor for TextMessageImpl.
     */
    public TextMessageImpl(String text) {
        this.text = text;
    }

    /**
     * @see javax.jms.TextMessage#setText(String)
     */
    public void setText(String arg0) throws JMSException {
        text = arg0;
    }

    /**
     * @see javax.jms.TextMessage#getText()
     */
    public String getText() throws JMSException {
        return text;
    }

    /**
     * @see javax.jms.Message#getJMSMessageID()
     */
    public String getJMSMessageID() throws JMSException {
        return null;
    }

    /**
     * @see javax.jms.Message#setJMSMessageID(String)
     */
    public void setJMSMessageID(String arg0) throws JMSException {
    }

    /**
     * @see javax.jms.Message#getJMSTimestamp()
     */
    public long getJMSTimestamp() throws JMSException {
        return 0;
    }

    /**
     * @see javax.jms.Message#setJMSTimestamp(long)
     */
    public void setJMSTimestamp(long arg0) throws JMSException {
    }

    /**
     * @see javax.jms.Message#getJMSCorrelationIDAsBytes()
     */
    public byte[] getJMSCorrelationIDAsBytes() throws JMSException {
        return null;
    }

    /**
     * @see javax.jms.Message#setJMSCorrelationIDAsBytes(byte[])
     */
    public void setJMSCorrelationIDAsBytes(byte[] arg0) throws JMSException {
    }

    /**
     * @see javax.jms.Message#setJMSCorrelationID(String)
     */
    public void setJMSCorrelationID(String arg0) throws JMSException {
    }

    /**
     * @see javax.jms.Message#getJMSCorrelationID()
     */
    public String getJMSCorrelationID() throws JMSException {
        return null;
    }

    /**
     * @see javax.jms.Message#getJMSReplyTo()
     */
    public Destination getJMSReplyTo() throws JMSException {
        return null;
    }

    /**
     * @see javax.jms.Message#setJMSReplyTo(Destination)
     */
    public void setJMSReplyTo(Destination arg0) throws JMSException {
    }

    /**
     * @see javax.jms.Message#getJMSDestination()
     */
    public Destination getJMSDestination() throws JMSException {
        return null;
    }

    /**
     * @see javax.jms.Message#setJMSDestination(Destination)
     */
    public void setJMSDestination(Destination arg0) throws JMSException {
    }

    /**
     * @see javax.jms.Message#getJMSDeliveryMode()
     */
    public int getJMSDeliveryMode() throws JMSException {
        return 0;
    }

    /**
     * @see javax.jms.Message#setJMSDeliveryMode(int)
     */
    public void setJMSDeliveryMode(int arg0) throws JMSException {
    }

    /**
     * @see javax.jms.Message#getJMSRedelivered()
     */
    public boolean getJMSRedelivered() throws JMSException {
        return false;
    }

    /**
     * @see javax.jms.Message#setJMSRedelivered(boolean)
     */
    public void setJMSRedelivered(boolean arg0) throws JMSException {
    }

    /**
     * @see javax.jms.Message#getJMSType()
     */
    public String getJMSType() throws JMSException {
        return null;
    }

    /**
     * @see javax.jms.Message#setJMSType(String)
     */
    public void setJMSType(String arg0) throws JMSException {
    }

    /**
     * @see javax.jms.Message#getJMSExpiration()
     */
    public long getJMSExpiration() throws JMSException {
        return 0;
    }

    /**
     * @see javax.jms.Message#setJMSExpiration(long)
     */
    public void setJMSExpiration(long arg0) throws JMSException {
    }

    /**
     * @see javax.jms.Message#getJMSPriority()
     */
    public int getJMSPriority() throws JMSException {
        return 0;
    }

    /**
     * @see javax.jms.Message#setJMSPriority(int)
     */
    public void setJMSPriority(int arg0) throws JMSException {
    }

    /**
     * @see javax.jms.Message#clearProperties()
     */
    public void clearProperties() throws JMSException {
    }

    /**
     * @see javax.jms.Message#propertyExists(String)
     */
    public boolean propertyExists(String arg0) throws JMSException {
        return false;
    }

    /**
     * @see javax.jms.Message#getBooleanProperty(String)
     */
    public boolean getBooleanProperty(String arg0) throws JMSException {
        return false;
    }

    /**
     * @see javax.jms.Message#getByteProperty(String)
     */
    public byte getByteProperty(String arg0) throws JMSException {
        return 0;
    }

    /**
     * @see javax.jms.Message#getShortProperty(String)
     */
    public short getShortProperty(String arg0) throws JMSException {
        return 0;
    }

    /**
     * @see javax.jms.Message#getIntProperty(String)
     */
    public int getIntProperty(String arg0) throws JMSException {
        return 0;
    }

    /**
     * @see javax.jms.Message#getLongProperty(String)
     */
    public long getLongProperty(String arg0) throws JMSException {
        return 0;
    }

    /**
     * @see javax.jms.Message#getFloatProperty(String)
     */
    public float getFloatProperty(String arg0) throws JMSException {
        return 0;
    }

    /**
     * @see javax.jms.Message#getDoubleProperty(String)
     */
    public double getDoubleProperty(String arg0) throws JMSException {
        return 0;
    }

    /**
     * @see javax.jms.Message#getStringProperty(String)
     */
    public String getStringProperty(String arg0) throws JMSException {
        return null;
    }

    /**
     * @see javax.jms.Message#getObjectProperty(String)
     */
    public Object getObjectProperty(String arg0) throws JMSException {
        return null;
    }

    /**
     * @see javax.jms.Message#getPropertyNames()
     */
    public Enumeration getPropertyNames() throws JMSException {
        return null;
    }

    /**
     * @see javax.jms.Message#setBooleanProperty(String, boolean)
     */
    public void setBooleanProperty(String arg0, boolean arg1) throws JMSException {
    }

    /**
     * @see javax.jms.Message#setByteProperty(String, byte)
     */
    public void setByteProperty(String arg0, byte arg1) throws JMSException {
    }

    /**
     * @see javax.jms.Message#setShortProperty(String, short)
     */
    public void setShortProperty(String arg0, short arg1) throws JMSException {
    }

    /**
     * @see javax.jms.Message#setIntProperty(String, int)
     */
    public void setIntProperty(String arg0, int arg1) throws JMSException {
    }

    /**
     * @see javax.jms.Message#setLongProperty(String, long)
     */
    public void setLongProperty(String arg0, long arg1) throws JMSException {
    }

    /**
     * @see javax.jms.Message#setFloatProperty(String, float)
     */
    public void setFloatProperty(String arg0, float arg1) throws JMSException {
    }

    /**
     * @see javax.jms.Message#setDoubleProperty(String, double)
     */
    public void setDoubleProperty(String arg0, double arg1) throws JMSException {
    }

    /**
     * @see javax.jms.Message#setStringProperty(String, String)
     */
    public void setStringProperty(String arg0, String arg1) throws JMSException {
    }

    /**
     * @see javax.jms.Message#setObjectProperty(String, Object)
     */
    public void setObjectProperty(String arg0, Object arg1) throws JMSException {
    }

    /**
     * @see javax.jms.Message#acknowledge()
     */
    public void acknowledge() throws JMSException {
    }

    /**
     * @see javax.jms.Message#clearBody()
     */
    public void clearBody() throws JMSException {
    }

}
