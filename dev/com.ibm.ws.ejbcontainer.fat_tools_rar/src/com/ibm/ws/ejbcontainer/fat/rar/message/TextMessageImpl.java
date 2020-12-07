/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.fat.rar.message;

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
    @Override
    public void setText(String arg0) throws JMSException {
        text = arg0;
    }

    /**
     * @see javax.jms.TextMessage#getText()
     */
    @Override
    public String getText() throws JMSException {
        return text;
    }

    /**
     * @see javax.jms.Message#getJMSMessageID()
     */
    @Override
    public String getJMSMessageID() throws JMSException {
        return null;
    }

    /**
     * @see javax.jms.Message#setJMSMessageID(String)
     */
    @Override
    public void setJMSMessageID(String arg0) throws JMSException {
    }

    /**
     * @see javax.jms.Message#getJMSTimestamp()
     */
    @Override
    public long getJMSTimestamp() throws JMSException {
        return 0;
    }

    /**
     * @see javax.jms.Message#setJMSTimestamp(long)
     */
    @Override
    public void setJMSTimestamp(long arg0) throws JMSException {
    }

    /**
     * @see javax.jms.Message#getJMSCorrelationIDAsBytes()
     */
    @Override
    public byte[] getJMSCorrelationIDAsBytes() throws JMSException {
        return null;
    }

    /**
     * @see javax.jms.Message#setJMSCorrelationIDAsBytes(byte[])
     */
    @Override
    public void setJMSCorrelationIDAsBytes(byte[] arg0) throws JMSException {
    }

    /**
     * @see javax.jms.Message#setJMSCorrelationID(String)
     */
    @Override
    public void setJMSCorrelationID(String arg0) throws JMSException {
    }

    /**
     * @see javax.jms.Message#getJMSCorrelationID()
     */
    @Override
    public String getJMSCorrelationID() throws JMSException {
        return null;
    }

    /**
     * @see javax.jms.Message#getJMSReplyTo()
     */
    @Override
    public Destination getJMSReplyTo() throws JMSException {
        return null;
    }

    /**
     * @see javax.jms.Message#setJMSReplyTo(Destination)
     */
    @Override
    public void setJMSReplyTo(Destination arg0) throws JMSException {
    }

    /**
     * @see javax.jms.Message#getJMSDestination()
     */
    @Override
    public Destination getJMSDestination() throws JMSException {
        return null;
    }

    /**
     * @see javax.jms.Message#setJMSDestination(Destination)
     */
    @Override
    public void setJMSDestination(Destination arg0) throws JMSException {
    }

    /**
     * @see javax.jms.Message#getJMSDeliveryMode()
     */
    @Override
    public int getJMSDeliveryMode() throws JMSException {
        return 0;
    }

    /**
     * @see javax.jms.Message#setJMSDeliveryMode(int)
     */
    @Override
    public void setJMSDeliveryMode(int arg0) throws JMSException {
    }

    /**
     * @see javax.jms.Message#getJMSRedelivered()
     */
    @Override
    public boolean getJMSRedelivered() throws JMSException {
        return false;
    }

    /**
     * @see javax.jms.Message#setJMSRedelivered(boolean)
     */
    @Override
    public void setJMSRedelivered(boolean arg0) throws JMSException {
    }

    /**
     * @see javax.jms.Message#getJMSType()
     */
    @Override
    public String getJMSType() throws JMSException {
        return null;
    }

    /**
     * @see javax.jms.Message#setJMSType(String)
     */
    @Override
    public void setJMSType(String arg0) throws JMSException {
    }

    /**
     * @see javax.jms.Message#getJMSExpiration()
     */
    @Override
    public long getJMSExpiration() throws JMSException {
        return 0;
    }

    /**
     * @see javax.jms.Message#setJMSExpiration(long)
     */
    @Override
    public void setJMSExpiration(long arg0) throws JMSException {
    }

    /**
     * @see javax.jms.Message#getJMSPriority()
     */
    @Override
    public int getJMSPriority() throws JMSException {
        return 0;
    }

    /**
     * @see javax.jms.Message#setJMSPriority(int)
     */
    @Override
    public void setJMSPriority(int arg0) throws JMSException {
    }

    /**
     * @see javax.jms.Message#clearProperties()
     */
    @Override
    public void clearProperties() throws JMSException {
    }

    /**
     * @see javax.jms.Message#propertyExists(String)
     */
    @Override
    public boolean propertyExists(String arg0) throws JMSException {
        return false;
    }

    /**
     * @see javax.jms.Message#getBooleanProperty(String)
     */
    @Override
    public boolean getBooleanProperty(String arg0) throws JMSException {
        return false;
    }

    /**
     * @see javax.jms.Message#getByteProperty(String)
     */
    @Override
    public byte getByteProperty(String arg0) throws JMSException {
        return 0;
    }

    /**
     * @see javax.jms.Message#getShortProperty(String)
     */
    @Override
    public short getShortProperty(String arg0) throws JMSException {
        return 0;
    }

    /**
     * @see javax.jms.Message#getIntProperty(String)
     */
    @Override
    public int getIntProperty(String arg0) throws JMSException {
        return 0;
    }

    /**
     * @see javax.jms.Message#getLongProperty(String)
     */
    @Override
    public long getLongProperty(String arg0) throws JMSException {
        return 0;
    }

    /**
     * @see javax.jms.Message#getFloatProperty(String)
     */
    @Override
    public float getFloatProperty(String arg0) throws JMSException {
        return 0;
    }

    /**
     * @see javax.jms.Message#getDoubleProperty(String)
     */
    @Override
    public double getDoubleProperty(String arg0) throws JMSException {
        return 0;
    }

    /**
     * @see javax.jms.Message#getStringProperty(String)
     */
    @Override
    public String getStringProperty(String arg0) throws JMSException {
        return null;
    }

    /**
     * @see javax.jms.Message#getObjectProperty(String)
     */
    @Override
    public Object getObjectProperty(String arg0) throws JMSException {
        return null;
    }

    /**
     * @see javax.jms.Message#getPropertyNames()
     */
    @Override
    public Enumeration getPropertyNames() throws JMSException {
        return null;
    }

    /**
     * @see javax.jms.Message#setBooleanProperty(String, boolean)
     */
    @Override
    public void setBooleanProperty(String arg0, boolean arg1) throws JMSException {
    }

    /**
     * @see javax.jms.Message#setByteProperty(String, byte)
     */
    @Override
    public void setByteProperty(String arg0, byte arg1) throws JMSException {
    }

    /**
     * @see javax.jms.Message#setShortProperty(String, short)
     */
    @Override
    public void setShortProperty(String arg0, short arg1) throws JMSException {
    }

    /**
     * @see javax.jms.Message#setIntProperty(String, int)
     */
    @Override
    public void setIntProperty(String arg0, int arg1) throws JMSException {
    }

    /**
     * @see javax.jms.Message#setLongProperty(String, long)
     */
    @Override
    public void setLongProperty(String arg0, long arg1) throws JMSException {
    }

    /**
     * @see javax.jms.Message#setFloatProperty(String, float)
     */
    @Override
    public void setFloatProperty(String arg0, float arg1) throws JMSException {
    }

    /**
     * @see javax.jms.Message#setDoubleProperty(String, double)
     */
    @Override
    public void setDoubleProperty(String arg0, double arg1) throws JMSException {
    }

    /**
     * @see javax.jms.Message#setStringProperty(String, String)
     */
    @Override
    public void setStringProperty(String arg0, String arg1) throws JMSException {
    }

    /**
     * @see javax.jms.Message#setObjectProperty(String, Object)
     */
    @Override
    public void setObjectProperty(String arg0, Object arg1) throws JMSException {
    }

    /**
     * @see javax.jms.Message#acknowledge()
     */
    @Override
    public void acknowledge() throws JMSException {
    }

    /**
     * @see javax.jms.Message#clearBody()
     */
    @Override
    public void clearBody() throws JMSException {
    }
}