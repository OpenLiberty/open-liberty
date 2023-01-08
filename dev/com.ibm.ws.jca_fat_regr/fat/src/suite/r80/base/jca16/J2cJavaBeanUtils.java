/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package suite.r80.base.jca16;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class <CODE>J2cJavaBeanUtils</CODE> provides utility methods that
 * for validating JCA requirements for RAR JavaBeans.
 */
public class J2cJavaBeanUtils {

    private final static String CLASSNAME = J2cJavaBeanUtils.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASSNAME);

    @SuppressWarnings("unchecked")
    /**
     * Determine whether an object of the target class can be assigned (widened)
     * to the source class.
     * 
     * @param srcCls     The source class.
     * @param tgtclsName The name of the target class.
     * @return true if an instance of target class may be assigned
     *         to an instance of the target class; otherwise, return false.
     */
    public static Boolean isAssignableFrom(Class srcCls, String tgtClsName) {
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.entering(CLASSNAME, "isAssignableFrom", new Object[] { srcCls, tgtClsName });
        }
        Boolean result = false;

        Class tgtCls = null;
        try {
            tgtCls = Class.forName(tgtClsName);
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.warning("Target class failed to load.");
        }
        result = tgtCls != null && srcCls.isAssignableFrom(tgtCls);

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.exiting(CLASSNAME, "isAssignableFrom", result);
        }
        return result;
    }

}
