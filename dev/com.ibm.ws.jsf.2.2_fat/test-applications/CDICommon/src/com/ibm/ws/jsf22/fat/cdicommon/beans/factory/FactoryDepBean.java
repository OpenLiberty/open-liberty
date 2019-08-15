/*
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.jsf22.fat.cdicommon.beans.factory;

import javax.enterprise.context.Dependent;
import javax.faces.context.ExternalContext;
import javax.inject.Inject;

/**
 *
 */
@Dependent
public class FactoryDepBean {

    @Inject
    FactoryAppBean appBean;

    boolean incFirst = true;

    private boolean logFirst = true;

    public void incrementAppCount() {
        if (incFirst) {
            appBean.incrementAndGetCount();
            incFirst = false;
        }
    }

    public void logFirst(ExternalContext ec, String clazz, String method, String message) {
        if (logFirst) {
            logFirst = false;
            ec.log("FactoryDepBean logging - " + clazz + "|" + method + "|" + message);
            appBean.addMessage("FactoryDepBean logging - " + clazz + "|" + method + "|" + getName() + ":" + message);
        }
    }

    public void addMessageNoLogJustFirst(String clazz, String method, String message) {
        if (logFirst) {
            logFirst = false;
            appBean.addMessage("FactoryDepBean logging - " + clazz + "|" + method + "|" + getName() + ":" + message);
        }
    }

    public String getName() {
        return this.getClass().getSimpleName();
    }

}
