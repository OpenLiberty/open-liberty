/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
package com.ibm.ws.microprofile.appConfig.cdi.libertyTests;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@RequestScoped
public class LibertyBuiltInConverterInjectionBean {

    @Inject
    @ConfigProperty(name = "ATOMIC_INTEGER_KEY")
    AtomicInteger ATOMIC_INTEGER_KEY;

    @Inject
    @ConfigProperty(name = "ATOMIC_LONG_KEY")
    AtomicLong ATOMIC_LONG_KEY;

    /**
     * @return the aTOMIC_INTEGER_KEY
     */
    public AtomicInteger getATOMIC_INTEGER_KEY() {
        return ATOMIC_INTEGER_KEY;
    }

    /**
     * @return the aTOMIC_LONG_KEY
     */
    public AtomicLong getATOMIC_LONG_KEY() {
        return ATOMIC_LONG_KEY;
    }

}
