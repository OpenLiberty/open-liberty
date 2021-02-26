/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.client.fat;

import javax.annotation.Resource;
import javax.ejb.Remote;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.mail.MailSessionDefinition;
import javax.mail.MailSessionDefinitions;
import javax.mail.Session;
import javax.validation.ValidatorFactory;

import com.ibm.ws.security.client.fat.view.MySimpleInjectionBeanRemote;

/**
 * Session Bean implementation class MySimpleInjectionBean
 */
@Singleton
@Startup
@Remote(MySimpleInjectionBeanRemote.class)
@MailSessionDefinitions({
    @MailSessionDefinition(
            from="smtp@testserver.com", 
            user="imap@testserver.com", 
            password="imapPa$$word4U2C",
            host="localhost",
            name="java:global/env/myMailSession",
            storeProtocol="imap",
            description="global scoped mailSession",
            properties={"mail.imap.host=localhost", "mail.imap.port=7890", "scope=global"}),
    @MailSessionDefinition(
            from="smtp@testserver.com", 
            user="imap@testserver.com", 
            password="imapPa$$word4U2C",
            host="localhost",
            name="java:app/env/myMailSession",
            storeProtocol="imap",
            description="app scoped mailSession",
            properties={"mail.imap.host=localhost", "mail.imap.port=7890", "scope=app"})
})
public class MySimpleInjectionBean implements MySimpleInjectionBeanRemote {

    /*
     * <mailSession mailSessionID="testIMAPMailSession" 
                 jndiName="TestingApp/IMAPMailSessionServlet/testIMAPMailSession"
                 description="mailSession for testing IMAP protocol"
                 storeProtocol="imap"
                 host="localhost"
                 user="imap@testserver.com"
                 password="imapPa$$word4U2C"
                 from="smtp@testserver.com"                 >
        <property name="mail.imap.host" value="localhost" />
        <property name="mail.imap.port" value="${bvt.prop.imap_port}" />
        <property name="scope" value="global" />
     */
    @Resource(name="java:global/env/myValidatorFactory")
    ValidatorFactory validatorFactory;

    @Resource(lookup="java:global/env/myMailSession")
    Session mailSessionGlobal;
    
    @Resource(lookup="java:app/env/myMailSession")
    Session mailSessionApp;

    /**
     * Default constructor. 
     */
    public MySimpleInjectionBean() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public int add(int x, int y) {
        return x+y;
    }

    @Override
    public String getMailSessionScopeProperty(String sessionName) {
        Session session;
    
        if ("global".equals(sessionName)) {
            session = mailSessionGlobal;
        } else {
            session = mailSessionApp;
        }

        return session.getProperty("scope");
    }
}

