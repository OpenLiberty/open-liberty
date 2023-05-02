/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
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
package web.ejbcdi.server;

import java.sql.SQLException;

import javax.jws.WebService;
import javax.naming.NamingException;

import web.ejbcdi.utils.CommonUtils;

@WebService
public class NormalService {
	public String normalSayHelloToOther(String method, String server) throws NamingException, SQLException {
		return CommonUtils.sayHello(method, server);
	}
}
