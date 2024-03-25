/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.remote.configtests.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.EJBs;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import com.ibm.ws.ejbcontainer.remote.configtests.ejb.ConfigTestsLocalBusiness;
import com.ibm.ws.ejbcontainer.remote.configtests.ejb.ConfigTestsLocalEJB;
import com.ibm.ws.ejbcontainer.remote.configtests.ejb.ConfigTestsLocalHome;
import com.ibm.ws.ejbcontainer.remote.configtests.ejb.ConfigTestsRemoteBusiness;
import com.ibm.ws.ejbcontainer.remote.configtests.ejb.ConfigTestsRemoteEJB;
import com.ibm.ws.ejbcontainer.remote.configtests.ejb.ConfigTestsRemoteHome;
import com.ibm.ws.ejbcontainer.remote.configtests.ejb.JavaColonLookupLocalEJB;
import com.ibm.ws.ejbcontainer.remote.configtests.ejb.JavaColonLookupLocalHome;
import com.ibm.ws.ejbcontainer.remote.configtests.web.ejb.ConfigTestsWarLocalBusiness;
import com.ibm.ws.ejbcontainer.remote.configtests.web.ejb.ConfigTestsWarLocalEJB;
import com.ibm.ws.ejbcontainer.remote.configtests.web.ejb.ConfigTestsWarLocalHome;
import com.ibm.ws.ejbcontainer.remote.configtests.web.ejb.ConfigTestsWarRemoteBusiness;
import com.ibm.ws.ejbcontainer.remote.configtests.web.ejb.ConfigTestsWarRemoteEJB;
import com.ibm.ws.ejbcontainer.remote.configtests.web.ejb.ConfigTestsWarRemoteHome;

import componenttest.app.FATServlet;

/**
 * Test that EJB remote bindings in JNDI are properly updated when the EJB Remote feature
 * and ORB component are dynamically updated.
 */
@SuppressWarnings("serial")
@EJBs({
        @EJB(name = "ConfigTestsLocalBusiness", beanInterface = ConfigTestsLocalBusiness.class),
        @EJB(name = "ConfigTestsLocalHome", beanInterface = ConfigTestsLocalHome.class),
        @EJB(name = "ConfigTestsRemoteBusiness", beanInterface = ConfigTestsRemoteBusiness.class),
        @EJB(name = "ConfigTestsRemoteHome", beanInterface = ConfigTestsRemoteHome.class),
        @EJB(name = "ConfigTestsWarLocalBusiness", beanInterface = ConfigTestsWarLocalBusiness.class),
        @EJB(name = "ConfigTestsWarLocalHome", beanInterface = ConfigTestsWarLocalHome.class),
        @EJB(name = "ConfigTestsWarRemoteBusiness", beanInterface = ConfigTestsWarRemoteBusiness.class),
        @EJB(name = "ConfigTestsWarRemoteHome", beanInterface = ConfigTestsWarRemoteHome.class)
})
@WebServlet("/RebindConfigUpdateServlet")
public class RebindConfigUpdateServlet extends FATServlet {

    private InitialContext ctx = null;

    @PostConstruct
    protected void setUp() {
        try {
            ctx = new InitialContext();
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    // ------------------------------------------------------------------------
    // testBindWhenEjbRemoteAdded
    // ------------------------------------------------------------------------

    public void testBindWhenEjbRemoteAdded_Initial() throws Exception {
        testHelper(false);
    }

    public void testBindWhenEjbRemoteAdded_Added() throws Exception {
        testHelper(true);
    }

    // ------------------------------------------------------------------------
    // testRebindWhenEjbRemoteRemovedAdded
    // ------------------------------------------------------------------------

    public void testRebindWhenEjbRemoteRemovedAdded_Initial() throws Exception {
        testHelper(true);
    }

    public void testRebindWhenEjbRemoteRemovedAdded_Removed() throws Exception {
        testHelper(false);
    }

    public void testRebindWhenEjbRemoteRemovedAdded_Added() throws Exception {
        testHelper(true);
    }

    // ------------------------------------------------------------------------
    // testRebindAfterEjbContainerUpdated
    // ------------------------------------------------------------------------

    public void testRebindAfterEjbContainerUpdated_Initial() throws Exception {
        testHelper(true);
    }

    public void testRebindAfterEjbContainerUpdated_Updated() throws Exception {
        testHelper(true);
    }

    // ------------------------------------------------------------------------
    // testRebindAfterOrbUpdated
    // ------------------------------------------------------------------------

    public void testRebindAfterOrbUpdated_Initial() throws Exception {
        testHelper(true);
    }

    public void testRebindAfterOrbUpdated_Updated() throws Exception {
        testHelper(true);
    }

    // ------------------------------------------------------------------------
    // testRebindAfterOrbLateStart
    // ------------------------------------------------------------------------

    public void testRebindAfterOrbLateStart_Initial() throws Exception {
        testHelper(false);
    }

    public void testRebindAfterOrbLateStart_Started() throws Exception {
        testHelper(true);
    }

    // ------------------------------------------------------------------------
    // testRebindAfterOrbStoppedStarted
    // ------------------------------------------------------------------------

    public void testRebindAfterOrbStoppedStarted_Initial() throws Exception {
        testHelper(true);
    }

    public void testRebindAfterOrbStoppedStarted_Stopped() throws Exception {
        testHelper(false);
    }

    public void testRebindAfterOrbStoppedStarted_Started() throws Exception {
        testHelper(true);
    }

    // ------------------------------------------------------------------------
    // common implementation follows...
    // ------------------------------------------------------------------------

    private void testHelper(boolean bindToRemote) throws Exception {
        // Local short
        ConfigTestsLocalEJB bean = lookupShort();
        assertNotNull("1 ---> ConfigTestsLocalEJB short default lookup did not succeed.", bean);
        String str = bean.getString();
        assertEquals("2 ---> getString() returned unexpected value", "Success", str);

        // Local Long
        ConfigTestsLocalEJB longBean = lookupLong();
        assertNotNull("3 ---> ConfigTestsLocalEJB long default lookup did not succeed.", longBean);
        String lstr = longBean.getString();
        assertEquals("4 ---> getString() returned unexpected value", "Success", lstr);

        // Remote Short
        ConfigTestsRemoteEJB rbean = lookupRemoteShort(bindToRemote);
        if (bindToRemote) {
            assertNotNull("5 ---> ConfigTestsRemoteEJB short default lookup did not succeed.", rbean);
            String rstr = rbean.getString();
            assertEquals("6 ---> getString() returned unexpected value", "Success", rstr);
        } else {
            assertNull("5/6 ---> ConfigTestsRemoteEJB short default lookup should have failed.", rbean);
        }

        // Remote Long
        ConfigTestsRemoteEJB rlongBean = lookupRemoteLong(bindToRemote);
        if (bindToRemote) {
            assertNotNull("7 ---> ConfigTestsRemoteEJB long default lookup did not succeed.", rlongBean);
            String rlstr = rlongBean.getString();
            assertEquals("8 ---> getString() returned unexpected value", "Success", rlstr);
        } else {
            assertNull("7/8 ---> ConfigTestsRemoteEJB long default lookup should have failed.", rlongBean);
        }

        // use helper bean to lookup java namespaces because of java:module
        JavaColonLookupLocalHome home = (JavaColonLookupLocalHome) ctx.lookup("ejblocal:ConfigTestsTestApp/ConfigTestsEJB.jar/JavaColonLookupBean#com.ibm.ws.ejbcontainer.remote.configtests.ejb.JavaColonLookupLocalHome");
        JavaColonLookupLocalEJB jclbean = home.create();
        jclbean.lookupJavaNamespaces(true, bindToRemote); // 9 - 26

        // Local java:comp
        ConfigTestsLocalEJB compbean = lookupComp();
        assertNotNull("27 ---> ConfigTestsLocalEJB java:comp lookup did not succeed.", compbean);
        String cstr = compbean.getString();
        assertEquals("28 ---> getString() returned unexpected value", "Success", cstr);

        // Local java:comp business
        ConfigTestsLocalBusiness compbBean = lookupCompBusiness();
        assertNotNull("29 ---> ConfigTestsLocalBusiness java:comp lookup did not succeed.", compbBean);
        String cbstr = compbBean.getString();
        assertEquals("30 ---> getString() returned unexpected value", "Success", cbstr);

        // Remote java:comp
        ConfigTestsRemoteEJB rcbean = lookupRemoteComp(bindToRemote);
        if (bindToRemote) {
            assertNotNull("31 ---> ConfigTestsRemoteEJB java:comp lookup did not succeed.", rcbean);
            String rcstr = rcbean.getString();
            assertEquals("32 ---> getString() returned unexpected value", "Success", rcstr);
        } else {
            assertNull("31/32 ---> ConfigTestsRemoteEJB java:comp lookup should have failed.", rcbean);
        }

        // Remote java:comp business
        ConfigTestsRemoteBusiness rcbBean = lookupRemoteCompBusiness(bindToRemote);
        if (bindToRemote) {
            assertNotNull("33 ---> ConfigTestsRemoteBusiness java:comp lookup did not succeed.", rcbBean);
            String rcbstr = rcbBean.getString();
            assertEquals("34 ---> getString() returned unexpected value", "Success", rcbstr);
        } else {
            assertNull("33/34 ---> ConfigTestsRemoteBusiness java:comp lookup should have failed.", rcbBean);
        }

        // Local short - war
        ConfigTestsWarLocalEJB wbean = lookupWarShort();
        assertNotNull("35 ---> ConfigTestsWarLocalEJB short default lookup did not succeed.", wbean);
        String wstr = wbean.getString();
        assertEquals("36 ---> getString() returned unexpected value", "Success", wstr);

        // Local Long - war
        ConfigTestsWarLocalEJB longWarBean = lookupWarLong();
        assertNotNull("37 ---> ConfigTestsWarLocalEJB long default lookup did not succeed.", longWarBean);
        String lwstr = longWarBean.getString();
        assertEquals("38 ---> getString() returned unexpected value", "Success", lwstr);

        // Remote Short - war
        ConfigTestsWarRemoteEJB rwbean = lookupWarRemoteShort(bindToRemote);
        if (bindToRemote) {
            assertNotNull("39 ---> ConfigTestsWarRemoteEJB short default lookup did not succeed.", rwbean);
            String rwstr = rwbean.getString();
            assertEquals("40 ---> getString() returned unexpected value", "Success", rwstr);
        } else {
            assertNull("39/40 ---> ConfigTestsWarRemoteEJB short default lookup should have failed.", rwbean);
        }

        // Remote Long - war
        ConfigTestsWarRemoteEJB rlongWarBean = lookupWarRemoteLong(bindToRemote);
        if (bindToRemote) {
            assertNotNull("41 ---> ConfigTestsWarRemoteEJB long default lookup did not succeed.", rlongWarBean);
            String rlwstr = rlongWarBean.getString();
            assertEquals("42 ---> getString() returned unexpected value", "Success", rlwstr);
        } else {
            assertNull("41/42 ---> ConfigTestsWarRemoteEJB long default lookup should have failed.", rlongWarBean);
        }

        // war remote java:global
        ConfigTestsWarRemoteEJB rgwbean = lookupWarRemoteJavaNamespace(bindToRemote,
                                                                       "java:global/ConfigTestsTestApp/ConfigTestsWeb/ConfigTestsWarTestBean!com.ibm.ws.ejbcontainer.remote.configtests.web.ejb.ConfigTestsWarRemoteHome");
        if (bindToRemote) {
            assertNotNull("43 ---> ConfigTestsWarRemoteEJB java:global lookup did not succeed.", rbean);
            String rgwstr = rgwbean.getString();
            assertEquals("44 ---> getString() returned unexpected value", "Success", rgwstr);
        } else {
            assertNull("43/44 --> ConfigTestsWarRemoteEJB java:global lookup should have failed.", rgwbean);
        }

        // war remote java:app
        ConfigTestsWarRemoteEJB rawbean = lookupWarRemoteJavaNamespace(bindToRemote,
                                                                       "java:app/ConfigTestsWeb/ConfigTestsWarTestBean!com.ibm.ws.ejbcontainer.remote.configtests.web.ejb.ConfigTestsWarRemoteHome");
        if (bindToRemote) {
            assertNotNull("45 ---> ConfigTestsWarRemoteEJB java:app lookup did not succeed.", rbean);
            String rawstr = rawbean.getString();
            assertEquals("46 ---> getString() returned unexpected value", "Success", rawstr);
        } else {
            assertNull("45/46 --> ConfigTestsWarRemoteEJB java:app lookup should have failed.", rawbean);
        }

        // war remote java:module
        ConfigTestsWarRemoteEJB rmwbean = lookupWarRemoteJavaNamespace(bindToRemote,
                                                                       "java:module/ConfigTestsWarTestBean!com.ibm.ws.ejbcontainer.remote.configtests.web.ejb.ConfigTestsWarRemoteHome");
        if (bindToRemote) {
            assertNotNull("47 ---> ConfigTestsWarRemoteEJB java:module lookup did not succeed.", rmwbean);
            String rmwstr = rmwbean.getString();
            assertEquals("48 ---> getString() returned unexpected value", "Success", rmwstr);
        } else {
            assertNull("47/48 --> ConfigTestsWarRemoteEJB java:module lookup should have failed.", rmwbean);
        }

        // war local java:global
        ConfigTestsWarLocalEJB gwbean = lookupWarLocalJavaNamespace("java:global/ConfigTestsTestApp/ConfigTestsWeb/ConfigTestsWarTestBean!com.ibm.ws.ejbcontainer.remote.configtests.web.ejb.ConfigTestsWarLocalHome");
        assertNotNull("49 ---> ConfigTestsWarLocalEJB java:global lookup did not succeed.", gwbean);
        String gwstr = bean.getString();
        assertEquals("50 ---> getString() returned unexpected value", "Success", gwstr);

        // war local java:app
        ConfigTestsWarLocalEJB awbean = lookupWarLocalJavaNamespace("java:app/ConfigTestsWeb/ConfigTestsWarTestBean!com.ibm.ws.ejbcontainer.remote.configtests.web.ejb.ConfigTestsWarLocalHome");
        assertNotNull("51 ---> ConfigTestsWarLocalEJB java:app lookup did not succeed.", awbean);
        String awstr = awbean.getString();
        assertEquals("52 ---> getString() returned unexpected value", "Success", awstr);

        // war local java:module
        ConfigTestsWarLocalEJB mwbean = lookupWarLocalJavaNamespace("java:module/ConfigTestsWarTestBean!com.ibm.ws.ejbcontainer.remote.configtests.web.ejb.ConfigTestsWarLocalHome");
        assertNotNull("53 ---> ConfigTestsWarLocalEJB java:module lookup did not succeed.", mwbean);
        String mwstr = mwbean.getString();
        assertEquals("54 ---> getString() returned unexpected value", "Success", mwstr);

        // war Local java:comp
        ConfigTestsWarLocalEJB compwbean = lookupWarComp();
        assertNotNull("55 ---> ConfigTestsWarLocalEJB java:comp lookup did not succeed.", compwbean);
        String cwstr = compwbean.getString();
        assertEquals("56 ---> getString() returned unexpected value", "Success", cwstr);

        // war Local java:comp business
        ConfigTestsWarLocalBusiness compbwBean = lookupWarCompBusiness();
        assertNotNull("57 ---> ConfigTestsLocalWarBusiness java:comp lookup did not succeed.", compbwBean);
        String cbwstr = compbwBean.getString();
        assertEquals("58 ---> getString() returned unexpected value", "Success", cbwstr);

        // war Remote java:comp
        ConfigTestsWarRemoteEJB rcwbean = lookupWarRemoteComp(bindToRemote);
        if (bindToRemote) {
            assertNotNull("59 ---> ConfigTestsWarRemoteEJB java:comp lookup did not succeed.", rcwbean);
            String rcwstr = rcwbean.getString();
            assertEquals("60 ---> getString() returned unexpected value", "Success", rcwstr);
        } else {
            assertNull("59/60 ---> ConfigTestsWarRemoteEJB java:comp lookup should have failed.", rcwbean);
        }

        // war Remote java:comp business
        ConfigTestsWarRemoteBusiness rcbwBean = lookupWarRemoteCompBusiness(bindToRemote);
        if (bindToRemote) {
            assertNotNull("61 ---> ConfigTestsWarRemoteBusiness java:comp lookup did not succeed.", rcbwBean);
            String rcbwstr = rcbwBean.getString();
            assertEquals("62 ---> getString() returned unexpected value", "Success", rcbwstr);
        } else {
            assertNull("61/62 ---> ConfigTestsWarRemoteBusiness java:comp lookup should have failed.", rcbwBean);
        }
    }

    private ConfigTestsLocalEJB lookupShort() throws Exception {
        ConfigTestsLocalHome home = (ConfigTestsLocalHome) ctx.lookup("ejblocal:com.ibm.ws.ejbcontainer.remote.configtests.ejb.ConfigTestsLocalHome");
        return home.create();
    }

    private ConfigTestsWarLocalEJB lookupWarShort() throws Exception {
        ConfigTestsWarLocalHome home = (ConfigTestsWarLocalHome) ctx.lookup("ejblocal:com.ibm.ws.ejbcontainer.remote.configtests.web.ejb.ConfigTestsWarLocalHome");
        return home.create();
    }

    private ConfigTestsLocalEJB lookupLong() throws Exception {
        ConfigTestsLocalHome home = (ConfigTestsLocalHome) ctx.lookup("ejblocal:ConfigTestsTestApp/ConfigTestsEJB.jar/ConfigTestsTestBean#com.ibm.ws.ejbcontainer.remote.configtests.ejb.ConfigTestsLocalHome");
        return home.create();
    }

    private ConfigTestsWarLocalEJB lookupWarLong() throws Exception {
        ConfigTestsWarLocalHome home = (ConfigTestsWarLocalHome) ctx.lookup("ejblocal:ConfigTestsTestApp/ConfigTestsWeb.war/ConfigTestsWarTestBean#com.ibm.ws.ejbcontainer.remote.configtests.web.ejb.ConfigTestsWarLocalHome");
        return home.create();
    }

    private ConfigTestsLocalEJB lookupComp() throws Exception {
        ConfigTestsLocalHome home = (ConfigTestsLocalHome) ctx.lookup("java:comp/env/ConfigTestsLocalHome");
        return home.create();
    }

    private ConfigTestsLocalBusiness lookupCompBusiness() throws Exception {
        return (ConfigTestsLocalBusiness) ctx.lookup("java:comp/env/ConfigTestsLocalBusiness");
    }

    private ConfigTestsWarLocalEJB lookupWarComp() throws Exception {
        ConfigTestsWarLocalHome home = (ConfigTestsWarLocalHome) ctx.lookup("java:comp/env/ConfigTestsWarLocalHome");
        return home.create();
    }

    private ConfigTestsWarLocalBusiness lookupWarCompBusiness() throws Exception {
        return (ConfigTestsWarLocalBusiness) ctx.lookup("java:comp/env/ConfigTestsWarLocalBusiness");
    }

    private ConfigTestsRemoteEJB lookupRemoteShort(boolean shouldWork) throws Exception {
        try {
            ConfigTestsRemoteHome home = (ConfigTestsRemoteHome) ctx.lookup("com.ibm.ws.ejbcontainer.remote.configtests.ejb.ConfigTestsRemoteHome");
            return home.create();
        } catch (NamingException e) {
            if (shouldWork) {
                e.printStackTrace(System.out);
                fail("RebindConfigUpdateServlet Remote Short lookup didn't work: com.ibm.ws.ejbcontainer.remote.configtests.ejb.ConfigTestsRemoteHome");
            } else {
                System.out.println("Caught expected exception : " + e);
            }
        }
        return null;
    }

    private ConfigTestsWarRemoteEJB lookupWarRemoteShort(boolean shouldWork) throws Exception {
        try {
            ConfigTestsWarRemoteHome home = (ConfigTestsWarRemoteHome) ctx.lookup("com.ibm.ws.ejbcontainer.remote.configtests.web.ejb.ConfigTestsWarRemoteHome");
            return home.create();
        } catch (NamingException e) {
            if (shouldWork) {
                e.printStackTrace(System.out);
                fail("RebindConfigUpdateServlet War Remote Short lookup didn't work: com.ibm.ws.ejbcontainer.remote.configtests.web.ejb.ConfigTestsWarRemoteHome");
            } else {
                System.out.println("Caught expected exception : " + e);
            }
        }
        return null;
    }

    private ConfigTestsRemoteEJB lookupRemoteLong(boolean shouldWork) throws Exception {
        try {
            ConfigTestsRemoteHome home = (ConfigTestsRemoteHome) ctx.lookup("ejb/ConfigTestsTestApp/ConfigTestsEJB.jar/ConfigTestsTestBean#com.ibm.ws.ejbcontainer.remote.configtests.ejb.ConfigTestsRemoteHome");
            return home.create();
        } catch (NamingException e) {
            if (shouldWork) {
                e.printStackTrace(System.out);
                fail("RebindConfigUpdateServlet Remote Long lookup didn't work: ejb/ConfigTestsTestApp/ConfigTestsEJB.jar/ConfigTestsTestBean#com.ibm.ws.ejbcontainer.remote.configtests.ejb.ConfigTestsRemoteHome");
            } else {
                System.out.println("Caught expected exception : " + e);
            }
        }
        return null;
    }

    private ConfigTestsWarRemoteEJB lookupWarRemoteLong(boolean shouldWork) throws Exception {
        try {
            ConfigTestsWarRemoteHome home = (ConfigTestsWarRemoteHome) ctx.lookup("ejb/ConfigTestsTestApp/ConfigTestsWeb.war/ConfigTestsWarTestBean#com.ibm.ws.ejbcontainer.remote.configtests.web.ejb.ConfigTestsWarRemoteHome");
            return home.create();
        } catch (NamingException e) {
            if (shouldWork) {
                e.printStackTrace(System.out);
                fail("RebindConfigUpdateServlet War Remote Long lookup didn't work: ejb/ConfigTestsTestApp/ConfigTestsWeb.war/ConfigTestsWarTestBean#com.ibm.ws.ejbcontainer.remote.configtests.web.ejb.ConfigTestsWarRemoteHome");
            } else {
                System.out.println("Caught expected exception : " + e);
            }
        }
        return null;
    }

    private ConfigTestsRemoteEJB lookupRemoteComp(boolean shouldWork) throws Exception {
        try {
            ConfigTestsRemoteHome home = (ConfigTestsRemoteHome) ctx.lookup("java:comp/env/ConfigTestsRemoteHome");
            return home.create();
        } catch (NamingException e) {
            if (shouldWork) {
                e.printStackTrace(System.out);
                fail("RebindConfigUpdateServlet Remote java:comp lookup didn't work: java:comp/env/ConfigTestsRemoteHome");
            } else {
                System.out.println("Caught expected exception : " + e);
            }
        }
        return null;
    }

    private ConfigTestsRemoteBusiness lookupRemoteCompBusiness(boolean shouldWork) throws Exception {
        try {
            return (ConfigTestsRemoteBusiness) ctx.lookup("java:comp/env/ConfigTestsRemoteBusiness");
        } catch (NamingException e) {
            if (shouldWork) {
                e.printStackTrace(System.out);
                fail("RebindConfigUpdateServlet Remote Business java:comp lookup didn't work: java:comp/env/ConfigTestsRemoteBusiness");
            } else {
                System.out.println("Caught expected exception : " + e);
            }
        }
        return null;
    }

    private ConfigTestsWarRemoteEJB lookupWarRemoteComp(boolean shouldWork) throws Exception {
        try {
            ConfigTestsWarRemoteHome home = (ConfigTestsWarRemoteHome) ctx.lookup("java:comp/env/ConfigTestsWarRemoteHome");
            return home.create();
        } catch (NamingException e) {
            if (shouldWork) {
                e.printStackTrace(System.out);
                fail("RebindConfigUpdateServlet WAR Remote java:comp lookup didn't work: java:comp/env/ConfigTestsWarRemoteHome");
            } else {
                System.out.println("Caught expected exception : " + e);
            }
        }
        return null;
    }

    private ConfigTestsWarRemoteBusiness lookupWarRemoteCompBusiness(boolean shouldWork) throws Exception {
        try {
            return (ConfigTestsWarRemoteBusiness) ctx.lookup("java:comp/env/ConfigTestsWarRemoteBusiness");
        } catch (NamingException e) {
            if (shouldWork) {
                e.printStackTrace(System.out);
                fail("RebindConfigUpdateServlet WAR Remote Business java:comp lookup didn't work: java:comp/env/ConfigTestsWarRemoteBusiness");
            } else {
                System.out.println("Caught expected exception : " + e);
            }
        }
        return null;
    }

    private ConfigTestsWarRemoteEJB lookupWarRemoteJavaNamespace(boolean shouldWork, String lookupString) throws Exception {
        try {
            ConfigTestsWarRemoteHome home = (ConfigTestsWarRemoteHome) ctx.lookup(lookupString);
            return home.create();
        } catch (NamingException e) {
            if (shouldWork) {
                e.printStackTrace(System.out);
                fail("ConfigTestsWarRemoteEJB Java Namespace lookup didn't work: " + lookupString);
            } else {
                System.out.println("Caught expected exception : " + e);
            }
        }
        return null;
    }

    private ConfigTestsWarLocalEJB lookupWarLocalJavaNamespace(String lookupString) throws Exception {
        try {
            ConfigTestsWarLocalHome home = (ConfigTestsWarLocalHome) ctx.lookup(lookupString);
            return home.create();
        } catch (NamingException e) {
            e.printStackTrace(System.out);
            fail("ConfigTestsWarLocalEJB Java Namespace lookup didn't work: " + lookupString);
        }
        return null;
    }

}
