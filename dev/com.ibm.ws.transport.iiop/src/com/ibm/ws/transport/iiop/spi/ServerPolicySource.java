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
package com.ibm.ws.transport.iiop.spi;

import java.util.List;

import org.omg.CORBA.Policy;


/**
 *
 */
public interface ServerPolicySource {

    /**
     * @return
     * @throws Exception
     */
    public abstract void addConfiguredPolicies(List<Policy> policies, ORBRef server) throws Exception;

}
