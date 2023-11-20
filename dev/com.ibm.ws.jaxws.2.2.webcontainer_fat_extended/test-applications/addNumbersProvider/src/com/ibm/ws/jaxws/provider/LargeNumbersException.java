/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package com.ibm.ws.jaxws.provider;

import javax.xml.ws.WebFault;

/*
 * The LargeNumbersException tests whether or not @WebFault annotated Exception
 * will properly print the message element without passing it to its superclass.
 */
@WebFault()
public class LargeNumbersException extends Exception {

    private static final long serialVersionUID = -2844570262814091172L;

    private static String message;

    public LargeNumbersException() {

    }

    public LargeNumbersException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return this.message;
    }
}
