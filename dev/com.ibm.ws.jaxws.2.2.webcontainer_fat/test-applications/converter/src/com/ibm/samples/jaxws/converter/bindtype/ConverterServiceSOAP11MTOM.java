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
package com.ibm.samples.jaxws.converter.bindtype;

import java.math.BigDecimal;

import javax.jws.WebService;
import javax.xml.ws.BindingType;
import javax.xml.ws.soap.SOAPBinding;

@WebService(serviceName = "ConverterSvcName-bindtype-soap11mtom",
            portName = "ConverterPort",
            endpointInterface = "com.ibm.samples.jaxws.converter.bindtype.Converter",
            targetNamespace = "http://jaxws.samples.ibm.com")
@BindingType(SOAPBinding.SOAP11HTTP_MTOM_BINDING)
public class ConverterServiceSOAP11MTOM implements Converter {

    private final BigDecimal rupeeRate = new BigDecimal("40.58");
    private final BigDecimal euroRate = new BigDecimal("0.018368");

    @Override
    public BigDecimal dollarToRupees(BigDecimal dollars) {
        BigDecimal result = dollars.multiply(rupeeRate);
        return result.setScale(2, BigDecimal.ROUND_UP);
    }

    @Override
    public BigDecimal rupeesToEuro(BigDecimal rupees) {
        BigDecimal result = rupees.multiply(euroRate);
        return result.setScale(2, BigDecimal.ROUND_UP);
    }

}
