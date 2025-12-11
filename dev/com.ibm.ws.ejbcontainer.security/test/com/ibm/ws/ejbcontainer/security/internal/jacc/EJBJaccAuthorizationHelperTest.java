/*******************************************************************************
 * Copyright (c) 2015, 2024 IBM Corporation and others.
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

package com.ibm.ws.ejbcontainer.security.internal.jacc;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.Principal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.ejb.EnterpriseBean;
import javax.security.auth.Subject;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.ejbcontainer.EJBComponentMetaData;
import com.ibm.ws.ejbcontainer.EJBMethodInterface;
import com.ibm.ws.ejbcontainer.EJBMethodMetaData;
import com.ibm.ws.ejbcontainer.EJBRequestData;
import com.ibm.ws.ejbcontainer.InternalConstants;
import com.ibm.ws.ejbcontainer.security.internal.EJBAccessDeniedException;
import com.ibm.ws.ejbcontainer.security.jacc.EJBJaccService;
import com.ibm.ws.security.authentication.principals.WSPrincipal;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

import test.common.SharedOutputManager;

public class EJBJaccAuthorizationHelperTest {
    static final SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    static final String KEY_EJB_JACC_SERVICE = "eJBJaccService";

    private final Mockery context = new JUnit4Mockery();
    private final EJBRequestData erd = context.mock(EJBRequestData.class);
    private final EJBMethodMetaData emmd = context.mock(EJBMethodMetaData.class);
    private final EJBComponentMetaData ecmd = context.mock(EJBComponentMetaData.class);
    private final J2EEName jen = context.mock(J2EEName.class);
    @SuppressWarnings("unchecked")
    private final ServiceReference<EJBJaccService> jsr = context.mock(ServiceReference.class, "jaccServiceRef");
    private final EJBJaccService js = context.mock(EJBJaccService.class);
    private final ComponentContext cc = context.mock(ComponentContext.class);
    private final EnterpriseBean eb = context.mock(EnterpriseBean.class);
    private final WSPrincipal wp = new WSPrincipal("securityName", "accessId", "BASIC");
    private final AtomicServiceReference<EJBJaccService> ajsr = new AtomicServiceReference<EJBJaccService>(KEY_EJB_JACC_SERVICE);

    /**
     * Tests authorizeEJB method normal role.
     * Expected result: valid output
     */
    @Test
    public void authorizeEJBNormalNoMethodArgDenied() {
        final String METHOD_NAME = "endsWith";
        final String METHOD_SIGNATURE = "signature";
        final String METHOD_INTERFACE_NAME = EJBMethodInterface.forValue(InternalConstants.METHOD_INTF_LOCAL).specName();
        final String APP_NAME = "ApplicationName";
        final String MODULE_NAME = "ModuleName";
        final String BEAN_NAME = "BeanName";
        final Set<Principal> principals = new HashSet<Principal>();
        final Set<?> credentials = new HashSet<String>();
        principals.add(wp);
        final Subject SUBJECT = new Subject(false, principals, credentials, credentials);

        context.checking(new Expectations() {
            {
                allowing(erd).getEJBMethodMetaData();
                will(returnValue(emmd));
                allowing(erd).getMethodArguments();
                will(returnValue(null));
                allowing(emmd).getEJBComponentMetaData();
                will(returnValue(ecmd));
                allowing(ecmd).getJ2EEName();
                will(returnValue(jen));
                allowing(jen).getApplication();
                will(returnValue(APP_NAME));
                allowing(jen).getModule();
                will(returnValue(MODULE_NAME));
                allowing(jen).getComponent();
                will(returnValue(BEAN_NAME));
                allowing(emmd).getEJBMethodInterface();
                will(returnValue(EJBMethodInterface.forValue(InternalConstants.METHOD_INTF_LOCAL)));
                allowing(emmd).getMethodSignature();
                will(returnValue(METHOD_SIGNATURE));
                allowing(emmd).getMethodName();
                will(returnValue(METHOD_NAME));
                allowing(erd).getBeanInstance();
                will(returnValue(new String()));
                allowing(jsr).getProperty(Constants.SERVICE_ID);
                will(returnValue(0L));
                allowing(jsr).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));
                one(cc).locateService("eJBJaccService", jsr);
                will(returnValue(js));
                one(js).isAuthorized(APP_NAME, MODULE_NAME, BEAN_NAME, METHOD_NAME, METHOD_INTERFACE_NAME, METHOD_SIGNATURE, null, null, SUBJECT);
                will(returnValue(false));
            }
        });
        ajsr.setReference(jsr);
        ajsr.activate(cc);
        EJBJaccAuthorizationHelper ejah = new EJBJaccAuthorizationHelper(ajsr);
        try {
            ejah.authorizeEJB(erd, SUBJECT);
            fail("EJBAcessDeniedException is not caught");
        } catch (EJBAccessDeniedException ee) {
            // success
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("An unexpected exception is caught : " + e);
        }
    }

    /**
     * Tests authorizeEJB method normal role with valid bean and method arg.
     * Expected result: valid output
     */
    @Test
    public void authorizeEJBNormalMethodArgEenterpriseBeanGranted() {
        final String METHOD_NAME = "endsWith";
        final String METHOD_SIGNATURE = "signature";
        final String METHOD_INTERFACE_NAME = EJBMethodInterface.forValue(InternalConstants.METHOD_INTF_LOCAL).specName();
        final String APP_NAME = "ApplicationName";
        final String MODULE_NAME = "ModuleName";
        final String BEAN_NAME = "BeanName";
        final Set<Principal> principals = new HashSet<Principal>();
        final Set<?> credentials = new HashSet<String>();
        principals.add(wp);
        final Subject SUBJECT = new Subject(false, principals, credentials, credentials);
        final Object[] ARG_LIST = new Object[1];
        ARG_LIST[0] = new String();

        context.checking(new Expectations() {
            {
                allowing(erd).getEJBMethodMetaData();
                will(returnValue(emmd));
                allowing(erd).getMethodArguments();
                will(returnValue(ARG_LIST));
                allowing(emmd).getEJBComponentMetaData();
                will(returnValue(ecmd));
                allowing(ecmd).getJ2EEName();
                will(returnValue(jen));
                allowing(jen).getApplication();
                will(returnValue(APP_NAME));
                allowing(jen).getModule();
                will(returnValue(MODULE_NAME));
                allowing(jen).getComponent();
                will(returnValue(BEAN_NAME));
                allowing(emmd).getEJBMethodInterface();
                will(returnValue(EJBMethodInterface.forValue(InternalConstants.METHOD_INTF_LOCAL)));
                allowing(emmd).getMethodSignature();
                will(returnValue(METHOD_SIGNATURE));
                allowing(emmd).getMethodName();
                will(returnValue(METHOD_NAME));
                allowing(erd).getBeanInstance();
                will(returnValue(eb));
                allowing(jsr).getProperty(Constants.SERVICE_ID);
                will(returnValue(0L));
                allowing(jsr).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));
                one(cc).locateService("eJBJaccService", jsr);
                will(returnValue(js));
                one(js).isAuthorized(APP_NAME, MODULE_NAME, BEAN_NAME, METHOD_NAME, METHOD_INTERFACE_NAME, METHOD_SIGNATURE, Arrays.asList(ARG_LIST), eb, SUBJECT);
                will(returnValue(true));
            }
        });
        ajsr.setReference(jsr);
        ajsr.activate(cc);
        EJBJaccAuthorizationHelper ejah = new EJBJaccAuthorizationHelper(ajsr);
        try {
            ejah.authorizeEJB(erd, SUBJECT);
            // success
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("An unexpected exception is caught : " + e);
        }
    }

    /**
     * Tests isCallerInRole method normal role with valid bean and method arg.
     * Expected result: valid output
     */
    @Test
    public void isCallerInRoleFalse() {
        final String METHOD_NAME = "endsWith";
        final String APP_NAME = "ApplicationName";
        final String MODULE_NAME = "ModuleName";
        final String BEAN_NAME = "BeanName";
        final String ROLE = "RoleName";
        final Set<Principal> principals = new HashSet<Principal>();
        final Set<?> credentials = new HashSet<String>();
        principals.add(wp);
        final Subject SUBJECT = new Subject(false, principals, credentials, credentials);
        final Object[] ARG_LIST = new Object[1];
        ARG_LIST[0] = new String();

        context.checking(new Expectations() {
            {
                allowing(ecmd).getJ2EEName();
                will(returnValue(jen));
                allowing(jen).getApplication();
                will(returnValue(APP_NAME));
                allowing(jen).getModule();
                will(returnValue(MODULE_NAME));
                allowing(jen).getComponent();
                will(returnValue(BEAN_NAME));
                one(erd).getEJBMethodMetaData();
                will(returnValue(emmd));
                one(emmd).getMethodName();
                will(returnValue(METHOD_NAME));
                one(erd).getMethodArguments();
                will(returnValue(ARG_LIST));
                exactly(2).of(erd).getBeanInstance();
                will(returnValue(eb));
                allowing(jsr).getProperty(Constants.SERVICE_ID);
                will(returnValue(0L));
                allowing(jsr).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));
                one(cc).locateService("eJBJaccService", jsr);
                will(returnValue(js));
                one(js).isSubjectInRole(APP_NAME, MODULE_NAME, BEAN_NAME, METHOD_NAME, Arrays.asList(ARG_LIST), ROLE, eb, SUBJECT);
                will(returnValue(false));
            }
        });

        ajsr.setReference(jsr);
        ajsr.activate(cc);
        EJBJaccAuthorizationHelper ejah = new EJBJaccAuthorizationHelper(ajsr);
        assertFalse(ejah.isCallerInRole(ecmd, erd, ROLE, null, SUBJECT));
    }

    /**
     * Tests isCallerInRole method normal role with no bean and no method arg.
     * Expected result: valid output
     */
    @Test
    public void isCallerInRoleTrue() {
        final String METHOD_NAME = "endsWith";
        final String APP_NAME = "ApplicationName";
        final String MODULE_NAME = "ModuleName";
        final String BEAN_NAME = "BeanName";
        final String ROLE = "RoleName";
        final Set<Principal> principals = new HashSet<Principal>();
        final Set<?> credentials = new HashSet<String>();
        principals.add(wp);
        final Subject SUBJECT = new Subject(false, principals, credentials, credentials);

        context.checking(new Expectations() {
            {
                allowing(ecmd).getJ2EEName();
                will(returnValue(jen));
                allowing(jen).getApplication();
                will(returnValue(APP_NAME));
                allowing(jen).getModule();
                will(returnValue(MODULE_NAME));
                allowing(jen).getComponent();
                will(returnValue(BEAN_NAME));
                one(erd).getEJBMethodMetaData();
                will(returnValue(emmd));
                one(emmd).getMethodName();
                will(returnValue(METHOD_NAME));
                one(erd).getMethodArguments();
                will(returnValue(new Object[0]));
                one(erd).getBeanInstance();
                will(returnValue(new String()));
                allowing(jsr).getProperty(Constants.SERVICE_ID);
                will(returnValue(0L));
                allowing(jsr).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));
                one(cc).locateService("eJBJaccService", jsr);
                will(returnValue(js));
                one(js).isSubjectInRole(APP_NAME, MODULE_NAME, BEAN_NAME, METHOD_NAME, null, ROLE, null, SUBJECT);
                will(returnValue(true));
            }
        });

        ajsr.setReference(jsr);
        ajsr.activate(cc);
        EJBJaccAuthorizationHelper ejah = new EJBJaccAuthorizationHelper(ajsr);
        assertTrue(ejah.isCallerInRole(ecmd, erd, ROLE, null, SUBJECT));
    }

}
