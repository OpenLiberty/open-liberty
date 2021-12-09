/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.wsat.testservice.impl;

import javax.jws.WebService;

import com.ibm.ws.jaxws.wsat.testservice.Simple;

@WebService(endpointInterface = "com.ibm.ws.jaxws.wsat.testservice.Simple")
public class SimpleImpl implements Simple {

	@Override
	public String echo(String msg) {
		return msg;
	}

}
