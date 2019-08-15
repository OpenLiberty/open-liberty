/*******************************************************************************
 * Copyright (c) 2002, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ejb2x.base.spec.sll.ejb;

/**
 */
public class SLLApplException extends Exception {
    private static final long serialVersionUID = 2741659039631682488L;
    public boolean passed;
    public Throwable t;

    /**
    */
    public SLLApplException() {
        super();
    }

    /**
    */
    public SLLApplException(boolean passed, Throwable t, String message) {
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
