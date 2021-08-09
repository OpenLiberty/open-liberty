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

package com.ibm.ejb2x.base.spec.sfl.ejb;

/**
 */
public class SFLApplException extends Exception {
    private static final long serialVersionUID = -8006581336763786387L;
    public boolean passed;
    public Throwable t;

    /**
    */
    public SFLApplException() {
        super();
    }

    /**
    */
    public SFLApplException(boolean passed, Throwable t, String message) {
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
