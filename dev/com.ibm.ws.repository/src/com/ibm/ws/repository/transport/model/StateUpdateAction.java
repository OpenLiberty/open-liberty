/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.repository.transport.model;

import com.ibm.ws.repository.common.enums.StateAction;

/**
 * This is a wrapper object to store an action to take on the state of an asset.
 */
public class StateUpdateAction extends AbstractJSON {

    private StateAction action;

    /**
     * Default constructor that does not set the action property
     */
    public StateUpdateAction() {

    }

    /**
     * Construct an instance of this class with the action set
     *
     * @param action
     */
    public StateUpdateAction(StateAction action) {
        this.action = action;
    }

    public StateAction getAction() {
        return action;
    }

    public void setAction(StateAction action) {
        this.action = action;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((action == null) ? 0 : action.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        StateUpdateAction other = (StateUpdateAction) obj;
        if (action == null) {
            if (other.action != null)
                return false;
        } else if (!action.equals(other.action))
            return false;
        return true;
    }

}
