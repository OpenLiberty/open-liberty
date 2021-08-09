/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.servlet.event;

import java.util.EventListener;


public interface FilterInvocationListener
extends EventListener
{

    /**
     * Triggered just prior to the execution of Filter.doFilter().
     *
     * @see  javax.servlet.Filter
     */	
	public abstract void onFilterStartDoFilter(FilterInvocationEvent filterinvocationevent);

    /**
     * Triggered just after the execution of Filter.doFilter().
     *
     * @see  javax.servlet.Filter
     */	
	public abstract void onFilterFinishDoFilter(FilterInvocationEvent filterinvocationevent);

}
