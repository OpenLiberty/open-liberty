/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.ras.dummyspec;

/**
 *
 */
public class AnotherDummySpecClass implements ExceptionMaker {

    private final ExceptionMaker maker;

    /**
     * @param dummyInternalClass
     */
    public AnotherDummySpecClass(ExceptionMaker maker) {
        this.maker = maker;
    }

    public AnotherDummySpecClass() {
        this.maker = null;
    }

    /**
     * @param truncatableThrowableTest
     * @return
     */
    public Exception callback(ExceptionMaker maker) {
        return maker.constructException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.ras.TruncatableThrowable.ExceptionMaker#constructException()
     */
    @Override
    public Exception constructException() {
        if (maker == null) {
            return new Exception();
        } else {
            return maker.constructException();
        }

    }

}
