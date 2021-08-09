/*******************************************************************************
 * Copyright (c) 1997, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.webcontainer.collaborator;

/**
 * @author asisin
 *
 */
public class TxCollaboratorConfig 
{
    Object suspendTx;
    Object dispatchContext;
    private boolean beginner;
    Object incumbentTx;

    /**
     * @return
     */
    public Object getDispatchContext()
    {
        return dispatchContext;
    }

    /**
     * @return
     */
    public Object getSuspendTx()
    {
        return suspendTx;
    }

    /**
     * @param object
     */
    public void setDispatchContext(Object object)
    {
        dispatchContext = object;
    }

    /**
     * @param object
     */
    public void setSuspendTx(Object object)
    {
        suspendTx = object;
    }

    /**
     * @param object
     */
    public void setBeginner(boolean beginner)
    {
        this.beginner = beginner;
    }

    /**
     * @return
     */
    public boolean getBeginner()
    {
        return beginner;
    }

    /**
     * @return the incumbentTX
     */
    public Object getIncumbentTx() {
        return incumbentTx;
    }

    /**
     * @param incumbentTX the incumbentTX to set
     */
    public void setIncumbentTx(Object incumbentTx) {
        this.incumbentTx = incumbentTx;
    }
}