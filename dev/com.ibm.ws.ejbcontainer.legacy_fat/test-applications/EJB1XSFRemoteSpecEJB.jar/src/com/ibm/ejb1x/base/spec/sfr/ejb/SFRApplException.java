/*******************************************************************************
 * Copyright (c) 2002, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ejb1x.base.spec.sfr.ejb;

/**
 */
public class SFRApplException extends Exception {
    private static final long serialVersionUID = -5619974751183829193L;
    public boolean passed;
    public Throwable t;

    /**
    */
    public SFRApplException() {
        super();
    }

    /**
    */
    public SFRApplException(boolean passed, Throwable t, String message) {
        super(message);
        this.passed = passed;
        this.t = t;
    }

    /**
    */
    @Override
    public String toString() {
        return t.getClass().getName() + ":" + passed;
    }
}
