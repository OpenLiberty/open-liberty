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
package com.ibm.ws.microprofile.config14.configAccessor.cacheFor.web;

import static org.junit.Assert.assertEquals;

import java.time.temporal.ChronoUnit;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Provider;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Dependent
public class CacheForBean {

    @Inject
    @ConfigProperty(name = "key1", cacheFor = 1000, cacheTimeUnit = ChronoUnit.MILLIS)
    Provider<String> value;

    public void cacheForTest() {
        assertEquals("Value not correctly resolved", "value1", value.get());
    }

}
