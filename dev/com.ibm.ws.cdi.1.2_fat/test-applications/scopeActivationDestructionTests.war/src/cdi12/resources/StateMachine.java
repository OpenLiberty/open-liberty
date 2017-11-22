/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package cdi12.resources;

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