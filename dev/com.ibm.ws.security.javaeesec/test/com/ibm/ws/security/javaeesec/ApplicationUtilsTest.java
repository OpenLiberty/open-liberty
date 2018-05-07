/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;

import com.ibm.ws.container.service.metadata.MetaDataEvent;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfigChangeEvent;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleCoordinator;

/**
 * Test ApplicationUtils.
 */
public class ApplicationUtilsTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private MetaDataEvent<ApplicationMetaData> event1, event2;
    private ApplicationMetaData amd1, amd2;
    private J2EEName j2eename1, j2eename2;
    private ComponentContext cc; 
    private ApplicationRecycleCoordinator arc;
    private WebAppSecurityConfigChangeEvent changeevent1;

    private static final String APP1 = "application1";
    private static final String APP2 = "application2";
    private static final String APP3 = "application3";


    @Before
    public void setUp() throws Exception {
        event1 = mockery.mock(MetaDataEvent.class, "event1");
        event2 = mockery.mock(MetaDataEvent.class, "event2");
        amd1 = mockery.mock(ApplicationMetaData.class, "amd1");
        amd2 = mockery.mock(ApplicationMetaData.class, "amd2");
        j2eename1 = mockery.mock(J2EEName.class, "name1");
        j2eename2 = mockery.mock(J2EEName.class, "name2");
        cc = mockery.mock(ComponentContext.class, "cc");
        arc = mockery.mock(ApplicationRecycleCoordinator.class, "arc");
        changeevent1 = mockery.mock(WebAppSecurityConfigChangeEvent.class, "ce1");
    }

    @After
    public void tearDown() throws Exception {
        mockery.assertIsSatisfied();
    }

    @Test
    public void testRegisterApplication() throws Exception {
        ApplicationUtils au = new ApplicationUtils();
        ApplicationUtils.registerApplication(APP1);
        ApplicationUtils.registerApplication(APP2);
        assertTrue(APP1 + " should exist in the table.", au.isApplicationRegistered(APP1));
        assertTrue(APP2 + " should exist in the table.", au.isApplicationRegistered(APP2));
        assertFalse(APP3 + " should not exist in the table.", au.isApplicationRegistered(APP3));
        assertEquals("The number of applications should be 2.", 2, au.numberOfApplications());
        au.clearApplications();
    }

    @Test
    public void testApplicationMetaDataDestroyed() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(event1).getMetaData();
                will(returnValue(amd1));
                one(amd1).getJ2EEName();
                will(returnValue(j2eename1));
                one(j2eename1).getApplication();
                will(returnValue(APP1));
                one(event2).getMetaData();
                will(returnValue(amd2));
                one(amd2).getJ2EEName();
                will(returnValue(j2eename2));
                one(j2eename2).getApplication();
                will(returnValue(APP2));
            }
        });

        ApplicationUtils au = new ApplicationUtils();
        au.registerApplication(APP1);
        au.registerApplication(APP3);
     
        au.applicationMetaDataDestroyed(event2);
        assertEquals("The number of applications should be 2.", 2, au.numberOfApplications());
        au.applicationMetaDataDestroyed(event1);
        assertEquals("The number of applications should be 1.", 1, au.numberOfApplications());
        au.clearApplications();
    }

    @Test
    public void testNotifyWebAppSecurityConfigChangedRecycle() throws Exception {
        List<String> list = new ArrayList<String>() {
            {
                add("loginFormURL");
            }
        };
        mockery.checking(new Expectations() {
            {
                one(cc).locateService("appCoord");
                will(returnValue(arc));
                one(arc).recycleApplications(with(any(Set.class)));
                one(changeevent1).getModifiedAttributeList();
                will(returnValue(list));
            }
        });

        ApplicationUtils au = new ApplicationUtils();
        au.registerApplication(APP1);
        au.registerApplication(APP3);
     
        au.activate(cc);
        au.notifyWebAppSecurityConfigChanged(changeevent1);
        assertEquals("The number of applications should be 0 after recycle.", 0, au.numberOfApplications());
        au.clearApplications();
    }

    @Test
    public void testNotifyWebAppSecurityConfigChangedNotRecycle() throws Exception {
        List<String> list = new ArrayList<String>() {
            {
                add("unrelated");
            }
        };
        mockery.checking(new Expectations() {
            {
                never(cc).locateService("appCoord");
                one(changeevent1).getModifiedAttributeList();
                will(returnValue(list));
            }
        });

        ApplicationUtils au = new ApplicationUtils();
        au.registerApplication(APP1);
        au.registerApplication(APP3);
     
        au.activate(cc);
        au.notifyWebAppSecurityConfigChanged(changeevent1);
        assertEquals("The number of applications should be 2.", 2, au.numberOfApplications());
        au.clearApplications();
    }

    @Test
    public void testIsAppRestartRequired() throws Exception {
        String CFG_KEY_FAIL_OVER_TO_BASICAUTH = "allowFailOverToBasicAuth";
        String CFG_KEY_LOGIN_FORM_URL = "loginFormURL";
        String CFG_KEY_LOGIN_ERROR_URL = "loginErrorURL";
        String CFG_KEY_ALLOW_FAIL_OVER_TO_AUTH_METHOD = "allowAuthenticationFailOverToAuthMethod";
        String CFG_KEY_OVERRIDE_HAM = "overrideHttpAuthMethod";
        String CFG_KEY_LOGIN_FORM_CONTEXT_ROOT = "contextRootForFormAuthenticationMechanism";
        String CFG_KEY_BASIC_AUTH_REALM_NAME = "basicAuthenticationMechanismRealmName";
        String[] positiveList = {CFG_KEY_FAIL_OVER_TO_BASICAUTH, CFG_KEY_LOGIN_FORM_URL, CFG_KEY_LOGIN_ERROR_URL, CFG_KEY_ALLOW_FAIL_OVER_TO_AUTH_METHOD, CFG_KEY_OVERRIDE_HAM, CFG_KEY_LOGIN_FORM_CONTEXT_ROOT, CFG_KEY_BASIC_AUTH_REALM_NAME};

        for (String value : positiveList) {
            List<String> list = new ArrayList<String>() {
                {
                    add(value);
                }
            };
            assertTrue("true should be returned. data : " + value, ApplicationUtils.isAppRestartRequired(list));
        }

        List<String> list = new ArrayList<String>() {
            {
                add("webAlwaysLogin");
            }
        };
        assertFalse("false should be returned. data : webAlwaysLogin", ApplicationUtils.isAppRestartRequired(list));
        assertFalse("false should be returned. data : null", ApplicationUtils.isAppRestartRequired(null));
        assertFalse("false should be returned. data : emptyString", ApplicationUtils.isAppRestartRequired(new ArrayList<String>()));

    }

}
