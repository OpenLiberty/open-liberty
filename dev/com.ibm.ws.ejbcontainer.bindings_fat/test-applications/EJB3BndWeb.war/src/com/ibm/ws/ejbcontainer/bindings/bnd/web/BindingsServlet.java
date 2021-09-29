/*******************************************************************************
 * Copyright (c) 2007, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.bindings.bnd.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import javax.annotation.PostConstruct;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.bindings.bnd.ejb.LocalTargetFourBiz1;
import com.ibm.ws.ejbcontainer.bindings.bnd.ejb.LocalTargetFourBiz2;
import com.ibm.ws.ejbcontainer.bindings.bnd.ejb.LocalTargetOne;
import com.ibm.ws.ejbcontainer.bindings.bnd.ejb.LocalTargetOneBiz;
import com.ibm.ws.ejbcontainer.bindings.bnd.ejb.LocalTargetOneHome;
import com.ibm.ws.ejbcontainer.bindings.bnd.ejb.LocalTargetThreeBiz1;
import com.ibm.ws.ejbcontainer.bindings.bnd.ejb.LocalTargetThreeBiz2;
import com.ibm.ws.ejbcontainer.bindings.bnd.ejb.LocalTargetThreeBiz3;
import com.ibm.ws.ejbcontainer.bindings.bnd.ejb.RemoteTargetFourBiz1;
import com.ibm.ws.ejbcontainer.bindings.bnd.ejb.RemoteTargetFourBiz2;
import com.ibm.ws.ejbcontainer.bindings.bnd.ejb.RemoteTargetOne;
import com.ibm.ws.ejbcontainer.bindings.bnd.ejb.RemoteTargetOneBiz;
import com.ibm.ws.ejbcontainer.bindings.bnd.ejb.RemoteTargetOneHome;
import com.ibm.ws.ejbcontainer.bindings.bnd.ejb.RemoteTargetTwoBiz1;
import com.ibm.ws.ejbcontainer.bindings.bnd.ejb.RemoteTargetTwoBiz2;
import com.ibm.ws.ejbcontainer.bindings.bnd.ejb.RemoteTargetTwoBiz3;
import com.ibm.ws.ejbcontainer.bindings.bnd.ejb.SubLocalHome;
import com.ibm.ws.ejbcontainer.bindings.bnd.ejb.SubRemoteHome;
import com.ibm.ws.ejbcontainer.bindings.bnd.ejb.SupLocal;
import com.ibm.ws.ejbcontainer.bindings.bnd.ejb.SupLocalHome;
import com.ibm.ws.ejbcontainer.bindings.bnd.ejb.SupRemote;
import com.ibm.ws.ejbcontainer.bindings.bnd.ejb.SupRemoteHome;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt>Test Name:
 * <dd>BindingTest
 *
 * <dt>Test Descriptions:
 * <dd>This test bucket covers additional binding format elements/attributes
 * that were not available for EJB3 Beta, e.g. local/remote business interface bindings, local/remote home bindings
 *
 * <dt>Test Matrix:
 * <dd> <br>
 * Sub-tests
 * <ul>
 * <li> testTargetOneLBIBND - testing the local business interface binding for a session bean TargetOneBean
 * <li> <interface class="com.ibm.ws.ejbcontainer.bindings.bnd.ejb.LocalTarget" binding-name="ejblocal:ejb/LocalTarget"/>
 *
 * <li> testTargetOneRBIBND - testing the remote business interface binding for a session bean TargetOneBean
 * <li> <interface class="com.ibm.ws.ejbcontainer.bindings.bnd.ejb.RemoteTarget" binding-name="ejb/RemoteTarget"/>
 *
 * <li> testTargetOneLHIBND - testing the local home interface binding for a session bean TargetOneBean
 * <li> <session name="Targetbean" local-home-binding-name="ejblocal:ejb/LocalTargetHome"/>
 *
 * <li> testTargetOneRHIBND - testing the remote home interface binding for a session bean TargetOneBean
 * <li> <session name="Targetbean" remote-home-binding-name="ejb/RemoteTargetHome"/>
 *
 * <li> testTargetTwoRBIBND1 - testing the remote business interface 1 binding for a session bean TargetTwoBean
 * <li> <interface class="com.ibm.ws.ejbcontainer.bindings.bnd.ejb.RemoteTargetTwoBiz1" binding-name="ejb/RemoteTargetTwoBiz1"/>
 *
 * <li> testTargetTwoRBIBND2 - testing the remote business interface 2 binding for a session bean TargetTwoBean
 * <li> <interface class="com.ibm.ws.ejbcontainer.bindings.bnd.ejb.RemoteTargetTwoBiz2" binding-name="ejb/RemoteTargetTwoBiz2"/>
 *
 * <li> testTargetTwoRBIBND3 - testing the remote business interface 3 binding for a session bean TargetTwoBean
 * <li> <interface class="com.ibm.ws.ejbcontainer.bindings.bnd.ejb.RemoteTargetTwoBiz3" binding-name="ejb/RemoteTargetTwoBiz3"/>
 *
 * <li> testTargetThreeLBIBND1 - testing the local business interface 1 binding for a session bean TargetThreeBean
 * <li> <interface class="com.ibm.ws.ejbcontainer.bindings.bnd.ejb.LocalTargetThreeBiz1" binding-name="ejblocal:ejb/LocalTargetThreeBiz1"/>
 *
 * <li> testTargetThreeLBIBND2 - testing the local business interface 2 binding for a session bean TargetThreeBean
 * <li> <interface class="com.ibm.ws.ejbcontainer.bindings.bnd.ejb.LocalTargetThreeBiz2" binding-name="ejblocal:ejb/LocalTargetThreeBiz2"/>
 *
 * <li> testTargetThreeLBIBND3 - testing the local business interface 3 binding for a session bean TargetThreeBean
 * <li> <interface class="com.ibm.ws.ejbcontainer.bindings.bnd.ejb.LocalTargetThreeBiz3" binding-name="ejblocal:ejb/LocalTargetThreeBiz3"/>
 *
 * <li> testTargetFourLBIBND1 - testing the local business interface 1 binding for a session bean TargetFourBean
 * <li> <interface class="com.ibm.ws.ejbcontainer.bindings.bnd.ejb.LocalTargetThreeBiz1" binding-name="ejblocal:ejb/LocalTargetFourBiz1"/>
 *
 * <li> testTargetFourLBIBND2 - testing the local business interface 2 binding for a session bean TargetTFourBean
 * <li> <interface class="com.ibm.ws.ejbcontainer.bindings.bnd.ejb.LocalTargetFourBiz2" binding-name="ejblocal:ejb/LocalTargetFourBiz2"/>
 *
 * <li> testTargetFourRBIBND1 - testing the remote business interface 1 binding for a session bean TargetFourBean
 * <li> <interface class="com.ibm.ws.ejbcontainer.bindings.bnd.ejb.RemoteTargetFourBiz1" binding-name="ejb/RemoteTargetFourBiz1"/>
 *
 * <li> testTargetFourRBIBND2 - testing the remote business interface 2 binding for a session bean TargetTFourBean
 * <li> <interface class="com.ibm.ws.ejbcontainer.bindings.bnd.ejb.RemoteTargetFourBiz2" binding-name="ejb/RemoteTargetFourBiz2"/>
 *
 * <li> testSubBeanLHIBND - testing the local home interface binding for a subclass session bean SubBean
 * <li> <session name="Subbean" local-home-binding-name="ejblocal:ejb/SubLocalHome"/>
 *
 * <li> testSubBeanRHIBND - testing the remote home interface binding for a subclass session bean SubBean
 * <li> <session name="Subbean" remote-home-binding-name="ejb/SubRemoteHome"/>
 *
 * <li> testSupBeanLHIBND - testing the local home interface binding for a super class session bean SubBean
 * <li> <session name="Supbean" local-home-binding-name="ejblocal:ejb/SupLocalHome"/>
 *
 * <li> testSupBeanRHIBND - testing the remote home interface binding for a super class session bean SupBean
 * <li> <session name="Supbean" remote-home-binding-name="ejb/SupRemoteHome"/>
 *
 * </dl>
 */

@SuppressWarnings("serial")
@WebServlet("/BindingsServlet")
public class BindingsServlet extends FATServlet {

    private InitialContext ctx = null;

    @PostConstruct
    protected void setUp() {
        try {
            ctx = new InitialContext();
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    /*
     * testTargetOneLBIBND() Test Local Business Interface binding
     */
    @Test
    public void testTargetOneLBIBND() throws Exception {
        LocalTargetOneBiz lto1 = null;

        try {
            lto1 = (LocalTargetOneBiz) FATHelper.lookupJavaBinding("ejblocal:ejb/LocalTargetOneBiz");
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("1 ---> Check LocalTargetOne lookup failed");
        }

        assertEquals("2 ---> echo() returned unexpected value", "Success1", lto1.echo("Success1"));
    }

    /*
     * testTargetOneRBIBND()
     *
     * Test Remote Business Interface binding
     */
    @Test
    public void testTargetOneRBIBND() throws Exception {
        RemoteTargetOneBiz rto1 = null;

        try {
            rto1 = (RemoteTargetOneBiz) ctx.lookup("ejb/RemoteTargetOneBiz");
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("1 ---> Check RemoteTargetOne lookup failed");
        }

        assertEquals("2 ---> echo() returned unexpected value", "Success1", rto1.echo("Success1"));
    }

    /*
     * testTargetOneLHIBND()
     *
     * Test Local Home Interface binding
     */
    @Test
    public void testTargetOneLHIBND() throws Exception {
        LocalTargetOneHome ltoh1 = null;

        try {
            ltoh1 = (LocalTargetOneHome) ctx.lookup("ejblocal:ejb/LocalTargetOneHome");
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("1 ---> Check LocalTargetOneHome lookup failed");
        }

        LocalTargetOne lot1 = ltoh1.create();
        assertNotNull("2 ---> check LocalTargetOne creation failed", lot1);

        assertEquals("3 ---> echo() returned unexpected value", "Success1", lot1.echo("Success1"));
    }

    /*
     * testTargetOneRHIBND()
     *
     * Test Remote Home Interface binding
     */
    @Test
    public void testTargetOneRHIBND() throws Exception {
        RemoteTargetOneHome rtoh1 = null;

        try {
            rtoh1 = (RemoteTargetOneHome) ctx.lookup("ejb/RemoteTargetOneHome");
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("1 ---> Check RemoteTargetOneHome lookup failed");
        }

        RemoteTargetOne rot1 = rtoh1.create();
        assertNotNull("2 ---> Check RemoteTargetOne creation failed", rot1);

        assertEquals("3 ---> echo() returned unexpected value", "Success1", rot1.echo("Success1"));
    }

    /*
     * testTargetTwoRBIBND1()
     *
     * Test Remote Business Interface binding 1 for TargetBeanTwo
     */
    @Test
    public void testTargetTwoRBIBND1() throws Exception {
        RemoteTargetTwoBiz1 rt2b1 = null;

        try {
            rt2b1 = (RemoteTargetTwoBiz1) ctx.lookup("ejb/RemoteTargetTwoBiz1");
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("1 ---> Check RemoteTargetTwoBiz1 lookup failed");
        }

        assertEquals("2 ---> ping1() returned unexpected value", "pong", rt2b1.ping1());
    }

    /*
     * testTargetTwoRBIBND2()
     *
     * Test Remote Business Interface binding 2 for TargetBeanTwo
     */
    @Test
    public void testTargetTwoRBIBND2() throws Exception {
        RemoteTargetTwoBiz2 rt2b2 = null;

        try {
            rt2b2 = (RemoteTargetTwoBiz2) ctx.lookup("ejb/RemoteTargetTwoBiz2");
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("1 ---> Check RemoteTargetTwoBiz2 lookup failed");
        }

        assertEquals("2 ---> ping2() returned unexpected value", "pong", rt2b2.ping2());
    }

    /*
     * testTargetTwoRBIBND3()
     *
     * Test Remote Business Interface binding 3 for TargetBeanTwo
     */
    @Test
    public void testTargetTwoRBIBND3() throws Exception {
        RemoteTargetTwoBiz3 rt2b3 = null;

        try {
            rt2b3 = (RemoteTargetTwoBiz3) ctx.lookup("ejb/RemoteTargetTwoBiz3");
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("1 ---> Check RemoteTargetTwoBiz3 lookup failed");
        }

        assertEquals("2 ---> ping3() returned unexpected value", "pong", rt2b3.ping3());
    }

    /*
     * testTargetThreeLBIBND1()
     *
     * Test Local Business Interface binding 1 for TargetBeanThree
     */
    @Test
    public void testTargetThreeLBIBND1() throws Exception {
        LocalTargetThreeBiz1 lt3b1 = null;

        try {
            lt3b1 = (LocalTargetThreeBiz1) ctx.lookup("ejblocal:ejb/LocalTargetThreeBiz1");
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("1 ---> Check LocalTargetThreeBiz1 lookup failed");
        }

        assertEquals("2 ---> ping1() returned unexpected value", "pong", lt3b1.ping1());
    }

    /*
     * testTargetThreeLBIBND2()
     *
     * Test Local Business Interface binding 2 for TargetBeanThree
     */
    @Test
    public void testTargetThreeLBIBND2() throws Exception {
        LocalTargetThreeBiz2 lt3b2 = null;

        try {
            lt3b2 = (LocalTargetThreeBiz2) ctx.lookup("ejblocal:ejb/LocalTargetThreeBiz2");
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("1 ---> Check LocalTargetThreeBiz2 lookup failed");
        }

        assertEquals("2 ---> ping2() returned unexpected value", "pong", lt3b2.ping2());
    }

    /*
     * testTargetThreeLBIBND3()
     *
     * Test Local Business Interface binding 3 for TargetBeanThree
     */
    @Test
    public void testTargetThreeLBIBND3() throws Exception {
        LocalTargetThreeBiz3 lt3b3 = null;

        try {
            lt3b3 = (LocalTargetThreeBiz3) ctx.lookup("ejblocal:ejb/LocalTargetThreeBiz3");
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("1 ---> Check LocalTargetThreeBiz3 lookup failed");
        }

        assertEquals("2 ---> ping3() returned unexpected value", "pong", lt3b3.ping3());
    }

    /*
     * testTargetFourRBIBND1()
     *
     * Test Remote Business Interface binding 1 for TargetBeanFour
     */
    @Test
    public void testTargetFourRBIBND1() throws Exception {
        RemoteTargetFourBiz1 rt4b1 = null;

        try {
            rt4b1 = (RemoteTargetFourBiz1) ctx.lookup("ejb/RemoteTargetFourBiz1");
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("1 ---> Check RemoteTargetFourBiz1 lookup failed");
        }

        assertEquals("2 ---> ping3() returned unexpected value", "pong", rt4b1.ping3());
    }

    /*
     * testTargetFourRBIBND2()
     *
     * Test Remote Business Interface binding 2 for TargetBeanFour
     */
    @Test
    public void testTargetFourRBIBND2() throws Exception {
        RemoteTargetFourBiz2 rt2b4 = null;

        try {
            rt2b4 = (RemoteTargetFourBiz2) ctx.lookup("ejb/RemoteTargetFourBiz2");
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("1 ---> Check RemoteTargetFourBiz2 lookup failed");
        }

        assertEquals("2 ---> ping4() returned unexpected value", "pong", rt2b4.ping4());
    }

    /*
     * testTargetFourLBIBND1()
     *
     * Test Local Business Interface binding 1 for TargetBeanThree
     */
    @Test
    public void testTargetFourLBIBND1() throws Exception {
        LocalTargetFourBiz1 lt4b1 = null;

        try {
            lt4b1 = (LocalTargetFourBiz1) ctx.lookup("ejblocal:ejb/LocalTargetFourBiz1");
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("1 ---> Check LocalTargetFourBiz1 lookup failed");
        }

        assertEquals("2 ---> ping1() returned unexpected value", "pong", lt4b1.ping1());
    }

    /*
     * testTargetFourLBIBND2()
     *
     * Test Local Business Interface binding 2 for TargetBeanFour
     */
    @Test
    public void testTargetFourLBIBND2() throws Exception {
        LocalTargetFourBiz2 lt3b2 = null;

        try {
            lt3b2 = (LocalTargetFourBiz2) ctx.lookup("ejblocal:ejb/LocalTargetFourBiz2");
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("1 ---> Check LocalTargetFourBiz2 lookup failed");
        }

        assertEquals("2 ---> ping2() returned unexpected value", "pong", lt3b2.ping2());
    }

    /*
     * testSubBeanLHIBND()
     *
     * Test Local Home Interface binding for SubBean
     */
    @Test
    public void testSubBeanLHIBND() throws Exception {
        SubLocalHome slh = null;

        try {
            slh = (SubLocalHome) ctx.lookup("ejblocal:ejb/SubLocalHome");
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("1 ---> Check SubLocalHome lookup failed");
        }

        SupLocal sl = slh.create();
        assertNotNull("2 ---> check SubLocal creation failed", sl);

        assertEquals("3 ---> echo() returned unexpected value", "pong", sl.ping());
    }

    /*
     * testSubBeanRHIBND()
     *
     * Test Remote Home Interface binding for SubBean
     */
    @Test
    public void testSubBeanRHIBND() throws Exception {
        SubRemoteHome srh = null;

        try {
            srh = (SubRemoteHome) ctx.lookup("ejb/SubRemoteHome");
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("1 ---> Check SubRemoteHome lookup failed");
        }

        SupRemote sr = srh.create();
        assertNotNull("2 ---> Check RemoteTargetOne creation failed", sr);

        assertEquals("3 ---> ping() returned unexpected value", "pong", sr.ping());
    }

    /*
     * testSupBeanLHIBND()
     *
     * Test Local Home Interface binding for SupBean
     */
    @Test
    public void testSupBeanLHIBND() throws Exception {
        SupLocalHome slh = null;

        try {
            slh = (SupLocalHome) ctx.lookup("ejblocal:ejb/SupLocalHome");
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("1 ---> Check SupLocalHome lookup failed");
        }

        SupLocal sl = slh.create();
        assertNotNull("2 ---> check SupLocal creation failed", slh);

        assertEquals("3 ---> echo() returned unexpected value", "pong", sl.ping());
    }

    /*
     * testSupBeanRHIBND()
     *
     * Test Remote Home Interface binding for SupBean
     */
    @Test
    public void testSupBeanRHIBND() throws Exception {
        SupRemoteHome srh = null;

        try {
            srh = (SupRemoteHome) ctx.lookup("ejb/SupRemoteHome");
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("1 ---> Check SupRemoteHome lookup failed");
        }

        SupRemote sr = srh.create();
        assertNotNull("2 ---> Check RemoteTargetOne creation failed", sr);

        assertEquals("3 ---> ping() returned unexpected value", "pong", sr.ping());
    }
}
