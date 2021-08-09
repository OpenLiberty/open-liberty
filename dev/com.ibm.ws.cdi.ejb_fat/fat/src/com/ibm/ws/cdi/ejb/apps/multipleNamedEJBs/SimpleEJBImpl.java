/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.ejb.apps.multipleNamedEJBs;

import javax.inject.Inject;

public class SimpleEJBImpl implements SimpleEJBLocalInterface1, SimpleEJBLocalInterface2 {

    /**
     * Deliberately store state from both interfaces in the same place
     * to test that they don't interfere with each other
     * when injected using different bean names.
     */
    private SimpleManagedBean bean;

    @Inject
    public SimpleEJBImpl(SimpleManagedBean injected) {
        this.bean = injected;
    }

    public SimpleEJBImpl() {
        throw new RuntimeException("Wrong Constructor called: SimpleEJBImpl()");
    }

    @Override
    public String getData1() {
        return bean.getData();
    }

    @Override
    public void setData1(String data) {
        bean.setData(data);
    }

    @Override
    public String getData2() {
        return bean.getData();
    }

    @Override
    public void setData2(String data) {
        bean.setData(data);
    }
}
