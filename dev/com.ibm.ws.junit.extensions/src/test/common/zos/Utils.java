/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.common.zos;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This class provides static utility functions.
 */
public class Utils {

    /**
     * Formats the date using the following pattern: "yyyy/MM/dd HH:mm:ss.SSS".
     */
    public static final DateFormat tsFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");

    /**
     * Formats the date using the following pattern: "yyyy/MM/dd HH:mm:ss.SSS (zz)".
     */
    public static final DateFormat tsFormatZone = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS (z)");

    /**
     * Formats the date using the following pattern: "HH.mm.ss".
     */
    public static final DateFormat tsFormatHMS = new SimpleDateFormat("dd MMM yyyy HH.mm.ss (z)");

    /**
     * The tag string is put at the beginning of each message printed via
     * Utils.println().
     */
    public static String msgTag = "";

    /**
     * Prints a line with the given message, along with a timestamp.
     * 
     * @param msg The message to print.
     */
    public static void println(String msg) {
        System.out.println(msgTag + " [" + getTimestamp() + "] " + msg);
    }

    /**
     * Gets the current time as a String, formatted using the tsFormatZone field.
     * 
     * @return Formatted timestamp.
     */
    public static String getTimestamp() {
        return tsFormatZone.format(new Date(System.currentTimeMillis()));
    }

}
