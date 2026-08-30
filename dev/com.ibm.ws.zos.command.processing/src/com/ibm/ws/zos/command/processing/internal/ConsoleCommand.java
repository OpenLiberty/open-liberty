/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.command.processing.internal;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

/**
 *
 */
public class ConsoleCommand {

    //private static final TraceComponent tc = Tr.register(ConsoleCommand.class);

    private int _commandType = 0;
    private int _errorCode = 0;
    private int _consoleID = 0;
    private String _consoleName = null;
    private long _cart = 0;
    private int _restOfCommandLength = 0;
    private String _restOfCommand = null;

    /*
     * Offsets into byte[] returned from native code mapped by CommandInfoArea
     * in server_command_functions.h
     */
    protected static final int I_cia_commandType = 0x00;
    protected static final int I_cia_errorCode = 0x04;
    protected static final int I_cia_consoleID = 0x08;
    protected static final int I_cia_consoleName = 0x0C;
    protected static final int I_cia_commandCART = 0x14;
    protected static final int I_cia_commandRestOfCommandLength = 0x1C;
    protected static final int I_cia_commandRestOfCommand = 0x20;

    public ConsoleCommand(final byte[] nativeCIA) throws UnsupportedEncodingException {

        byte[] temp = null;
        byte[] commandAttrs = nativeCIA;

        if (nativeCIA == null) {
            return;
        }

        /*
         * Extract components of native CommandInfoArea
         */
        ByteBuffer buf = ByteBuffer.wrap(commandAttrs);

        _commandType = buf.getInt(I_cia_commandType);

        _errorCode = buf.getInt(I_cia_errorCode);

        _consoleID = buf.getInt(I_cia_consoleID);

        temp = new byte[8];
        buf.position(I_cia_consoleName);
        buf.get(temp, 0, 8);

        // Convert from EBCDIC to ASCII.
        _consoleName = new String(temp, "Cp1047");

        _cart = buf.getLong(I_cia_commandCART);

        _restOfCommandLength = buf.getInt(I_cia_commandRestOfCommandLength);

        temp = new byte[_restOfCommandLength];
        buf.position(I_cia_commandRestOfCommand);
        buf.get(temp, 0, _restOfCommandLength);

        // Convert from EBCDIC to ASCII.
        _restOfCommand = new String(temp, "Cp1047");
        _restOfCommand = stripQuotes(_restOfCommand);
    }

    int getCommandType() {
        return _commandType;
    }

    int getErrorCode() {
        return _errorCode;
    }

    int getConsoleID() {
        return _consoleID;
    }

    String getConsoleName() {
        return _consoleName;
    }

    String getCommandString() {

        return _restOfCommand;
    }

    long getCart() {
        return _cart;
    }

    /**
     * Strips the single quotes from around the command.
     */
    private static String stripQuotes(String value) {
        if (value == null) {
            return value;
        }

        value = value.trim();
        if (value.startsWith("\'") && value.endsWith("\'") && (value.length() > 1)) {
            return value.substring(1, value.length() - 1);
        }

        return value;
    }

    /**
     * toString
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("ConsoleCommand: commandType = ");
        sb.append(Integer.toHexString(_commandType));
        sb.append(", errorCode = ");
        sb.append(Integer.toHexString(_errorCode));
        sb.append(", consoleID = ");
        sb.append(Integer.toHexString(_consoleID));
        sb.append(", consoleName = ");
        sb.append(_consoleName);
        sb.append(", CART = ");
        sb.append(Long.toHexString(_cart));
        sb.append(", rest of command length = ");
        sb.append(Integer.toHexString(_restOfCommandLength));
        sb.append(", rest of command text = ");
        sb.append(_restOfCommand);
        return sb.toString();
    }
}
