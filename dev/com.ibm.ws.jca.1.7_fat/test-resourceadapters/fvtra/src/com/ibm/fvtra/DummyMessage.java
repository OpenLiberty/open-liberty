/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.fvtra;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.TextMessage;

public class DummyMessage implements TextMessage {
    private final Map<String, Object> properties = new HashMap<String, Object>();
    private String text;

    @Override
    public void acknowledge() throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clearBody() throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clearProperties() throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean getBooleanProperty(String arg0) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte getByteProperty(String arg0) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public double getDoubleProperty(String arg0) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public float getFloatProperty(String arg0) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getIntProperty(String arg0) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getJMSCorrelationID() throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] getJMSCorrelationIDAsBytes() throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getJMSDeliveryMode() throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Destination getJMSDestination() throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getJMSExpiration() throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getJMSMessageID() throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getJMSPriority() throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean getJMSRedelivered() throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Destination getJMSReplyTo() throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getJMSTimestamp() throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getJMSType() throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getLongProperty(String arg0) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getObjectProperty(String arg0) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Enumeration<?> getPropertyNames() throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public short getShortProperty(String arg0) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getStringProperty(String name) throws JMSException {
        return (String) properties.get(name);
    }

    @Override
    public String getText() throws JMSException {
        return text;
    }

    @Override
    public boolean propertyExists(String arg0) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setBooleanProperty(String arg0, boolean arg1) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setByteProperty(String arg0, byte arg1) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDoubleProperty(String arg0, double arg1) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setFloatProperty(String arg0, float arg1) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setIntProperty(String arg0, int arg1) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setJMSCorrelationID(String arg0) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setJMSCorrelationIDAsBytes(byte[] arg0) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setJMSDeliveryMode(int arg0) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setJMSDestination(Destination arg0) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setJMSExpiration(long arg0) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setJMSMessageID(String arg0) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setJMSPriority(int arg0) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setJMSRedelivered(boolean arg0) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setJMSReplyTo(Destination arg0) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setJMSTimestamp(long arg0) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setJMSType(String arg0) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setLongProperty(String arg0, long arg1) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setObjectProperty(String arg0, Object arg1) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setShortProperty(String arg0, short arg1) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setStringProperty(String name, String value) throws JMSException {
        properties.put(name, value);
    }

    @Override
    public void setText(String text) throws JMSException {
        this.text = text;
    }

    @Override
    public <T> T getBody(Class<T> arg0) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getJMSDeliveryTime() throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isBodyAssignableTo(Class arg0) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setJMSDeliveryTime(long arg0) throws JMSException {
        throw new UnsupportedOperationException();
    }
}
