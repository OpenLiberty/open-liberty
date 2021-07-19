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
package com.ibm.ws390.ola;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Set;

import javax.resource.spi.ManagedConnection;

import org.junit.Test;

import com.ibm.websphere.ola.ConnectionSpecImpl;
import com.ibm.ws390.ola.jca.ConnectionRequestInfoImpl;
import com.ibm.ws390.ola.unittest.util.ConnectionRequestInfoImplHelper;

public class ManagedConnectionFactoryImplTest {

	test.common.SharedOutputManager outputMgr = test.common.SharedOutputManager.getInstance().trace("*=all");

	@org.junit.Rule
	public org.junit.rules.TestRule outputRule = outputMgr;

	/**
	 * Test connection matching when no properties have changed.
	 */
	@Test
	public void testMatchManagedConnection() throws Exception {
		// Create a default managed connection factory.  Default the OTMA client
		// name as we would if we were reading ra.xml.
		ManagedConnectionFactoryImpl mcf = new ManagedConnectionFactoryImpl();
		mcf.setOTMAClientName(ConnectionSpecImpl.DEFAULT_OTMA_CLIENT_NAME);
		
		// Create some connection parms.
		ConnectionSpecImpl csi = new ConnectionSpecImpl();
		csi.setConnectionWaitTimeout(30);
		csi.setRegisterName("FRED");
		ConnectionRequestInfoImpl crii = ConnectionRequestInfoImplHelper.create(csi);
		
		// Create a connection
		ManagedConnection mc = mcf.createManagedConnection(null, crii);
		
		// Make sure if we try to create another one, it matches.
		Set<ManagedConnection> mcSet = Collections.singleton(mc);
		ManagedConnection mc2 = mcf.matchManagedConnections(mcSet, null, crii);
		
		assertNotNull("Connection should have matched", mc2);
		assertTrue("Connections should be equal", mc == mc2);
	}

	/**
	 * Test connection matching when user IDs have changed.  We should match the
	 * connection since we support re-authentication.
	 */
	@Test
	public void testMatchManagedConnectionNewUser() throws Exception {
		// Create a default managed connection factory
		ManagedConnectionFactoryImpl mcf = new ManagedConnectionFactoryImpl();
		mcf.setOTMAClientName(ConnectionSpecImpl.DEFAULT_OTMA_CLIENT_NAME);
		
		// Create some connection parms.
		ConnectionSpecImpl csi = new ConnectionSpecImpl();
		csi.setConnectionWaitTimeout(30);
		csi.setUsername("FRED");
		csi.setPassword("FREDPASSWORD");
		csi.setRegisterName("FRED");
		ConnectionRequestInfoImpl crii = ConnectionRequestInfoImplHelper.create(csi);
		
		// Create a connection
		ManagedConnection mc = mcf.createManagedConnection(null, crii);
		
		// Make sure if we try to create another one, it matches.
		Set<ManagedConnection> mcSet = Collections.singleton(mc);
		csi.setUsername("BOB");
		csi.setUsername("BOBPASSWORD");
		crii = ConnectionRequestInfoImplHelper.create(csi);
		ManagedConnection mc2 = mcf.matchManagedConnections(mcSet, null, crii);
		
		assertNotNull("Connection should have matched", mc2);
		assertTrue("Connections should be equal", mc == mc2);
	}

	/**
	 * Test connection matching when some property has changed.
	 */
	@Test
	public void testMatchManagedConnectionNewProperty() throws Exception {
		// Create a default managed connection factory.  Default the OTMA client
		// name as we would if we were reading ra.xml.
		ManagedConnectionFactoryImpl mcf = new ManagedConnectionFactoryImpl();
		mcf.setOTMAClientName(ConnectionSpecImpl.DEFAULT_OTMA_CLIENT_NAME);
		
		// Create some connection parms.
		ConnectionSpecImpl csi = new ConnectionSpecImpl();
		csi.setConnectionWaitTimeout(30);
		csi.setRegisterName("FRED");
		ConnectionRequestInfoImpl crii = ConnectionRequestInfoImplHelper.create(csi);
		
		// Create a connection
		ManagedConnection mc = mcf.createManagedConnection(null, crii);
		
		// Change a property and make sure we don't have a match.
		Set<ManagedConnection> mcSet = Collections.singleton(mc);
		csi.setConnectionWaitTimeout(40);
		crii = ConnectionRequestInfoImplHelper.create(csi);
		ManagedConnection mc2 = mcf.matchManagedConnections(mcSet, null, crii);
		
		assertNull("Connection should not have matched", mc2);
	}
}
