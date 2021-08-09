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

import java.util.List;

import com.ibm.ws.javaee.dd.common.QName;

/**
 * Represents &lt;handler-chain> in &lt;handler-chains> in
 * &lt;port-component-ref>.
 */
public interface HandlerChain
{
    /**
     * @return &lt;service-name-pattern>
     */
    QName getServiceNamePattern();

    /**
     * @return &lt;port-name-pattern>
     */
    QName getPortNamePattern();

    /**
     * @return &lt;protocol-bindings> as a read-only list
     */
    List<String> getProtocolBindings();

    /**
     * @return &lt;handler> as a read-only list
     */
    List<Handler> getHandlers();
}
