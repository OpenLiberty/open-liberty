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

@WebService(name = "Converter", targetNamespace = "http://monitor2.jaxws.ws.ibm.com/")
public interface ConverterService {

    BigDecimal dollarToRupees(BigDecimal dollar);

    BigDecimal rupeesToEuro(BigDecimal rupee);
}
