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
package com.ibm.ws.microprofile.config14.nullDefaultInjection.web;

import static org.junit.Assert.assertNull;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Dependent
public class NullDefaultInjectionBean {

    @Inject
    @ConfigProperty(defaultValue = ConfigProperty.NULL_VALUE)
    String property;

    public void nullDefaultInjectionTest() {
        assertNull("Property is not null: " + property, property);
    }
}
