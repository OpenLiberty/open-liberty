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

package com.ibm.websphere.security.wim.scim20.exceptions;

/**
 * The specified filter yields many more results than the server is willing to
 * calculate or process. For example, a filter such as "(userName pr)" by itself
 * would return all entries with a "userName" and MAY not be acceptable to the
 * service provider.
 */
public class TooManyResultsException extends SCIMException {

    private static final long serialVersionUID = 1406667880354514801L;

    public TooManyResultsException(String msg) {
        super(400, "tooMany", msg);
    }
}
