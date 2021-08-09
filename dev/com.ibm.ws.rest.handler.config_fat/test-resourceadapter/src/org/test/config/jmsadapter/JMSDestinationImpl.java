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

import javax.jms.Destination;
import javax.resource.spi.AdministeredObject;
import javax.resource.spi.ConfigProperty;

@AdministeredObject(adminObjectInterfaces = Destination.class)
public class JMSDestinationImpl implements Destination {
    @ConfigProperty
    private String destinationName;

    public String getDestinationName() {
        return destinationName;
    }

    public void setDestinationName(String value) {
        this.destinationName = value;
    }
}
