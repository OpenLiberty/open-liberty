/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ejs.ras;

/**
 * @see com.ibm.websphere.ras.DataFormatHelper
 */
@Deprecated
public class DataFormatHelper {
    /**
     * @see com.ibm.websphere.ras.DataFormatHelper#padHexString(int, int)
     */
    @Deprecated
    public static final String padHexString(int num, int width) {
        return com.ibm.websphere.ras.DataFormatHelper.padHexString(num, width);
    }

    /**
     * @see com.ibm.websphere.ras.DataFormatHelper#throwableToString(Throwable)
     */
    @Deprecated
    public static final String throwableToString(Throwable t) {
        return com.ibm.websphere.ras.DataFormatHelper.throwableToString(t);
    }

    /**
     * @see com.ibm.websphere.ras.DataFormatHelper#escape(String)
     */
    @Deprecated
    public final static String escape(String src) {
        return com.ibm.websphere.ras.DataFormatHelper.escape(src);
    }
}
