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
package com.ibm.samples.jaxws.spring.wsdlfirst.stub;

import javax.xml.ws.WebFault;

@WebFault(name = "NoSuchCustomer", targetNamespace = "http://customerservice.example.com/")
public class NoSuchCustomerException extends Exception {

    private com.ibm.samples.jaxws.spring.wsdlfirst.stub.NoSuchCustomer noSuchCustomer;

    public NoSuchCustomerException() {
        super();
    }

    public NoSuchCustomerException(String message) {
        super(message);
    }

    public NoSuchCustomerException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoSuchCustomerException(String message, com.ibm.samples.jaxws.spring.wsdlfirst.stub.NoSuchCustomer noSuchCustomer) {
        super(message);
        this.noSuchCustomer = noSuchCustomer;
    }

    public NoSuchCustomerException(String message, com.ibm.samples.jaxws.spring.wsdlfirst.stub.NoSuchCustomer noSuchCustomer, Throwable cause) {
        super(message, cause);
        this.noSuchCustomer = noSuchCustomer;
    }

    public com.ibm.samples.jaxws.spring.wsdlfirst.stub.NoSuchCustomer getFaultInfo() {
        return this.noSuchCustomer;
    }
}
