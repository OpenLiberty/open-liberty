/*******************************************************************************
 * Copyright (c) 2004, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.hpel.impl;

/**
 * 
 * The MessageConverter class provides the functions required to convert message IDs used in WAS
 * from the WAS v5 format to the IBM SWG message standard compliant structure available in later WAS releases.
 * 
 */
public class HpelMessageConverter {

    /**
     * Determines if a message starts with IBM SWG message ID.
     * 
     * @param msg the log record message
     * @return substring containing message ID or null if message does not
     *         start with IBM SWG pattern.
     */
    public static String getMessageId(String msg) {
        if (msg == null) {
            return null;
        }
        // check for IBM SWG pattern - "AAAAANNNNS:"
        // 5 chars followed by 4 numbers, followed by one of E/W/I, followed by a colon
        if (msg.length() >= 10) { // 10 chars in pattern 

            // copy first 9 characters into an array
            char[] lm = new char[9];
            msg.getChars(0, 9, lm, 0);

            // match characters in array against AAAAANNNNS: pattern

            if ((lm[8] >= '0') && (lm[8] <= '9')
                && (lm[7] >= '0') && (lm[7] <= '9')
                && (lm[6] >= '0') && (lm[6] <= '9')
                && (lm[5] >= '0') && (lm[5] <= '9')
                && ((lm[4] >= 'A') && (lm[4] <= 'Z') || (lm[4] >= '0') && (lm[4] <= '9'))
                && ((lm[3] >= 'A') && (lm[3] <= 'Z') || (lm[3] >= '0') && (lm[3] <= '9'))
                && ((lm[2] >= 'A') && (lm[2] <= 'Z') || (lm[2] >= '0') && (lm[2] <= '9'))
                && ((lm[1] >= 'A') && (lm[1] <= 'Z') || (lm[1] >= '0') && (lm[1] <= '9'))
                && ((lm[0] >= 'A') && (lm[0] <= 'Z') || (lm[0] >= '0') && (lm[0] <= '9'))) {
                return msg.substring(0, 10);
            }

        }

        // Check for IBM4.4.1 pattern - "AAAANNNNS:"
        // 4 chars followed by 4 numbers, followed by one of E/W/I, followed by a colon
        if (msg.length() >= 9) { // 9 chars in pattern

            // copy first 8 characters into an array
            char[] lm = new char[8];
            msg.getChars(0, 8, lm, 0);

            // match characters in array against AAAAANNNNS: pattern
            if ((lm[7] >= '0') && (lm[7] <= '9')
                && (lm[6] >= '0') && (lm[6] <= '9')
                && (lm[5] >= '0') && (lm[5] <= '9')
                && (lm[4] >= '0') && (lm[4] <= '9')
                && ((lm[3] >= 'A') && (lm[3] <= 'Z') || (lm[3] >= '0') && (lm[3] <= '9'))
                && ((lm[2] >= 'A') && (lm[2] <= 'Z') || (lm[2] >= '0') && (lm[2] <= '9'))
                && ((lm[1] >= 'A') && (lm[1] <= 'Z') || (lm[1] >= '0') && (lm[1] <= '9'))
                && ((lm[0] >= 'A') && (lm[0] <= 'Z') || (lm[0] >= '0') && (lm[0] <= '9'))) {

                // v5
                return msg.substring(0, 9);
            }
        }
        return null;
    }

}
