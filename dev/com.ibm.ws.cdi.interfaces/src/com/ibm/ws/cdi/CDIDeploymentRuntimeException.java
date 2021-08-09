/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi;

import javax.enterprise.inject.spi.DeploymentException;

public class CDIDeploymentRuntimeException extends DeploymentException {

    private static final long serialVersionUID = 5729749912023008025L;

    public CDIDeploymentRuntimeException(String message) {
        super(message);
    }

    public CDIDeploymentRuntimeException(Throwable t) {
        super(t);
    }

    public CDIDeploymentRuntimeException(String message, Throwable t) {
        super(message, t);
    }

}
