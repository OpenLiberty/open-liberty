/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
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

package com.ibm.ws.ejbcontainer.security.jacc_fat;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Performs testing of EJB pure annotations with a Stateless bean and application-bnd role mappings in server.xml.
 * This tests the class level annotations RunAs (Employee), RolesAllowed (Manager) and DeclareRoles(DeclaredRole01) with a
 * variety of method level annotations. This test invokes SecurityEJBA01Bean methods with a variety of method signatures to insure that
 * annotations are processed correctly with methods of the same name and different signature.
 *
 * This test covers the application-bnd role mappings in server.xml while PureAnnAppBndXMLBindingsTest covers
 * application-bnd mappings in ibm-application-bnd.xml.
 *
 * This test covers the positive RunAs testing while PureAnnA05Test covers the negative scenarios.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class PureAnnA01Test extends PureAnnA01Base {

    protected static Class<?> logClass = PureAnnA01Test.class;

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {

        Log.info(logClass, "setUp", "Starting the server....");
        commonSetup(logClass, Constants.SERVER_EJB,
                    Constants.APPLICATION_SECURITY_EJB, Constants.SERVLET_SECURITY_EJB, Constants.CONTEXT_ROOT_SECURITY_EJB);

    }

    @Override
    protected TestName getName() {
        return name;
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet with a class level RunAs (Employee). There is no
     * <LI> annotation at method level in the invoked EJB (SecurityEJBA01Bean) so it is invoked as Manager role. This first
     * <LI> EJB injects and invokes a second EJB (SecurityRunAsBean) which requires Employee role to access the employee method.
     * <LI> The server.xml contains a bad password for the RunAs user, so the caller identity which is the user2 in Manager role will be
     * <LI> used and access will be denied.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Access denied to run-as user with invalid password in server.xml.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA01_RunAsSpecified_DenyAccessBadRunAsPassword() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());
        String waitForMessage = "CWWKT0016I.*/securityejb/";
        List<String> msgs = new ArrayList<String>();
        msgs.add(waitForMessage);

        try {
            testHelper.reconfigureServer(Constants.BAD_RUNAS_PWD_SERVER_XML, getName().getMethodName(), msgs, Constants.DO_NOT_RESTART_SERVER);
            String queryString = "/SimpleServlet?testInstance=ejb01&testMethod=runAsSpecified";
            String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
            verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE,
                                           Constants.MANAGER_USER,
                                           Constants.EMPLOYEE_METHOD);
        } finally {
            testHelper.reconfigureServer(Constants.DEFAULT_CONFIG_FILE, getName().getMethodName(), Constants.DO_NOT_RESTART_SERVER);
        }

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> The server.xml for this test contains a run-as user along with the correct run-as password for the user.
     * <LI> Attempt to access an EJB method injected into a servlet with a class level RunAs (Employee). There is no
     * <LI> annotation at method level in the invoked EJB (SecurityEJBA01Bean) so it is invoked as Manager role. This first
     * <LI> EJB injects and invokes a second EJB (SecurityRunAsBean) which requires Employee role to access the employee method.
     * <LI> The RunAs (Employee) annotation results in the second EJB employee being invoked successfully with run-as user.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Manager role allowed access to first EJB method which invokes second EJB that requires Employee role with run-as user.
     * <LI>
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA01_RunAsSpecified_AllowAccessGoodRunAsPasswordInServerXml() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());
        String waitForMessage = "CWWKT0016I.*/securityejb/";
        List<String> msgs = new ArrayList<String>();
        msgs.add(waitForMessage);

        try {
            testHelper.reconfigureServer(Constants.GOOD_RUNAS_PWD_SERVER_XML, getName().getMethodName(), msgs, Constants.DO_NOT_RESTART_SERVER);
            String queryString = "/SimpleServlet?testInstance=ejb01&testMethod=runAsSpecified";
            String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
            verifyResponseWithoutDeprecated(response, Constants.RUN_AS_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_TRUE);
        } finally {
            testHelper.reconfigureServer(Constants.DEFAULT_CONFIG_FILE, getName().getMethodName(), Constants.DO_NOT_RESTART_SERVER);
        }

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

}