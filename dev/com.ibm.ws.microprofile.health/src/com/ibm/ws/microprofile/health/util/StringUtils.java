/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.health.util;

/**
 * A lite-weight impl of apache.commons.lang3.StringUtils/ObjectUtils.
 */
public class StringUtils {

    /**
     * @return true if the string is null or "" or only whitespace
     */
    public static boolean isEmpty(String str) {
        return (str == null) || str.trim().length() == 0;
    }

}
