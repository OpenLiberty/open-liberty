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
package web.ejbcdi.servlet;

import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.servlet.http.HttpServletRequest;

import web.ejbcdi.utils.CommonUtils;

@Stateless
@TransactionManagement(value=TransactionManagementType.BEAN)
public class EJBClient2 {
	public String invokeCall(HttpServletRequest request) {
		return CommonUtils.mainLogic(request);
	}
}
