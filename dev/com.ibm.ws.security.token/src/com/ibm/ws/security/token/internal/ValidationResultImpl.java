/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.token.internal;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.wsspi.security.token.ValidationResult;

/**
 * @see com.ibm.wsspi.security.token.ValidationResult
 *
 * @author IBM Corp.
 * @version 7.0.0
 * @since 7.0.0
 * @ibm-spi
 *
 */
public class ValidationResultImpl implements ValidationResult {
    public final static String REALM_DELIMITER = "/";
    public final static String TYPE_DELIMITER = ":";
    public final static String USER_TYPE_DELIMITER = "user:";
    public final static String EMPTY_STRING = new String("");

    private final String uniqueId;
    private String realm = null;

    public ValidationResultImpl(String uniqueId, String realm) {
        this.realm = realm;
        this.uniqueId = uniqueId;
    }

    @Override
    public String getRealmFromUniqueId() {
        if (realm == null) {
            return getRealmFromUniqueID(uniqueId);
        } else
            return realm;
    }

    @Override
    public String getUniqueId() {
        String result = uniqueId;
        return result;
    }

    @Override
    public String getUserFromUniqueId() {
        String result = null;
        if (realm == null) {
            result = getUserFromUniqueID(uniqueId);
        } else {
            if (realm != null) {
                Pattern pattern = Pattern.compile("([^:]+):(" + Pattern.quote(realm) + ")/(.*)");
                Matcher m = pattern.matcher(uniqueId);
                if (m.matches()) {
                    if (m.group(3).length() > 0) {
                        return m.group(3);
                    }
                }
            }

        }
        return result;
    }

    @Override
    public boolean requiresLogin() {
        return true;
    }

    private static String getUserFromUniqueID(String uniqueID) {
        if (uniqueID == null) {
            return EMPTY_STRING;
        }
        if (uniqueID.startsWith(USER_TYPE_DELIMITER)) {
            uniqueID = uniqueID.trim();
            int realmDelimiterIndex = uniqueID.indexOf(REALM_DELIMITER);
            if (realmDelimiterIndex < 0) {
                return EMPTY_STRING;
            } else {
                return uniqueID.substring(realmDelimiterIndex + 1);
            }
        }
        return EMPTY_STRING;
    }

    /**
     * <p>
     * This method accepts the uniqueID returned from the validateLTPAToken method.
     * It returns the realm portion of this string. The realm can be used to
     * determine where the token came from.
     * </p>
     *
     * @param String WebSphere uniqueID
     * @return String realm
     **/

    private static String getRealmFromUniqueID(String uniqueID) {

        int index = uniqueID.indexOf(TYPE_DELIMITER);

        if (uniqueID.startsWith(USER_TYPE_DELIMITER)) {
            uniqueID = uniqueID.substring(index + 1);
        }
        return getRealm(uniqueID);
    }

    private static String getRealm(String realmSecurityName) {
        if (realmSecurityName == null)
            return EMPTY_STRING;

        realmSecurityName = realmSecurityName.trim();

        {
            int realmDelimiterIndex = realmSecurityName.indexOf(REALM_DELIMITER);
            if (realmDelimiterIndex < 0) {
                return null;
            }
            return realmSecurityName.substring(0, realmDelimiterIndex);
        }
    }

    @Override
    public String toString() {
        return super.toString() + " uniqueId = " + uniqueId;
    }
}
