/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
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