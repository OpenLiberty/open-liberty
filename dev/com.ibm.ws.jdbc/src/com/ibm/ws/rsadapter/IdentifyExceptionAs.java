/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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

package com.ibm.ws.rsadapter;

/**
 * Values for <code>identifyException as=...</code>.
 */
public enum IdentifyExceptionAs {
    AuthorizationError(null),
    None("java.lang.Void"),
    StaleConnection("com.ibm.websphere.ce.cm.StaleConnectionException"),
    StaleStatement("com.ibm.websphere.ce.cm.StaleStatementException"),
    Unsupported(null);

    public final String legacyClassName;

    private IdentifyExceptionAs(String legacyClassName) {
        this.legacyClassName = legacyClassName;
    }
}
