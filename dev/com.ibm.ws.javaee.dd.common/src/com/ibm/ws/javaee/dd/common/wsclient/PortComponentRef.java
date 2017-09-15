/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.dd.common.wsclient;

/**
 * Represents &lt;port-component-ref> in &lt;service-ref>.
 */
public interface PortComponentRef
{
    /**
     * @return &lt;service-endpoint-interface>
     */
    String getServiceEndpointInterfaceName();

    /**
     * @return true if &lt;enable-mtom> is specified
     * @see #isEnableMtom
     */
    boolean isSetEnableMtom();

    /**
     * @return &lt;enable-mtom> if specified
     * @see #isSetEnableMtom
     */
    boolean isEnableMtom();

    /**
     * @return true if &lt;mtom-threshold> is specified
     * @see #getMtomThreshold
     */
    boolean isSetMtomThreshold();

    /**
     * @return &lt;mtom-threshold> if specified
     * @see #isSetMtomThreshold
     */
    int getMtomThreshold();

    /**
     * @return &lt;addressing>, or null if unspecified
     */
    Addressing getAddressing();

    /**
     * @return &lt;respect-binding>, or null if unspecified
     */
    RespectBinding getRespectBinding();

    /**
     * @return &lt;port-component-link>, or null if unspecified
     */
    String getPortComponentLink();
}
