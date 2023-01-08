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

package com.ibm.ejb1x.base.spec.slr.ejb;

/**
 */
public class SLRApplException extends Exception {
    private static final long serialVersionUID = 3243376335810489693L;
    public boolean passed;
    public Throwable t;

    /**
    */
    public SLRApplException() {
        super();
    }

    /**
    */
    public SLRApplException(boolean passed, Throwable t, String message) {
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
