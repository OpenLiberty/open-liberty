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

import com.ibm.ws.wsat.ejbcdi.utils.CommonUtils;

@WebService
public class NormalService {
	public String normalSayHelloToOther(String method, String server) throws NamingException, SQLException {
		return CommonUtils.sayHello(method, server);
	}
}
