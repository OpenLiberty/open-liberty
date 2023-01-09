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
package com.ibm.ws.jaxws.provider;

import javax.xml.ws.WebFault;

@WebFault()
public class AddNumbersException extends Exception {
    /**  */
    private static final long serialVersionUID = -2844570262814091172L;
    private String message = null;

    public AddNumbersException() {

    }

    public AddNumbersException(String message) {
        this.message = message;
    }

    public String getInfo() {
        return message;
    }
}
