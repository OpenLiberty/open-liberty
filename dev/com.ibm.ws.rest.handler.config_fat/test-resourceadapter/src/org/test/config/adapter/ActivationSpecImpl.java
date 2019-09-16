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
package org.test.config.adapter;

import javax.resource.ResourceException;
import javax.resource.cci.MessageListener;
import javax.resource.spi.Activation;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.ConfigProperty;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.ResourceAdapter;

@Activation(messageListeners = MessageListener.class)
public class ActivationSpecImpl implements ActivationSpec {
    private ResourceAdapter adapter;

    @ConfigProperty
    private Short maxSize;

    @ConfigProperty
    private String messageSelector;

    @ConfigProperty(defaultValue = "8")
    private Byte minSize;

    @ConfigProperty(defaultValue = "1.618")
    private Float multiplicationFactor;

    public Short getMaxSize() {
        return maxSize;
    }

    public String getMessageSelector() {
        return messageSelector;
    }

    public Byte getMinSize() {
        return minSize;
    }

    public Float getMultiplicationFactor() {
        return multiplicationFactor;
    }

    @Override
    public ResourceAdapter getResourceAdapter() {
        return adapter;
    }

    public void setMaxSize(Short value) {
        this.maxSize = value;
    }

    public void setMessageSelector(String value) {
        this.messageSelector = value;
    }

    public void setMinSize(Byte value) {
        this.minSize = value;
    }

    public void setMultiplicationFactor(Float value) {
        this.multiplicationFactor = value;
    }

    @Override
    public void setResourceAdapter(ResourceAdapter adapter) throws ResourceException {
        this.adapter = adapter;
    }

    @Override
    public void validate() throws InvalidPropertyException {
    }
}