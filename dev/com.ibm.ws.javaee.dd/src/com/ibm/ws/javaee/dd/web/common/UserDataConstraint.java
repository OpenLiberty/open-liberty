/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.dd.web.common;

import com.ibm.ws.javaee.dd.common.Describable;

/**
 *
 */
public interface UserDataConstraint
                extends Describable {

    /**
     * Represents "NONE" for {@link #getTransportGuarantee}.
     */
    static final int TRANSPORT_GUARANTEE_NONE = 0;

    /**
     * Represents "INTEGRAL" for {@link #getTransportGuarantee}.
     */
    static final int TRANSPORT_GUARANTEE_INTEGRAL = 1;

    /**
     * Represents "CONFIDENTIAL" for {@link #getTransportGuarantee}.
     */
    static final int TRANSPORT_GUARANTEE_CONFIDENTIAL = 2;

    int getTransportGuarantee();

}
