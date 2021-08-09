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
public class AuditEncryptionException extends java.lang.Exception {
    public AuditEncryptionException() {
        super();
    }

    public AuditEncryptionException(String message) {
        super(message);
    }

    public AuditEncryptionException(Exception e) {
        super(e);
    }

    public AuditEncryptionException(String message, Exception e) {
        super(message, e);
    }
}
