/*******************************************************************************
 * Copyright (c) 2015, 2022 IBM Corporation and others.
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

package com.ibm.ws.cdi.visibility.tests.ejb.jarInRar.ejbJar;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

import com.ibm.ws.cdi.visibility.tests.ejb.jarInRar.jar.Amigo;

@LocalBean
@Singleton
@Startup
public class MySingletonStartupBean {

    @Inject
    BeanManager beanManager;

    @Inject
    Instance<Amigo> beanInstance;

    public void testBeanFromJarInRarInjectedIntoEJB() {
        assertFalse("Amigo bean reference is unsatisfied", beanInstance.isUnsatisfied());
        assertFalse("Amigo bean reference is ambiguous", beanInstance.isAmbiguous());
        assertTrue(beanInstance.get().yoQueroBurritos());
    }
}
