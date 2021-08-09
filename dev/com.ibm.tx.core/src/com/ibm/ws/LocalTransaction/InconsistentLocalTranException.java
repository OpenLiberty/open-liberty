package com.ibm.ws.LocalTransaction;
/*******************************************************************************
 * Copyright (c) 2002, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/**
 * 
 * <p> This class is private to WAS.
 * Any use of this class outside the WAS Express/ND codebase 
 * is not supported.
 *
 */

/**
 * Thrown when a LocalTransactionCoordinator detects a failure completing
 * one of its resources. The exception indicates that the outcomes of the
 * RMLTs in the LTC scope are mixed. The exception contains a vector
 * of names of the failed resources.
 *
 */
public final class InconsistentLocalTranException extends Exception
{

    private static final long serialVersionUID = -8482185434516436562L;

    private String[] ivResources;


    /**
     * Constructor.
     * 
     * @param message  The exception description
     * @param resource The failing resources
     */
    public InconsistentLocalTranException(String message, String[] resources)
    {

        super(message);

        ivResources = resources;
    }

    /**
     * Returns the identifier of the failing resource which caused 
     * the exception.
     * 
     * @return The failing resources identifier
     */
    public String[] getFailingResources()
    {

        return ivResources;
    }
}
