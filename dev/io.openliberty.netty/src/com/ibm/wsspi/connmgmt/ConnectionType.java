/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.connmgmt;

import java.util.Map;

import com.ibm.ws.channelfw.internal.InboundVirtualConnection;
import com.ibm.wsspi.channelfw.OutboundVirtualConnection;
import com.ibm.wsspi.channelfw.VirtualConnection;

/**
 * Identifies the "type" of connection required by a ZIOPConnectionKey.
 * 
 * Contructor and setters are private, use one of final static variables for
 * well-defined types.
 */
public final class ConnectionType {
    protected static final String CONNECTION_TYPE_VC_KEY = "CFW_CONNECTION_TYPE";

    /**
     * Get the connection type from the virtual connection.
     * 
     * @param vc
     * @return ConnectionType
     */
    public static ConnectionType getVCConnectionType(VirtualConnection vc) {
        if (vc == null) {
            return null;
        }
        return (ConnectionType) vc.getStateMap().get(CONNECTION_TYPE_VC_KEY);
    }

    /**
     * Set the connection type on the virtual connection. This will overlay
     * any preset value.
     * 
     * @param vc
     *            VirtualConnection containing simple state for this connection
     * @param connType
     *            ConnectionType for the VirtualConnection
     */
    public static void setVCConnectionType(VirtualConnection vc, ConnectionType connType) {
        if (vc == null || connType == null) {
            return;
        }

        Map<Object, Object> map = vc.getStateMap();

        // Internal connections are both inbound and outbound (they're connections
        // to ourselves)
        // so while we prevent setting Outbound ConnTypes for inbound connections
        // and vice versa,
        // we don't prevent internal types from being set as either.
        if (vc instanceof InboundVirtualConnection && ConnectionType.isOutbound(connType.type)) {
            throw new IllegalStateException("Cannot set outbound ConnectionType on inbound VirtualConnection");
        } else if (vc instanceof OutboundVirtualConnection && ConnectionType.isInbound(connType.type)) {
            throw new IllegalStateException("Cannot set inbound ConnectionType on outbound VirtualConnection");
        }

        map.put(CONNECTION_TYPE_VC_KEY, connType);
    }

    /**
     * Assign the default ConnectionType (INBOUND or OUTBOUND) to
     * the VirtualConnection.
     * 
     * @param vc
     *            VirtualConnection to add ConnectionType to
     */
    public static void setDefaultVCConnectionType(VirtualConnection vc) {
        if (vc == null) {
            return;
        }

        Map<Object, Object> map = vc.getStateMap();

        // Pick defaults for INBOUND/OUTBOUND
        // setVCConnectionType will only set these values in the VC if
        // a type has not been preset.
        if (map.get(CONNECTION_TYPE_VC_KEY) == null) {
            if (vc instanceof InboundVirtualConnection) {
                map.put(CONNECTION_TYPE_VC_KEY, ConnectionType.INBOUND);
            } else {
                map.put(CONNECTION_TYPE_VC_KEY, ConnectionType.OUTBOUND);
            }
        }
    }

    // ---------------- INTERNAL STATIC VARIABLE DECLARATIONS
    // -------------------------------

    /** outbound connection types */
    private static final byte TYPE_OUTBOUND_MIN = 0, TYPE_OUTBOUND = 1,
                    // 2 reserved, present in previous releases
                    TYPE_OUTBOUND_CR_TO_REMOTE = 3, TYPE_OUTBOUND_SR_TO_CR_REMOTE = 4, TYPE_OUTBOUND_MAX = 10;

    /** internal connection types */
    private static final byte TYPE_INTERNAL_CR_SR = -1;

    /** inbound connection types */
    private static final byte TYPE_INBOUND_MIN = 20, TYPE_INBOUND = 21,
                    // 22 reserved, present in previous releases
                    TYPE_INBOUND_CR = 23, TYPE_INBOUND_MAX = 30;

    // ---------------- WELL-KNOWN VALID CONNECTION TYPES
    // -----------------------------------

    /** outbound connection types */
    public static final ConnectionType OUTBOUND = new ConnectionType(TYPE_OUTBOUND, "OUT:REMOTE"),
                    OUTBOUND_CR_TO_REMOTE = new ConnectionType(TYPE_OUTBOUND_CR_TO_REMOTE, "OUT:CR"),
                    OUTBOUND_SR_TO_CR_REMOTE = new ConnectionType(TYPE_OUTBOUND_SR_TO_CR_REMOTE, "OUT:SR-CR");

    /** internal connection types */
    public static final ConnectionType INTERNAL_CR_SR = new ConnectionType(TYPE_INTERNAL_CR_SR, "INTERNAL:CR-SR");

    /** inbound connection types */
    public static final ConnectionType INBOUND = new ConnectionType(TYPE_INBOUND, "IN:REMOTE"), INBOUND_CR = new ConnectionType(TYPE_INBOUND_CR, "IN:CR");

    /**
     * Set the connection type on the virtual connection.
     * 
     * @param type
     *            ConnectionType for the VirtualConnection
     * @return ConnectionType
     */
    public static ConnectionType getConnectionType(byte type) {
        switch (type) {
            case TYPE_OUTBOUND:
                return OUTBOUND;
            case TYPE_OUTBOUND_CR_TO_REMOTE:
                return OUTBOUND_CR_TO_REMOTE;
            case TYPE_OUTBOUND_SR_TO_CR_REMOTE:
                return OUTBOUND_SR_TO_CR_REMOTE;

            case TYPE_INBOUND:
                return INBOUND;
            case TYPE_INBOUND_CR:
                return INBOUND_CR;

            case TYPE_INTERNAL_CR_SR:
                return INTERNAL_CR_SR;
        }

        return null;
    }

    // ---------------- TESTS AGAINST ARBITRARY EXPORTED CONNECTION TYPE
    // -------------------

    /**
     * Return true for outbound connection type.
     * 
     * @param type
     * @return boolean
     */
    static final boolean isOutbound(byte type) {
        return (type > TYPE_OUTBOUND_MIN && type < TYPE_OUTBOUND_MAX);
    }

    /**
     * Return true for inbound connection type.
     * 
     * @param type
     * @return boolean
     */
    static final boolean isInbound(byte type) {
        return (type > TYPE_INBOUND_MIN && type < TYPE_INBOUND_MAX);
    }

    /**
     * Return true for server internal connections.
     * 
     * @param type
     * @return boolean
     */
    static final boolean isInternal(byte type) {
        return (type < 0);
    }

    // ------------ INSTANCE METHODS -------------------------------

    /** private type attribute. See static instances of ZIOPConnectionKeyType. */
    private final byte type;

    /** private stringified type */
    private final String typeString;

    /** private constructor. All instances of this class are static/final. */
    private ConnectionType(byte type, String typeString) {
        this.type = type;
        this.typeString = typeString;
    }

    /**
     * Flatten ConnectionType (return the byte).
     * 
     * @return byte
     */
    public byte export() {
        return this.type;
    }

    /**
     * Overriding implementation of parent's method.
     */
    public boolean equals(Object that) {
        if (that == null || that.getClass() != this.getClass()) {
            return false;
        }
        return ((ConnectionType) that).type == this.type;
    }

    /**
     * @see Object#hashCode()
     */
    public int hashCode() {
        return this.typeString.hashCode();
    }

    /**
     * Overriding implementation of parent's method.
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return this.typeString;
    }
}
