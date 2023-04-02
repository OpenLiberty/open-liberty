/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package com.ibm.ws.cdi.api.fat.apps.injectInjectionPointParam;

import static org.junit.Assert.assertNotNull;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;

@Dependent
public class TestBean {

    private final BeanManager bm;
    private final InjectionPoint ip;

    @Inject
    public TestBean(BeanManager bm, InjectionPoint ip) {
        this.bm = bm;
        this.ip = ip;
    }

    public void assertBeanManagerAndInjectionPoint() {
        assertNotNull(bm);
        assertNotNull(ip);
    }

}
