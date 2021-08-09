/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2listeners.war.cdi.listeners.beans;

import javax.enterprise.context.Dependent;

import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.DependentFieldBean;

/**
 * CDI Testing: HttpSessionListener dependent field bean.
 */
@Dependent
public class CDIDependentFieldBean_SL extends DependentFieldBean {
    /** Default serialization ID */
    private static final long serialVersionUID = 1L;
}
