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
package com.ibm.ws.jaxrs.fat.checkFeature.api.impl;

import javax.ejb.Local;
import javax.ejb.Stateless;

import com.ibm.ws.jaxrs.fat.checkFeature.api.StatelessService;

@Stateless
@Local(StatelessService.class)
public class StatelessServiceImpl implements StatelessService {

    @Override
    public String getMessage() {
        return "message from EJB";
    }

}
