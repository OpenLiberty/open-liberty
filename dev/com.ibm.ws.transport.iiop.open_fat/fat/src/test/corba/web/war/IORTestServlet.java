/*
 * Copyright (c) 2020,2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package test.corba.web.war;

import java.util.Objects;

import javax.annotation.Resource;
import javax.rmi.CORBA.Stub;
import javax.servlet.annotation.WebServlet;

import org.junit.Assert;
import org.junit.Test;
import org.omg.CORBA.ORB;

import componenttest.app.FATServlet;
import shared.ClientUtil;
import shared.IIOPClientTestLogic;
import shared.TestRemote;

@WebServlet("/IORTestServlet")
@SuppressWarnings("serial")
public class IORTestServlet extends FATServlet implements IIOPClientTestLogic {
    @Resource
    private ORB orb;
    @Override
    public ORB getOrb() {return orb;}

    @Test
    public void testOrbInjected() {
        System.out.println( "### Examining orb reference: " + orb + " ###");
        Objects.requireNonNull(orb);
    }

    @Test
    public void testEjbLookup() throws Exception {
        Objects.requireNonNull(ClientUtil.lookupTestBean(orb));
    }

    @Test
    public void testBusinessEjbLookup() throws Exception {
        Objects.requireNonNull(ClientUtil.lookupBusinessBean(orb));
    }

    @Test
    public void testEjbIorHasExactlyOneProfile() throws Exception {
        TestRemote bean = ClientUtil.lookupTestBean(orb);
        final int numProfiles = ClientUtil.getNumProfiles((Stub) bean, orb);
        System.out.printf("### IOR retrieved, with %d profile(s).%n", numProfiles);
        Assert.assertEquals("There should be only one profile in the IOR", 1, numProfiles);
    }
}
