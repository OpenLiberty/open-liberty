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
public class GlobalState {

    public static String cheat = "";

    private static State RequestScopedState = State.NOTSTARTED;
    private static State ApplicationScopedState = State.NOTSTARTED;
    private static State SessionScopedState = State.NOTSTARTED;
    private static State ConversationScopedState = State.NOTSTARTED;

    private static int RequestScopedStarts = 0;
    private static int ApplicationScopedStarts = 0;
    private static int SessionScopedStarts = 0;
    private static int ConversationScopedStarts = 0;

    private static int RequestScopedStops = 0;
    private static int ApplicationScopedStops = 0;
    private static int SessionScopedStops = 0;
    private static int ConversationScopedStops = 0;

    public static String buildString() {
        StringBuilder sb = new StringBuilder();

        sb.append("Applicaiton Scoped Bean: " + ApplicationScopedState.toString() + System.lineSeparator());
        sb.append("Request Scoped Bean: " + RequestScopedState.toString() + System.lineSeparator());
        sb.append("Session Scoped Bean: " + SessionScopedState.toString() + System.lineSeparator());
        sb.append("Conversation Scoped Bean: " + ConversationScopedState.toString() + System.lineSeparator());

        sb.append("Applicaiton Scoped Bean Starts: " + ApplicationScopedStarts + System.lineSeparator());
        sb.append("Request Scoped Bean Starts: " + RequestScopedStarts + System.lineSeparator());
        sb.append("Session Scoped Bean Starts: " + SessionScopedStarts + System.lineSeparator());
        sb.append("Conversation Scoped Bean Starts: " + ConversationScopedStarts + System.lineSeparator());

        sb.append("Applicaiton Scoped Bean Stops: " + ApplicationScopedStops + System.lineSeparator());
        sb.append("Request Scoped Bean Stops: " + RequestScopedStops + System.lineSeparator());
        sb.append("Session Scoped Bean Stops: " + SessionScopedStops + System.lineSeparator());
        sb.append("Conversation Scoped Bean Stops: " + ConversationScopedStops + System.lineSeparator());

        sb.append(cheat);

        return sb.toString();
    }

    public static void RequestStop() {
        RequestScopedStops++;
    }

/*
 * public static void advanceApplicationState(Move move) {
 * ApplicationScopedState = StateMachine.Move(move, ApplicationScopedState);
 * }
 *
 * public static void advanceRequestState(Move move) {
 * RequestScopedState = StateMachine.Move(move, RequestScopedState);
 * }
 *
 * public static void advanceSessionState(Move move) {
 * SessionScopedState = StateMachine.Move(move, SessionScopedState);
 * }
 *
 * public static void advanceConversationState(Move move) {
 * ConversationScopedState = StateMachine.Move(move, ConversationScopedState);
 * }
 */

    public static void recordApplicationStop() {
        ApplicationScopedStops++;
        ApplicationScopedState = StateMachine.Move(Move.STOP, ApplicationScopedState);
    }

    public static void recordSessionStop() {
        SessionScopedState = StateMachine.Move(Move.STOP, SessionScopedState);
        SessionScopedStops++;
    }

    public static void recordConversatoinStop() {
        ConversationScopedState = StateMachine.Move(Move.STOP, ConversationScopedState);
        ConversationScopedStops++;
    }

    public static void recordRequestStop() {
        RequestScopedState = StateMachine.Move(Move.STOP, RequestScopedState);
        RequestScopedStops++;
    }

    public static void recordRequestStart() {
        RequestScopedState = StateMachine.Move(Move.START, RequestScopedState);
        RequestScopedStarts++;
    }

    public static void recordApplicationStart() {
        ApplicationScopedState = StateMachine.Move(Move.START, ApplicationScopedState);
        ApplicationScopedStarts++;
    }

    public static void recordSessionStart() {
        SessionScopedState = StateMachine.Move(Move.START, SessionScopedState);
        SessionScopedStarts++;
    }

    public static void recordConversatoinStart() {
        ConversationScopedState = StateMachine.Move(Move.START, ConversationScopedState);
        ConversationScopedStarts++;
    }

}
