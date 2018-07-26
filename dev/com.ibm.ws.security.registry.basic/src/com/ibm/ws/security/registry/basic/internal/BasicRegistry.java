/*******************************************************************************
 * Copyright (c) 2011,2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.registry.basic.internal;

import java.rmi.RemoteException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.felix.scr.ext.annotation.DSExt;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.ras.ProtectedString;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.security.X509CertificateMapper;
import com.ibm.ws.bnd.metatype.annotation.Ext;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.registry.CertificateMapFailedException;
import com.ibm.ws.security.registry.CertificateMapNotSupportedException;
import com.ibm.ws.security.registry.CustomRegistryException;
import com.ibm.ws.security.registry.EntryNotFoundException;
import com.ibm.ws.security.registry.LDAPUtils;
import com.ibm.ws.security.registry.NotImplementedException;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.SearchResult;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

/**
 * Configuration of BasicRegistry has the following expectations:
 * 1. Realm name does not need to be specified, a default value will be used
 * 2. All user names, user passwords, group names, and group member lists
 * will have their leading and trailing whitespace removed. Inner whitespace
 * is preserved, e.g. " user with spaces " is stored as "user with spaces".
 * 3. ignoreCase does not need to be specified, a default value of false will
 * be used.
 */
@ObjectClassDefinition(factoryPid = "com.ibm.ws.security.registry.basic.config",
                       name = "%basic.config", description = "%basic.config.desc", localization = Ext.LOCALIZATION)
@Ext.Alias("basicRegistry")
@Ext.ObjectClassClass(UserRegistry.class)
interface BasicRegistryConfig {

    public static final String MAP_MODE_PRINCIPAL_CN = "PRINCIPAL_CN";
    public static final String MAP_MODE_CUSTOM = "CUSTOM";
    public static final String MAP_MODE_NOT_SUPPORTED = "NOT_SUPPORTED";

    @AttributeDefinition(name = "%realm", description = "%realm.desc", defaultValue = BasicRegistry.DEFAULT_REALM_NAME)
    String realm();

    @AttributeDefinition(name = "%ignoreCaseForAuthentication", description = "%ignoreCaseForAuthentication.desc", defaultValue = "false")
    boolean ignoreCaseForAuthentication();

    @AttributeDefinition(name = "%basic.user", description = "%basic.user.desc", required = false)
    @Ext.FlatReferencePid("com.ibm.ws.security.registry.basic.config.user")
    User[] user();

    @AttributeDefinition(name = "%basic.group", description = "%basic.group.desc", required = false)
    @Ext.FlatReferencePid("com.ibm.ws.security.registry.basic.config.group")
    Group[] group();

    @AttributeDefinition(name = Ext.INTERNAL, description = Ext.INTERNAL_DESC, required = false)
    String config_id();

    @AttributeDefinition(name = "%certificate.map.mode", description = "%certificate.map.mode.desc", required = false,
                         options = { @Option(value = MAP_MODE_PRINCIPAL_CN, label = "%certificate.map.mode.principal_cn"),
                                     @Option(value = MAP_MODE_CUSTOM, label = "%certificate.map.mode.custom"),
                                     @Option(value = MAP_MODE_NOT_SUPPORTED, label = "%certificate.map.mode.not.supported") },
                         defaultValue = MAP_MODE_PRINCIPAL_CN)
    String certificateMapMode();

    @AttributeDefinition(name = "%certificate.mapper.id", description = "%certificate.mapper.id.desc", type = AttributeType.STRING, required = false)
    String certificateMapperId();

    @AttributeDefinition(name = Ext.INTERNAL, description = Ext.INTERNAL_DESC, defaultValue = "(x509.certificate.mapper.id=${certificateMapperId})")
    String CertificateMapper_target();
}

@ObjectClassDefinition(factoryPid = "com.ibm.ws.security.registry.basic.config.user", name = "%basic.user", description = "%basic.user.desc",
                       localization = Ext.LOCALIZATION)
interface User {

    @AttributeDefinition(name = "%user.name", description = "%user.name.desc")
    String name();

    @AttributeDefinition(type = AttributeType.STRING, name = "%user.password", description = "%user.password.desc")
    @Ext.Type("passwordHash")
    SerializableProtectedString password();
}

@ObjectClassDefinition(factoryPid = "com.ibm.ws.security.registry.basic.config.group", name = "%basic.group", description = "%basic.group.desc",
                       localization = Ext.LOCALIZATION)
interface Group {

    @AttributeDefinition(name = "%group.name", description = "%group.name.desc")
    String name();

    @AttributeDefinition(name = "%basic.group.member", description = "%basic.group.member.desc", required = false)
    @Ext.FlatReferencePid("com.ibm.ws.security.registry.basic.config.group.member")
    Member[] member();
}

@ObjectClassDefinition(factoryPid = "com.ibm.ws.security.registry.basic.config.group.member", name = "%basic.group.member", description = "%basic.group.member.desc",
                       localization = Ext.LOCALIZATION)
interface Member {

    @AttributeDefinition(name = "%member.name", description = "%member.name.desc")
    String name();
}

@Component(configurationPid = "com.ibm.ws.security.registry.basic.config",
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           property = { "service.vendor=IBM", "com.ibm.ws.security.registry.type=Basic" })
@DSExt.ConfigureWithInterfaces
public class BasicRegistry implements UserRegistry {
    private static final TraceComponent tc = Tr.register(BasicRegistry.class);

    protected static final String DEFAULT_REALM_NAME = "BasicRegistry";

    static final String TYPE = "Basic";

    private volatile State state;

    /**
     * Certificate mapping mode.
     */
    private String certificateMapMode = null;

    /**
     * The {@link X509CertificateMapper} reference.
     */
    private final AtomicReference<X509CertificateMapper> iCertificateMapperRef = new AtomicReference<X509CertificateMapper>();

    private class State {
        final String realm;// = DEFAULT_REALM_NAME;
        final boolean ignoreCaseForAuthentication;// = Boolean.FALSE;
        final Map<String, BasicPassword> users;
        final Map<String, List<String>> groups;

        public State(String realm, boolean ignoreCaseForAuthentication, Map<String, BasicPassword> users, Map<String, List<String>> groups) {
            super();
            this.realm = realm;
            this.ignoreCaseForAuthentication = ignoreCaseForAuthentication;
            this.users = users;
            this.groups = groups;
        }

    }

    @Activate
    @Modified
    protected void activate(BasicRegistryConfig config) {
        Map<String, BasicPassword> users = users(config.user());
        state = new State(config.realm(), config.ignoreCaseForAuthentication(), users, group(config.group(), users));
        if (state.users.isEmpty()) {
            Tr.warning(tc, "BASIC_REGISTRY_NO_USERS_DEFINED", new Object[] { config.config_id() });
        }

        /*
         * Determine the X.509 certificate authentication mode.
         */
        this.certificateMapMode = config.certificateMapMode();
    }

    private static Map<String, BasicPassword> users(User[] users) {
        Set<String> bad = new HashSet<String>();
        Map<String, BasicPassword> result = new HashMap<String, BasicPassword>(users.length);
        for (User user : users) {
            String name = user.name().trim();
            if (name.isEmpty()) {
                String message = TraceNLS.getStringFromBundle(BasicRegistry.class,
                                                              TraceConstants.MESSAGE_BUNDLE,
                                                              "USER_MUST_DEFINE_NAME",
                                                              "A user element must define a name.");
                Tr.error(tc, "BASIC_REGISTRY_INVALID_USER_DEFINITION", message);
                continue;
            }
            if (result.containsKey(name) || bad.contains(name)) {
                bad.add(name);
                result.remove(name);
                Tr.error(tc, "BASIC_REGISTRY_SAME_USER_DEFINITION", name);
                continue;
            }
            //So much for trying to protect passwords!!! lets call intern() too!! why not???
            String decodedString = new String(user.password().getChars()).trim();
            if (decodedString.isEmpty()) {
                bad.add(name);
                String message = TraceNLS.getFormattedMessage(BasicRegistry.class,
                                                              TraceConstants.MESSAGE_BUNDLE,
                                                              "USER_MUST_DEFINE_PASSWORD",
                                                              new Object[] { name },
                                                              "The user element with name ''{0}'' must define a password.");
                Tr.error(tc, "BASIC_REGISTRY_INVALID_USER_DEFINITION", message);
                continue;
            }
            boolean isHashed = PasswordUtil.isHashed(decodedString);
            if (!isHashed) {
                // the password might be encoded.
                decodedString = PasswordUtil.passwordDecode(decodedString);
            }

            BasicPassword decodedPassword = new BasicPassword(decodedString, isHashed);
            result.put(name, decodedPassword);
        }
        return result;
    }

    private static Map<String, List<String>> group(Group[] groups, Map<String, BasicPassword> users) {
        Set<String> bad = new HashSet<String>();
        Map<String, List<String>> result = new HashMap<String, List<String>>(groups.length);
        for (Group group : groups) {
            String groupName = group.name().trim();
            if (groupName.isEmpty()) {
                String message = TraceNLS.getStringFromBundle(BasicRegistry.class,
                                                              TraceConstants.MESSAGE_BUNDLE,
                                                              "GROUP_MUST_DEFINE_NAME",
                                                              "A group element must define a name.");
                Tr.error(tc, "BASIC_REGISTRY_INVALID_GROUP_DEFINITION", message);
                continue;
            }
            if (result.containsKey(groupName) || bad.contains(groupName)) {
                bad.add(groupName);
                result.remove(groupName);
                Tr.error(tc, "BASIC_REGISTRY_SAME_GROUP_DEFINITION", groupName);
                continue;
            }
            List<String> members = new ArrayList<String>(group.member().length);
            for (Member member : group.member()) {
                String memberName = member.name().trim();
                if (memberName.isEmpty()) {
                    String message = TraceNLS.getStringFromBundle(BasicRegistry.class,
                                                                  TraceConstants.MESSAGE_BUNDLE,
                                                                  "MEMBER_MUST_DEFINE_NAME",
                                                                  "A member element must define a name.");
                    Tr.error(tc, "BASIC_REGISTRY_INVALID_MEMBER_DEFINITION", message);
                    continue;
                }
                if (members.contains(memberName)) {
                    Tr.warning(tc, "BASIC_REGISTRY_SAME_MEMBER_DEFINITION", new Object[] { memberName, groupName });
                } else {
                    members.add(memberName);
                    if (!users.containsKey(memberName)) {
                        Tr.warning(tc, "BASIC_REGISTRY_UNKNOWN_MEMBER_DEFINITION", memberName, groupName);
                    }
                }
            }
            result.put(groupName, members);
        }
        return result;
    }

    @Deactivate
    protected void deactivate() {
        state = null;
    }

    /** {@inheritDoc} */
    @Override
    public String getRealm() {
        return state.realm;
    }

    /** {@inheritDoc} */
    @Override
    public String checkPassword(String userSecurityName, @Sensitive String password) throws RegistryException {
        if (userSecurityName == null) {
            throw new IllegalArgumentException("userSecurityName is null");
        }
        if (userSecurityName.isEmpty()) {
            throw new IllegalArgumentException("userSecurityName is an empty String");
        }
        if (password == null) {
            throw new IllegalArgumentException("password is null");
        }
        if (password.trim().isEmpty()) {
            throw new IllegalArgumentException("password is an empty String");
        }
        boolean valid = false;

        BasicPassword storedPassObj = null;
        if (state.ignoreCaseForAuthentication) {
            for (Map.Entry<String, BasicPassword> entry : state.users.entrySet()) {
                String keyUserName = entry.getKey();
                if (keyUserName.equalsIgnoreCase(userSecurityName)) {
                    storedPassObj = entry.getValue();
                }
            }
        } else {
            storedPassObj = state.users.get(userSecurityName);
        }

        if (storedPassObj != null) {
            if (!storedPassObj.isHashed()) {
                ProtectedString inPass = new ProtectedString(password.toCharArray());
                ProtectedString storedPass = storedPassObj.getPassword();
                if (storedPass != null && storedPass.equals(inPass)) {
                    valid = true;
                }
            } else {
                String storedPass = storedPassObj.getHashedPassword();
                if (storedPass != null) {
                    HashMap<String, String> props = new HashMap<String, String>();
                    props.put(PasswordUtil.PROPERTY_HASH_ENCODED, storedPass);
                    String inPass = null;
                    try {
                        inPass = PasswordUtil.encode(password, PasswordUtil.getCryptoAlgorithm(storedPass), props);
                    } catch (Exception e) {
                        //fail to encode password.
                        throw new IllegalArgumentException("password encoding failure : " + e.getMessage());
                    }
                    if (storedPass.equals(inPass)) {
                        valid = true;
                    }
                }
            }
        }
        if (valid) {
            return userSecurityName;
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore({ com.ibm.websphere.security.CertificateMapNotSupportedException.class, com.ibm.websphere.security.CertificateMapFailedException.class })
    public String mapCertificate(X509Certificate[] chain) throws CertificateMapNotSupportedException, CertificateMapFailedException, RegistryException {

        if (BasicRegistryConfig.MAP_MODE_NOT_SUPPORTED.equalsIgnoreCase(certificateMapMode)) {

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Certificate authentication has been disabled for this basic registry.");
            }
            String msg = Tr.formatMessage(tc, "BASIC_REGISTRY_CERT_IGNORED");
            throw new CertificateMapNotSupportedException(msg);

        } else if (BasicRegistryConfig.MAP_MODE_CUSTOM.equalsIgnoreCase(certificateMapMode)) {

            if (chain == null || chain.length == 0) {
                throw new IllegalArgumentException("cert is null");
            }

            /*
             * Use the custom certificate mapper.
             */
            try {
                X509CertificateMapper mapper = iCertificateMapperRef.get();
                if (mapper == null) {
                    String msg = Tr.formatMessage(tc, "BASIC_REGISTRY_MAPPER_NOT_BOUND");
                    throw new CertificateMapFailedException(msg);
                }

                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Using custom X.509 certificate mapper: " + mapper.getClass());
                }

                String name = mapper.mapCertificate(chain);
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "The custom X.509 certificate mapper returned the following mapping: " + name);
                }

                if (name == null || !isValidUser(name)) {
                    String msg = Tr.formatMessage(tc, "BASIC_REGISTRY_MAPPED_NAME_NOT_FOUND", name);
                    throw new CertificateMapFailedException(msg);
                }
                return name;

            } catch (com.ibm.websphere.security.CertificateMapNotSupportedException e) {
                String msg = Tr.formatMessage(tc, "BASIC_REGISTRY_CUSTOM_MAPPER_NOT_SUPPORTED");
                throw new CertificateMapNotSupportedException(msg, e);
            } catch (com.ibm.websphere.security.CertificateMapFailedException e) {
                String msg = Tr.formatMessage(tc, "BASIC_REGISTRY_CUSTOM_MAPPER_FAILED");
                throw new CertificateMapFailedException(msg, e);
            }

        } else {

            if (chain == null || chain.length == 0 || chain[0] == null) {
                throw new IllegalArgumentException("cert is null");
            }

            // getSubjectDN is denigrated
            String dn = chain[0].getSubjectX500Principal().getName();
            String name = LDAPUtils.getCNFromDN(dn);
            if (name == null || !isValidUser(name)) {
                String msg = Tr.formatMessage(tc, "BASIC_REGISTRY_NAME_NOT_FOUND", dn);
                throw new CertificateMapFailedException(msg);
            }
            return name;

        }

    }

    /** {@inheritDoc} */
    @Override
    public boolean isValidUser(String userSecurityName) throws RegistryException {
        if (userSecurityName == null) {
            throw new IllegalArgumentException("userSecurityName is null");
        }
        String userName = userSecurityName.trim();
        if (userName.isEmpty()) {
            throw new IllegalArgumentException("userSecurityName is an empty String");
        }
        if (state.ignoreCaseForAuthentication) {
            for (Map.Entry<String, BasicPassword> entry : state.users.entrySet()) {
                String keyUserName = entry.getKey();
                if (keyUserName.equalsIgnoreCase(userSecurityName)) {
                    return true;
                }
            }
            return false;
        }
        return state.users.containsKey(userName);
    }

    /** {@inheritDoc} */
    @Override
    public SearchResult getUsers(String pattern, int limit) throws RegistryException {
        // support ignoreCaseForAuthentication for user lookup
        if (state.ignoreCaseForAuthentication) {
            pattern = "(?i)" + pattern;
        }
        return searchMap(state.users, pattern, limit);
    }

    /**
     * Convert the "command line" style pattern into a regular expression.
     * The conversion is as follows:
     * <ul>
     * <li>* -> .*</li>
     * <li>? -> .</li>
     * </ul>
     *
     * @param pattern
     * @return regular expression
     */
    private String convertToRegex(String pattern) {
        return pattern.replace("*", ".*");
    }

    /**
     * Finds key entries in the specified Map<String,?> which match the specified pattern.
     *
     * @see #getUsers(String, int)
     * @see #getGroups(String, int)
     * @param pattern pattern to match
     * @param limit limit of entries to return
     * @return a SearchResult object
     */
    private SearchResult searchMap(Map<String, ?> map, String pattern, int limit) {

        if (pattern == null) {
            throw new IllegalArgumentException("pattern is null");
        }
        if (pattern.isEmpty()) {
            throw new IllegalArgumentException("pattern is an empty String");
        }

        String regexPattern = convertToRegex(pattern);

        if (limit < 0) {
            return new SearchResult();
        }
        if (map.size() == 0) {
            return new SearchResult();
        }
        int count = 0;
        // Set the stopping point 1 past our limit. If we reach
        // this point, then we know there are more entries than
        // limit which match pattern, so we can say hasMore is
        // true. We have to keep trying to match 1 past the limit
        // because if we stop at limit, we can't be sure the
        // other entries will match pattern.
        int stoppingPoint = (limit == 0) ? 0 : limit + 1;
        boolean hasMore = false;
        List<String> matched = new ArrayList<String>();
        Set<String> userNames = map.keySet();
        Iterator<String> itr = userNames.iterator();
        while (itr.hasNext()) {
            String name = itr.next();
            if (name.matches(regexPattern)) {
                matched.add(name);
                count++;
                if (count == stoppingPoint) {
                    matched.remove(name);
                    hasMore = true;
                    break;
                }
            }
        }

        if (count > 0) {
            return new SearchResult(matched, hasMore);
        } else {
            return new SearchResult();
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getUserDisplayName(String userSecurityName) throws EntryNotFoundException, RegistryException {
        if (userSecurityName == null) {
            throw new IllegalArgumentException("userSecurityName is null");
        }
        if (userSecurityName.isEmpty()) {
            throw new IllegalArgumentException("userSecurityName is an empty String");
        }

        if (!isValidUser(userSecurityName)) {
            throw new EntryNotFoundException(userSecurityName + " does not exist");
        }
        return userSecurityName;
    }

    /** {@inheritDoc} */
    @Override
    public String getUniqueUserId(String userSecurityName) throws EntryNotFoundException, RegistryException {
        if (userSecurityName == null) {
            throw new IllegalArgumentException("userSecurityName is null");
        }
        if (userSecurityName.isEmpty()) {
            throw new IllegalArgumentException("userSecurityName is an empty String");
        }

        if (!isValidUser(userSecurityName)) {
            throw new EntryNotFoundException(userSecurityName + " does not exist");
        }
        return userSecurityName;
    }

    /** {@inheritDoc} */
    @Override
    public String getUserSecurityName(String uniqueUserId) throws EntryNotFoundException, RegistryException {
        if (uniqueUserId == null) {
            throw new IllegalArgumentException("uniqueUserId is null");
        }
        if (uniqueUserId.isEmpty()) {
            throw new IllegalArgumentException("uniqueUserId is an empty String");
        }

        if (!isValidUser(uniqueUserId)) {
            throw new EntryNotFoundException(uniqueUserId + " does not exist");
        }
        return uniqueUserId;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isValidGroup(String groupSecurityName) throws RegistryException {
        if (groupSecurityName == null) {
            throw new IllegalArgumentException("groupSecurityName is null");
        }
        if (groupSecurityName.isEmpty()) {
            throw new IllegalArgumentException("groupSecurityName is an empty String");
        }

        return state.groups.containsKey(groupSecurityName);
    }

    /** {@inheritDoc} */
    @Override
    public SearchResult getGroups(String pattern, int limit) throws RegistryException {
        return searchMap(state.groups, pattern, limit);
    }

    /** {@inheritDoc} */
    @Override
    public String getGroupDisplayName(String groupSecurityName) throws EntryNotFoundException, RegistryException {
        if (groupSecurityName == null) {
            throw new IllegalArgumentException("groupSecurityName is null");
        }
        if (groupSecurityName.isEmpty()) {
            throw new IllegalArgumentException("groupSecurityName is an empty String");
        }

        if (!isValidGroup(groupSecurityName)) {
            throw new EntryNotFoundException(groupSecurityName + " does not exist");
        }
        return groupSecurityName;
    }

    /** {@inheritDoc} */
    @Override
    public String getUniqueGroupId(String groupSecurityName) throws EntryNotFoundException, RegistryException {
        if (groupSecurityName == null) {
            throw new IllegalArgumentException("groupSecurityName is null");
        }
        if (groupSecurityName.isEmpty()) {
            throw new IllegalArgumentException("groupSecurityName is an empty String");
        }

        if (!isValidGroup(groupSecurityName)) {
            throw new EntryNotFoundException(groupSecurityName + " does not exist");
        }
        return groupSecurityName;
    }

    /** {@inheritDoc} */
    @Override
    public String getGroupSecurityName(String uniqueGroupId) throws EntryNotFoundException, RegistryException {
        if (uniqueGroupId == null) {
            throw new IllegalArgumentException("uniqueGroupId is null");
        }
        if (uniqueGroupId.isEmpty()) {
            throw new IllegalArgumentException("uniqueGroupId is an empty String");
        }

        if (!isValidGroup(uniqueGroupId)) {
            throw new EntryNotFoundException(uniqueGroupId + " does not exist");
        }
        return uniqueGroupId;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getUniqueGroupIdsForUser(String uniqueUserId) throws EntryNotFoundException, RegistryException {
        return getGroupsForUser(uniqueUserId);
    }

    /** {@inheritDoc} */
    //TODO this doesn't work for case insensitive
    @Override
    public List<String> getGroupsForUser(String userSecurityName) throws EntryNotFoundException, RegistryException {
        if (userSecurityName == null) {
            throw new IllegalArgumentException("userSecurityName is null");
        }
        if (userSecurityName.isEmpty()) {
            throw new IllegalArgumentException("userSecurityName is an empty String");
        }

        if (!isValidUser(userSecurityName)) {
            throw new EntryNotFoundException(userSecurityName + " does not exist");
        }

        List<String> matched = new ArrayList<String>();
        Set<String> groupNames = state.groups.keySet();
        Iterator<String> itr = groupNames.iterator();
        while (itr.hasNext()) {
            String groupName = itr.next();
            if (state.groups.get(groupName).contains(userSecurityName)) {
                matched.add(groupName);
            }
        }

        return matched;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.registry.UserRegistry#getUsersForGroup(java.lang.String, int)
     */

    @Override
    public SearchResult getUsersForGroup(String groupSecurityName,
                                         int limit) throws NotImplementedException, EntryNotFoundException, CustomRegistryException, RemoteException, RegistryException {
        if (groupSecurityName == null) {
            throw new IllegalArgumentException("groupSecurityName is null");
        }
        if (groupSecurityName.isEmpty()) {
            throw new IllegalArgumentException("groupSecurityName is an empty String");
        }
        if (limit < 0) {
            throw new IllegalArgumentException("limit is less than zero");
        }

        if (!isValidGroup(groupSecurityName)) {
            throw new EntryNotFoundException(groupSecurityName + " does not exist");
        }

        List<String> members = new ArrayList<String>(state.groups.get(groupSecurityName));

        if (limit == 0) {
            return new SearchResult(members, Boolean.FALSE);
        } else {
            Iterator<String> iter = members.iterator();
            int numberToReturn = 0;
            List<String> limitMembers = new ArrayList<String>();
            while (iter.hasNext() && numberToReturn < limit) {
                numberToReturn++;
                limitMembers.add(iter.next());
            }
            if (iter.hasNext()) {
                return new SearchResult(limitMembers, Boolean.TRUE);
            } else {
                return new SearchResult(limitMembers, Boolean.FALSE);
            }
        }

    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, target = "(id=unbound)")
    protected void setCertificateMapper(X509CertificateMapper reference) {
        iCertificateMapperRef.set(reference);
    }

    protected void unsetCertificateMapper(X509CertificateMapper reference) {
        iCertificateMapperRef.compareAndSet(reference, null);
    }
}
