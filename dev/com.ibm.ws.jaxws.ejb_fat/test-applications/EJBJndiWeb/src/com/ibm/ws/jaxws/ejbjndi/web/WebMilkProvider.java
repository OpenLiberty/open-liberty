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
package com.ibm.ws.jaxws.ejbjndi.web;

import javax.jws.WebService;

import com.ibm.ws.jaxws.ejbjndi.common.MilkProvider;

@WebService
public class WebMilkProvider implements MilkProvider {

    @Override
    public String take(String amount) {
        return WebMilkProvider.class.getName() + " [" + amount + "]";
    }

}
