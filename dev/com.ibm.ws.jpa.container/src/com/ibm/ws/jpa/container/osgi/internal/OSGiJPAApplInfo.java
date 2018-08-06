/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.container.osgi.internal;

import java.io.PrintWriter;

import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.jpa.JPAPuId;
import com.ibm.ws.jpa.management.AbstractJPAComponent;
import com.ibm.ws.jpa.management.JPAApplInfo;
import com.ibm.ws.jpa.management.JPAPUnitInfo;
import com.ibm.ws.jpa.management.JPAPXml;
import com.ibm.ws.jpa.management.JPAScopeInfo;
import com.ibm.wsspi.adaptable.module.Container;

public class OSGiJPAApplInfo extends JPAApplInfo {
    private final ApplicationInfo appInfo;

    OSGiJPAApplInfo(AbstractJPAComponent jpaComponent, String name, ApplicationInfo appInfo) {
        super(jpaComponent, name);
        this.appInfo = appInfo;
    }

    @Override
    protected JPAPUnitInfo createJPAPUnitInfo(JPAPuId puId, JPAPXml pxml, JPAScopeInfo scopeInfo) {
        return new OSGiJPAPUnitInfo(this, puId, pxml.getClassLoader(), scopeInfo);
    }

    void introspect(PrintWriter out) {
        doIntrospect(out);
    }

    Container getContainer() {
        return appInfo.getContainer();
    }
}
