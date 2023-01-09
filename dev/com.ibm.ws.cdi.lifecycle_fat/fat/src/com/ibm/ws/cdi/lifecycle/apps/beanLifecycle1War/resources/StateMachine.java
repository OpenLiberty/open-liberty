/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.lifecycle.apps.beanLifecycle1War.resources;

/**
 *
 */
public class StateMachine {

    public static State Move(Move move, State currentState) {

        if (move == Move.START) {
            switch (currentState) {
                case NOTSTARTED:
                    return State.STARTEDUP;
                case STARTEDUP:
                    return State.STARTEDUP; //The test framework and the second application each start their own session. Thus I don't set this to StartedUpTwice
                case STARTEDTHENSTOPPED:
                    return State.STARTEDTHENSTOPPED;
                case STOPPEDBEFORESTARTED:
                    return State.STOPPEDBEFORESTARTED;
                case STARTEDTWICE:
                    return State.STARTEDTWICE;
            }
            return null;
        } else {

            switch (currentState) {
                case NOTSTARTED:
                    return State.STOPPEDBEFORESTARTED;
                case STARTEDUP:
                    return State.STARTEDTHENSTOPPED;
                case STARTEDTHENSTOPPED:
                    return State.STARTEDTHENSTOPPED;
                case STOPPEDBEFORESTARTED:
                    return State.STOPPEDBEFORESTARTED;
                case STARTEDTWICE:
                    return State.STARTEDTWICE;
            }

            return null;
        }

    }
}