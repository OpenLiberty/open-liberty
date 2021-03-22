/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.internal;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.joda.time.DateTime;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.EntryNotFoundException;
import com.ibm.websphere.security.UserRegistry;
import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.saml2.Saml20Attribute;
import com.ibm.websphere.security.saml2.Saml20Token;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.SsoRequest;
import com.ibm.ws.security.saml.TraceConstants;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.internal.utils.HashUtils;
import com.ibm.ws.security.saml.sso20.internal.utils.SamlUtil;
import com.ibm.ws.security.saml.sso20.token.Saml20TokenImpl;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.security.saml2.UserCredentialResolver;
import com.ibm.wsspi.security.saml2.UserIdentityException;

/**
 *
 */
public class AssertionToSubject {
    public static final TraceComponent tc = Tr.register(AssertionToSubject.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    SsoConfig ssoConfig = null;
    Saml20Token token = null;
    SsoRequest samlRequest = null;

    static public final String KEY_USER_RESOLVER = "userResolver";
    static ConcurrentServiceReferenceMap<String, UserCredentialResolver> activatedUserResolverRef = new ConcurrentServiceReferenceMap<String, UserCredentialResolver>(KEY_USER_RESOLVER);
    static final String fixedStr = "_ibm";

    public AssertionToSubject(SsoRequest samlRequest, SsoConfig ssoConfig, Saml20Token token) {
        this.ssoConfig = ssoConfig;
        this.token = token;
        this.samlRequest = samlRequest;
    }

    public static void setActivatedUserResolverRef(ConcurrentServiceReferenceMap<String, UserCredentialResolver> userResolverRef) {
        activatedUserResolverRef = userResolverRef;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "activatedUserResolverRef size():" + activatedUserResolverRef.size());
        }
    }

    public String getUser() throws SamlException {
        String user = null;
        if (activatedUserResolverRef.size() > 0) {
            user = getUserFromUserResolver(user);
            if (user != null && !user.isEmpty()) {
                return user;
            }
        }
        user = this.token.getSAMLNameID();

        String name = this.ssoConfig.getUserIdentifier();
        if (name != null && !name.isEmpty()) {
            user = null;
            List<Saml20Attribute> attributes = this.token.getSAMLAttributes();
            Iterator<Saml20Attribute> it = attributes.iterator();
            while (it.hasNext()) {
                Saml20Attribute attribute = it.next();
                if (name.equals(attribute.getName())) {
                    if (attribute.getValuesAsString().size() == 1) {
                        user = attribute.getValuesAsString().get(0);
                    }
                }
            }
            if (user == null) {
                //when the attribute can not find a value.
                throw new SamlException("SAML20_ATTRIBUTE_ERR",
                                //SAML20_NO_USER_IN_SAML=CWWKS5069E: No name Id was defined in the SAML attributes.
                                null, // cause
                                false, // FFDC is not handled yet
                                new Object[] { "NameID" });
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "user from Token Attributes:" + user);
            }
        }
        if (user == null || user.isEmpty()) {
            //when the attribute can not find a value.
            throw new SamlException("SAML20_ATTRIBUTE_ERR",
                            //SAML20_NO_VALUE_FOUND_FROM_SAML=CWWKS5071E: No [{0}] was found in the SAML token.
                            null, // cause
                            false, // FFDC is handled
                            new Object[] { "NameID" });
        }
        return user;
    }

    /**
     * @return
     * @throws SamlException
     */
    @FFDCIgnore({ UserIdentityException.class })
    String getUserFromUserResolver(String user) throws SamlException {
        String userid = null;
        Iterator<UserCredentialResolver> userIdResolvers = activatedUserResolverRef.getServices();
        if (userIdResolvers.hasNext()) {
            UserCredentialResolver userIdResolver = userIdResolvers.next();
            try {
                userid = userIdResolver.mapSAMLAssertionToUser(token);
            } catch (UserIdentityException e) {
                throw new SamlException("SAML20_CANNOT_RESOLVE_ASSERTION",
                                // SAML20_CANNOT_RESOLVE_ASSERTION=CWWKS5076E: The UserCredentialResolver fails to resolve the SAML Assertion and throws a UserIdentityException with message [{0}].
                                e, false, // ffdc is not hanlded yet
                                new Object[] { e });
            }
        }
        return userid;
    }

    public String getRealm() throws SamlException {
        String realm = null;
        if (activatedUserResolverRef.size() > 0) {
            realm = getRealmFromUserResolver();
            if (realm != null && !realm.isEmpty()) {
                return realm;
            }
        }
        realm = ssoConfig.getRealmName();
        if (realm != null && !realm.isEmpty()) {
            return realm;
        }
        realm = this.token.getSAMLIssuerName();
        String name = this.ssoConfig.getRealmIdentifier();
        if (name != null && !name.isEmpty()) {
            realm = null;
            List<Saml20Attribute> attributes = this.token.getSAMLAttributes();
            Iterator<Saml20Attribute> it = attributes.iterator();
            while (it.hasNext()) {
                Saml20Attribute attribute = it.next();
                if (name.equals(attribute.getName())) {
                    if (attribute.getValuesAsString().size() == 1) {
                        realm = attribute.getValuesAsString().get(0);
                    }
                }
            }
            if (realm == null) {
                //when the attribute can not find a value.
                throw new SamlException("SAML20_ATTRIBUTE_ERR",
                                // SAML20_NO_REALM_IN_SAML=CWWKS5068E: No realm was defined in the SAML attributes.
                                null, // cause
                                false, // FFDC is not handled yet
                                new Object[] { "realm" });
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "realm from Token Attributes:" + realm);
            }
        }
        if (realm == null) {
            //when the attribute can not find a value.
            throw new SamlException("SAML20_ATTRIBUTE_ERR",
                            //SAML20_NO_VALUE_FOUND_FROM_SAML=CWWKS5071E: No [{0}] was found in the SAML token.
                            null, // cause
                            false, // FFDC is not handled yet
                            new Object[] { "realm" });
        }
        return realm;
    }

    /**
     * @return
     * @throws SamlException
     */
    @FFDCIgnore({ UserIdentityException.class })
    String getRealmFromUserResolver() throws SamlException {
        String realm = null;
        Iterator<UserCredentialResolver> userIdResolvers = activatedUserResolverRef.getServices();
        if (userIdResolvers.hasNext()) {
            UserCredentialResolver userIdResolver = userIdResolvers.next();
            try {
                realm = userIdResolver.mapSAMLAssertionToRealm(token);
            } catch (UserIdentityException e) {
                throw new SamlException("SAML20_CANNOT_RESOLVE_ASSERTION",
                                // SAML20_CANNOT_RESOLVE_ASSERTION=CWWKS5076E: The UserCredentialResolver fails to resolve the SAML Assertion and throws a UserIdentityException with message [{0}].
                                e, new Object[] { e });
            }
        }
        return realm;
    }

    public String getUserUniqueIdentity(String user, String realm) throws SamlException {
        String uid = null;
        if (activatedUserResolverRef.size() > 0) {
            uid = getUserUniqueIDFromUserResolver(user);
            if (uid != null && !uid.isEmpty()) {
                return uid;
            }
        }
        uid = user;
        String name = this.ssoConfig.getUserUniqueIdentifier();
        if (name != null && !name.isEmpty()) {
            uid = null;
            List<Saml20Attribute> attributes = this.token.getSAMLAttributes();
            Iterator<Saml20Attribute> it = attributes.iterator();
            while (it.hasNext()) {
                Saml20Attribute attribute = it.next();
                if (name.equals(attribute.getName())) {
                    if (attribute.getValuesAsString().size() == 1) {
                        uid = attribute.getValuesAsString().get(0);
                    }
                }
            }
            if (uid == null) {
                //when the attribute can not find a value.
                throw new SamlException("SAML20_ATTRIBUTE_ERR",
                                // SAML20_NO_UNIQUE_ID_IN_SAML=CWWKS5070E: No unique Id was defined in the SAML attributes.
                                null, // cause
                                false, // FFDC is not handled yet
                                new Object[] { "unique nameID" });
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "unique nameId from Token Attributes:" + uid);
            }
        }
        if (uid == null) {
            //when the attribute can not find a value.
            throw new SamlException("SAML20_ATTRIBUTE_ERR",
                            //SAML20_NO_VALUE_FOUND_FROM_SAML=CWWKS5071E: No [{0}] was found in the SAML token.
                            null, // cause
                            false, // FFDC is not handled yet
                            new Object[] { "unique nameID" });
        }
        // work with tfim, tfim has to define
        //    realmIdentifier="com.ibm.wsspi.security.cred.realm"
        //     in order for its unique User ID to be the unique
        String prefix = "user:" + realm + "/";
        if (!uid.startsWith(prefix)) {
            int userIndex = uid.indexOf("/");
            if (uid.startsWith("user:") && userIndex > 0) {
                uid = prefix + uid.substring(userIndex + 1);
            } else {
                uid = prefix + uid;
            }
        }
        return uid;
    }

    /**
     * @param user
     * @return
     * @throws SamlException
     */
    @FFDCIgnore({ UserIdentityException.class })
    String getUserUniqueIDFromUserResolver(String user) throws SamlException {
        String uid = null;
        Iterator<UserCredentialResolver> userIdResolvers = activatedUserResolverRef.getServices();
        if (userIdResolvers.hasNext()) {
            UserCredentialResolver userIdResolver = userIdResolvers.next();
            try {
                uid = userIdResolver.mapSAMLAssertionToUserUniqueID(token);
            } catch (UserIdentityException e) {
                throw new SamlException("SAML20_CANNOT_RESOLVE_ASSERTION",
                                // SAML20_CANNOT_RESOLVE_ASSERTION=CWWKS5076E: The UserCredentialResolver fails to resolve the SAML Assertion and throws a UserIdentityException with message [{0}].
                                e, new Object[] { e });
            }
        }
        return uid;
    }

    public List<String> getGroupUniqueIdentityFromRegistry(String realm) throws WSSecurityException, RemoteException, SamlException {
        if (activatedUserResolverRef.size() > 0) {
            List<String> newGroups = getGroupsFromUserResolver();
            if (newGroups != null && newGroups.size() > 0) {
                // let map it to the userRegistry and return;
                return mapGroupsToUserRegistry(newGroups, realm);
            }
        }
        List<String> groups = new ArrayList<String>();
        String name = this.ssoConfig.getGroupIdentifier();
        if (name != null) {
            // to work with tfim, the SP configuration has to define
            //    realmIdentifier="com.ibm.wsspi.security.cred.realm"
            //     in order for its unique User ID to be the unique
            String idpRealmPrefix = "group:" + realm + "/";

            List<Saml20Attribute> attributes = this.token.getSAMLAttributes();
            Iterator<Saml20Attribute> it = attributes.iterator();
            while (it.hasNext()) {
                Saml20Attribute attribute = it.next();
                if (name.equals(attribute.getName())) { //consider multiple group attributes
                    if (!attribute.getValuesAsString().isEmpty()) {
                        Iterator<String> it2 = attribute.getValuesAsString().iterator();
                        while (it2.hasNext()) {
                            String idpGroup = it2.next();
                            mapGroupToUserRegistry(groups, idpGroup, idpRealmPrefix);
                        }
                    }
                }
            }
        }

        return groups;
    }

    @FFDCIgnore({ EntryNotFoundException.class })
    List<String> mapGroupToUserRegistry(List<String> groups, String origGroup, String origRealmPrefix) throws RemoteException, WSSecurityException {
        UserRegistry reg = com.ibm.wsspi.security.registry.RegistryHelper.getUserRegistry(null);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "UserRegistry:" + reg);
        }
        // strip off the prefix of "group:<idpDefineRealm>/" from the idpGroup if matches
        if (origGroup != null && origGroup.startsWith(origRealmPrefix)) {
            origGroup = origGroup.substring(origRealmPrefix.length());
        } else if (origGroup != null && origGroup.startsWith("group:")) {
            int groupIndex = origGroup.indexOf("/");
            if (groupIndex > 0) {
                origGroup = origGroup.substring(groupIndex + 1);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "original Group:" + origGroup);
        }
        try {
            String groupDN = reg.getUniqueGroupId(origGroup);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "groupDN from registry:" + groupDN);
            }
            //TODO when the attribute can not find a value...
            String group = origRealmPrefix + groupDN;
            groups.add(group);
            List<String> localGroups = reg.getUniqueGroupIds(groupDN);
            Iterator<String> it3 = localGroups.listIterator();
            while (it3.hasNext()) {
                groupDN = it3.next();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "groupDN from GroupIds:" + groupDN);
                }
                group = origRealmPrefix + groupDN;
                groups.add(group);
            }
        } catch (EntryNotFoundException e) {
            // Can not find the unique Group Entry
            // It's OK
        }
        return groups;
    }

    /**
     * @param groups
     * @param realm
     * @return
     * @throws WSSecurityException
     * @throws RemoteException
     */
    List<String> mapGroupsToUserRegistry(List<String> oldGroups, String realm) throws RemoteException, WSSecurityException {
        String origRealmPrefix = "group:" + realm + "/";
        List<String> groups = new ArrayList<String>();
        for (String group : oldGroups) {
            mapGroupToUserRegistry(groups, group, origRealmPrefix);
        }
        return groups;
    }

    /**
     * @return
     */
    @FFDCIgnore({ UserIdentityException.class })
    List<String> getGroupsFromUserResolver() throws SamlException {
        List<String> groups = null;
        Iterator<UserCredentialResolver> userIdResolvers = activatedUserResolverRef.getServices();
        if (userIdResolvers.hasNext()) {
            UserCredentialResolver userIdResolver = userIdResolvers.next();
            try {
                groups = userIdResolver.mapSAMLAssertionToGroups(token);
            } catch (UserIdentityException e) {
                throw new SamlException("SAML20_CANNOT_RESOLVE_ASSERTION",
                                // SAML20_CANNOT_RESOLVE_ASSERTION=CWWKS5076E: The UserCredentialResolver fails to resolve the SAML Assertion and throws a UserIdentityException with message [{0}].
                                e, new Object[] { e });
            }
        }
        return groups;
    }

    public List<String> getGroupUniqueIdentity(String realm) throws SamlException {
        ArrayList<String> groups = new ArrayList<String>();
        if (activatedUserResolverRef.size() > 0) {
            List<String> newGroups = getGroupsFromUserResolver();
            if (newGroups != null && newGroups.size() > 0) {
                // let map it to the userRegistry and return;
                String groupPrefix = "group:" + realm + "/";
                for (String group : newGroups) {
                    if (!group.startsWith("group:")) {
                        group = groupPrefix + group;
                    }
                    groups.add(group);
                }
                return groups;
            }
        }

        String name = this.ssoConfig.getGroupIdentifier();
        if (name != null && !name.isEmpty()) {
            // to work with tfim, the SP configuration has to define
            //    realmIdentifier="com.ibm.wsspi.security.cred.realm"
            //     in order for its unique User ID to be the unique
            String idpRealmPrefix = "group:" + realm + "/";
            List<Saml20Attribute> attributes = this.token.getSAMLAttributes();
            Iterator<Saml20Attribute> it = attributes.iterator();
            while (it.hasNext()) {
                Saml20Attribute attribute = it.next();
                if (name.equals(attribute.getName())) { //consider multiple group attributes
                    if (!attribute.getValuesAsString().isEmpty()) {
                        Iterator<String> it2 = attribute.getValuesAsString().iterator();
                        while (it2.hasNext()) {
                            String groupDN = it2.next();
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "groupDN from Token Attributes:" + groupDN);
                            }
                            String group = groupDN;
                            if (!groupDN.startsWith(idpRealmPrefix)) {
                                //our realm and tfim realms don't match
                                //group=group:idpRealm/group1
                                //take out the idpRealm and add our realm
                                int groupIndex = groupDN.indexOf("/");
                                if (groupDN.startsWith("group:") && groupIndex > 0) {
                                    group = idpRealmPrefix + groupDN.substring(groupIndex + 1);
                                } else {
                                    group = idpRealmPrefix + groupDN;
                                }
                            }
                            groups.add(group);
                        }
                    }
                    break;
                }
            }
        }
        return groups;
    }

    @Trivial
    public String getCustomCacheKeyValue(String providerName) {
        if (samlRequest.isDisableLtpaCookie()) {
            String preCookieValue = SamlUtil.generateRandom();
            samlRequest.setSpCookieValue(preCookieValue);
            return getAfterDigestValue(providerName, preCookieValue);
        } else {
            String value = providerName + "_" + SamlUtil.hash(token.getSAMLAsString());
            samlRequest.setSpCookieValue(value);
            return value;
        }
    }

    @Sensitive
    public static String getAfterDigestValue(String providerName, String preValue) {
        String preDigest = providerName + "_" + preValue + fixedStr;
        return HashUtils.digest(preDigest);
    }

    /**
     * @param hashtable
     * @param saml20Token
     */
    @Trivial
    public void handleSessionNotOnOrAfter(Hashtable<String, Object> hashtable, Saml20Token saml20Token) {
        if (samlRequest.isDisableLtpaCookie()) {
            long lSessionNotOnOrAfter = 0;
            if (saml20Token instanceof Saml20TokenImpl) {
                lSessionNotOnOrAfter = ((Saml20TokenImpl) saml20Token).getSessionNotOnOrAfter();
            }
            if (lSessionNotOnOrAfter == 0) {
                lSessionNotOnOrAfter = (new DateTime()).getMillis() + ssoConfig.getSessionNotOnOrAfter();
            }
            hashtable.put(Constants.SP_COOKIE_AND_SESSION_NOT_ON_OR_AFTER, Long.valueOf(lSessionNotOnOrAfter));
        }
    }

}
