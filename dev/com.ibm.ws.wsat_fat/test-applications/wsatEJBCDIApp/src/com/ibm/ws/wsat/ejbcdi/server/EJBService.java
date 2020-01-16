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

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jws.WebService;
import javax.naming.NamingException;

import com.ibm.ws.wsat.ejbcdi.utils.CommonUtils;

@WebService
@Stateless
public class EJBService {
	public String testEJBSayHelloToOther(String method, String server) throws NamingException, SQLException {
		System.out.println("EJBService: Start ejbSayHelloToOther");
		return CommonUtils.sayHello(method, server);
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW) 
	public String testEJBSayHelloToOtherWithRequiresNew(String method, String server) throws NamingException, SQLException {
		System.out.println("EJBService: Start ejbSayHelloToOther");
		return CommonUtils.sayHello(method, server);
	}
	
	@TransactionAttribute(TransactionAttributeType.MANDATORY) 
	public String testEJBSayHelloToOtherWithMandatory(String method, String server) throws NamingException, SQLException {
		System.out.println("EJBService: Start ejbSayHelloToOtherWithMandatory");
		return CommonUtils.sayHello(method, server);
	}
	
	@TransactionAttribute(TransactionAttributeType.NEVER) 
	public String testEJBSayHelloToOtherWithNever(String method, String server) throws NamingException, SQLException {
		System.out.println("EJBService: Start ejbSayHelloToOtherWithNever");
		return CommonUtils.sayHello(method, server);
	}
	
	@TransactionAttribute(TransactionAttributeType.SUPPORTS) 
	public String testEJBSayHelloToOtherWithSupports(String method, String server) throws NamingException, SQLException {
		System.out.println("EJBService: Start ejbSayHelloToOtherWithSupports");
		return CommonUtils.sayHello(method, server);
	}
	
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED) 
	public String testEJBSayHelloToOtherWithNotSupported(String method, String server) throws NamingException, SQLException {
		System.out.println("EJBService: Start ejbSayHelloToOtherWithNotSupported");
		return CommonUtils.sayHello(method, server);
	}
}
