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
package fat.cxf.basic.jaxws.annotations.bindingtype.checkdefaults.client;

import java.net.URL;

import javax.xml.ws.WebServiceException;

import fat.cxf.basic.jaxws.annotations.bindingtype.checkdefaults.server12.AddNumbersImpl;
import fat.cxf.basic.jaxws.annotations.bindingtype.checkdefaults.server12.AddNumbersImplService;

/**
 * Copied from tWAS based com.ibm.ws.jaxws_fat annotations/bindingtype/checkdefaults/** bucket
 *
 * A Simple client that calls the AddNumbersImpl which is a SOAP 1.1\2 Service for adding two passed numbers together
 */
public class AddNumbersClient12 {

    public AddNumbersClient12() {
        //Empty body
    }

    /*
     * public static void main(String[] args) {
     * int result = 0;
     * int number1 = -20,
     * number2 = -20;
     *
     * result = addNumbers(number1, number2);
     * System.out.println("The result of " + number1 + " + " + number2 +
     * "is " + result);
     * }
     */

    public static int addNumbers(int number1, int number2, URL wsdlLocation) {
        try {
            AddNumbersImpl port = new AddNumbersImplService(wsdlLocation).getAddNumbersImplPort();
            return port.addTwoNumbers(number1, number2);
        } catch (WebServiceException ex) {
            ex.printStackTrace();
            return -1;
        }
    }

}
