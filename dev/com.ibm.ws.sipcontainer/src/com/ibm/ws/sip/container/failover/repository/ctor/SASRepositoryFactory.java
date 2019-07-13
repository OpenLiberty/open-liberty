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
package com.ibm.ws.sip.container.failover.repository.ctor;

import com.ibm.ws.sip.container.failover.repository.SASRepository;

public abstract class SASRepositoryFactory 
{
	/**
     * 
     * @return some SASRepository concrete class or null if not found such. 
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
	public abstract SASRepository createRepository() 
	throws ClassNotFoundException,InstantiationException, IllegalAccessException;
}
