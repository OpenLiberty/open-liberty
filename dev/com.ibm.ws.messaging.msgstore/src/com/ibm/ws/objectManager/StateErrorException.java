package com.ibm.ws.objectManager;

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

/**
 * Thrown when an operation is attempted on an Object in an invalid state
 * for that operation. The source of the exception is now in an error state,
 * this represents an internal error in the ObjectManager.
 */
public final class StateErrorException
                extends ObjectManagerException
{
    private static final long serialVersionUID = -5001583364893883148L;

    /**
     * StateErrorException.
     * 
     * @param Object which is throwing this StateErrorException.
     * @param int the previous state of the source Object.
     * @param String the descriptive name of the previous state.
     */
    protected StateErrorException(Object source, int state, String stateName)
    {
        super(source,
              StateErrorException.class
              , new Object[] { source, new Integer(state), stateName });
    } // StateErrorException(). 
} // class StateErrorException.
