/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.zos.request.logging;

/**
 * Interface to add user data to SMF 120 subtype 11 records.
 * To get a UserData reference when the zosRequestLogging-1.0 feature is enabled,
 * create a InitialContext and then look up com/ibm/websphere/zos/request/logging/UserData.
 * InitialContext ic = new InitialContext();
 * UserData userData = (UserData) ic.lookup("com/ibm/websphere/zos/request/logging/UserData");
 *
 * @ibm-api
 */
public interface UserData {
    /**
     * Return codes for adding user data.
     */
    /** OK return code: New type and value was added. */
    public static final int ADD_DATA_OK = 0;
    /** OK return code: Data for this type was already set, previous data will be replaced by the new data. */
    public static final int ADD_DATA_REPLACED_DATA = 4;
    /** Error return code: User Data > 2K in length. */
    public static final int ADD_DATA_FAILED_TOO_BIG = 8;
    /** Error return code: reached the maximum number of user data blocks per record. */
    public static final int ADD_DATA_FAILED_TOO_MANY = 12;
    /** Error return code: User Data parameter is null. */
    public static final int ADD_DATA_FAILED_DATA_NULL = 24;
    /** Error return code: User Data parameter length is zero. */
    public static final int ADD_DATA_FAILED_DATA_LENGTH_ZERO = 28;
    /** Error return code: UnsupportedEncodingException occurred converting the String data to Cp1047. */
    public static final int ADD_DATA_FAILED_CONVERSION_ERROR = 32;

    /** Maximum size of a user data block. */
    public static final int USER_DATA_MAX_SIZE = 2048;
    /** Maximum number of user data blocks. */
    public static final int USER_DATA_MAX_COUNT = 5;

    /** Current user data block content version. */
    public static final int CURRENT_USER_DATA_BLOCK_VERSION = 2;

    /**
     * Used to provide data to be included in the user data section of the SMF 120 Subtype 11 record.
     *
     * @param identifier Used to identify the owner of (and thus how to format) the user data. The value 0 is unused. The value 1 is reserved for use by WebSphere Application
     *                       Server. Values <65535 are reserved for use by IBM. Values >= 65535 are available for use by customers. No more than 5 unique types of user data can be
     *                       added
     *                       to a single request's record. Subsequent additions of an already present user data type will overwrite the previous data of that type such that each
     *                       user data
     *                       type in a given request record shall be unique.
     * @param dataBytes  The block of data to be included in the SMF 120 subtype 11 user data section. Length cannot exceed 2KB.
     *
     * @return Return code.
     */
    public int add(int identifier, byte[] dataBytes);

    /**
     * Used to provide data to be included in the user data section of the SMF 120 Subtype 11 record.
     *
     * @param identifier Used to identify the owner of (and thus how to format) the user data. The value 0 is unused. The value 1 is reserved for use by WebSphere Application
     *                       Server. Values <65535 are reserved for use by IBM. Values >= 65535 are available for use by customers. No more than 5 unique types of user data can be
     *                       added to
     *                       a single request's record. Subsequent additions of an already present user data type will overwrite the previous data of that type such that each user
     *                       data type
     *                       in a given request record shall be unique.
     * @param data       The block of data to be included in the SMF 120 subtype 11 user data section. Length cannot exceed 2KB.
     *
     * @return Return code.
     */
    public int add(int identifier, String data);

}
