/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsat.ejbcdi.server;

import java.sql.SQLException;

import javax.jws.WebService;
import javax.naming.NamingException;
import javax.transaction.Transactional;

import com.ibm.ws.wsat.ejbcdi.utils.CommonUtils;

@WebService
public class CDIService {
	public String testCDISayHelloToOther(String method, String server) throws NamingException, SQLException {
		System.out.println("EJBService: Start cdiSayHelloToOther");
		return CommonUtils.sayHello(method, server);
	}
	
	@Transactional(Transactional.TxType.REQUIRES_NEW)
	public String testCDISayHelloToOtherWithRequiresNew(String method, String server) throws NamingException, SQLException {
		System.out.println("EJBService: Start cdiSayHelloToOtherWithRequiresNew");
        return CommonUtils.sayHello(method, server);
	}
	
	@Transactional(Transactional.TxType.MANDATORY)
	public String testCDISayHelloToOtherWithMandatory(String method, String server) throws NamingException, SQLException {
		System.out.println("EJBService: Start cdiSayHelloToOtherWithMandatory");
        return CommonUtils.sayHello(method, server);
	}
	
	@Transactional(Transactional.TxType.NEVER)
	public String testCDISayHelloToOtherWithNever(String method, String server) throws NamingException, SQLException {
		System.out.println("EJBService: Start cdiSayHelloToOtherWithNever");
        return CommonUtils.sayHello(method, server);
	}
	
	@Transactional(Transactional.TxType.SUPPORTS)
	public String testCDISayHelloToOtherWithSupports(String method, String server) throws NamingException, SQLException {
		System.out.println("EJBService: Start cdiSayHelloToOtherWithSupports");
        return CommonUtils.sayHello(method, server);
	}
	
	@Transactional(Transactional.TxType.NOT_SUPPORTED)
	public String testCDISayHelloToOtherWithNotSupported(String method, String server) throws NamingException, SQLException {
		System.out.println("EJBService: Start cdiSayHelloToOtherWithNotSupported");
        return CommonUtils.sayHello(method, server);
	}
}
