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
package com.ibm.websphere.ras.dummyinternal;

import com.ibm.websphere.ras.dummyspec.ExceptionMaker;

/**
 *
 */
public class DummyInternalClass implements ExceptionMaker {

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
        return new Exception();
    }

}
