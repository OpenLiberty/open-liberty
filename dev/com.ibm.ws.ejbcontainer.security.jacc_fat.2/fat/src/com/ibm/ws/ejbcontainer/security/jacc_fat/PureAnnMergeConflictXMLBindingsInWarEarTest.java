/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.security.jacc_fat;

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
 * Performs testing of EJB pure annotations with a Stateless bean and role definitions split across server.xml and ibm-application-bnd.xml
 * This tests the class level annotations RunAs (Employee), RolesAllowed (Manager) and DeclareRoles(DeclaredRole01) with a
 * variety of method level annotations. This test invokes SecurityEJBA01Bean methods with a variety of method signatures to insure that
 * annotations are processed correctly with methods of the same name and different signature.
 *
 * This test extends PureAnnA01Base, so it runs the same set of tests in PureAnnA01Test with
 * a variation in the configuration -- the Manager role mapping is defined in server.xml and the Employee role mapping is
 * defined in ibm-application-bnd.xml. Then there are dynamic update tests which cover conflicts in server.xml to show that
 * server.xml takes precedence. These conflict tests introduce an Employee role section to server.xml where 1) a user4 which does not
 * exist in ibm-application-bnd is added to server.xml Employee role, 2) Group 1 is removed from server.xml but exists in
 * ibm-application-bnd.xml for Employee role, and 3) the user to be used in run-as is changed from user99 in ibm-application-bnd.xml to
 * user98 in server.xml.
 *
 * UPDATE: FOR Jacc we do not run the PureAnnTestBase
 *
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class PureAnnMergeConflictXMLBindingsInWarEarTest extends EJBAnnTestBase {

    protected static Class<?> logClass = PureAnnMergeConflictXMLBindingsInWarEarTest.class;

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {

        Log.info(logClass, "setUp", "Starting the server....");
        commonSetup(logClass, Constants.SERVER_EJB_MERGE_BINDINGS,
                    Constants.APPLICATION_SECURITY_EJB_INWAR_EAR_XMLMERGE, Constants.SERVLET_SECURITY_EJB, Constants.CONTEXT_ROOT_SECURITY_EJB_INWAR_EAR_XMLMERGE);

    }

    protected TestName getName() {
        return name;
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> This test shows that when a group is removed from server.xml but exists in ibm-application-bnd.xml that server.xml
     * <LI> takes precedence.
     * <LI> Attempt to access an EJB method injected into a servlet where the RolesAllowed
     * <LI> annotation at method level (Employee Manager) overrides the class level (Manager).
     * <LI> This test covers invoking the EJB method employeeAndManagerwithParams(String,String) with two String parameters.
     * * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Access is denied for a userId in Employee role in ibm-application-bnd.xml which has been removed from server.xml.
     * <LI> UPDATE: For JACC the user should be granted permission since the JACC ignores the ibm-application-bnd.xml config.
     * </OL>
     */
    @Test
    public void testPureAnnMergeConflict_EmployeeAndManagerWithParams_DenyAccessUserRemovedFromEmployeeRoleServerXML() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        try {
            testHelper.reconfigureServer(Constants.MERGE_CONFLICT_RUNAS_SERVER_XML, getName().getMethodName(), Constants.DO_NOT_RESTART_SERVER);

            String queryString = "/SimpleServlet?testInstance=ejb01&testMethod=employeeAndManagerwithParams";
            String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
            verifyResponse(response, Constants.EMPLOYEE_USER_PRINCIPAL, Constants.EMPLOYEE_USER_IDENTITY, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_TRUE);
        } finally {
            testHelper.reconfigureServer(Constants.DEFAULT_MERGE_SERVER_XML, getName().getMethodName(), Constants.DO_NOT_RESTART_SERVER);
        }

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> This test shows that new user4 introduced in Employee role in server.xml takes precedence over ibm-application-bnd.xml.
     * <LI> Attempt to access an EJB method injected into a servlet where the RolesAllowed
     * <LI> annotation at method level (Employee Manager) overrides the class level (Manager).
     * <LI> This test covers invoking the EJB method employeeAndManagerwithParams(String,String) with two String parameters.
     * * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId (user4) in Employee role in server.xml is allowed to access the EJB method even when same user is
     * <LI> not in ibm-application-bnd.xml.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureAnnMergeConflict_EmployeeAndManagerWithParams_PermitAccessNewUserInEmployeeRoleInServerXML() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        try {
            testHelper.reconfigureServer(Constants.MERGE_CONFLICT_RUNAS_SERVER_XML, getName().getMethodName(), Constants.DO_NOT_RESTART_SERVER);
            String queryString = "/SimpleServlet?testInstance=ejb01&testMethod=employeeAndManagerwithParams";
            String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_CONFLICT_USER, Constants.EMPLOYEE_CONFLICT_PWD);
            verifyResponse(response, Constants.EMPLOYEE_CONFLICT_USER_PRINCIPAL, Constants.EMPLOYEE_CONFLICT_USER_IDENTITY, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_TRUE);
        } finally {
            testHelper.reconfigureServer(Constants.DEFAULT_MERGE_SERVER_XML, getName().getMethodName(), Constants.DO_NOT_RESTART_SERVER);
        }

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> This tests a conflict in role mappings in server.xml vs ibm-application-bnd.xml to show that server.xml
     * <LI> overrides the definitions in ibm-application-bnd.xml. Server.xml is modified to use a different run-as user name
     * <LI> (user98) vs the ibm-application-bnd (user99).
     * <LI>
     * <LI> Attempt to access an EJB method injected into a servlet with a class level RunAs (Employee). There is no
     * <LI> annotation at method level in the invoked EJB (SecurityEJBA01Bean) so it is invoked as Manager role. This first
     * <LI> EJB injects and invokes a second EJB (SecurityRunAsBean) which requires Employee role to access the employee method.
     * <LI> The RunAs (Employee) annotation results in the second EJB employee being invoked successfully with run-as user defined
     * <LI> in the server.xml - user98.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Manager role allowed access to first EJB method which invokes second EJB that requires Employee role with run-as user as
     * <LI> defined in server.xml.
     * <LI>
     * <LI>
     * </OL>
     */
    @Test
    public void testPureAnnMergeConflict_RunAsSpecified_AllowAccessDifferentRunAsUserInServerXml() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        try {
            testHelper.reconfigureServer(Constants.MERGE_CONFLICT_RUNAS_SERVER_XML, getName().getMethodName(), Constants.DO_NOT_RESTART_SERVER);
            String queryString = "/SimpleServlet?testInstance=ejb01&testMethod=runAsSpecified";
            String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
            verifyResponseWithoutDeprecated(response, Constants.RUN_AS_USER2, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_TRUE);
        } finally {
            testHelper.reconfigureServer(Constants.DEFAULT_MERGE_SERVER_XML, getName().getMethodName(), Constants.DO_NOT_RESTART_SERVER);
        }

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

}