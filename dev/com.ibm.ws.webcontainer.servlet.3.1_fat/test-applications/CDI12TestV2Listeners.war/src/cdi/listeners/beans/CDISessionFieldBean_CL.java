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

import javax.enterprise.context.SessionScoped;

import cdi.beans.v2.SessionFieldBean;

/**
 * CDI Testing: ServletContextListener session field bean.
 */
@SessionScoped
public class CDISessionFieldBean_CL extends SessionFieldBean {
    /** Default serialization ID */
    private static final long serialVersionUID = 1L;
}
