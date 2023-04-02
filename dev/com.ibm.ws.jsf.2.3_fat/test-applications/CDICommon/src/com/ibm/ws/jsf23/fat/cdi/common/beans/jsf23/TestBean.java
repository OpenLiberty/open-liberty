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
package com.ibm.ws.jsf23.fat.cdi.common.beans.jsf23;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.inject.Inject;

import com.ibm.ws.jsf23.fat.cdi.common.beans.injected.FieldBean;
import com.ibm.ws.jsf23.fat.cdi.common.beans.injected.MethodBean;
import com.ibm.ws.jsf23.fat.cdi.common.beans.injected.TestBeanType;

/**
 * Bean that tests field and method injection. No constructor injection.
 */
@ManagedBean(name = "testBean")
@SessionScoped
public class TestBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private String data = ":TestBean:";

    // Field Injected bean
    @Inject
    @TestBeanType
    private FieldBean _fieldBean;
    //private final FieldBean _fieldBean = null;

    private MethodBean _methodBean = null;

    @PostConstruct
    public void start() {
        data += ":PostConstructCalled:";
    }

    @PreDestroy
    public void stop() {
        System.out.println("TestBean preDestroy called.");
    }

    // Method Injected bean
    @Inject
    public void setMethodBean(MethodBean bean) {
        _methodBean = bean;
    }

    public void setData(String newData) {
        this.data += newData;
    }

    public String getData() {

        if (_fieldBean == null)
            data += ":FieldInjectionFailed:";
        else
            data += _fieldBean.getData();

        if (_methodBean == null)
            data += ":MethodInjectionFailed:";
        else
            data += _methodBean.getData();

        return data;
    }

    public String nextPage() {
        return "TestBean";
    }

}
