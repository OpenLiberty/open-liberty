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
 * Thrown when the object manager detects that an Object is in an invalid state for the
 * operation being attempted. The operation is abandoned with no change to the Object.
 */
public final class InvalidStateException
                extends ObjectManagerException
{
    private static final long serialVersionUID = -3269797940476314238L;

    /**
     * InvalidStateException.
     * 
     * @param Object which is throwing this InvalidStateException.
     * @param int the state of the source Object.
     * @param String the descriptive name of the state.
     */
    protected InvalidStateException(Object source, int state, String stateName)
    {
        super(source,
              InvalidStateException.class
              , new Object[] { source, new Integer(state), stateName });
    } // InvalidStateException(). 

} // End of class InvalidStateException.
