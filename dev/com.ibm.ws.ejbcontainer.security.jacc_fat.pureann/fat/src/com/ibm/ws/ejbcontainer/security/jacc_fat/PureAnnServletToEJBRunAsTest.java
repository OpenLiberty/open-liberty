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
 * Performs testing of EJB pure annotations with application bnd information in server.xml where there are several
 * levels of RunAs annotations as described below.
 * a) Servlet SecurityEJBRunAsServlet contains RunAs (Manager) so the EJB it invokes will
 * be run as a user in Manager role.
 * b) First EJB, SecurityEJBA01Bean contains RunAs (Employee) so the EJB it invokes will
 * be run as a user in Employee role. It invokes SecurityEJBRunAsBean.
 * c) Second EJB, SecurityEJBRunAsBean requires Employee role to invoke its method.
 *
 *
 * This test consists of a positive and negative test.
 * 1. For the positive test, the server.xml file names the run as users
 * Employee Role run-as user is user99.
 * Manager Role run-as user is user98.
 * The servlet is invoked with user5, EJB1 is invoked with run-as user user98,
 * and EJB2 is invoked with run-as user user99.
 *
 * 2. For the negative test, the server.xml file is altered to remove
 * run-as user99 by commenting out from server.xml.
 * <!-- <run-as userid="user99" /> -->
 * Manager Role run-as user is user98.
 *
 * The servlet is invoked with user5, EJB1 is invoked with user98. Because the run-as is missing
 * from server.xml, EJB2 is not invoked with user99, but instead with user98. The test verifies that
 * an error message is received for Authorization failed for user98 because the user is not granted access to
 * the Employee role.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class PureAnnServletToEJBRunAsTest extends EJBAnnTestBase {

    protected static Class<?> logClass = PureAnnServletToEJBRunAsTest.class;

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {

        Log.info(logClass, "setUp", "Starting the server....");
        commonSetup(logClass, Constants.SERVER_EJB,
                    Constants.APPLICATION_SECURITY_EJB, Constants.SERVLET_SECURITY_EJB_RUNAS, Constants.CONTEXT_ROOT_SECURITY_EJB);

    }

    protected TestName getName() {
        return name;
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> A user, user5, accesses SecurityEJBRunAsServlet. This servlet has @RunAs (Manager) annotation.
     * <LI> SecurityEJBRunAsServlet invokes SecurityEJBA01Bean with run-as user98. SecurityEJBA01Bean has @RunAs (Employee) annotation.
     * <LI> SecurityEJBA01Bean invokes a second EJB, SecurityEJBRunAsBean, with run-as user99 in Employee role.
     * <LI> The test verifies that the SecurityEJBRunAsBean is invoked successfully with run-as user99 in Employee role.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Manager role allowed access to first EJB method which invokes second EJB that requires Employee role with run-as user99.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureAnnRunAs_RunAsSpecified_PermitAccessEmployee() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());
        String waitForMessage = "CWWKT0016I.*/securityejb/";
        List<String> msgs = new ArrayList<String>();
        msgs.add(waitForMessage);
        testHelper.reconfigureServer(Constants.SERVLET_TO_EJB_RUNAS_SERVER_XML, name.getMethodName(), msgs, Constants.DO_NOT_RESTART_SERVER);
        String queryString = "/SimpleRunAsServlet?testInstance=ejb01&testMethod=runAsSpecified";
        String response = generateResponseFromServlet(queryString, Constants.DECLARED_ROLE_USER, Constants.DECLARED_ROLE_USER_PWD);
        verifyResponseWithoutDeprecated(response, Constants.RUN_AS_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_TRUE);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> A user, user5, accesses SecurityEJBRunAsServlet. This servlet has @RunAs (Manager) annotation.
     * <LI> SecurityEJBRunAsServlet invokes SecurityEJBA01Bean with run-as user98. SecurityEJBA01Bean has @RunAs (Employee) annotation.
     * <LI> SecurityEJBA01Bean invokes a second EJB, SecurityEJBRunAsBean which requires Employee role. Since the
     * <LI> server.xml file has the run-as user99 commented out, the previous identity, user98 is used in the access attempt.
     * <LI> The test verifies that access is denied for user98 in Manager role because Employee role is required.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Test verifies that error message specifies that user98 is denied access because role Employee is required.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureAnnRunAs_RunAsSpecified_serverXML_MissingRunAs() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());
        String waitForMessage = "CWWKT0016I.*/securityejb/";
        List<String> msgs = new ArrayList<String>();
        msgs.add(waitForMessage);
        testHelper.reconfigureServer(Constants.SERVLET_TO_EJB_RUNAS_MISSING, name.getMethodName(), msgs, Constants.DO_NOT_RESTART_SERVER);
        String queryString = "/SimpleRunAsServlet?testInstance=ejb01&testMethod=runAsSpecified";
        String response = generateResponseFromServlet(queryString, Constants.DECLARED_ROLE_USER, Constants.DECLARED_ROLE_USER_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE, Constants.RUN_AS_USER2,
                                       Constants.EMPLOYEE_METHOD);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

}