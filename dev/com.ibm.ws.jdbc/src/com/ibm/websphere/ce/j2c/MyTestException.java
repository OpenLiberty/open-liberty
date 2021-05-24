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
package com.ibm.websphere.ce.j2c;

import javax.resource.spi.ResourceAllocationException;

public class MyTestException extends ResourceAllocationException {
    private static final long serialVersionUID = 1;

    public MyTestException(String reason) {
        super(reason);
    }

    public MyTestException(String reason, String errorCode) {
        super(reason, errorCode);
    }
}
