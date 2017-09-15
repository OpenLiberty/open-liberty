/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
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
import java.util.Arrays;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;

import com.ibm.ws.javaee.dd.ejb.DependsOn;
import com.ibm.ws.javaee.dd.ejb.EnterpriseBean;
import com.ibm.ws.javaee.dd.ejb.NamedMethod;
import com.ibm.ws.javaee.dd.ejb.Session;
import com.ibm.ws.javaee.dd.ejb.Timer;
import com.ibm.ws.javaee.dd.ejb.TransactionalBean;

public class SessionMockery extends ComponentViewableBeanMockery<SessionMockery> {
    private final List<String> localBusiness = new ArrayList<String>();
    private final List<String> remoteBusiness = new ArrayList<String>();
    private boolean localBean;
    private String serviceEndpoint;
    private int type = Session.SESSION_TYPE_UNSPECIFIED;
    private Boolean initOnStartup;
    private DependsOn dependsOn;
    private int transactionType = TransactionalBean.TRANSACTION_TYPE_UNSPECIFIED;
    private NamedMethod timeoutMethod;
    private final List<Timer> timers = new ArrayList<Timer>();
    private Boolean passivationCapable;

    SessionMockery(Mockery mockery, String name) {
        super(mockery, name, EnterpriseBean.KIND_SESSION);
    }

    public SessionMockery localBusiness(String... names) {
        this.localBusiness.addAll(Arrays.asList(names));
        return this;
    }

    public SessionMockery remoteBusiness(String... names) {
        this.remoteBusiness.addAll(Arrays.asList(names));
        return this;
    }

    public SessionMockery localBean() {
        this.localBean = true;
        return this;
    }

    public SessionMockery serviceEndpoint(String serviceEndpoint) {
        this.serviceEndpoint = serviceEndpoint;
        return this;
    }

    public SessionMockery type(int type) {
        this.type = type;
        return this;
    }

    public SessionMockery initOnStartup(boolean initOnStartup) {
        this.initOnStartup = initOnStartup;
        return this;
    }

    public SessionMockery dependsOn(final String... links) {
        this.dependsOn = new DependsOn() {
            @Override
            public List<String> getEjbName() {
                return Arrays.asList(links);
            }
        };
        return this;
    }

    public SessionMockery transactionType(int type) {
        this.transactionType = type;
        return this;
    }

    public SessionMockery timeoutMethod(String name, String... params) {
        this.timeoutMethod = new NamedMethodImpl(name, params);
        return this;
    }

    public SessionMockery timer() {
        this.timers.add(null);
        return this;
    }

    public SessionMockery passivationCapable(boolean passivationCapable) {
        this.passivationCapable = passivationCapable;
        return this;
    }

    public Session mock() {
        final Session session = mockComponentViewableBean(Session.class);
        mockery.checking(new Expectations() {
            {
                allowing(session).getLocalBusinessInterfaceNames();
                will(returnValue(localBusiness));

                allowing(session).getRemoteBusinessInterfaceNames();
                will(returnValue(remoteBusiness));

                allowing(session).isLocalBean();
                will(returnValue(localBean));

                allowing(session).getServiceEndpointInterfaceName();
                will(returnValue(serviceEndpoint));

                allowing(session).getSessionTypeValue();
                will(returnValue(type));

                allowing(session).isSetInitOnStartup();
                will(returnValue(initOnStartup != null));

                allowing(session).isInitOnStartup();
                will(returnValue(initOnStartup != null && initOnStartup));

                allowing(session).getDependsOn();
                will(returnValue(dependsOn));

                allowing(session).getTransactionTypeValue();
                will(returnValue(transactionType));

                allowing(session).getTimeoutMethod();
                will(returnValue(timeoutMethod));

                allowing(session).getTimers();
                will(returnValue(timers));

                allowing(session).isSetPassivationCapable();
                will(returnValue(passivationCapable != null));

                allowing(session).isPassivationCapable();
                will(returnValue(passivationCapable != null && passivationCapable));
            }
        });
        return session;
    }
}
