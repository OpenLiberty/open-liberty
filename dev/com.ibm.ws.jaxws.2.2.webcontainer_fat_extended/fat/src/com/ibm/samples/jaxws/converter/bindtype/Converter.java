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

@WebService(name = "ConverterPortType",
            targetNamespace = "http://bindtype.converter.jaxws.samples.ibm.com")
public interface Converter {
    public BigDecimal dollarToRupees(BigDecimal dollars);

    public BigDecimal rupeesToEuro(BigDecimal rupees);
}
