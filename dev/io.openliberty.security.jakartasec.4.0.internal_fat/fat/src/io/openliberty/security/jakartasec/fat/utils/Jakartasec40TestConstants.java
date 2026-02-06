/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.jakartasec.fat.utils;

public class Jakartasec40TestConstants {

    /**
     * Identity Store
     */

    // Test credentials
    public static final String VALID_PASSWORD = "secret1";
    public static final String INVALID_PASSWORD = "bad_password";

    // Users with valid groups
    public static final String USER_JASMINE = "jasmine"; // plain text password
    public static final String USER_LISA = "lisa"; // XOR encoded password
    public static final String USER_FRANK = "frank"; // Hash encoded password
    public static final String USER_SALLY = "sally"; // AES encoded password
    public static final String USER_THEO = "theo"; // AES encoded password

    // Users with invalid groups or bad encoding
    public static final String USER_BILL = "bill"; // valid password but wrong groups
    public static final String USER_JOHNNY = "johnny"; // bad XOR encoding

    // Expected messages
    public static final String PRODUCTION_USE_WARNING_MSG = "CWWKS2600W"; // Warning about using in-memory store
    public static final String WRONG_CRED_ERROR_MSG = "CWWKS1859E"; // The password was not decrypted because a decoding error was reported

    // XOR and AES-256 encoded (All decode to "secret1")
    public static final String PASSWORD_XOR_VALID = "{xor}LDo8LTorbg==";
    public static final String PASSWORD_AES_VALID = "{aes}ARAFCrWIYJCL7ZBNjN+MKcJoozBmbZyJPood6X6sCqMIRBaouZSd4B0u0Gcgdp/tXWwuOwi/mqYLS0cGTwuU95tgP4Y+6hgtvG2ST2gT+ghTPhLJWfiXTrvBRvR5yf2F9hkmM7SG/WRQNA==";
    public static final String PASSWORD_HASH_VALID = "{hash}ARAAAAAUUEJLREYyV2l0aEhtYWNTSEE1MTIwAAAAIKJLaCvuDfiQYK8H/6SWdTzmbMxndGqWyUWnCaA3ZPOZQAAAACBnjkRtcavFNC2k2qbwEitLlmHZKTYRmeuuCh8z1nFyyw==";

    // XOR, but bad encoding
    public static final String PASSWORD_XOR_INVALID = "{xor}LLTxlkwjljsdforbg=";

    /**
     * Multiple HAMs
     */
    public static final String HAM_ORDER_FOUND_MESSAGE = "Order of HttpAuthenticationMechanisms found";
    public static final String INBUILT_HAM_PRIORITY_ORDER_MESSAGE = "FormAuthenticationMechanism, BasicHttpAuthenticationMechanism";
    public static final String CUSTOM_HAM_PRIORITY_ORDER_MESSAGE = "CustomHAMThree Priority = 300, CustomHAMTwo Priority = 200, CustomHAMOne Priority = 100";

}
