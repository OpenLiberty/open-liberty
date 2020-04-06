/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package cdi.beans;

import javax.enterprise.inject.Produces;

/**
 *
 */
public class FieldBean {

    protected String value;

    /**
     * @param string
     */
    public void setData(String string) {
        value = string;
    }

    public String getData() {
        return this.getClass() + (value == null ? ":" : ":" + value);
    }

    private final String producerText = ":ProducerInjected:";

    @Produces
    @ProducerType
    String getProducerType() {
        return producerText;
    }
}
