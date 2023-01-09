/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
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
package com.ibm.ws.microprofile.appConfig.cdi.test;

import org.eclipse.microprofile.config.spi.Converter;

/**
 *
 */
public class BlueCarConverter implements Converter<Car<Blue>> {

    /** {@inheritDoc} */
    @Override
    public Car<Blue> convert(String value) {
        if ("".equals(value)) {
            return null;
        } else {
            Car<Blue> blueCar = new Car<Blue>(value, new Blue());
            return blueCar;
        }
    }

}