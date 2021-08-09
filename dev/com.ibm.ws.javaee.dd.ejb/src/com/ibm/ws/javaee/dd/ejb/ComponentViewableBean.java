/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.dd.ejb;

/**
 * Represents common elements for beans that support component views.
 */
public interface ComponentViewableBean
                extends EnterpriseBean
{
    /**
     * @return &lt;home>, or null if unspecified
     */
    String getHomeInterfaceName();

    /**
     * @return &lt;remote>, or null if unspecified
     */
    String getRemoteInterfaceName();

    /**
     * @return &lt;local-home>, or null if unspecified
     */
    String getLocalHomeInterfaceName();

    /**
     * @return &lt;local>, or null if unspecified
     */
    String getLocalInterfaceName();
}
