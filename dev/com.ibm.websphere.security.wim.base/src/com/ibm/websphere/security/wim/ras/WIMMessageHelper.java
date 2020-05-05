/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security.wim.ras;

/**
 * Helper class for vmm Message
 *
 **/
public class WIMMessageHelper {

    /**
     * Create an object array with null parameter to be used as parameters to be passed to a message
     *
     * @return The object array containing a single null element.
     */
    public static Object[] generateNullMsgParms() {
        Object parms[] = new Object[1];
        parms[0] = null;
        return parms;
    }

    /**
     * Create an object array to be used as parameters to be passed to a message
     *
     * @param parm1 Value of the first parameter to be substituted into the message text.
     *
     * @return The object array containing the specified arguments.
     */
    public static Object[] generateMsgParms(Object parm1) {
        Object parms[] = new Object[1];
        parms[0] = parm1;
        return parms;
    }

    /**
     * Create an object array to be used as parameters to be passed to a message.
     *
     * @param parm1 Value of the first parameter to be substituted into the message text.
     * @param parm2 Value of the first parameter to be substituted into the message text.
     *
     * @return The object array containing the specified arguments.
     */
    public static Object[] generateMsgParms(Object parm1, Object parm2) {
        Object parms[] = new Object[2];
        parms[0] = parm1;
        parms[1] = parm2;
        return parms;
    }

    /**
     * Create an object array to be used as parameters to be passed to a message.
     *
     * @param parm1 Value of the first parameter to be substituted into the message text.
     * @param parm2 Value of the second parameter to be substituted into the message text.
     * @param parm3 Value of the third parameter to be substituted into the message text.
     *
     * @return The object array containing the specified arguments.
     */
    public static Object[] generateMsgParms(Object parm1, Object parm2, Object parm3) {
        Object parms[] = new Object[3];
        parms[0] = parm1;
        parms[1] = parm2;
        parms[2] = parm3;
        return parms;
    }

    /**
     * Create an object array to be used as parameters to be passed to a message.
     *
     * @param parm1 Value of the first parameter to be substituted into the message text.
     * @param parm2 Value of the second parameter to be substituted into the message text.
     * @param parm3 Value of the third parameter to be substituted into the message text.
     * @param parm4 Value of the fourth parameter to be substituted into the message text.
     *
     * @return The object array containing the specified arguments.
     */
    public static Object[] generateMsgParms(Object parm1, Object parm2, Object parm3, Object parm4) {
        Object parms[] = new Object[4];
        parms[0] = parm1;
        parms[1] = parm2;
        parms[2] = parm3;
        parms[3] = parm4;
        return parms;
    }

    /**
     * Create an object array to be used as parameters to be passed to a message.
     *
     * @param parm1 Value of the first parameter to be substituted into the message text.
     * @param parm2 Value of the second parameter to be substituted into the message text.
     * @param parm3 Value of the third parameter to be substituted into the message text.
     * @param parm4 Value of the fourth parameter to be substituted into the message text.
     * @param parm5 Value of the fifth parameter to be substituted into the message text.
     *
     * @return The object array containing the specified arguments.
     */
    public static Object[] generateMsgParms(Object parm1, Object parm2, Object parm3, Object parm4, Object parm5) {
        Object parms[] = new Object[5];
        parms[0] = parm1;
        parms[1] = parm2;
        parms[2] = parm3;
        parms[3] = parm4;
        parms[4] = parm5;
        return parms;
    }

    /**
     * Create an object array to be used as parameters to be passed to a message.
     *
     * @param parm1 Value of the first parameter to be substituted into the message text.
     * @param parm2 Value of the second parameter to be substituted into the message text.
     * @param parm3 Value of the third parameter to be substituted into the message text.
     * @param parm4 Value of the fourth parameter to be substituted into the message text.
     * @param parm5 Value of the fifth parameter to be substituted into the message text.
     * @param parm6 Value of the sixth parameter to be substituted into the message text.
     *
     * @return The object array containing the specified arguments.
     */
    public static Object[] generateMsgParms(Object parm1, Object parm2, Object parm3, Object parm4, Object parm5,
                                            Object parm6) {
        Object parms[] = new Object[6];
        parms[0] = parm1;
        parms[1] = parm2;
        parms[2] = parm3;
        parms[3] = parm4;
        parms[4] = parm5;
        parms[5] = parm6;
        return parms;
    }

    /**
     * Create an object array to be used as parameters to be passed to a message.
     *
     * @param parm1 Value of the first parameter to be substituted into the message text.
     * @param parm2 Value of the second parameter to be substituted into the message text.
     * @param parm3 Value of the third parameter to be substituted into the message text.
     * @param parm4 Value of the fourth parameter to be substituted into the message text.
     * @param parm5 Value of the fifth parameter to be substituted into the message text.
     * @param parm6 Value of the sixth parameter to be substituted into the message text.
     * @param parm7 Value of the seventh parameter to be substituted into the message text. *
     *
     * @return The object array containing the specified arguments.
     */
    public static Object[] generateMsgParms(Object parm1, Object parm2, Object parm3, Object parm4, Object parm5,
                                            Object parm6, Object parm7) {
        Object parms[] = new Object[7];
        parms[0] = parm1;
        parms[1] = parm2;
        parms[2] = parm3;
        parms[3] = parm4;
        parms[4] = parm5;
        parms[5] = parm6;
        parms[6] = parm7;
        return parms;
    }

    /**
     * Create an object array to be used as parameters to be passed to a message.
     *
     * @param parm1 Value of the first parameter to be substituted into the message text.
     * @param parm2 Value of the second parameter to be substituted into the message text.
     * @param parm3 Value of the third parameter to be substituted into the message text.
     * @param parm4 Value of the fourth parameter to be substituted into the message text.
     * @param parm5 Value of the fifth parameter to be substituted into the message text.
     * @param parm6 Value of the sixth parameter to be substituted into the message text.
     * @param parm7 Value of the seventh parameter to be substituted into the message text.
     * @param parm8 Value of the eighth parameter to be substituted into the message text.
     *
     * @return The object array containing the specified arguments.
     */
    public static Object[] generateMsgParms(Object parm1, Object parm2, Object parm3, Object parm4, Object parm5,
                                            Object parm6, Object parm7, Object parm8) {
        Object parms[] = new Object[8];
        parms[0] = parm1;
        parms[1] = parm2;
        parms[2] = parm3;
        parms[3] = parm4;
        parms[4] = parm5;
        parms[5] = parm6;
        parms[6] = parm7;
        parms[7] = parm8;
        return parms;
    }
}
