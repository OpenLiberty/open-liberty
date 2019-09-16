/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.test.config.jmsadapter;

import javax.jms.MessageListener;
import javax.resource.ResourceException;
import javax.resource.spi.Activation;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.ConfigProperty;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.ResourceAdapter;

@Activation(messageListeners = MessageListener.class)
public class JMSActivationSpecImpl implements ActivationSpec {
    private ResourceAdapter adapter;

    @ConfigProperty(type = String.class)
    private String destination;

    @ConfigProperty(type = String.class, defaultValue = "javax.jms.Topic")
    private String destinationType;

    @ConfigProperty
    private String messageSelector;

    public String getDestination() {
        return destination;
    }

    public String getDestinationType() {
        return destinationType;
    }

    public String getMessageSelector() {
        return messageSelector;
    }

    @Override
    public ResourceAdapter getResourceAdapter() {
        return adapter;
    }

    public void setDestination(String value) {
        this.destination = value;
    }

    public void setDestinationType(String value) {
        this.destinationType = value;
    }

    public void setMessageSelector(String value) {
        this.messageSelector = value;
    }

    @Override
    public void setResourceAdapter(ResourceAdapter adapter) throws ResourceException {
        this.adapter = adapter;
    }

    @Override
    public void validate() throws InvalidPropertyException {
    }
}