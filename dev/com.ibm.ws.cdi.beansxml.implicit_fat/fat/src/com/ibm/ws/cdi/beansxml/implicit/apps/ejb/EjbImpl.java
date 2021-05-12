/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.beansxml.implicit.apps.ejb;

import javax.inject.Inject;

public class EjbImpl implements FirstManagedBeanInterface, SecondManagedBeanInterface {

    private OtherManagedSimpleBean bean;

    @Inject
    public EjbImpl(OtherManagedSimpleBean injected) {
        this.bean = injected;
    }

    public EjbImpl() {
        throw new RuntimeException("Wrong Constructor called: EjbImpl()");
    }

    @Override
    public void setValue1(String value) {
        bean.setOtherValue(value);
    }

    @Override
    public String getValue1() {
        return bean.getOtherValue();
    }

    @Override
    public void setValue2(String value) {
        bean.setOtherValue(value);
    }

    @Override
    public String getValue2() {
        return bean.getOtherValue();
    }
}
