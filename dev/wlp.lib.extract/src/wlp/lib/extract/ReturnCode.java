/*******************************************************************************
 * Copyright (c) 2012, 2017 IBM Corporation and others.
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
package wlp.lib.extract;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.ResourceBundle;

/**
 *
 */
public class ReturnCode {
    public static final ReturnCode OK = new ReturnCode(0);
    public static final int OK_INT = 0;
    public static final int NOT_FOUND = 1;
    public static final int UNREADABLE = 2;
    public static final int BAD_INPUT = 3;
    public static final int BAD_OUTPUT = 4;
    public static final int NOT_APPLICABLE_FEATURE = 5;

    private final int code;
    private final String msgKey;
    private final Object[] params;
    private ResourceBundle resourceBundle = null;

    public ReturnCode(int code, String msgKey, Object... params) {
        this.code = code;
        this.msgKey = msgKey;
        this.params = params;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + code;
        result = prime * result + ((msgKey == null) ? 0 : msgKey.hashCode());
        return result;
    }

    /**
     * @param ok2
     */
    public ReturnCode(int code) {
        this(code, null, (Object[]) null);
    }

    /**
     * @param code
     * @param msgKey
     * @param params
     */
    public ReturnCode(int code, String msgKey, String params) {
        this(code, msgKey, new Object[] { params });
    }

    public int getCode() {
        return code;
    }

    public synchronized String getErrorMessage() {
        if (msgKey == null) {
            return "";
        }
        if (resourceBundle == null) {
            resourceBundle = ResourceBundle.getBundle(SelfExtract.class.getName() + "Messages");
        }
        return MessageFormat.format(resourceBundle.getString(msgKey), params);
    }

    public String getMessageKey() {
        return msgKey;
    }

    public Object[] getParameters() {
        return (null == params || params.length == 0) ? new Object[0] : Arrays.copyOf(params, params.length);
    }

    /**
     *
     * @param rc
     * @return
     */
    public static boolean isReturnCodeOK(ReturnCode rc) {
        return rc.getCode() == ReturnCode.OK.getCode();
    }
}