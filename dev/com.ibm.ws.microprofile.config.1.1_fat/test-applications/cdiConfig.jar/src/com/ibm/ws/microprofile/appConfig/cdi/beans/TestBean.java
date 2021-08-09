/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.appConfig.cdi.beans;

import java.io.Serializable;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class TestBean implements Serializable {

    /**  */
    private static final long serialVersionUID = 333492054092155085L;

    @Inject
    @ConfigProperty(name = "SYS_PROP_ONE")
    private int SYS_PROP_ONE;

    public int getSysPropOne() {
        return SYS_PROP_ONE;
    }

}
