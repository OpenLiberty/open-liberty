/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package com.ibm.ws.jaxrs.fat.beanparam;

import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;

public class InnerBeanParamEntity {

    @FormParam("innerForm")
    public String innerForm;

    @CookieParam("innerCookie")
    public String innerCookie;
}