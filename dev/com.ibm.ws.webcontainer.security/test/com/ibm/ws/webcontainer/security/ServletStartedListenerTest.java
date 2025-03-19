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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletSecurityElement;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.container.service.metadata.MetaDataEvent;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.webcontainer.security.metadata.SecurityConstraintCollection;
import com.ibm.ws.webcontainer.security.metadata.SecurityMetadata;
import com.ibm.ws.webcontainer.webapp.WebAppConfigExtended;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;
import com.ibm.wsspi.webcontainer.servlet.IServletConfig;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

import test.common.SharedOutputManager;

public class ServletStartedListenerTest {
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
    private final Container mc = context.mock(Container.class);
    private final WebModuleMetaData wmmd = context.mock(WebModuleMetaData.class);
    private final ModuleMetaData mmd = context.mock(ModuleMetaData.class);
    private final WebAppConfigExtended wac = context.mock(WebAppConfigExtended.class);
    @SuppressWarnings("unchecked")
    private final ServiceReference<WebJaccService> jsr = context.mock(ServiceReference.class, "jaccServiceRef");
    private final WebJaccService js = context.mock(WebJaccService.class);
    private final ComponentContext cc = context.mock(ComponentContext.class);
    private final Iterator it = context.mock(Iterator.class);
    @SuppressWarnings("rawtypes")
    private final MetaDataEvent mde = context.mock(MetaDataEvent.class);
    private final SecurityMetadata smd = context.mock(SecurityMetadata.class);
    private final SecurityConstraintCollection scc = context.mock(SecurityConstraintCollection.class);
    private final IServletConfig isc = context.mock(IServletConfig.class);
    private final String APP_NAME = "ApplicationName";
    private final String MODULE_NAME = "ModuleName";

    /**
     * Tests started method
     * Expected result: jacc code is invoked.
     */
    @Test
    public void startedWithJaccEnabled() {
        final List<IServletConfig> sl = new ArrayList<IServletConfig>();
        sl.add(isc);
        final Iterator<IServletConfig> si = sl.iterator();
        final Iterator<IServletConfig> si2 = sl.iterator();
        final List<String> rm = new ArrayList<String>();
        final List<String> mp = new ArrayList<String>();
        mp.add("role");
        final ServletSecurityElement sse = new ServletSecurityElement();
        try {
            context.checking(new Expectations() {
                {
                    one(mc).adapt(WebAppConfig.class);
                    will(returnValue(wac));
                    one(wac).getMetaData();
                    will(returnValue(wmmd));
                    allowing(wmmd).getSecurityMetaData();
                    will(returnValue(smd));
                    one(smd).isDenyUncoveredHttpMethods();
                    will(returnValue(false));
                    one(smd).getRoles();
                    will(returnValue(rm));
                    one(smd).setSecurityConstraintCollection(with(any(SecurityConstraintCollection.class)));
                    // getServletInfos is invoked twice,and returns an iteration object, therefore the 2nd one needs to return
                    // the fresh iterator. it seems like adding one(xx) twice works.
                    one(wac).getServletInfos();
                    will(returnValue(si));
                    one(wac).getServletInfos();
                    will(returnValue(si2));
                    one(isc).getRunAsRole();
                    will(returnValue(null));
                    allowing(isc).getServletSecurity();
                    will(returnValue(sse));
                    allowing(isc).getMappings();
                    will(returnValue(mp));
                    one(smd).getSecurityConstraintCollection();
                    will(returnValue(null));
                    one(mc).adapt(WebModuleMetaData.class);
                    will(returnValue(wmmd));
                    allowing(wmmd).setSecurityMetaData(smd);

                    one(wac).getApplicationName();
                    will(returnValue(APP_NAME));
                    one(wac).getModuleName();
                    will(returnValue(MODULE_NAME));
                    one(cc).locateService("webJaccService", jsr);
                    will(returnValue(js));
                    one(js).propagateWebConstraints(APP_NAME, MODULE_NAME, wac);
                }
            });
        } catch (UnableToAdaptException e) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            e.printStackTrace();
            fail("An exception is caught." + e);
        }
        ServletStartedListener ssl = new ServletStartedListener();
        ssl.setWebJaccService(jsr);
        ssl.activate(cc);

        ssl.starting(mc);
        ssl.started(mc);

        ssl.deactivate(cc);
        ssl.unsetWebJaccService(jsr);

        context.assertIsSatisfied();
    }

}
