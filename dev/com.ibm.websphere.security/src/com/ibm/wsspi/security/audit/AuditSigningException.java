/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
package com.ibm.wsspi.security.audit;

/**
 *
 */
public class AuditSigningException extends java.lang.Exception {
    public AuditSigningException() {
        super();
    }

    public AuditSigningException(String message) {
        super(message);
    }

    public AuditSigningException(Exception e) {
        super(e);
    }

    public AuditSigningException(String message, Exception e) {
        super(message, e);
    }
}
