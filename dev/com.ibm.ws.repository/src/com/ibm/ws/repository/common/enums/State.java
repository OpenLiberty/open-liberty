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

import java.util.Arrays;
import java.util.List;

public enum State {
    DRAFT("draft", new StateAction[] {
                                       null, // Draft
                                       StateAction.PUBLISH, // Awaiting approval
                                       StateAction.PUBLISH, // Need more info
                                       StateAction.PUBLISH // Publish
    }),
    AWAITING_APPROVAL("awaiting_approval", new StateAction[] {
                                                               StateAction.CANCEL, // Draft
                                                               null, // Awaiting approval 
                                                               StateAction.NEED_MORE_INFO, // Need more info 
                                                               StateAction.APPROVE // Publish
    }),
    NEED_MORE_INFO("need_more_info", new StateAction[] {
                                                         StateAction.PUBLISH, // Draft
                                                         StateAction.PUBLISH, // Awaiting approval 
                                                         null, // Need more info 
                                                         StateAction.PUBLISH // Publish
    }),
    PUBLISHED("published", new StateAction[] {
                                               StateAction.UNPUBLISH, // Draft
                                               StateAction.UNPUBLISH, // Awaiting approval 
                                               StateAction.UNPUBLISH, // Need more info 
                                               null // Publish
    });

    private final String state;
    private StateAction[] actions;

    private State(String state, StateAction[] actions) {
        this.state = state;
        this.actions = actions;
    }

    public boolean isStateActionAllowed(StateAction action) {
        List<StateAction> allowed = Arrays.asList(actions);
        return action == null ? false : allowed.contains(action);
    }

    public StateAction getNextAction(State target) {
        return actions[target.ordinal()];
    }

    public String getValue() {
        return state;
    }

    public static State forValue(String value) {
        for (State state : State.values()) {
            if (state.getValue().equals(value)) {
                return state;
            }
        }
        return null;
    }

}