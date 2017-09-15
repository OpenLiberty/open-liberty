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
package com.ibm.websphere.management.j2ee;


/**
 * Identifies a JCA resource. A JCAResource object manages one or more connection
 * factories. For each JCA resource provided on a server, there must be one
 * JCAResource OBJECT_NAME in the servers resources list that identifies it.
 */
public interface JCAResourceMBean extends J2EEResourceMBean {

    /**
     * A list of the connection factories available on the corresponding JCAResource
     * object. For each connection factory available to this JCAResource there must be one
     * JCAConnectionFactory OBJECT_NAME in the connectionFactories list that
     * identifies it.
     */
    String[] getconnectionFactories();

}
