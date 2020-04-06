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
package cdi.listeners.beans;

import javax.enterprise.context.Dependent;

import cdi.beans.v2.MethodBean;
import cdi.listeners.interceptors.CDILogInterceptorAnnotation_SIL;

/**
 * CDI Testing: HttpSessionIdListener method bean.xs
 */
@Dependent
@CDILogInterceptorAnnotation_SIL
public class CDIMethodBean_SIL extends MethodBean {
    // EMPTY
}