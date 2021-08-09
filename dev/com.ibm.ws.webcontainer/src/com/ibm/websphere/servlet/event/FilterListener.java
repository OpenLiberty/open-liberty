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


/**
 * Event listener interface used for notifications about fiters.
 * Most of these event have to do with the state management of a
 * filter's lifecycle.
 * 
 * @ibm-api
 */

public interface FilterListener
extends EventListener
{

    /**
     * Triggered just prior to the execution of Filter.init().
     *
     * @see  javax.servlet.Filter
     */	
	public abstract void onFilterStartInit(FilterEvent filterinvocationevent);

    /**
     * Triggered just after the execution of Filter.init().
     *
     * @see  javax.servlet.Filter
     */	
	public abstract void onFilterFinishInit(FilterEvent filterinvocationevent);

    /**
     * Triggered just prior to the execution of Filter.destroy().
     *
     * @see  javax.servlet.Filter
     */	
	public abstract void onFilterStartDestroy(FilterEvent filterinvocationevent);

    /**
     * Triggered just after the execution of Filter.destroy().
     *
     * @see  javax.servlet.Filter
     */	
	public abstract void onFilterFinishDestroy(FilterEvent filterinvocationevent);
}
