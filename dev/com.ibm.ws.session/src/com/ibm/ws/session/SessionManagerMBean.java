/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.session;

/**
 *
 */
public interface SessionManagerMBean {

    /**
     * A String representing the {@link javax.management.ObjectName} that this MBean maps to.
     */
    String OBJECT_NAME = "WebSphere:name=com.ibm.ws.jmx.mbeans.sessionManagerMBean";

    /**
     * The unique identifier for this server among a group of other servers.
     * 
     * @return a unique server identifier
     */
    public String getCloneID();
    
    /**
     * Gets the character used to delimit clone IDs in session cookies.
     * Usually either ':' or '+' will be returned.
     * 
     * @return the character used to delimit clone IDs in session cookies
     */
    public String getCloneSeparator();

    /**
     * Gets the default name of session cookies. Note that this value can be
     * overridden by individual applications at run time. Whenever possible,
     * the exact cookie name should be read from the ServletContext instead
     * of this value.
     * 
     * @return the default name of session cookies
     */
    public String getCookieName();  
}
