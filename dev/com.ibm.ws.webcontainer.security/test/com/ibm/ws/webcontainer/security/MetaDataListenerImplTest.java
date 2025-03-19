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

package com.ibm.ws.webcontainer.security;

import static org.junit.Assert.fail;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.container.service.metadata.MetaDataEvent;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.webcontainer.security.metadata.SecurityConstraintCollection;
import com.ibm.ws.webcontainer.security.metadata.SecurityMetadata;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

import test.common.SharedOutputManager;

public class MetaDataListenerImplTest {
    static final SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    static final String KEY_WEB_JACC_SERVICE = "webJaccService";

    private final Mockery context = new JUnit4Mockery();
    private final WebModuleMetaData wmmd = context.mock(WebModuleMetaData.class);
    private final ModuleMetaData mmd = context.mock(ModuleMetaData.class);
    private final WebAppConfig wac = context.mock(WebAppConfig.class);
    @SuppressWarnings("unchecked")
    private final ServiceReference<WebJaccService> jsr = context.mock(ServiceReference.class, "jaccServiceRef");
    private final WebJaccService js = context.mock(WebJaccService.class);
    private final ComponentContext cc = context.mock(ComponentContext.class);
    @SuppressWarnings("rawtypes")
    private final MetaDataEvent mde = context.mock(MetaDataEvent.class);
    private final SecurityMetadata smd = context.mock(SecurityMetadata.class);
    private final SecurityConstraintCollection scc = context.mock(SecurityConstraintCollection.class);
    private final String APP_NAME = "ApplicationName";
    private final String MODULE_NAME = "ModuleName";

    /**
     * Tests moduleMetaDataCreated method
     * Expected result: jacc code is invoked.
     */
    @Test
    public void moduleMetaDataCreatedWithJaccEnabled() {

        context.checking(new Expectations() {
            {
                one(mde).getMetaData();
                will(returnValue(wmmd));
                allowing(wmmd).getSecurityMetaData();
                will(returnValue(smd));
                one(smd).getSecurityConstraintCollection();
                will(returnValue(scc));
                one(wmmd).getConfiguration();
                will(returnValue(wac));
                one(wac).getApplicationName();
                will(returnValue(APP_NAME));
                one(wac).getModuleName();
                will(returnValue(MODULE_NAME));
                one(cc).locateService("webJaccService", jsr);
                will(returnValue(js));
                one(js).propagateWebConstraints(APP_NAME, MODULE_NAME, wac);
            }
        });
        MetaDataListenerImpl mdli = new MetaDataListenerImpl();
        mdli.setWebJaccService(jsr);
        mdli.activate(cc);

        try {
            mdli.moduleMetaDataCreated(mde);
            mdli.moduleMetaDataDestroyed(mde);
        } catch (MetaDataException e) {
            e.printStackTrace();
            fail("An exception is caught.");
        }

        mdli.deactivate(cc);
        mdli.unsetWebJaccService(jsr);

        context.assertIsSatisfied();
    }

    /**
     * Tests moduleMetaDataCreated method
     * Expected result: jacc code is not invoked.
     */
    @Test
    public void moduleMetaDataCreatedNoJaccEnabled() {

        context.checking(new Expectations() {
            {
                one(cc).locateService("webJaccService", jsr);
                will(returnValue(null));
                never(js).propagateWebConstraints(APP_NAME, MODULE_NAME, wac);
            }
        });
        MetaDataListenerImpl mdli = new MetaDataListenerImpl();
        mdli.setWebJaccService(jsr);
        mdli.activate(cc);

        try {
            mdli.moduleMetaDataCreated(mde);
        } catch (MetaDataException e) {
            e.printStackTrace();
            fail("An exception is caught.");
        }

        mdli.deactivate(cc);
        mdli.unsetWebJaccService(jsr);

        context.assertIsSatisfied();
    }

    /**
     * Tests moduleMetaDataCreated method
     * Expected result: jacc code is not invoked.
     */
    @Test
    public void moduleMetaDataCreatedNoWebModuleMetaData() {

        context.checking(new Expectations() {
            {
                one(mde).getMetaData();
                will(returnValue(mmd));
                one(cc).locateService("webJaccService", jsr);
                will(returnValue(js));
                never(js).propagateWebConstraints(APP_NAME, MODULE_NAME, wac);
            }
        });
        MetaDataListenerImpl mdli = new MetaDataListenerImpl();
        mdli.setWebJaccService(jsr);
        mdli.activate(cc);

        try {
            mdli.moduleMetaDataCreated(mde);
        } catch (MetaDataException e) {
            e.printStackTrace();
            fail("An exception is caught.");
        }

        mdli.deactivate(cc);
        mdli.unsetWebJaccService(jsr);

        context.assertIsSatisfied();
    }

    /**
     * Tests moduleMetaDataCreated method
     * Expected result: jacc code is not invoked.
     */
    @Test
    public void moduleMetaDataCreatedNoSecurityMetaData() {

        context.checking(new Expectations() {
            {
                one(mde).getMetaData();
                will(returnValue(wmmd));
                allowing(wmmd).getSecurityMetaData();
                will(returnValue(null));
                one(cc).locateService("webJaccService", jsr);
                will(returnValue(js));
                never(js).propagateWebConstraints(APP_NAME, MODULE_NAME, wac);
            }
        });
        MetaDataListenerImpl mdli = new MetaDataListenerImpl();
        mdli.setWebJaccService(jsr);
        mdli.activate(cc);

        try {
            mdli.moduleMetaDataCreated(mde);
        } catch (MetaDataException e) {
            e.printStackTrace();
            fail("An exception is caught.");
        }

        mdli.deactivate(cc);
        mdli.unsetWebJaccService(jsr);

        context.assertIsSatisfied();
    }

    /**
     * Tests moduleMetaDataCreated method
     * Expected result: jacc code is not invoked.
     */
    @Test
    public void moduleMetaDataCreatedNoSecurityConstraint() {

        context.checking(new Expectations() {
            {
                one(mde).getMetaData();
                will(returnValue(wmmd));
                allowing(wmmd).getSecurityMetaData();
                will(returnValue(smd));
                one(smd).getSecurityConstraintCollection();
                will(returnValue(null));
                one(cc).locateService("webJaccService", jsr);
                will(returnValue(js));
                never(js).propagateWebConstraints(APP_NAME, MODULE_NAME, wac);
            }
        });
        MetaDataListenerImpl mdli = new MetaDataListenerImpl();
        mdli.setWebJaccService(jsr);
        mdli.activate(cc);

        try {
            mdli.moduleMetaDataCreated(mde);
        } catch (MetaDataException e) {
            e.printStackTrace();
            fail("An exception is caught.");
        }

        mdli.deactivate(cc);
        mdli.unsetWebJaccService(jsr);

        context.assertIsSatisfied();
    }
}
