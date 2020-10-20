/*******************************************************************************
 * Copyright (c) 2001, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.samples.technologysamples.basiccalcclient;

import javax.naming.InitialContext;

import com.ibm.websphere.samples.technologysamples.basiccalcclient.common.BasicCalculatorClient;
import com.ibm.websphere.samples.technologysamples.ejb.stateless.basiccalculatorejb.BasicCalculatorHome;


public class BasicCalculatorClientJ2EE extends BasicCalculatorClient
{
    /**
     * Initializes the BasicCalculatorClient by establishing a connection to the 
     * remote EJB's using the java:comp namespace that the J2EE Application Client Container
     * provides.
     */
    public void init() throws javax.naming.NamingException, javax.ejb.CreateException, java.rmi.RemoteException {

        // Create the initial context using the default context given to 
        // us by the J2EE Client Container
        System.out.print("--Creating InitialContext... ");
		InitialContext initCtx = new InitialContext();
		System.out.println("Done.");
		
		// Lookup the Home using the corbaname namespace.
		String  iiopPortString = System.getProperty("ServerIIOPPort");
		System.out.print("--IIOP port is... " + iiopPortString);
		System.out.print("--Looking-up Home... ");
        //Object homeObject = initCtx.lookup("corbaname::localhost:" + iiopPortString + "/NameService#ejb/global/"
        //		+ "BasicCalculator/BasicCalculatorEJB/BasicCalculator!com%5c.ibm%5c.websphere%5c.samples"
        //		+ "%5c.technologysamples%5c.ejb%5c.stateless%5c.basiccalculatorejb%5c.BasicCalculatorHome"); 
        Object homeObject = initCtx.lookup("corbaname::localhost:" + iiopPortString + "/NameService#ejb/global/"
        		+ "BasicCalculator/BasicCalculatorEJB/BasicCalculator!com.ibm.websphere.samples"
        		+ ".technologysamples.ejb.stateless.basiccalculatorejb.BasicCalculatorHome"); 
		System.out.println("Done.");
		
		// Narrow to a real object
		System.out.print("--Narrowing... ");
		bcHome = (BasicCalculatorHome) javax.rmi.PortableRemoteObject.narrow(homeObject, BasicCalculatorHome.class);
		System.out.println("Done.");
		
		// Create the home
		System.out.print("--Creating Home... ");
		bc = bcHome.create( );
		System.out.println("Done.");        
    }
}