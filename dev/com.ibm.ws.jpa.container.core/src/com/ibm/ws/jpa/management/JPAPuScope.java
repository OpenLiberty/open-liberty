/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.management;

/**
 * Enumeration of persistence unit scope used in JPA service processing.
 */
public enum JPAPuScope
{
    // Persistence xmls defined in the persistance archive.
    EJB_Scope,
    // Persistence xmls defined in the WebApp module.
    Web_Scope,
    // Persistence xmls defined in the EJB module.
    EAR_Scope,
    // Persistence xmls defined in the Application Client module.
    Client_Scope;
}
