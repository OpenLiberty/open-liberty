/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsat.service;

import javax.xml.namespace.QName;

/**
 * Holder class for fault information
 */
public class WSATFault {

    // TODO: probably need a better place for these constants
    private static final String WSCOOR_NS = "http://docs.oasis-open.org/ws-tx/wscoor/2006/06";
    private static final String WSAT_NS = "http://docs.oasis-open.org/ws-tx/wsat/2006/06";

    private static final String CODE_SENDER = "Sender";

    private static final QName SUBCODE_INVALID_STATE = new QName(WSCOOR_NS, "InvalidState");
    private static final QName SUBCODE_INVALID_PROTOCOL = new QName(WSCOOR_NS, "InvalidProtocol");
    private static final QName SUBCODE_INVALID_PARAMETERS = new QName(WSCOOR_NS, "InvalidParameters");
    private static final QName SUBCODE_CANNOT_CREATE_CONTEXT = new QName(WSCOOR_NS, "CannotCreateContext");
    private static final QName SUBCODE_CANNOT_REGISTER_PARTICIPANT = new QName(WSCOOR_NS, "CannotRegisterParticipant");
    private static final QName SUBCODE_INCONSISTENT_INTERNAL_STATE = new QName(WSAT_NS, "InconsistentInternalState");
    private static final QName SUBCODE_UNKNOWN_TRANSACTION = new QName(WSAT_NS, "UnknownTransaction");

    // These messages are defined in the spec, so do not get translated?
    private static final String REASON_INVALID_STATE = "The message was invalid for the current state of the activity";
    private static final String REASON_INVALID_PROTOCOL = "The protocol is invalid or not supported by the coordinator";
    private static final String REASON_INVALID_PARAMETERS = "The message contained invalid parameters and could not be processed";
    private static final String REASON_CANNOT_CREATE_CONTEXT = "CoordinationContext could not be created";
    private static final String REASON_CANNOT_REGISTER_PARTICIPANT = "Participant could not be registered";
    private static final String REASON_INCONSISTENT_INTERNAL_STATE = "A global consistency failure has occurred. This is an unrecoverable condition";
    private static final String REASON_UNKNOWN_TRANSACTION = "The coordinator has no knowledge of the transaction. This is an unrecoverable condition";

    private final String code;
    private final QName subcode;
    private final String reason;
    private final String detail; // TODO: improve 'detail' handling

    /*
     * Helpers to construct known specific instances. Should always be one
     * of these if we are sending a fault.
     */
    public static WSATFault getInvalidState(String detail) {
        return new WSATFault(CODE_SENDER, SUBCODE_INVALID_STATE, REASON_INVALID_STATE, detail);
    }

    public static WSATFault getInvalidProtocol(String detail) {
        return new WSATFault(CODE_SENDER, SUBCODE_INVALID_PROTOCOL, REASON_INVALID_PROTOCOL, detail);
    }

    public static WSATFault getInvalidParameters(String detail) {
        return new WSATFault(CODE_SENDER, SUBCODE_INVALID_PARAMETERS, REASON_INVALID_PARAMETERS, detail);
    }

    public static WSATFault getCannotCreateContext(String detail) {
        return new WSATFault(CODE_SENDER, SUBCODE_CANNOT_CREATE_CONTEXT, REASON_CANNOT_CREATE_CONTEXT, detail);
    }

    public static WSATFault getCannotRegisterParticipant(String detail) {
        return new WSATFault(CODE_SENDER, SUBCODE_CANNOT_REGISTER_PARTICIPANT, REASON_CANNOT_REGISTER_PARTICIPANT, detail);
    }

    public static WSATFault getInconsistentInternalState(String detail) {
        return new WSATFault(CODE_SENDER, SUBCODE_INCONSISTENT_INTERNAL_STATE, REASON_INCONSISTENT_INTERNAL_STATE, detail);
    }

    public static WSATFault getUnknownTransaction(String detail) {
        return new WSATFault(CODE_SENDER, SUBCODE_UNKNOWN_TRANSACTION, REASON_UNKNOWN_TRANSACTION, detail);
    }

    /*
     * Constructor to create general instance when receiving a fault.
     */
    public WSATFault(String code, QName subcode, String reason, String detail) {
        this.code = code;
        this.subcode = subcode;
        this.reason = reason;
        this.detail = detail;
    }

    public String getCode() {
        return code;
    }

    public QName getSubcode() {
        return subcode;
    }

    public String getReason() {
        return reason;
    }

    public String getDetail() {
        return detail;
    }
}
