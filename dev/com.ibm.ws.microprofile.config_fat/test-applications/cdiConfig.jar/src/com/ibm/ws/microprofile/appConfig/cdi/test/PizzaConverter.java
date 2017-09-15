/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.appConfig.cdi.test;

import org.eclipse.microprofile.config.spi.Converter;

/**
 *
 */
public class PizzaConverter implements Converter<Pizza> {

    /** {@inheritDoc} */
    @Override
    public Pizza convert(String value) {
        if ("".equals(value)) {
            return null;
        } else {
            String[] portions = value.split(";");
            if (portions.length == 2) {
                return new Pizza(portions[0], Integer.parseInt(portions[1]));
            } else {
                return null;
            }

        }
    }

}