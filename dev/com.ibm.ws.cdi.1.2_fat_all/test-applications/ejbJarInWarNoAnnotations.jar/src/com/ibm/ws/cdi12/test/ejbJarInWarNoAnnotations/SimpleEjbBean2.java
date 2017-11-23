/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.test.ejbJarInWarNoAnnotations;

import javax.enterprise.context.ApplicationScoped;

/**
 *
 */
@ApplicationScoped
public class SimpleEjbBean2 {

    private String message2;

    public void setMessage2(String message) {
        this.message2 = message;
    }

    public String getMessage2() {
        return this.message2;
    }

}
