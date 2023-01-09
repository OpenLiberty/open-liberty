/*******************************************************************************
 * Copyright (c) 2006, 2021 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.injection.mix.ejbint;

import static javax.ejb.TransactionManagementType.BEAN;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;

import javax.ejb.CreateException;
import javax.ejb.RemoteHome;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.interceptor.ExcludeClassInterceptors;
import javax.interceptor.Interceptors;
import javax.sql.DataSource;

/**
 * Class for testing injection into datasource fields of an interceptor class
 * to show that authentication alias and custom-login-configuration specified
 * in bindings file gets used.
 */
@SuppressWarnings("serial")
@Stateless
@TransactionManagement(BEAN)
@RemoteHome(StatelessInterceptorInjectionRemoteHome.class)
@Interceptors({ AnnotationDSInjectionInterceptor.class, XMLDSInjectionInterceptor.class })
public class StatelessInterceptorInjectionBean implements SessionBean {
    private static final String PASSED = "Passed";

    /**
     * Resource reference to datasource when authentication alias is specified
     * in the resource ref binding in ibm-ejb-jar-bnd.xml file.
     */
    private DataSource ivAuthAliasDS;

    /**
     * Resource reference to datasource when custom-login-configuration is used
     * in the resource ref binding in ibm-ejb-jar-bnd.xml file.
     */
    private DataSource ivCustomLoginDS;

    /**
     * Set reference to datasource when authentication alias is specified
     * in the resource ref binding in ibm-ejb-jar-bnd.xml file.
     */
    public void setAuthAliasDS(DataSource ds) {
        ivAuthAliasDS = ds;
    }

    /**
     * Set reference to datasource when custom-login-configuration is used
     * in the resource ref binding in ibm-ejb-jar-bnd.xml file.
     */
    public void setCustomLoginDS(DataSource ds) {
        ivCustomLoginDS = ds;
    }

    /**
     * SessionContext.
     */
    SessionContext ivCtx;

    /**
     * Test injection of datasource references into an interceptor
     * class that uses the @Resource annotation when authentication alias
     * is specified in the resource ref binding in ibm-ejb-jar-bnd.xml file.
     */
    @ExcludeClassInterceptors
    @Interceptors({ AnnotationDSInjectionInterceptor.class })
    public String getAnnotationDSInterceptorResults() throws Exception {
        // Verify following resource ref in the bindings file:
        //
        //       <resource-ref name="AnnotationDS/jdbc/dsAuthAlias" binding-name="...">
        //            <authentication-alias name="dsAuthAlias" />
        //       </resource-ref>

        System.out.println("StatelessInterceptorInjectionBean: getAnnotationDSInterceptorResults");

        assertNotNull("ivAuthAliasDS AnnotationDS not injected", ivAuthAliasDS);
        assertNotNull("ivCustomLoginDS AnnotationDS not injected", ivCustomLoginDS);
        Connection con = ivCustomLoginDS.getConnection();

        assertTrue("ivCustomLoginDS did not have properties set", InjectionMixLoginModule.annCustomLoginModuleLogin);

        return PASSED;

    }

    /**
     * Test injection of datasource references into an interceptor
     * class that uses the <injection-target> inside of a <resource-ref>
     * that is within a <interceptor> stanza in the ejb-jar.xml file
     * and the custom-login-configuration is used
     * in the resource ref binding in ibm-ejb-jar-bnd.xml file.
     */
    @ExcludeClassInterceptors
    @Interceptors({ XMLDSInjectionInterceptor.class })
    public String getXMLDSInterceptorResults() throws Exception {
        // Verify following resource ref in the bindings file:
        //
        //       <resource-ref name="XMLDS/jdbc/dsAuthAlias" binding-name="jdbc/dsCustomLogin">
        //            <authentication-alias name="dsAuthAlias" />
        //       </resource-ref>

        System.out.println("StatelessInterceptorInjectionBean: getXMLDSInterceptorResults");

        assertNotNull("ivAuthAliasDS xmlDS not injected", ivAuthAliasDS);
        assertNotNull("ivCustomLoginDS xmlDS not injected", ivCustomLoginDS);
        Connection con = ivCustomLoginDS.getConnection();

        assertTrue("ivCustomLoginDS did not have properties set", InjectionMixLoginModule.xmlCustomLoginModuleLogin);

        return PASSED;
    }

    public void ejbCreate() throws CreateException {}

    @Override
    public void ejbRemove() {}

    @Override
    public void ejbActivate() {}

    @Override
    public void ejbPassivate() {}

    @Override
    public void setSessionContext(SessionContext sc) {
        ivCtx = sc;
    }
}
