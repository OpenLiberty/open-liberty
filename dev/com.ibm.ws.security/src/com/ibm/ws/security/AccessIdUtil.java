/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.internal.SecurityServiceImpl;

/**
 * Utility class to allow for easy construction and deconstruction of
 * an accessId. An accessId is of the format type:realm/uniqueId
 * Grammar: Type: [^:}+
 * ream: [^/]+
 * uniqueId: .+
 */
@Component(service = { AccessIdUtil.class },
           immediate = true,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = { "service.vendor=IBM" })
public final class AccessIdUtil {
    static final TraceComponent tc = Tr.register(AccessIdUtil.class);
    static final Pattern p = Pattern.compile("([^:]+):([^/]+)/(.+)");
    // this pattern considers protocol and hostname as a realm name.
    // for example, group:https://test.com/group1, then this splits the input as
    // "group", "https://test.com", "group1".
    // The original pattern p splits the same string as
    // "group", "https:", "/test.com/group1".
    static final Pattern ph = Pattern.compile("([^:]+):([^:]+://[^/]+)/(.+)");

    public static final String TYPE_SERVER = "server";
    public static final String TYPE_USER = "user";
    public static final String TYPE_GROUP = "group";
    public static final String TYPE_SEPARATOR = ":";
    public static final String REALM_SEPARATOR = "/";
    public static final String KEY_SECURITY_SERVICE = "securityService";

    private static volatile String[] realm = null;
    private static volatile Pattern realmPattern;

    @Reference(service = SecurityService.class,
               name = KEY_SECURITY_SERVICE,
               policy = ReferencePolicy.DYNAMIC,
               target = "(UserRegistry=*)",
               updated = "setSecurityService")
    protected void setSecurityService(ServiceReference<SecurityService> ref) {
        realm = (String[]) ref.getProperty(SecurityServiceImpl.KEY_USERREGISTRY);
        if (realm.length == 1) {
            realmPattern = Pattern.compile("([^:]+):(" + Pattern.quote(realm[0]) + ")/(.*)");
        } else {
            realmPattern = null;
        }
    }

    protected void unsetSecurityService(ServiceReference<SecurityService> ref) {
        realm = null;
        realmPattern = null;
    }

    /**
     * Constructs the full access identifier: type:realm/uniqueId
     * 
     * @param type Entity type, must not be null or empty
     * @param realm Realm, must not be null or empty
     * @param uniqueId Entity unique ID, must not be null or empty
     * @return An accessId representing the entity. Will not be null.
     */
    public static String createAccessId(String type, String realm, String uniqueId) {
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("An internal error occured. type is null");
        }
        if (realm == null || realm.isEmpty()) {
            throw new IllegalArgumentException("An internal error occured. realm is null");
        }
        if (uniqueId == null || uniqueId.isEmpty()) {
            throw new IllegalArgumentException("An internal error occured. uniqueId is null");
        }
        return type + TYPE_SEPARATOR + realm + REALM_SEPARATOR + uniqueId;
    }

    /**
     * Checks that the string is a complete accessId, of the format:
     * type:realm/uniqueId
     * 
     * @param accessId
     * @return true if the string is a complete accessId, false otherwise
     */
    private static boolean isCompleteAccessId(String accessId) {
        return matcher(accessId) != null;
    }

    static Matcher matcher(String accessId) {
        if (accessId == null || accessId.isEmpty()) {
            return null;
        }
        if (realmPattern != null) {
            Matcher m = realmPattern.matcher(accessId);
            if (m.matches()) {
                if (m.group(3).length() > 0)
                    return m;
                return null;
            }

        }
        Matcher m = ph.matcher(accessId);
        if (m.matches()) {
            return m;
        }

        m = p.matcher(accessId);
        if (m.matches()) {
            return m;
        }
        return null;
    }

    static boolean validateRealm(Matcher m) {
        String[] realms = realm;
        if (realms == null || realms.length != 1)
            return true;
        String r = m.group(2);
        return r.equals(realms[0]);
    }

    /**
     * Given an accessId, extract the entity type.
     * 
     * @param accessId
     * @return The type for the accessId, or {@code null} if the accessId is invalid
     */
    public static String getEntityType(String accessId) {
        Matcher m = matcher(accessId);
        if (m != null) {
            return m.group(1);
        }
        return null;
    }

    /**
     * Given an accessId, extract the realm.
     * 
     * @param accessId
     * @return The realm for the accessId, or {@code null} if the accessId is invalid
     */
    public static String getRealm(String accessId) {
        Matcher m = matcher(accessId);
        if (m != null) {
            return m.group(2);
        }
        return null;
    }

    /**
     * Given an accessId, extract the uniqueId.
     * 
     * @param accessId
     * @return The uniqueId for the accessId, or {@code null} if the accessId is invalid
     */
    public static String getUniqueId(String accessId) {
        Matcher m = matcher(accessId);
        if (m != null) {
            return m.group(3);
        }
        return null;
    }

    /**
     * Given an accessId and realm name, extract the uniqueId.
     * 
     * @param accessId
     * @param realm
     * @return The uniqueId for the accessId, or {@code null} if the accessId is invalid
     */
    public static String getUniqueId(String accessId, String realm) {
    
        if (realm != null) {
            Pattern pattern = Pattern.compile("([^:]+):(" + Pattern.quote(realm) + ")/(.*)");
            Matcher m = pattern.matcher(accessId);
            if (m.matches()) {
                if (m.group(3).length() > 0) {
                    return m.group(3);
                }
            }
        }
        // if there is no match, fall back.
        return getUniqueId(accessId);
    }

    /**
     * Checks to see if the specified accessId is complete.
     * 
     * @param accessId
     * @return boolean if accessId is complete and valid
     */
    public static boolean isAccessId(String accessId) {
        return isCompleteAccessId(accessId);
    }

    /**
     * Checks to see if the specified accessId begins with "server:".
     * 
     * @param accessId
     * @return boolean if accessId is valid and begins with "server:"
     */
    public static boolean isServerAccessId(String accessId) {
        return isAccessId(accessId) && accessId.startsWith(AccessIdUtil.TYPE_SERVER + AccessIdUtil.TYPE_SEPARATOR);
    }

    /**
     * Checks to see if the specified accessId begins with "user:".
     * 
     * @param accessId
     * @return boolean if accessId is valid and begins with "user:"
     */
    public static boolean isUserAccessId(String accessId) {
        return isAccessId(accessId) && accessId.startsWith(AccessIdUtil.TYPE_USER + AccessIdUtil.TYPE_SEPARATOR);
    }

    /**
     * Checks to see if the specified accessId begins with "group:".
     * 
     * @param accessId
     * @return boolean if accessId is valid and begins with "group:"
     */
    public static boolean isGroupAccessId(String accessId) {
        return isAccessId(accessId) && accessId.startsWith(AccessIdUtil.TYPE_GROUP + AccessIdUtil.TYPE_SEPARATOR);
    }

}
