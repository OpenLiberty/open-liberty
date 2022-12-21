/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.visibility.tests.vistest.runtimeExtSeeApp;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

import com.ibm.ws.cdi.visibility.tests.vistest.framework.TestingBean;
import com.ibm.ws.cdi.visibility.tests.vistest.framework.VisTester;
import com.ibm.ws.cdi.visibility.tests.vistest.qualifiers.InRuntimeExtSeeApp;

@ApplicationScoped
@InRuntimeExtSeeApp
public class RuntimeExtSeeAppTestingBean implements TestingBean {

    @Inject
    private BeanManager beanManager;

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.cdi.visibility.tests.vistest.framework.TestingBean#doTest()
     */
    @Override
    public String doTest() {
        return VisTester.doTest(beanManager);
    }
}
