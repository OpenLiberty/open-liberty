/*
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package com.ibm.ws.jsf22.fat.PI90507;

import java.io.Serializable;

import javax.annotation.PreDestroy;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

/**
 * Simple session scoped bean
 */
@ManagedBean(name = "bean")
@SessionScoped
public class MyBackingBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private TestActionListener testActionListener;

    public TestActionListener getTestActionListener() {
        return testActionListener;
    }

    public void setTestActionListener(TestActionListener testActionListener) {
        this.testActionListener = testActionListener;
    }

    @PreDestroy
    public void preDestroy() {
        System.out.println("MyBackingBean: Session timeout");
    }

}
