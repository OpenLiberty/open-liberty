/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package fat.cxf.basic.jaxws.annotations.bindingtype.checkdefaults.server12;

import javax.xml.ws.WebFault;

@WebFault(name = "AddNumbersException", targetNamespace = "http://server12.checkdefaults.bindingtype.annotations.jaxws.basic.cxf.fat/")
public class NegativeNumbersException extends Exception {
    String info;

    public NegativeNumbersException(String message, String detail) {
        super(message);
        this.info = detail;
    }

    public String getFaultInfo() {
        return info;
    }
}
