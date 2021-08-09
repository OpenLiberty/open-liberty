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
package com.ibm.ws.jaxws.monitor;

import java.math.BigDecimal;

import javax.jws.WebService;

@WebService(serviceName = "ConverterService")
public class ConverterServiceImpl implements ConverterService {

    private final BigDecimal rupeeRate = new BigDecimal("40.58");
    private final BigDecimal euroRate = new BigDecimal("0.018368");

    @Override
    public BigDecimal dollarToRupees(BigDecimal dollar) {
        BigDecimal result = dollar.multiply(this.rupeeRate);
        return result.setScale(2, 0);
    }

    @Override
    public BigDecimal rupeesToEuro(BigDecimal rupee) {
        BigDecimal result = rupee.multiply(this.euroRate);
        return result.setScale(2, 0);
    }

}
