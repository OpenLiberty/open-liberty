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

    public static final String WEB_APP_SECURITY_CONFIGURATION_UPDATED = "CWWKS9112A";
    public static final String SERVER_CONFIG_UPDATE_MESSAGES_REGEX = "CWWKG0017I.*|CWWKG0018I.*"; // CWWKG0017I: successfully updated, CWWKG0018I: not updated

    // Identity Store Handler
    public static final String ID_STORE_FOUND_MSG = "Found identity store: %s";
    public static final String ID_STORE_VALIDATION_SUCCESS_MSG = "Validation successful with store: %s";
    public static final String ID_STORE_VALIDATION_FAIL_MSG = "Validation failed with store: %s";

    // Test credentials
    public static final String VALID_PASSWORD = "reallysecretpassw0rd";
    public static final String INVALID_PASSWORD = "bad_password";

    // Users with valid groups
    public static final String USER_JASMINE = "jasmine"; // plain text password
    public static final String USER_LISA = "lisa"; // XOR encoded password
    public static final String USER_FRANK = "frank"; // Hash encoded password
    public static final String USER_SALLY = "sally"; // AES encoded password
    public static final String USER_THEO = "theo"; // AES encoded password

    // App Roles Users
    public final static String USER1 = "user1";
    public final static String USER1_PASSWORD = "user1pwd";
    public final static String USER2 = "user2";
    public final static String USER2_PASSWORD = "user2pwd";
    public final static String USER3 = "user3";
    public final static String USER3_PASSWORD = "user3pwd";
    public final static String USER4 = "user4";
    public final static String USER4_PASSWORD = "user4pwd";

    // Users with invalid groups or bad encoding
    public static final String USER_BILL = "bill"; // valid password but wrong groups
    public static final String USER_JOHNNY = "johnny"; // bad XOR encoding

    // Expected messages
    public static final String PRODUCTION_USE_WARNING_MSG = "CWWKS2600W"; // Warning about using in-memory store
    public static final String WRONG_CRED_ERROR_MSG = "CWWKS1859E"; // The password was not decrypted because a decoding error was reported
    public static final String EL_WARNING_MSG = "CWWKS2603W"; // The Expression Language (EL) expression for the ''{0}'' attribute of the identity store annotation cannot be resolved to a valid value
    public static final String AES_ENCRYPTED_PWD_WARNING_MSG = "CWWKS1865W"; // One or more of your AES-encrypted passwords were encrypted without a custom encryption key
    public static final String BADLY_DECODED_PWD_ERROR_MSG = "CWWKS1859E"; // The password was not decrypted because a decoding error was reported

    // XOR and AES-256 encoded (All decode to "reallysecretpassw0rd")
    public static final String PASSWORD_XOR_VALID = "{xor}LTo+MzMmLDo8LTorLz4sLChvLTs=";
    public static final String PASSWORD_AES_VALID = "{aes}ARB1cE2gxoEqBslQEaIs6YtFYjKFXresn8v7rkpD7I8EnQAGL8YayKrendxKW/zv4VOxwWNUv3yAUxSsAzeMi9nkWYBGf3LblwzeJYh3/t5hjoztC0/KCILy8GLlQbN4J66gr/IS9xpTL3iF0hiqOr/UzkA+8Nw=";
    public static final String PASSWORD_HASH_VALID = "{hash}ARAAAAAUUEJLREYyV2l0aEhtYWNTSEE1MTIwAAAAIH65H/2pM41HF6H5aauqbRDWcw1tZKyAF2R3/gLq2VNJQAAAACDRoEzIXF5ZrDd7rGpEOId7k6GB0ulMxbT4v2f2/72j2w==";

    // XOR, but bad encoding
    public static final String PASSWORD_XOR_INVALID = "{xor}LLTxlkwjljsdforbg=";

    /**
     * Multiple Identity Stores - Database store users
     */
    public static final String DB_USER_ALICE = "alice";
    public static final String DB_USER_RORY = "rory";
    public static final String DB_USER_CHARLIE = "charlie";
    public static final String DB_PASSWORD = "dbpassw0rd123";

    /**
     * Multiple HAMs
     */
    public static final String HAM_ORDER_FOUND_MESSAGE = "Order of HttpAuthenticationMechanisms found";
    public static final String INBUILT_HAM_PRIORITY_ORDER_MESSAGE = "FormAuthenticationMechanism, BasicHttpAuthenticationMechanism";
    public static final String CUSTOM_HAM_PRIORITY_ORDER_MESSAGE = "CustomHAMThree Priority = 300, CustomHAMTwo Priority = 200, CustomHAMOne Priority = 100";
    public static final String CUSTOM_WITH_INBUILT_PRIORITY_ORDER_MESSAGE = "CustomHAMOne Priority = 100, BasicHttpAuthenticationMechanism";

    /**
     * Multiple HAMs with qualifiers
     */
    public static final String HAM_BASIC_ADMIN_QUALIFIER_MESSAGE = "found adminHAM of \\[com.ibm.ws.security.javaeesec.cdi.beans.BasicHttpAuthenticationMechanism";
    public static final String HAM_BASIC_USER_QUALIFIER_MESSAGE = "found userHAM of \\[com.ibm.ws.security.javaeesec.cdi.beans.BasicHttpAuthenticationMechanism";
    public static final String HAM_CUSTOM_FORM_OPERATOR_QUALIFIER_MESSAGE = "found operatorHAM of \\[com.ibm.ws.security.javaeesec.cdi.beans.CustomFormAuthenticationMechanism";
    public static final String HAM_FORM_TESTER_QUALIFIER_MESSAGE = "found testerHAM of \\[com.ibm.ws.security.javaeesec.cdi.beans.FormAuthenticationMechanism";
    public static final String HAM_CUSTOM_HANDLER_PRIORITY_MESSAGE = "found Highest Priority HttpAuthenticationMechanism: BasicHttpAuthenticationMechanism";

    /**
     * CustomHAMs with qualifiers
     */
    public static final String CUSTOM_HAM_ONE_OPERATOR_QUALIFIER_MESSAGE = "found operatorHAM of \\[multiple.ham.custom.hams.CustomHAMOneOperator";
    public static final String CUSTOM_HAM_TWO_ADMIN_QUALIFIER_MESSAGE = "found adminHAM of \\[multiple.ham.custom.hams.CustomHAMTwoAdmin";
    public static final String HAM_CUSTOM_HANDLER_NO_TESTER_PRIORITY_MESSAGE = "found Highest Priority HttpAuthenticationMechanism: CustomHAMTwoAdmin";

}
