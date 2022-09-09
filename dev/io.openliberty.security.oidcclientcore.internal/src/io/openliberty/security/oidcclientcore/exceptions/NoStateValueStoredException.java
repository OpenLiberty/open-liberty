/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.exceptions;

public class NoStateValueStoredException extends AuthenticationResponseException {

    private static final long serialVersionUID = 1L;

    public NoStateValueStoredException(String clientId, String nlsMessage) {
        super(clientId, nlsMessage); // TODO: Create NLS message.
    }

}
