/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
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

package com.ibm.ws.ffdc;

public class DiagnosticModuleRegistrationFailureException extends Exception {
    private static final long serialVersionUID = -272181404821808015L;

    public DiagnosticModuleRegistrationFailureException() {
        super();
    }

    public DiagnosticModuleRegistrationFailureException(String s) {
        super(s);
    }

    public DiagnosticModuleRegistrationFailureException(String s, Throwable cause) {
        super(s, cause);
    }
}