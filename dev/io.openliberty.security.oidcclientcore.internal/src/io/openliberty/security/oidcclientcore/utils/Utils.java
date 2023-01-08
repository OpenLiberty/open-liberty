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
package io.openliberty.security.oidcclientcore.utils;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;

public class Utils {

    private static final TraceComponent tc = Tr.register(Utils.class);

    public static final int TIMESTAMP_LENGTH = 15;

    public static String getTimeStamp() {
        long lNumber = (new Date()).getTime();
        return getTimeStamp(lNumber);
    }

    public static String getTimeStamp(long lNumber) {
        String timeStamp = "" + lNumber;
        return StringUtils.leftPad(timeStamp, TIMESTAMP_LENGTH, '0');
    }

    @Sensitive
    @Trivial
    public static String getStrHashCode(String input) {
        if (input != null && !input.isEmpty()) {
            int hashCode = input.hashCode();
            if (hashCode < 0) {
                hashCode = hashCode * -1;
                return "n" + hashCode;
            } else {
                return "p" + hashCode;
            }
        } else {
            // This should not happen
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "hash() gets a null or empty parameter");
            }
            return "";
        }

    }

    public static long convertNormalizedTimeStampToLong(String input) {
        String timeStamp = input.substring(0, TIMESTAMP_LENGTH);
        return Long.parseLong(timeStamp);
    }

}
