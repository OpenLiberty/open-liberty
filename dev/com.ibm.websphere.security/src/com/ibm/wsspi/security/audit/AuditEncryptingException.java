/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.security.audit;

/**
 *
 */
public class AuditEncryptingException extends java.lang.Exception {
    public AuditEncryptingException() {
        super();
    }

    public AuditEncryptingException(String message) {
        super(message);
    }

    public AuditEncryptingException(Exception e) {
        super(e);
    }

    public AuditEncryptingException(String message, Exception e) {
        super(message, e);
    }
}
