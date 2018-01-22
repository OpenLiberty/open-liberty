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

package com.ibm.ws.repository.common.enums;

public enum StateAction {
    /**
     * Move the state from {@link State#draft} to {@link State#awaiting_approval}
     */
    PUBLISH("publish"),
    /**
     * Move the state from {@link State#awaiting_approval} to {@link State#published}
     */
    APPROVE("approve"),
    /**
     * Move the state from {@link State#awaiting_approval} to {@link State#draft}
     */
    CANCEL("cancel"),
    /**
     * Move the state from {@link State#awaiting_approval} to {@link State#need_more_info}
     */
    NEED_MORE_INFO("need_more_info"),
    /**
     * Move the state from {@link State#published} to {@link State#draft}
     */
    UNPUBLISH("unpublish");

    private final String action;

    private StateAction(String action) {
        this.action = action;
    }

    public String getValue() {
        return action;
    }

    public static StateAction forValue(String value) {
        for (StateAction action : StateAction.values()) {
            if (action.getValue().equals(value)) {
                return action;
            }
        }
        return null;
    }

}