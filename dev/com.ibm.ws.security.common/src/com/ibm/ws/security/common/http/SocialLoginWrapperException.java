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
package com.ibm.ws.security.common.http;

/**
 * Wrapper class used for wrapping exceptions that emit social login specific NLS messages in the security.common bundle. Wrapping
 * the exceptions should allow calling classes to emit their own error or warning messages if they choose, rather than emitting
 * social login NLS messages that may be unrelated or misleading.
 */
public class SocialLoginWrapperException extends AbstractHttpResponseException {

    private static final long serialVersionUID = 1L;

    public SocialLoginWrapperException(String url, int statusCode, String nlsMessage, Exception cause) {
        super(url, statusCode, nlsMessage, cause);
    }

}
