/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsat.test.servlet;

import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.servlet.http.HttpServletRequest;

import com.ibm.ws.wsat.test.utils.CommonUtils;

@Stateless
@TransactionManagement(value=TransactionManagementType.BEAN)
public class EJBClient2 {
	public String invokeCall(HttpServletRequest request) {
		return CommonUtils.mainLogic(request);
	}
}
