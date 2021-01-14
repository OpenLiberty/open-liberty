/*******************************************************************************
 * Copyright (c) 2015, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.beansxml.implicit.apps.ejb;

import javax.enterprise.context.Dependent;

/**
 *
 */
@Dependent
public class DependentEjbBean {

    private String message;

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return this.message;
    }

}
