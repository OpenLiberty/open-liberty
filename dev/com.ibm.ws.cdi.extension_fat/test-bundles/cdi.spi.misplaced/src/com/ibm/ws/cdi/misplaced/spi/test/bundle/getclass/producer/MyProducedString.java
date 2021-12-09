/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.misplaced.spi.test.bundle.getclass.producer;

public class MyProducedString {

    String s;

    public MyProducedString(String s) {
        this.s = s;
    }

    @Override
    public String toString() {
        return s;
    }
}
