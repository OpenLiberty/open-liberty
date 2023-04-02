/*******************************************************************************
 * Copyright (c) 2018, 2022 IBM Corporation and others.
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
package com.ibm.ws.jsf23.fat.cdi.common.beans.faces40;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

import com.ibm.ws.jsf23.fat.cdi.common.beans.injected.FieldBean;
import com.ibm.ws.jsf23.fat.cdi.common.beans.injected.MethodBean;
import com.ibm.ws.jsf23.fat.cdi.common.beans.injected.TestBeanType;

/**
 * Bean that tests field and method injection. No constructor injection.
 *
 * This @Named bean is only used during the EE10 Repeat. The jakarta.faces.bean
 * package was removed in Faces 4.0.
 */
@Named("testBean")
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
