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
package com.ibm.ws.jaxrs.fat.contextresolver.jaxb;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRegistry;

import com.ibm.ws.jaxrs.fat.contextresolver.User;

@XmlRegistry
public class ObjectFactory {

    @XmlElement(name = "user", namespace = "http://jaxb.context.tests")
    public User createUser() {
        return new User();
    }

}
