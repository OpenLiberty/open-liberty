/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.cdi.api.fat.apps.current.sharedLib;

import static org.junit.Assert.assertTrue;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;

@Dependent
public class SharedLibBean {

    @Inject
    private Instance<ISimpleBean> injectedInstance;

    public void testCdiCurrentSharedLib() {
        CDI<Object> cdi = CDI.current();

        // OK, we can get CDI.current, but does it return the right beans?
        // We should not be able to see a bean from the .war

        // Test with injected instance
        assertTrue("injectedInstance was satisfied", injectedInstance.isUnsatisfied());
        // Test with CDI.current()
        assertTrue("current instance was satisfied", cdi.select(ISimpleBean.class).isUnsatisfied());
    }

}
