/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.microprofile.faulttolerance_fat.multimodule.tests.configload;

public class CountingException extends Exception {

    private static final long serialVersionUID = 1L;
    private final int count;

    public CountingException(int count) {
        this.count = count;
    }

    public int getCount() {
        return count;
    }

}
