/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi.internal.ejbextdd;

import java.util.ArrayList;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;

import com.ibm.ws.javaee.dd.ejbext.EJBJarExt;
import com.ibm.ws.javaee.dd.ejbext.EnterpriseBean;
import com.ibm.ws.javaee.dd.ejbext.MessageDriven;
import com.ibm.ws.javaee.dd.ejbext.Session;

public class EJBJarExtMockery {
    private final Mockery mockery;
    private final List<EnterpriseBean> enterpriseBeans = new ArrayList<EnterpriseBean>();

    public EJBJarExtMockery(Mockery mockery) {
        this.mockery = mockery;
    }

    public EJBJarExtMockery sessionBean(Session bean) {
        this.enterpriseBeans.add(bean);
        return this;
    }

    public EJBJarExtMockery messageDrivenBean(MessageDriven bean) {
        this.enterpriseBeans.add(bean);
        return this;
    }

    public SessionMockery session(String name) {
        return new SessionMockery(mockery, name);
    }

    public MessageDrivenMockery messageDriven(String name) {
        return new MessageDrivenMockery(mockery, name);
    }

    public EJBJarExt mock() {
        final EJBJarExt ejbJarExt = mockery.mock(EJBJarExt.class);
        mockery.checking(new Expectations() {
            {
                allowing(ejbJarExt).getEnterpriseBeans();
                will(returnValue(enterpriseBeans));
            }
        });
        return ejbJarExt;
    }
}