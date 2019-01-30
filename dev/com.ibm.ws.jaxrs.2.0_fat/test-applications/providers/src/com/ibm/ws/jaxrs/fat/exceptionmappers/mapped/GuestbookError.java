/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.exceptionmappers.mapped;

public class GuestbookError extends Error {

    private static final long serialVersionUID = -1247655313421068853L;

    public GuestbookError(String message) {
        super(message);
    }
}
