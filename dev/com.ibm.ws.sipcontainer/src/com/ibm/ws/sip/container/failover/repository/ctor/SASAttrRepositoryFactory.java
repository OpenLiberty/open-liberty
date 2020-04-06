/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.failover.repository.ctor;

import com.ibm.ws.sip.container.failover.repository.SASAttrRepository;

public abstract class SASAttrRepositoryFactory {

    /**
     * Tries to dynamically load a class which implements some repository interface.
     * @return a Sip application session attribute manager
     * @throws ClassNotFoundException - when failed to find the implementation class by name
     * @throws InstantiationException - succeeded to find the class but failed to create an instance
     * @throws IllegalAccessException - found the class but security violation occured.
     */
	abstract public SASAttrRepository createRepository() 
	throws ClassNotFoundException,InstantiationException, IllegalAccessException;
}
