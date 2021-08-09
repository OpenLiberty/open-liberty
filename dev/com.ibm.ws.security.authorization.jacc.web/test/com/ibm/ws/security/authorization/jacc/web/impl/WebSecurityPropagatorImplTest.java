/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.authorization.jacc.web.impl;

import static org.junit.Assert.fail;

import java.security.Permissions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.security.jacc.PolicyConfiguration;
import javax.security.jacc.PolicyConfigurationFactory;
import javax.security.jacc.PolicyContextException;
import javax.security.jacc.WebResourcePermission;
import javax.security.jacc.WebRoleRefPermission;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ws.webcontainer.security.metadata.SecurityConstraint;
import com.ibm.ws.webcontainer.security.metadata.SecurityConstraintCollection;
import com.ibm.ws.webcontainer.security.metadata.SecurityMetadata;
import com.ibm.ws.webcontainer.security.metadata.WebResourceCollection;
import com.ibm.ws.webcontainer.webapp.WebAppConfigExtended;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;

import test.common.SharedOutputManager;

public class WebSecurityPropagatorImplTest {
    static final SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    private final Mockery context = new JUnit4Mockery();
    private final PolicyConfiguration pc = context.mock(PolicyConfiguration.class);
    private final WebAppConfigExtended wac = context.mock(WebAppConfigExtended.class);
    private final WebModuleMetaData wmmd = context.mock(WebModuleMetaData.class);
    private final SecurityMetadata smd = context.mock(SecurityMetadata.class);
    private final SecurityConstraintCollection scc = context.mock(SecurityConstraintCollection.class);
    private PolicyConfigurationFactory pcf = null;

    @Before
    public void setUp() {
        pcf = new DummyPolicyConfigurationFactory(pc);
    }

    @After
    public void tearDown() throws Exception {
        context.assertIsSatisfied();
    }

    /**
     * Tests propagateWebConstraints method
     * Expected result: no exception.
     */
    @Test
    public void propagateWebConstraintsNormal() {
        final String appName = "applicationName";
        final String contextId = "test#context#Id";
        final String role = "webRole";
        final List<String> roles = new ArrayList<String>();
        roles.add(role);
        final List<String> servletList = new ArrayList<String>();
        final String servletName = "testServlet";
        servletList.add(servletName);
        final Iterator<?> servletNames = servletList.iterator();
        final String refName = "refName";
        final String name = "roleName";
        final Map<String, String> roleRefs = new HashMap<String, String>();
        roleRefs.put(refName, name);
        final WebRoleRefPermission webRoleRefPerm = new WebRoleRefPermission(servletName, refName);
        final WebRoleRefPermission webRolePerm = new WebRoleRefPermission(servletName, role);
        final WebRoleRefPermission webRoleDefaultPerm = new WebRoleRefPermission("", role);
        final WebRoleRefPermission webRoleRefPermStarStar = new WebRoleRefPermission(servletName, "**");
        final String urlPattern1 = "/*";
        final String urlPattern2 = "/omission/*";
        final String urlPattern3 = "/allmethods/*";
        final List<String> urlPatterns1 = new ArrayList<String>();
        final List<String> urlPatterns2 = new ArrayList<String>();
        final List<String> urlPatterns3 = new ArrayList<String>();
        urlPatterns1.add(urlPattern1);
        urlPatterns2.add(urlPattern2);
        urlPatterns3.add(urlPattern3);
        final String method1 = "GET";
        final String method2 = "POST";
        final List<String> methods1 = new ArrayList<String>();
        final List<String> methods2 = new ArrayList<String>();
        methods1.add(method1);
        methods2.add(method2);
        final WebResourceCollection wrc1 = new WebResourceCollection(urlPatterns1, methods1);
        final WebResourceCollection wrc2 = new WebResourceCollection(urlPatterns2, new ArrayList<String>(), methods2);
        final WebResourceCollection wrc3 = new WebResourceCollection(urlPatterns3, new ArrayList<String>());
        final List<WebResourceCollection> wrcs = new ArrayList<WebResourceCollection>();
        wrcs.add(wrc1);
        wrcs.add(wrc2);
        wrcs.add(wrc3);
        final SecurityConstraint sc = new SecurityConstraint(wrcs, roles, false, false, false, false);
        final List<SecurityConstraint> scs = new ArrayList<SecurityConstraint>();
        scs.add(sc);
        final WebResourcePermission webResPerm1 = new WebResourcePermission(urlPattern1 + ":" + urlPattern2 + ":" + urlPattern3, method1);
        final WebResourcePermission webResPerm2 = new WebResourcePermission(urlPattern2, "!" + method2);
        final WebResourcePermission webResPerm3 = new WebResourcePermission(urlPattern3, (String) null);

        try {
            context.checking(new Expectations() {
                {
                    allowing(wac).getMetaData();
                    will(returnValue(wmmd));
                    allowing(wmmd).getSecurityMetaData();
                    will(returnValue(smd));
                    one(smd).getRoles();
                    will(returnValue(roles));
                    one(wac).getServletNames();
                    will(returnValue(servletNames));
                    one(smd).getRoleRefs(servletName);
                    will(returnValue(roleRefs));
                    one(pc).addToRole("**", webRoleRefPermStarStar);
                    one(pc).addToRole(name, webRoleRefPerm);
                    one(pc).addToRole(role, webRolePerm);
                    one(pc).addToRole(role, webRoleDefaultPerm);
                    one(smd).getSecurityConstraintCollection();
                    will(returnValue(scc));
                    one(scc).getSecurityConstraints();
                    will(returnValue(scs));
                    one(pc).addToRole(role, webResPerm1);
                    one(pc).addToRole(role, webResPerm2);
                    one(pc).addToRole(role, webResPerm3);
                    one(pc).addToUncheckedPolicy(with(any(Permissions.class)));
                    atMost(2).of(wac).getApplicationName();
                    will(returnValue(appName));
                    allowing(pc).linkConfiguration(with(any(PolicyConfiguration.class)));
                }
            });
        } catch (PolicyContextException e) {
            fail("PolicyContextException is caught : " + e);
        }
        WebSecurityPropagatorImpl wsp = new WebSecurityPropagatorImpl();
        wsp.propagateWebConstraints(pcf, contextId, wac);
    }

    /**
     * Tests propagateWebConstraints method
     * webconfig object is null.
     * Expected result: do nothing
     */
    @Test
    public void propagateWebConstraintsNullWebConfigObject() {
        final String contextId = "test#context#Id";

        WebSecurityPropagatorImpl wsp = new WebSecurityPropagatorImpl();
        wsp.propagateWebConstraints(pcf, contextId, null);
    }

    /**
     * Tests propagateWebConstraints method
     * Expected result: no exception.
     */
    @Test
    public void propagateWebConstraintsNormalAlternative1() {
        final String appName = "applicationName";
        final String contextId = "test#context#Id";
        final String role = "webRole";
        final List<String> roles = new ArrayList<String>();
        roles.add(role);
        final List<String> servletList = new ArrayList<String>();
        final String servletName = "testServlet";
        servletList.add(servletName);
        final Iterator<?> servletNames = servletList.iterator();
        final String refName = "refName";
        final String name = "roleName";
        final Map<String, String> roleRefs = new HashMap<String, String>();
        roleRefs.put(refName, name);
        final WebRoleRefPermission webRoleRefPermStarStar = new WebRoleRefPermission(servletName, "**");
        final WebRoleRefPermission webRoleRefPerm = new WebRoleRefPermission(servletName, refName);
        final WebRoleRefPermission webRolePerm = new WebRoleRefPermission(servletName, role);
        final WebRoleRefPermission webRoleDefaultPerm = new WebRoleRefPermission("", role);
        final String urlPattern1 = "/*";
        final String urlPattern2 = "/omission/*";
        final String urlPattern3 = "/allmethods/*";
        final List<String> urlPatterns1 = new ArrayList<String>();
        final List<String> urlPatterns2 = new ArrayList<String>();
        final List<String> urlPatterns3 = new ArrayList<String>();
        urlPatterns1.add(urlPattern1);
        urlPatterns2.add(urlPattern2);
        urlPatterns3.add(urlPattern3);
        final String method1 = "GET";
        final String method2 = "POST";
        final List<String> methods1 = new ArrayList<String>();
        final List<String> methods2 = new ArrayList<String>();
        methods1.add(method1);
        methods2.add(method2);
        final WebResourceCollection wrc1 = new WebResourceCollection(urlPatterns1, methods1);
        final WebResourceCollection wrc2 = new WebResourceCollection(urlPatterns2, new ArrayList<String>(), methods2, true);
        final WebResourceCollection wrc3 = new WebResourceCollection(urlPatterns3, new ArrayList<String>());
        final List<WebResourceCollection> wrcs = new ArrayList<WebResourceCollection>();
        wrcs.add(wrc1);
        wrcs.add(wrc2);
        wrcs.add(wrc3);
        final SecurityConstraint sc = new SecurityConstraint(wrcs, null, true, true, false, false);
        final List<SecurityConstraint> scs = new ArrayList<SecurityConstraint>();
        scs.add(sc);

        try {
            context.checking(new Expectations() {
                {
                    allowing(wac).getMetaData();
                    will(returnValue(wmmd));
                    allowing(wmmd).getSecurityMetaData();
                    will(returnValue(smd));
                    one(smd).getRoles();
                    will(returnValue(roles));
                    one(wac).getServletNames();
                    will(returnValue(servletNames));
                    one(smd).getRoleRefs(servletName);
                    will(returnValue(roleRefs));
                    one(pc).addToRole("**", webRoleRefPermStarStar);
                    one(pc).addToRole(name, webRoleRefPerm);
                    one(pc).addToRole(role, webRolePerm);
                    one(pc).addToRole(role, webRoleDefaultPerm);
                    one(smd).getSecurityConstraintCollection();
                    will(returnValue(scc));
                    one(scc).getSecurityConstraints();
                    will(returnValue(scs));
                    one(pc).addToExcludedPolicy(with(any(Permissions.class)));
                    atMost(2).of(wac).getApplicationName();
                    will(returnValue(appName));
                    allowing(pc).linkConfiguration(with(any(PolicyConfiguration.class)));
                }
            });
        } catch (PolicyContextException e) {
            fail("PolicyContextException is caught : " + e);
        }
        WebSecurityPropagatorImpl wsp = new WebSecurityPropagatorImpl();
        wsp.propagateWebConstraints(pcf, contextId, wac);
    }

    /**
     * Tests propagateWebConstraints method
     * Expected result: no exception.
     */
    @Test
    public void propagateWebConstraintsNormalAlternative2() {
        final String appName = "applicationName";
        final String contextId = "test#context#Id";
        final String role = "webRole";
        final List<String> roles = new ArrayList<String>();
        roles.add(role);
        final List<String> servletList = new ArrayList<String>();
        final String servletName = "testServlet";
        servletList.add(servletName);
        final Iterator<?> servletNames = servletList.iterator();
        final String refName = "refName";
        final String name = "roleName";
        final Map<String, String> roleRefs = new HashMap<String, String>();
        roleRefs.put(refName, name);
        final WebRoleRefPermission webRoleRefPermStarStar = new WebRoleRefPermission(servletName, "**");
        final WebRoleRefPermission webRoleRefPerm = new WebRoleRefPermission(servletName, refName);
        final WebRoleRefPermission webRolePerm = new WebRoleRefPermission(servletName, role);
        final WebRoleRefPermission webRoleDefaultPerm = new WebRoleRefPermission("", role);
        final String urlPattern1 = "/exact";
        final String urlPattern2 = "*.html";
        final String urlPattern3 = "/";
        final String urlPattern4 = "/";
        final List<String> urlPatterns1 = new ArrayList<String>();
        final List<String> urlPatterns2 = new ArrayList<String>();
        final List<String> urlPatterns3 = new ArrayList<String>();
        final List<String> urlPatterns4 = new ArrayList<String>();
        urlPatterns1.add(urlPattern1);
        urlPatterns2.add(urlPattern2);
        urlPatterns3.add(urlPattern3);
        urlPatterns4.add(urlPattern4);
        final String method1 = "GET";
        final String method2 = "POST";
        final List<String> methods1 = new ArrayList<String>();
        final List<String> methods2 = new ArrayList<String>();
        methods1.add(method1);
        methods2.add(method2);
        final WebResourceCollection wrc1 = new WebResourceCollection(urlPatterns1, methods1);
        final WebResourceCollection wrc2 = new WebResourceCollection(urlPatterns2, new ArrayList<String>(), methods2);
        final WebResourceCollection wrc3 = new WebResourceCollection(urlPatterns3, new ArrayList<String>());
        final WebResourceCollection wrc4 = new WebResourceCollection(urlPatterns4, methods2);
        final List<WebResourceCollection> wrcs = new ArrayList<WebResourceCollection>();
        wrcs.add(wrc1);
        wrcs.add(wrc2);
        wrcs.add(wrc3);
        wrcs.add(wrc4);
        final SecurityConstraint sc = new SecurityConstraint(wrcs, null, true, false, false, false);
        final List<SecurityConstraint> scs = new ArrayList<SecurityConstraint>();
        scs.add(sc);

        try {
            context.checking(new Expectations() {
                {
                    allowing(wac).getMetaData();
                    will(returnValue(wmmd));
                    allowing(wmmd).getSecurityMetaData();
                    will(returnValue(smd));
                    one(smd).getRoles();
                    will(returnValue(roles));
                    one(wac).getServletNames();
                    will(returnValue(servletNames));
                    one(smd).getRoleRefs(servletName);
                    will(returnValue(roleRefs));
                    one(pc).addToRole("**", webRoleRefPermStarStar);
                    one(pc).addToRole(name, webRoleRefPerm);
                    one(pc).addToRole(role, webRolePerm);
                    one(pc).addToRole(role, webRoleDefaultPerm);
                    one(smd).getSecurityConstraintCollection();
                    will(returnValue(scc));
                    one(scc).getSecurityConstraints();
                    will(returnValue(scs));
                    one(pc).addToUncheckedPolicy(with(any(Permissions.class)));
                    atMost(2).of(wac).getApplicationName();
                    will(returnValue(appName));
                    allowing(pc).linkConfiguration(with(any(PolicyConfiguration.class)));
                }
            });
        } catch (PolicyContextException e) {
            fail("PolicyContextException is caught : " + e);
        }
        WebSecurityPropagatorImpl wsp = new WebSecurityPropagatorImpl();
        wsp.propagateWebConstraints(pcf, contextId, wac);
    }
}
