/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package com.ibm.ws.microprofile.config14.test.apps.characterInjection;

import static org.junit.Assert.assertEquals;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Dependent
public class CharacterInjectionBean {

    @Inject
    @ConfigProperty(name = "char1")
    private Character property1;

    @Inject
    @ConfigProperty(name = "char1")
    private char property2;

    public void characterInjectionTest() {
        assertEquals(new Character('a'), property1);
    }

    public void charInjectionTest() {
        assertEquals('a', property2);
    }
}
