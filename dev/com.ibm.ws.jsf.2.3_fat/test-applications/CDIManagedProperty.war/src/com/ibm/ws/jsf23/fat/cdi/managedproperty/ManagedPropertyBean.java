/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
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
package com.ibm.ws.jsf23.fat.cdi.managedproperty;

import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.faces.annotation.ManagedProperty;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * A CDI Bean used to test ManagedProperty injection.
 *
 */
@Named
@RequestScoped
public class ManagedPropertyBean {

    @Inject
    @ManagedProperty("#{testBean.number}")
    private int numberManagedProperty;

    @Inject
    @ManagedProperty("#{testBean.text}")
    private String textManagedProperty;

    @Inject
    @ManagedProperty("#{testBean.list}")
    private List<String> listManagedProperty;

    @Inject
    @ManagedProperty("#{testBean.stringArray}")
    private String[] stringArrayManagedProperty;

    @Inject
    @ManagedProperty("#{testBean}")
    private TestBean bean;

    public String test() {

        return "numberManagedProperty = " + numberManagedProperty +
               "    textManagedProperty =  " + textManagedProperty +
               "    listManagedProperty = " + listManagedProperty.get(0) +
               "    stringArrayManagedProperty = " + stringArrayManagedProperty[0] +
               "    bean = " + bean.toString();

    }
}
