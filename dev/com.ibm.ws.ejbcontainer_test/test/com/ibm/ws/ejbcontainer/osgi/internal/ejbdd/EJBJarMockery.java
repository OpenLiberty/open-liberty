/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi.internal.ejbdd;

import java.util.ArrayList;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;

import com.ibm.ws.javaee.dd.ejb.EJBJar;
import com.ibm.ws.javaee.dd.ejb.EnterpriseBean;
import com.ibm.ws.javaee.dd.ejb.Interceptors;

public class EJBJarMockery {
    private final Mockery mockery;
    private String moduleName;
    private boolean metadataComplete;
    private final List<EnterpriseBean> enterpriseBeans = new ArrayList<EnterpriseBean>();
    private int version = EJBJar.VERSION_3_0;
    private Interceptors interceptors = null;

    public EJBJarMockery(Mockery mockery) {
        this.mockery = mockery;
    }

    public EJBJarMockery moduleName(String name) {
        this.moduleName = name;
        return this;
    }

    public EJBJarMockery versionId(int version) {
        this.version = version;
        return this;
    }

    public EJBJarMockery metadataComplete() {
        this.metadataComplete = true;
        return this;
    }

    public EJBJarMockery enterpriseBean(EnterpriseBean bean) {
        this.enterpriseBeans.add(bean);
        return this;
    }

    public EJBJarMockery interceptors(String... interceptorClassNames) {
        this.interceptors = new InterceptorsImpl(interceptorClassNames);
        return this;
    }

    public SessionMockery session(String name) {
        return new SessionMockery(mockery, name);
    }

    public MessageDrivenMockery messageDriven(String name) {
        return new MessageDrivenMockery(mockery, name);
    }

    public EntityMockery entity(String name, int persistenceType) {
        return new EntityMockery(mockery, name, persistenceType);
    }

    public EJBJar mock() {
        final EJBJar ejbJar = mockery.mock(EJBJar.class);
        mockery.checking(new Expectations() {
            {
                allowing(ejbJar).getModuleName();
                will(returnValue(moduleName));

                allowing(ejbJar).getVersionID();
                will(returnValue(version));

                allowing(ejbJar).isMetadataComplete();
                will(returnValue(metadataComplete));

                allowing(ejbJar).getEnterpriseBeans();
                will(returnValue(enterpriseBeans));

                allowing(ejbJar).getInterceptors();
                will(returnValue(interceptors));

            }
        });
        return ejbJar;
    }
}
