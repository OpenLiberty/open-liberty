/*******************************************************************************
 * Copyright (c) 2015, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.wsat.components;

public interface WSATConfigService {

    public boolean isSSLEnabled();

    public boolean isClientAuthEnabled();

    public String getSSLReferenceId();

    public String getWSATUrl();

    public long getAsyncResponseTimeout();

    /**
     * Returns {@code true} when {@code propagation="check_wsdl"} is configured on
     * {@code <wsAtomicTransaction/>}.  When {@code false} (the default {@code "always"}
     * mode), WS-AT coordination context is propagated unconditionally.
     */
    public boolean isCheckWsdlMode();
}