/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.bindings.serverxml.bnd.web;

import static org.junit.Assert.assertNotNull;

import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;
import javax.servlet.annotation.WebServlet;

import com.ibm.ejb3x.BindingName.ejb.BindingNameIntf;
import com.ibm.ejb3x.BindingName.ejb.RemoteBindingNameIntf;
import com.ibm.ejb3x.ComponentIDBnd.ejb.ComponentIDBnd;
import com.ibm.ejb3x.ComponentIDBnd.ejb.ComponentIDBndHome;
import com.ibm.ejb3x.HomeBindingName.ejb.LocalHomeBindingName;
import com.ibm.ejb3x.HomeBindingName.ejb.LocalHomeBindingNameHome;
import com.ibm.ejb3x.HomeBindingName.ejb.RemoteHomeBindingName;
import com.ibm.ejb3x.HomeBindingName.ejb.RemoteHomeBindingNameHome;
import com.ibm.ejb3x.SimpleBindingName.ejb.SimpleBindingName;
import com.ibm.ejb3x.SimpleBindingName.ejb.SimpleBindingNameHome;

import componenttest.app.FATServlet;

/**
 */
@SuppressWarnings("serial")
@WebServlet("/ServerXMLBindingsTestServlet")
public class ServerXMLBindingsTestServlet extends FATServlet {

    public void lookupServerXMLBindings() throws Exception {
        InitialContext ctx = new InitialContext();

        // Component-id --------------------------------------
        ComponentIDBndHome compIDbeanHome = (ComponentIDBndHome) ctx.lookup("ejblocal:ejb/ServerXMLMyEJB1#com.ibm.ejb3x.ComponentIDBnd.ejb.ComponentIDBndHome");
        assertNotNull("lookup component-id was null", compIDbeanHome);
        ComponentIDBnd compIDbean = compIDbeanHome.create();
        assertNotNull("home.create() component-id was null", compIDbean);
        assertNotNull("bean.method() for component-id was null", compIDbean.foo());

        // local-home-binding --------------------------------
        LocalHomeBindingNameHome LHBbeanHome = (LocalHomeBindingNameHome) ctx.lookup("ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/ServerXMLHomeBindingNameHome1");
        assertNotNull("lookup local-home-binding was null", LHBbeanHome);
        LocalHomeBindingName LHBbean = LHBbeanHome.create();
        assertNotNull("home.create() for local-home-binding was null", LHBbean);
        assertNotNull("bean.method() for local-home-binding was null", LHBbean.foo());

        // remote-home-binding -------------------------------
        Object RHBlookup = ctx.lookup("com/ibm/ejb3x/HomeBindingName/ejb/ServerXMLHomeBindingNameHome3");
        RemoteHomeBindingNameHome RHBbeanHome = (RemoteHomeBindingNameHome) PortableRemoteObject.narrow(RHBlookup, RemoteHomeBindingNameHome.class);
        assertNotNull("lookup for remote-home-binding was null", RHBbeanHome);
        RemoteHomeBindingName RHBbean = RHBbeanHome.create();
        assertNotNull("lookup for remote-home-binding was null", RHBbean);
        assertNotNull("bean.method() for remote-home-binding was null", RHBbean.foo());

        // simple-binding-name -------------------------------
        SimpleBindingNameHome SBNbeanHome = (SimpleBindingNameHome) ctx.lookup("ejblocal:com/ibm/ejb3x/SimpleBindingName/ejb/ServerXMLSimpleBindingNameHome1");
        assertNotNull("lookup for simple-binding-name was null", SBNbeanHome);
        SimpleBindingName SBNbean = SBNbeanHome.create();
        assertNotNull("lookup for simple-binding-name was null", SBNbean);
        assertNotNull("bean.method() for simple-binding-name was null", SBNbean.foo());

        // binding-name --------------------------------------
        // Local
        BindingNameIntf BNbeanHome = (BindingNameIntf) ctx.lookup("ejblocal:ejb/ServerXMLBindingNameIntf5");
        assertNotNull("lookup binding-name for local was null", BNbeanHome);
        assertNotNull("bean.method() for binding-name for local was null", BNbeanHome.foo());

        // Remote
        Object RBNlookup = ctx.lookup("ejb/ServerXMLBindingNameIntf5");
        RemoteBindingNameIntf RBNbeanHome = (RemoteBindingNameIntf) PortableRemoteObject.narrow(RBNlookup, RemoteBindingNameIntf.class);
        assertNotNull("lookup binding-name for remote was null", RBNbeanHome);
        assertNotNull("bean.method() for binding-name for remote was null", RBNbeanHome.foo());

        // EJB in WARs ----------------------------------------
        // local-home-binding
        WarTestLocalHome LHBWarbeanHome = (WarTestLocalHome) ctx.lookup("ejblocal:com/ibm/ws/ejbcontainer/ServerXML/ejb/ServerXMLWarTestBeanLocal");
        assertNotNull("lookup local-home-binding IN WAR was null", LHBWarbeanHome);
        WarLocalEJB LHBWarbean = LHBWarbeanHome.create();
        assertNotNull("home.create() for local-home-binding IN WAR was null", LHBWarbean);
        assertNotNull("bean.method() for local-home-binding IN WARwas null", LHBWarbean.getString());

        // remote-home-binding
        Object RHBWarlookup = ctx.lookup("com/ibm/ws/ejbcontainer/ServerXML/ejb/ServerXMLWarTestBeanRemote");
        WarTestRemoteHome RHBWarbeanHome = (WarTestRemoteHome) PortableRemoteObject.narrow(RHBWarlookup, WarTestRemoteHome.class);
        assertNotNull("lookup for remote-home-binding IN WAR was null", RHBWarbeanHome);
        WarRemoteEJB RHBWarbean = RHBWarbeanHome.create();
        assertNotNull("home.create() for remote-home-binding IN WAR was null", RHBWarbean);
        assertNotNull("bean.method() for remote-home-binding IN WAR was null", RHBWarbean.getString());
    }

}
