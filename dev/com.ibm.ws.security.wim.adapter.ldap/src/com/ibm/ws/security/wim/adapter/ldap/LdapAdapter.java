/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim.adapter.ldap;

import static com.ibm.ws.security.wim.adapter.ldap.LdapConstants.LDAP_ALL_GROUP_MEMBERSHIP;
import static com.ibm.ws.security.wim.adapter.ldap.LdapConstants.LDAP_ATTR_IBM_ALL_GROUP;
import static com.ibm.ws.security.wim.adapter.ldap.LdapConstants.LDAP_ATTR_OBJECTCLASS;
import static com.ibm.ws.security.wim.adapter.ldap.LdapConstants.LDAP_ATTR_OBJECTCLASS_ARRAY;
import static com.ibm.ws.security.wim.adapter.ldap.LdapConstants.LDAP_ATTR_SYNTAX_GUID;
import static com.ibm.ws.security.wim.adapter.ldap.LdapConstants.LDAP_ATTR_SYNTAX_OCTETSTRING;
import static com.ibm.ws.security.wim.adapter.ldap.LdapConstants.LDAP_ATTR_SYNTAX_STRING;
import static com.ibm.ws.security.wim.adapter.ldap.LdapConstants.LDAP_ATTR_UNICODEPWD;
import static com.ibm.ws.security.wim.adapter.ldap.LdapConstants.LDAP_ATTR_USER_ACCOUNT_CONTROL;
import static com.ibm.ws.security.wim.adapter.ldap.LdapConstants.LDAP_ATTR_USER_PASSWORD;
import static com.ibm.ws.security.wim.adapter.ldap.LdapConstants.LDAP_DIRECT_GROUP_MEMBERSHIP;
import static com.ibm.ws.security.wim.adapter.ldap.LdapConstants.LDAP_DN;
import static com.ibm.wsspi.security.wim.SchemaConstants.DATA_TYPE_BASE_64_BINARY;
import static com.ibm.wsspi.security.wim.SchemaConstants.DATA_TYPE_BOOLEAN;
import static com.ibm.wsspi.security.wim.SchemaConstants.DATA_TYPE_DATE;
import static com.ibm.wsspi.security.wim.SchemaConstants.DATA_TYPE_DATE_TIME;
import static com.ibm.wsspi.security.wim.SchemaConstants.DATA_TYPE_IDENTIFIER_TYPE;
import static com.ibm.wsspi.security.wim.SchemaConstants.DATA_TYPE_INT;
import static com.ibm.wsspi.security.wim.SchemaConstants.DATA_TYPE_LANG_TYPE;
import static com.ibm.wsspi.security.wim.SchemaConstants.DATA_TYPE_LONG;
import static com.ibm.wsspi.security.wim.SchemaConstants.DATA_TYPE_STRING;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.rmi.RemoteException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.naming.InvalidNameException;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;
import javax.naming.directory.Attribute;
import javax.naming.directory.AttributeInUseException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.NoSuchAttributeException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.Subject;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.X509CertificateMapper;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.websphere.security.wim.ConfigConstants;
import com.ibm.websphere.security.wim.Service;
import com.ibm.websphere.security.wim.ras.WIMMessageHelper;
import com.ibm.websphere.security.wim.ras.WIMMessageKey;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.wim.BaseRepository;
import com.ibm.ws.security.wim.ConfiguredRepository;
import com.ibm.ws.security.wim.adapter.ldap.change.ChangeHandlerFactory;
import com.ibm.ws.security.wim.adapter.ldap.change.IChangeHandler;
import com.ibm.ws.security.wim.adapter.ldap.context.TimedDirContext;
import com.ibm.ws.security.wim.util.ControlsHelper;
import com.ibm.ws.security.wim.util.NodeHelper;
import com.ibm.ws.security.wim.util.SchemaConstantsInternal;
import com.ibm.ws.security.wim.util.UniqueIdGenerator;
import com.ibm.ws.security.wim.util.UniqueNameHelper;
import com.ibm.ws.security.wim.xpath.ParseException;
import com.ibm.ws.security.wim.xpath.TokenMgrError;
import com.ibm.ws.security.wim.xpath.WIMXPathInterpreter;
import com.ibm.ws.security.wim.xpath.ldap.util.LdapXPathTranslateHelper;
import com.ibm.ws.security.wim.xpath.mapping.datatype.PropertyNode;
import com.ibm.ws.security.wim.xpath.mapping.datatype.XPathNode;
import com.ibm.ws.ssl.optional.SSLSupportOptional;
import com.ibm.wsspi.security.wim.SchemaConstants;
import com.ibm.wsspi.security.wim.exception.AuthenticationNotSupportedException;
import com.ibm.wsspi.security.wim.exception.CertificateMapFailedException;
import com.ibm.wsspi.security.wim.exception.CertificateMapNotSupportedException;
import com.ibm.wsspi.security.wim.exception.ChangeControlException;
import com.ibm.wsspi.security.wim.exception.EntityHasDescendantsException;
import com.ibm.wsspi.security.wim.exception.EntityNotFoundException;
import com.ibm.wsspi.security.wim.exception.EntityTypeNotSupportedException;
import com.ibm.wsspi.security.wim.exception.InvalidEntityTypeException;
import com.ibm.wsspi.security.wim.exception.InvalidPropertyValueException;
import com.ibm.wsspi.security.wim.exception.MissingMandatoryPropertyException;
import com.ibm.wsspi.security.wim.exception.PasswordCheckFailedException;
import com.ibm.wsspi.security.wim.exception.SearchControlException;
import com.ibm.wsspi.security.wim.exception.UpdatePropertyException;
import com.ibm.wsspi.security.wim.exception.WIMApplicationException;
import com.ibm.wsspi.security.wim.exception.WIMException;
import com.ibm.wsspi.security.wim.exception.WIMSystemException;
import com.ibm.wsspi.security.wim.model.AncestorControl;
import com.ibm.wsspi.security.wim.model.CacheControl;
import com.ibm.wsspi.security.wim.model.ChangeControl;
import com.ibm.wsspi.security.wim.model.ChangeResponseControl;
import com.ibm.wsspi.security.wim.model.CheckGroupMembershipControl;
import com.ibm.wsspi.security.wim.model.CheckPointType;
import com.ibm.wsspi.security.wim.model.Context;
import com.ibm.wsspi.security.wim.model.Control;
import com.ibm.wsspi.security.wim.model.DeleteControl;
import com.ibm.wsspi.security.wim.model.DescendantControl;
import com.ibm.wsspi.security.wim.model.Entity;
import com.ibm.wsspi.security.wim.model.Group;
import com.ibm.wsspi.security.wim.model.GroupMemberControl;
import com.ibm.wsspi.security.wim.model.GroupMembershipControl;
import com.ibm.wsspi.security.wim.model.IdentifierType;
import com.ibm.wsspi.security.wim.model.LangType;
import com.ibm.wsspi.security.wim.model.LoginAccount;
import com.ibm.wsspi.security.wim.model.LoginControl;
import com.ibm.wsspi.security.wim.model.Person;
import com.ibm.wsspi.security.wim.model.PersonAccount;
import com.ibm.wsspi.security.wim.model.PropertyControl;
import com.ibm.wsspi.security.wim.model.Root;
import com.ibm.wsspi.security.wim.model.SearchControl;

@Component(configurationPolicy = ConfigurationPolicy.REQUIRE,
           configurationPid = "com.ibm.ws.security.registry.ldap.config",
           property = "service.vendor=IBM")
public class LdapAdapter extends BaseRepository implements ConfiguredRepository {

    /**
     * Register the class to trace service.
     */
    private final static TraceComponent tc = Tr.register(LdapAdapter.class);

    /**
     * The ID for the LDAP Repository.
     */
    private String reposId = null;

    /**
     * Name of the realm configured for the repository.
     */
    private String reposRealm = null;

    /**
     * The key to extract the id of the configuration. The value returned by this is used as the repository id.
     */
    private static final String KEY_ID = "config.id";

    /**
     * The key to extract the realm of the configuration. The value returned by this is used as the repository realm.
     */
    private static final String REALM = "realm";

    /**
     * The LDAP connection class. This class performs the actual LDAP operations.
     */
    protected LdapConnection iLdapConn = null;

    /**
     * The LDAP configuration manager class. This class is initialized with the configuration parameters. It provides utility
     * methods to use the configuration data.
     */
    private LdapConfigManager iLdapConfigMgr = null;

    /**
     * Change Handler
     */
    protected IChangeHandler changeHandler = null;

    /**
     * Is the configured repository an Active Directory
     */
    private boolean isActiveDirectory = false;

    /**
     * The {@link X509CertificateMapper} reference.
     */
    private final AtomicReference<X509CertificateMapper> iCertificateMapperRef = new AtomicReference<X509CertificateMapper>();

    @Activate
    protected void activated(Map<String, Object> properties, ComponentContext cc) throws WIMException {
        super.activate(properties, cc);
        initialize(properties);
    }

    @Modified
    protected void modified(Map<String, Object> properties) throws WIMException {
        super.modify(properties);
        initialize(properties);
    }

    @Override
    @Deactivate
    protected void deactivate(int reason, ComponentContext cc) {
        super.deactivate(reason, cc);
    }

    /**
     * Function that is invoked when the LdapAdapter is initialized.
     * It extracts the repository configuration details and sets itself up with the required properties.
     *
     * @param configProps
     */
    public void initialize(Map<String, Object> configProps) throws WIMException {
        reposId = (String) configProps.get(KEY_ID);
        reposRealm = (String) configProps.get(REALM);

        if (String.valueOf(configProps.get(ConfigConstants.CONFIG_PROP_SUPPORT_CHANGE_LOG)).equalsIgnoreCase(ConfigConstants.CONFIG_SUPPORT_CHANGE_LOG_NATIVE)) {
            //Construct a change handler depending on the type of LDAP repository
            changeHandler = ChangeHandlerFactory.getChangeHandler(iLdapConn);
        }

        iLdapConfigMgr = new LdapConfigManager();
        iLdapConfigMgr.initialize(configProps);

        iLdapConn = new LdapConnection(iLdapConfigMgr);
        iLdapConn.initialize(configProps);

        isActiveDirectory = iLdapConfigMgr.isActiveDirectory();
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    protected void setSSLSupport(SSLSupportOptional sslSupport) {}

    /**
     * Method to get the given Entity from the underlying repository
     *
     * @param root The incoming Root object.
     */
    @Override
    public Root get(Root root) throws WIMException {
        final String METHODNAME = "get";
        Root outRoot = new Root();

        Map<String, Control> ctrlMap = ControlsHelper.getControlMap(root);
        PropertyControl propertyCtrl = (PropertyControl) ctrlMap.get(SchemaConstants.DO_PROPERTY_CONTROL);
        GroupMembershipControl grpMbrshipCtrl = (GroupMembershipControl) ctrlMap.get(SchemaConstants.DO_GROUP_MEMBERSHIP_CONTROL);
        GroupMemberControl grpMbrCtrl = (GroupMemberControl) ctrlMap.get(SchemaConstants.DO_GROUP_MEMBER_CONTROL);
        AncestorControl ancesCtrl = (AncestorControl) ctrlMap.get(SchemaConstants.DO_ANCESTOR_CONTROL);
        DescendantControl descCtrl = (DescendantControl) ctrlMap.get(SchemaConstants.DO_DESCENDANT_CONTROL);
        CheckGroupMembershipControl chkGrpMbrshipCtrl = (CheckGroupMembershipControl) ctrlMap.get(SchemaConstants.DO_CHECK_GROUP_MEMBERSHIP_CONTROL);
        CacheControl cacheCtrl = (CacheControl) ctrlMap.get(SchemaConstants.DO_CACHE_CONTROL);

        boolean clearCacheForMembership = false;

        // If a cache control is passed, the cache should be cleared
        if (cacheCtrl != null) {
            String cacheMode = cacheCtrl.getMode();
            if (tc.isDebugEnabled())
                Tr.debug(tc, METHODNAME + " Cache Control is passed with mode " + cacheMode);

            // If the mode is 'clearAll', then invalidate the caches
            if (cacheMode != null && SchemaConstants.CACHE_MODE_CLEARALL.equalsIgnoreCase(cacheMode)) {
                // Invalidate the attributes cache
                iLdapConn.invalidateAttributeCache();

                // Invalidate the search cache
                iLdapConn.invalidateSearchCache();

                // Log this clearAll call
                String uniqueName = getCallerUniqueName();
                if (tc.isWarningEnabled())
                    Tr.warning(tc, WIMMessageKey.CLEAR_ALL_CLEAR_CACHE_MODE, WIMMessageHelper.generateMsgParms(reposId, cacheMode, uniqueName));
            }

            // If the mode is 'clearEntity', then invalidate the attribute cache for the DN
            else if (cacheMode != null && SchemaConstants.CACHE_MODE_CLEAR_ENTITY.equalsIgnoreCase(cacheMode)) {
                // Get the entities
                List<Entity> entities = root.getEntities();

                // Iterate over the entities and remove each one from the cache.
                for (Entity inEntity : entities) {
                    // Get the identifier
                    IdentifierType id = inEntity.getIdentifier();
                    String externalName = id.getExternalName();
                    String extId = id.getExternalId();
                    String uniqueName = id.getUniqueName();

                    // Invalidate attibutes
                    iLdapConn.invalidateAttributes(externalName, extId, uniqueName);
                }

                // Set the flag that would be used to determine if the search
                // cache needs to be cleared later, depending on whether
                // the user has passed the Group Member and/or Group Membership controls.
                clearCacheForMembership = true;
            } else if (tc.isWarningEnabled())
                Tr.warning(tc, WIMMessageKey.UNKNOWN_CLEAR_CACHE_MODE, WIMMessageHelper.generateMsgParms(reposId, cacheMode));
        }

        List<String> propNames = null;
        if (propertyCtrl != null) {
            propNames = propertyCtrl.getProperties();
        }
        boolean needMbrAttr = false;

        if (grpMbrCtrl != null
            && (iLdapConfigMgr.getMembershipAttribute() == null || !iLdapConfigMgr.isDefaultMbrAttr() || iLdapConfigMgr.isRacf())) {
            needMbrAttr = true;
        }

        for (Entity inEntity : root.getEntities()) {
            String inEntityType = inEntity.getTypeName();
            List<String> properties = iLdapConfigMgr.getSupportedProperties(inEntityType, propNames);

            List<String> EntityType = new ArrayList<String>(1);
            EntityType.add(inEntityType);

            IdentifierType inId = inEntity.getIdentifier();

            // Only retrieve member attr when there grpMbrCtrl is not null and no grpMbrship attr.
            LdapEntry ldapEntry = iLdapConn.getEntityByIdentifier(inId, EntityType, properties,
                                                                  (grpMbrshipCtrl != null || chkGrpMbrshipCtrl != null), needMbrAttr);

            // New:: Change to Input/Output property <<START>>
            if (ldapEntry != null && propNames != null) {
                if (propNames.contains(SchemaConstantsInternal.PROP_DISPLAY_BRIDGE_PRINCIPAL_NAME)) {
                    String type = ldapEntry.getType();
                    LdapEntity entity = iLdapConfigMgr.getLdapEntity(type);
                    String attrName = entity.getAttribute(SchemaConstants.PROP_PRINCIPAL_NAME);
                    String[] attrIds = new String[] { attrName };
                    ArrayList<String> entityTypes = new ArrayList<String>(1);
                    entityTypes.add(type);
                    Attributes attrs = iLdapConn.getAttributesByUniqueName(ldapEntry.getUniqueName(), attrIds, entityTypes);
                    if (attrs != null) {
                        NamingEnumeration<? extends Attribute> attEnum = attrs.getAll();
                        while (attEnum.hasMoreElements()) {
                            try {
                                ldapEntry.getAttributes().put(attEnum.next());
                            } catch (NamingException e) {
                                /* Ignore. */
                            }
                        }
                    }

                    if (properties != null)
                        properties.add(SchemaConstants.PROP_PRINCIPAL_NAME);
                    else {
                        properties = new ArrayList<String>();
                        properties.add(SchemaConstants.PROP_PRINCIPAL_NAME);
                    }
                } else if (propNames.contains(SchemaConstantsInternal.PROP_DISPLAY_BRIDGE_CN)) {
                    String type = ldapEntry.getType();
                    LdapEntity entity = iLdapConfigMgr.getLdapEntity(type);
                    String attrName = entity.getAttribute("cn");
                    String[] attrIds = new String[] { attrName };
                    ArrayList<String> entityTypes = new ArrayList<String>(1);
                    entityTypes.add(type);
                    Attributes attrs = iLdapConn.getAttributesByUniqueName(ldapEntry.getUniqueName(), attrIds, entityTypes);
                    if (attrs != null) {
                        NamingEnumeration<? extends Attribute> attEnum = attrs.getAll();
                        while (attEnum.hasMoreElements()) {
                            try {
                                ldapEntry.getAttributes().put(attEnum.next());
                            } catch (NamingException e) {
                                /* Ignore. */
                            }
                        }
                    }

                    if (properties != null)
                        properties.add("cn");
                    else {
                        properties = new ArrayList<String>();
                        properties.add("cn");
                    }
                }
            }
            // New:: Change to Input/Output property <<END>>

            Entity outEntity = createEntityFromLdapEntry(outRoot, SchemaConstants.DO_ENTITIES, ldapEntry, properties);

            // If Cache needs to be cleared
            if (clearCacheForMembership) {
                // If fetched entity is a group
                if (iLdapConfigMgr.isGroup(ldapEntry.getType())) {
                    // If group member control is passed, clear the search cache.
                    // This is done in order to correctly fetch members of dynamic groups.
                    if (grpMbrCtrl != null)
                        iLdapConn.invalidateSearchCache();
                    // If the membership control is passed, clear the search cache as the membership will be determined
                    // by a search
                    if ((grpMbrshipCtrl != null || chkGrpMbrshipCtrl != null))
                        iLdapConn.invalidateSearchCache();
                }
                // If fetched entity is a person or person account
                else if (iLdapConfigMgr.isPerson(ldapEntry.getType()) || iLdapConfigMgr.isPersonAccount(ldapEntry.getType())) {
                    // If the membership control is passed.
                    if ((grpMbrshipCtrl != null || chkGrpMbrshipCtrl != null)) {
                        // If membership attribute is not specified, clear the search cache.
                        if (iLdapConfigMgr.getMembershipAttribute() != null) {
                            String mbrshipAttrName = iLdapConfigMgr.getMembershipAttribute();
                            Attribute mbrshipAttr = ldapEntry.getAttributes().get(mbrshipAttrName);
                            try {
                                // If membership attr is not found
                                if (mbrshipAttr == null || (mbrshipAttr.size() == 1 && mbrshipAttr.get(0) == null)) {
                                    iLdapConn.invalidateSearchCache();
                                }

                                // If membership attr does not include dynamic groups i.e. the membership attribute is defined but its scope is not "All"
                                // or if the membership attribute does not support dynamic groups, then the search results cache need to be cleared.
                                if (iLdapConfigMgr.getMembershipAttributeScope() != LDAP_ALL_GROUP_MEMBERSHIP && iLdapConfigMgr.supportDynamicGroup()) {
                                    iLdapConn.invalidateSearchCache();
                                }
                            } catch (NamingException e) {
                                // If there was any exception in fetching the attributes, clear the search results cache.
                                iLdapConn.invalidateSearchCache();
                            }
                        } else {
                            // If membership attribute is not defined, then clear the search results cache.
                            iLdapConn.invalidateSearchCache();
                        }
                    }
                }
            }

            // Retrieve groups
            getGroups(outEntity, ldapEntry, grpMbrshipCtrl);

            // Retrieve members if this is a group
            if (iLdapConfigMgr.isGroup(ldapEntry.getType())) {
                getMembers(outEntity, ldapEntry, grpMbrCtrl);
            }

            // Retrieve descendants
            getDescendants(outEntity, ldapEntry, descCtrl);

            // Retrieve ancestors
            getAncestors(outEntity, ldapEntry, ancesCtrl);

            if (chkGrpMbrshipCtrl != null) {
                int level = chkGrpMbrshipCtrl.getLevel();
                CheckGroupMembershipControl reChkCtrl = new CheckGroupMembershipControl();
                reChkCtrl.setInGroup(isMemberInGroup(inEntity, ldapEntry, level));
            }
        }

        return outRoot;
    }

    /**
     * Method to login for the given PersonAccount object.
     *
     * @param root The incoming Root object.
     */
    @Override
    @FFDCIgnore({ EntityNotFoundException.class, InvalidNameException.class, NamingException.class })
    public Root login(Root root) throws WIMException {
        List<Entity> entities = root.getEntities();
        LoginAccount inAccount = (LoginAccount) entities.get(0);
        String qName = inAccount.getTypeName();
        Map<String, Control> ctrlMap = ControlsHelper.getControlMap(root);
        LoginControl loginCtrl = (LoginControl) ctrlMap.get(SchemaConstants.DO_LOGIN_CONTROL);
        if (loginCtrl == null) {
            loginCtrl = new LoginControl();
        }
        LdapSearchControl srchCtrl = null;

        String principalName = inAccount.getPrincipalName();

        if (principalName != null)
            principalName = principalName.replace("*", "\\*");

        byte[] pwd = inAccount.getPassword();
        List<byte[]> certList = inAccount.getCertificate();
        int certListSize = certList.size();

        LdapEntry acctEntry = null;

        Root outRoot = new Root();
        if (certListSize > 0) {
            X509Certificate[] certs = new X509Certificate[certListSize];
            try {
                for (int i = 0; i < certs.length; i++) {
                    ByteArrayInputStream bais = new ByteArrayInputStream(certList.get(i));
                    CertificateFactory cf = CertificateFactory.getInstance("X.509");
                    certs[i] = (X509Certificate) cf.generateCertificate(bais);
                    bais.close();
                }
            } catch (IOException e) {
                String msg = Tr.formatMessage(tc, WIMMessageKey.CERTIFICATE_MAP_FAILED, (Object) null);
                throw new CertificateMapFailedException(WIMMessageKey.CERTIFICATE_MAP_FAILED, msg);
            } catch (CertificateException e) {
                String msg = Tr.formatMessage(tc, WIMMessageKey.CERTIFICATE_MAP_FAILED, (Object) null);
                throw new CertificateMapFailedException(WIMMessageKey.CERTIFICATE_MAP_FAILED, msg);
            }
            srchCtrl = getLdapSearchControl(loginCtrl, false, false);
            acctEntry = mapCertificate(certs, srchCtrl);
            if (acctEntry == null)
                return outRoot;
        } else {
            if (principalName == null || principalName.trim().isEmpty()) {
                String msg = Tr.formatMessage(tc, WIMMessageKey.MISSING_OR_EMPTY_PRINCIPAL_NAME, (Object) null);
                throw new PasswordCheckFailedException(WIMMessageKey.MISSING_OR_EMPTY_PRINCIPAL_NAME, msg);
            }
            if (pwd == null || pwd.length == 0) {
                String msg = Tr.formatMessage(tc, WIMMessageKey.MISSING_OR_EMPTY_PASSWORD, (Object) null);
                throw new PasswordCheckFailedException(WIMMessageKey.MISSING_OR_EMPTY_PASSWORD, msg);
            }

            String quote = "'";
            if (principalName.indexOf("'") != -1) {
                quote = "\"";
            }

            String searchExpr = "@xsi:type=" + quote + qName + quote + " and "
                                + SchemaConstants.PROP_PRINCIPAL_NAME + "=" + quote + principalName + quote;

            loginCtrl.setExpression(searchExpr);
            srchCtrl = getLdapSearchControl(loginCtrl, false, false);
            String[] searchBases = srchCtrl.getBases();
            String sFilter = srchCtrl.getFilter();

            if (iLdapConfigMgr.getUseEncodingInSearchExpression() != null)
                sFilter = LdapHelper.encodeAttribute(sFilter, iLdapConfigMgr.getUseEncodingInSearchExpression());

            //Check is input principalName is DN, if it is DN use the same for login otherwise use userFilter
            String principalNameDN = null;
            try {
                principalNameDN = new LdapName(principalName).toString();
            } catch (InvalidNameException e) {
                e.getMessage();
            }
            Filter userFilter = iLdapConfigMgr.getUserFilter();
            if (principalNameDN == null && userFilter != null) {
                sFilter = userFilter.prepare(principalName);
                sFilter = setAttributeNamesInFilter(sFilter, SchemaConstants.DO_PERSON_ACCOUNT);
            }

            int countLimit = srchCtrl.getCountLimit();
            int timeLimit = srchCtrl.getTimeLimit();
            List<String> entityTypes = srchCtrl.getEntityTypes();
            List<String> propNames = srchCtrl.getPropertyNames();
            // Add login properties to search
            propNames.addAll(iLdapConfigMgr.getLoginProperties());
            int scope = srchCtrl.getScope();

            int count = 0;
            for (int i = 0; i < searchBases.length; i++) {
                try {
                    Set<LdapEntry> ldapEntries = iLdapConn.searchEntities(searchBases[i], sFilter, null, scope, entityTypes,
                                                                          propNames, false, false, countLimit, timeLimit);
                    if (ldapEntries.size() > 1) {
                        // TODO : Add MULTIPLE_PRINCIPALS_FOUND to LdapUtilMesssages.nlsprops and uncomment below if loop
                        /*
                         * if(tc.isErrorEnabled()){
                         * Tr.error(tc, WIMMessageKey.MULTIPLE_PRINCIPALS_FOUND, WIMMessageHelper.generateMsgParms(principalName));
                         * }
                         */
                        String msg = Tr.formatMessage(tc, WIMMessageKey.MULTIPLE_PRINCIPALS_FOUND, WIMMessageHelper.generateMsgParms(principalName));
                        throw new PasswordCheckFailedException(WIMMessageKey.MULTIPLE_PRINCIPALS_FOUND, msg);
                    } else if (ldapEntries.size() == 1) {
                        if (count == 0) {
                            acctEntry = ldapEntries.iterator().next();
                        }
                        count++;
                        if (count > 1) {
                            //Uncomment this when MULTIPLE_PRINCIPALS_FOUND is added to LdapUtilMesssages.nlsprops
                            /*
                             * if(tc.isErrorEnabled()){
                             * Tr.error(tc, WIMMessageKey.MULTIPLE_PRINCIPALS_FOUND, WIMMessageHelper.generateMsgParms(principalName));
                             * }
                             */
                            String msg = Tr.formatMessage(tc, WIMMessageKey.MULTIPLE_PRINCIPALS_FOUND, WIMMessageHelper.generateMsgParms(principalName));
                            throw new PasswordCheckFailedException(WIMMessageKey.MULTIPLE_PRINCIPALS_FOUND, msg);
                        }
                    }
                } catch (EntityNotFoundException e) {
                    continue;
                }
            }
            if (acctEntry == null) {
                return outRoot;
            }
            String dn = acctEntry.getDN();

            if (!iLdapConn.isIgnoreCase()) {
                boolean foundMatch = false;
                Attributes attrs = acctEntry.getAttributes();
                List<String> loginProperties = iLdapConfigMgr.getLoginAttributes();

                for (String loginProp : loginProperties) {
                    try {
                        Attribute loginAttr = attrs.get(loginProp);
                        if (loginAttr != null) {
                            String attributeValue = String.valueOf(loginAttr.get());
                            if (principalName.equals(attributeValue)) {
                                foundMatch = true;
                                break;
                            }
                        } else {
                            foundMatch = true;
                            break;
                        }
                    } catch (NamingException e) {
                        continue;
                    }
                }

                if (!foundMatch)
                    return outRoot;
            }

            authenticateWithPassword(dn, pwd, principalName);
        }

        createEntityFromLdapEntry(outRoot, SchemaConstants.DO_ENTITIES, acctEntry, loginCtrl.getProperties());

        return outRoot;
    }

    /**
     * Method to perform a search on the underlying repository based on the specified search criteria
     *
     * @param root The incoming Root object.
     */
    @Override
    @FFDCIgnore(InvalidNameException.class)
    public Root search(Root root) throws WIMException {
        final String METHODNAME = "search";
        boolean bFirstChangeSearchCall = false;

        String searchExpr = null;

        Map<String, Control> ctrlMap = ControlsHelper.getControlMap(root);

        // Extract the cache control
        CacheControl cacheCtrl = (CacheControl) ctrlMap.get(SchemaConstants.DO_CACHE_CONTROL);

        // If a cache control is passed, the cache should be cleared
        if (cacheCtrl != null) {
            String cacheMode = cacheCtrl.getMode();
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, METHODNAME + " Cache Control is passed with mode " + cacheMode);
            }

            // If the mode is 'clearAll', then invalidate the caches
            if (cacheMode != null && SchemaConstants.CACHE_MODE_CLEARALL.equalsIgnoreCase(cacheMode)) {
                // Invalidate the attributes cache
                iLdapConn.invalidateAttributeCache();

                // Invalidate the search cache
                iLdapConn.invalidateSearchCache();

                // Log this clearAll call
                String uniqueName = getCallerUniqueName();
                if (tc.isWarningEnabled())
                    Tr.warning(tc, WIMMessageKey.CLEAR_ALL_CLEAR_CACHE_MODE, WIMMessageHelper.generateMsgParms(reposId, cacheMode, uniqueName));
            }
            // If the mode is 'clearEntity', then log it as not supported
            else if (cacheMode != null && SchemaConstants.CACHE_MODE_CLEAR_ENTITY.equalsIgnoreCase(cacheMode)) {
                if (tc.isWarningEnabled())
                    Tr.warning(tc, WIMMessageKey.UNSUPPORTED_CLEAR_CACHE_MODE, WIMMessageHelper.generateMsgParms(reposId, cacheMode));
            } else if (tc.isWarningEnabled())
                Tr.warning(tc, WIMMessageKey.UNKNOWN_CLEAR_CACHE_MODE, WIMMessageHelper.generateMsgParms(reposId, cacheMode));
        }

        // Check whether this is a search for changed entities
        boolean bChangeSearch = true;
        SearchControl searchControl = (ChangeControl) ctrlMap.get(SchemaConstants.DO_CHANGE_CONTROL);
        if (searchControl == null) {
            bChangeSearch = false;
            searchControl = (SearchControl) ctrlMap.get(SchemaConstants.DO_SEARCH_CONTROL);
        } else {
            List<CheckPointType> checkpoint = ((ChangeControl) searchControl).getCheckPoint();
            if (checkpoint.size() == 0) {
                bFirstChangeSearchCall = true;
            }
        }
        if (searchControl != null) {
            searchExpr = searchControl.getExpression();
            if (!bFirstChangeSearchCall) {
                if (searchExpr == null || searchExpr.length() == 0) {
                    throw new SearchControlException(WIMMessageKey.MISSING_SEARCH_EXPRESSION, Tr.formatMessage(
                                                                                                               tc,
                                                                                                               WIMMessageKey.MISSING_SEARCH_EXPRESSION,
                                                                                                               null));
                }
            }
        }
        // Check if the base entries were specified by the client
        // If specified by client, then don't override them with entity search base
        Set<String> realms = getSpecifiedRealms(root);
        boolean isSearchBaseSetByClient = realms.contains("n/a"); // Indicates search bases were set by client

        // read the custom property allowDNPrincipalNameAsLiteral
        String customProperty = getContextProperty(root, SchemaConstants.ALLOW_DN_PRINCIPALNAME_AS_LITERAL);
        boolean ignoreDNBaseSearch = customProperty.equalsIgnoreCase("true");

        LdapSearchControl srchCtrl = getLdapSearchControl(searchControl, isSearchBaseSetByClient, ignoreDNBaseSearch);

        String[] searchBases = srchCtrl.getBases();
        String sFilter = srchCtrl.getFilter();
        int countLimit = srchCtrl.getCountLimit();
        int timeLimit = srchCtrl.getTimeLimit();
        List<String> entityTypes = srchCtrl.getEntityTypes();
        List<String> propNames = srchCtrl.getPropertyNames();
        int scope = srchCtrl.getScope();

        Root outRoot = new Root();

        if (bChangeSearch) {

            ChangeControl changeControl = (ChangeControl) searchControl;

            /*
             * It is possible that we retrieve changes since the specified checkpoint and
             * while we process them more changes occur in the repository. As a result if
             * current checkpoint is retrieved after processing the changed entries, we
             * may lose some changes. Hence, we need to first retrieve the current checkpoint
             * and then process changed entities.
             */

            // Get the current checkpoint
            String checkPoint = changeHandler.getCurrentCheckPoint();

            CheckPointType currCheckPointDO = new CheckPointType();
            currCheckPointDO.setRepositoryId(reposId);
            currCheckPointDO.setRepositoryCheckPoint(checkPoint);

            ChangeResponseControl changeResponseCtrl = new ChangeResponseControl();
            changeResponseCtrl.getCheckPoint().add(currCheckPointDO);

            // Get changed entities since the checkpoint specified in ChangeControl
            List<CheckPointType> checkPointList = changeControl.getCheckPoint();

            if ((checkPointList != null) && (checkPointList.size() > 0)) {
                /*
                 * At the adapter level, only the checkpoint corresponding to this
                 * repository is expected to be in the ChangeControl. Hence no need
                 * to check for repository ID
                 */
                CheckPointType checkPointDO = checkPointList.get(0);
                checkPoint = checkPointDO.getRepositoryCheckPoint();
                if (checkPoint == null) {
                    throw new ChangeControlException(WIMMessageKey.NULL_CHECKPOINT_VALUE, Tr.formatMessage(
                                                                                                           tc,
                                                                                                           WIMMessageKey.NULL_CHECKPOINT_VALUE,
                                                                                                           null));
                }

                boolean isAttrCacheNotEmpty = true;
                List<String> changeTypes = changeControl.getChangeTypes();
                for (int i = 0; i < searchBases.length; i++) {
                    List<LdapEntry> ldapEntriesList = changeHandler.searchChangedEntities(checkPoint, changeTypes, searchBases[i],
                                                                                          sFilter, scope, entityTypes, propNames, countLimit, timeLimit);

                    if (ldapEntriesList != null) {
                        for (int j = 0; j < ldapEntriesList.size(); j++) {
                            LdapEntry entry = ldapEntriesList.get(j);
                            // invalidating the attribute cache
                            // checking for active directory so as to check if it is a deleted change type the fetch the DN from the unique name
                            if (isActiveDirectory) {
                                if (SchemaConstants.CHANGETYPE_DELETE.equalsIgnoreCase(entry.getChangeType())) {
                                    iLdapConn.invalidateAttributes(iLdapConfigMgr.switchToLdapNode(entry.getUniqueName()), entry.getExtId(), entry.getUniqueName());
                                } else {
                                    iLdapConn.invalidateAttributes(entry.getDN(), entry.getExtId(), entry.getUniqueName());
                                }
                            } else {
                                if (isAttrCacheNotEmpty) {//checking if attribute cache is invalidated.
                                    if (entry.getExtId() == null && SchemaConstants.CHANGETYPE_DELETE.equalsIgnoreCase(entry.getChangeType())) {
                                        iLdapConn.invalidateAttributeCache();// clear the cache if extId of deleted entry is not known
                                        isAttrCacheNotEmpty = false;
                                    } else {
                                        iLdapConn.invalidateAttributes(entry.getDN(), entry.getExtId(), entry.getUniqueName());
                                    }
                                }
                            }
                            createEntityFromLdapEntry(outRoot, SchemaConstants.DO_ENTITIES, entry, propNames);
                        }
                        // invalidating the search cache
                        if (ldapEntriesList.size() > 0) {
                            iLdapConn.invalidateSearchCache();
                        }
                    }
                }
            }
        } else {
            if (root.isSetContexts()) {
                String inputPattern = getContextProperty(root, SchemaConstants.USE_USER_FILTER_FOR_SEARCH);
                if (inputPattern != null && inputPattern.length() > 0) {
                    String dn = null;
                    try {
                        dn = new LdapName(inputPattern).toString();
                    } catch (InvalidNameException e) {
                        e.getMessage();
                    }
                    if (dn == null) {
                        Filter f = iLdapConfigMgr.getUserFilter();
                        if (f != null)
                            sFilter = f.prepare(inputPattern);
                    }
                } else {
                    // Check if group filter pattern is specified
                    inputPattern = getContextProperty(root, SchemaConstants.USE_GROUP_FILTER_FOR_SEARCH);
                    if (inputPattern != null && inputPattern.length() > 0) {
                        String dn = null;
                        try {
                            dn = new LdapName(inputPattern).toString();
                        } catch (InvalidNameException e) {
                            e.getMessage();
                        }
                        if (dn == null) {
                            Filter f = iLdapConfigMgr.getGroupFilter();
                            if (f != null)
                                sFilter = f.prepare(inputPattern);
                        }
                    }
                }
            }

            for (int i = 0; i < searchBases.length; i++) {
                Set<LdapEntry> ldapEntriesSet = iLdapConn.searchEntities(searchBases[i], sFilter, null, scope, entityTypes, propNames,
                                                                         false, false, countLimit, timeLimit);
                for (LdapEntry entry : ldapEntriesSet) {
                    createEntityFromLdapEntry(outRoot, SchemaConstants.DO_ENTITIES, entry, propNames);
                }
            }
        }

        return outRoot;
    }

    /**
     * Helper function returns the caller's unique name.
     * Created to use for logging the calls to clear cache with clearAll mode.
     *
     * @return Caller's principalName
     * @throws WIMApplicationException
     */
    private String getCallerUniqueName() throws WIMApplicationException {
        String uniqueName = null;
        Subject subject = null;
        WSCredential cred = null;

        try {
            /* Get the subject */
            if ((subject = WSSubject.getRunAsSubject()) == null) {
                subject = WSSubject.getCallerSubject();
            }

            /* Get the credential */
            if (subject != null) {
                Iterator<WSCredential> iter = subject.getPublicCredentials(WSCredential.class).iterator();
                if (iter.hasNext()) {
                    cred = iter.next();
                }
            }

            /* Get the unique name */
            if (cred == null)
                return null;
            else
                uniqueName = cred.getUniqueSecurityName();
        } //throw exception in case there is some issue while retrieving authentication details from subject
        catch (Exception excp) {
            excp.getMessage();
            return null;
        }

        //return unique name obtained from subject
        return uniqueName;
    }

    /**
     * Create an Entity object corresponding to the LdapEntry object returned by the LdapConnection object.
     *
     * @param parentDO
     * @param propName
     * @param ldapEntry
     * @param propNames
     * @return
     * @throws WIMException
     */
    @Trivial // parentDO can be very large, override entry / exit to avoid printing out such a large object to trace
    private Entity createEntityFromLdapEntry(Object parentDO, String propName, LdapEntry ldapEntry, List<String> propNames) throws WIMException {
        final String METHODNAME = "createEntityFromLdapEntry";
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, METHODNAME, (parentDO == null) ? "null" : parentDO.getClass(), propName, ldapEntry, propNames);
        }

        String outEntityType = ldapEntry.getType();
        Entity outEntity = null;
        // For changed entities, when change type is delete, it is possible that
        // the type of entity (which maps to the LDAP entry's objectclass is not
        // available.
        if (outEntityType != null) {
            if (outEntityType.equalsIgnoreCase(SchemaConstants.DO_PERSON))
                outEntity = new Person();
            else if (outEntityType.equalsIgnoreCase(SchemaConstants.DO_PERSON_ACCOUNT))
                outEntity = new PersonAccount();
            else if (outEntityType.equalsIgnoreCase(SchemaConstants.DO_GROUP))
                outEntity = new Group();
            else
                outEntity = new Entity();
        } else {
            outEntity = new Entity();
        }

        if (parentDO instanceof Root) {
            if (SchemaConstants.DO_ENTITIES.equalsIgnoreCase(propName))
                ((Root) parentDO).getEntities().add(outEntity);
        } else if (parentDO instanceof Entity) {
            if (SchemaConstants.DO_GROUP.equalsIgnoreCase(propName)) {
                /*
                 * May get back plain entities if objectclass for group entity and
                 * group filters don't match up identically.
                 */
                if (outEntity instanceof Group) {
                    ((Entity) parentDO).getGroups().add((Group) outEntity);
                } else {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, METHODNAME + " Expected group entity. Group will excluded from group membership. Entity is " + outEntity, ldapEntry);
                    }
                }
            } else if (SchemaConstants.DO_MEMBERS.equalsIgnoreCase(propName)) {
                ((Group) parentDO).getMembers().add(outEntity);
            } else if (SchemaConstants.DO_CHILDREN.equalsIgnoreCase(propName)) {
                ((Entity) parentDO).getChildren().add(outEntity);
            }
        }

        IdentifierType outId = new IdentifierType();
        outEntity.setIdentifier(outId);
        outId.setUniqueName(ldapEntry.getUniqueName());
        outId.setExternalId(ldapEntry.getExtId());
        outId.setExternalName(ldapEntry.getDN());
        outId.setRepositoryId(reposId);

        String changeType = ldapEntry.getChangeType();
        if (changeType != null) {
            outEntity.setChangeType(changeType);
            if (SchemaConstants.CHANGETYPE_DELETE.equals(changeType) == false) {
                populateEntity(outEntity, propNames, ldapEntry.getAttributes());
            }
        } else {
            populateEntity(outEntity, propNames, ldapEntry.getAttributes());
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, METHODNAME, outEntity);
        }
        return outEntity;
    }

    /**
     * @param entity
     * @param propNames
     * @param attrs
     * @throws WIMException
     *             NOTE: BEHAVIOR CHANGE USING APAR PM46133
     *             Before: Properties having binary data-type were not being parsed and pushed into populated Entity.
     *             This method used to iterate every attribute returned by JNDI_CALL and parse them according to their Property-to-Attribute mapping from wimconfig.xml
     *             After: Whenever a LDAP returned object(attributes) ;then they'd be returned as
     *             DN: CN=myCN1,o=ibm ExtId: E525BE85647D750E882578E300444D6A UniqueName: CN=myCN1,o=ibm Type: PersonAccount
     *             Attributes: {
     *             dominounid=Attribute ID: dominounid
     *             Attribute values: E525BE85647D750E882578E300444D6A
     *             ; jpegphoto;binary=Attribute ID: jpegphoto;binary
     *             Attribute values: [B@4b3a4b3a
     *             ; objectclass=Attribute ID: objectclass
     *             Attribute values: inetorgperson,organizationalPerson,person,top
     *             ; sn=Attribute ID: sn
     *             Attribute values: mySN1
     *             ; cn=Attribute ID: cn
     *             Attribute values: myCN1
     *             }
     *             all attributes EXCEPT jpegphoto(binary type) are having a common syntax <attrName>=Attribute ID:<attrName> Attribute values:<someValue>
     *             Attributes of type binary have added it's type in the attribute name itself; hence method can't find/validate said attribute (jpegphoto;binary);
     *             hence fails to retrieve attribute and populate it's value in Entry.
     *             PS. These changes are not going to affect original behaviour; New changes will be serving binary dataType attributes too!
     */
    private void populateEntity(Entity entity, List<String> propNames, Attributes attrs) throws WIMException {
        if (propNames == null || propNames.size() == 0 || attrs == null) {
            return;
        }

        String entityType = entity.getTypeName();
        LdapEntity ldapEntity = iLdapConfigMgr.getLdapEntity(entityType);
        Set<String> allSupportedAttrs = ldapEntity.getAttributes();

        List<String> supportedProps = iLdapConfigMgr.getSupportedProperties(entityType, propNames);

        try {
            List<String> propVisitedList = new ArrayList<String>();
            for (NamingEnumeration<?> neu = attrs.getAll(); neu.hasMore();) {
                boolean contain = false;
                Attribute attr = (Attribute) neu.next();
                String attrName = attr.getID();
                int pos = attrName.indexOf(';');
                if (pos > 0) {
                    attrName = attrName.substring(0, pos);
                }
                if (allSupportedAttrs.contains(attrName)) {
                    contain = true;
                } else {
                    for (String curAttr : allSupportedAttrs) {
                        if (curAttr.equalsIgnoreCase(attrName)) {
                            contain = true;
                            break;
                        }
                    }
                }
                if (!contain) {
                    if (LDAP_ATTR_USER_PASSWORD.equalsIgnoreCase(attrName) || LDAP_ATTR_UNICODEPWD.equalsIgnoreCase(attrName))
                        contain = true;
                }

                Set<String> props = iLdapConfigMgr.getPropertyName(ldapEntity, attrName);
                boolean exclude = false;
                if (props.contains("ibmPrimaryEmail") && props.contains("ibm-primaryEmail")) {
                    exclude = true;
                }
                if (props.contains("ibmJobTitle") && props.contains("ibm-jobTitle")) {
                    exclude = true;
                }

                for (String propName : props) {
                    if (propName.equalsIgnoreCase("ibmPrimaryEmail") && exclude) {
                        continue;
                    }
                    if (propName.equalsIgnoreCase("ibmJobTitle") && exclude) {
                        continue;
                    }
                    for (int i = 0; i < supportedProps.size(); i++) {
                        String reqPropName = supportedProps.get(i);
                        if ((SchemaConstants.VALUE_ALL_PROPERTIES.equals(reqPropName) && contain)
                            || reqPropName.equalsIgnoreCase(propName)) {
                            Object prop = entity.get(propName);
                            if (/* prop != null && */(!propVisitedList.contains(propName) || !attrName.equalsIgnoreCase(propName))) {
                                setPropertyValue(entity, attr, propName, iLdapConfigMgr.getLdapAttribute(attrName));
                                if (!propVisitedList.contains(propName)) {
                                    propVisitedList.add(propName);
                                }
                            }
                            break;
                        }
                    }
                }
            }
        } catch (NamingException e) {
            throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, Tr.formatMessage(
                                                                                          tc,
                                                                                          WIMMessageKey.NAMING_EXCEPTION,
                                                                                          WIMMessageHelper.generateMsgParms(e.toString(true))));
        }
    }

    /**
     * Convert the value into an appropriate string value.
     *
     * @param isOctet
     * @param ldapValue
     * @return
     */
    @Trivial
    private String getString(boolean isOctet, Object ldapValue) {
        if (isOctet) {
            return LdapHelper.getOctetString((byte[]) ldapValue);
        } else {
            return ldapValue.toString();
        }
    }

    /**
     * Convert the value into an appropriate date string value.
     *
     * @param ldapValue
     * @return
     */
    private String getDateString(Object ldapValue) throws WIMSystemException {
        return String.valueOf(getDateString(ldapValue, false));
    }

    /**
     * Convert the value into an XMLGregorianCalendar.
     *
     * @param ldapValue
     * @return
     */
    private Date getDateObject(Object ldapValue) throws WIMSystemException {
        return (Date) getDateString(ldapValue, true);
    }

    private Object getDateString(Object ldapValue, boolean getCalendar) throws WIMSystemException {
        if (ldapValue instanceof Date) {
            return LdapHelper.getDateString((Date) ldapValue);
        }

        String timestampFormat = null;
        DateFormat dateFormat = null;
        StringBuffer originValue = new StringBuffer(ldapValue.toString());
        // IDS format:     20050711150348.000000Z
        // SUN ONE format: 20050721194630Z
        // AD format:      20040708135722.0Z
        // DM format:      20050723062910Z
        // NDS format:     20060120153334Z

        int pos = originValue.indexOf("Z");
        if (pos == -1) {
            pos = originValue.indexOf("z");
        }
        if (pos != -1) {
            originValue.replace(pos, pos, "-0000");
        }

        timestampFormat = iLdapConfigMgr.getTimestampFormat();
        if (timestampFormat != null) {
            dateFormat = new SimpleDateFormat(timestampFormat);
        } else {
            if (LdapConstants.IDS_LDAP_SERVER.equalsIgnoreCase(iLdapConfigMgr.getLdapType())) {
                int position = originValue.indexOf("-");
                if (originValue.indexOf(".-") == -1) {
                    while (originValue.substring(0, position).length() < 21) {
                        if (originValue.indexOf(".") == -1) {
                            originValue.replace(position, position, ".");
                            position += 1;
                        }
                        originValue.replace(position, position, "0");
                        position = originValue.indexOf("-");
                    }
                }
                dateFormat = new SimpleDateFormat("yyyyMMddHHmmss.SSSZ");
                originValue = new StringBuffer(originValue.substring(0, 18) + originValue.substring(21));
            } else if (LdapConstants.SUN_LDAP_SERVER.equalsIgnoreCase(iLdapConfigMgr.getLdapType())
                       || LdapConstants.DOMINO_LDAP_SERVER.equalsIgnoreCase(iLdapConfigMgr.getLdapType())
                       || LdapConstants.NOVELL_LDAP_SERVER.equalsIgnoreCase(iLdapConfigMgr.getLdapType())) {
                dateFormat = new SimpleDateFormat("yyyyMMddHHmmssZ");
            } else {
                if (originValue.toString().contains(".")) {
                    dateFormat = new SimpleDateFormat("yyyyMMddHHmmss.SZ");
                } else {
                    dateFormat = new SimpleDateFormat("yyyyMMddHHmmssZ");
                }
            }
        }

        Date date = null;
        try {
            date = dateFormat.parse(originValue.toString());
        } catch (java.text.ParseException e) {
            throw new WIMSystemException(WIMMessageKey.SYSTEM_EXCEPTION, Tr.formatMessage(
                                                                                          tc,
                                                                                          WIMMessageKey.SYSTEM_EXCEPTION,
                                                                                          WIMMessageHelper.generateMsgParms(e.toString() + ".  Timestamp received from Ldap: "
                                                                                                                            + ldapValue)));
        }

        if (getCalendar)
            return date;
        else
            return LdapHelper.getDateString(date);
    }

    /**
     * Create an IdentifierType object for the LdapEntry.
     *
     * @param ldapEntry
     * @return
     * @throws WIMException
     */
    @Trivial
    private IdentifierType createIdentiferFromLdapEntry(LdapEntry ldapEntry) throws WIMException {
        IdentifierType outId = new IdentifierType();
        outId.setUniqueName(ldapEntry.getUniqueName());
        outId.setExternalId(ldapEntry.getExtId());
        outId.setExternalName(ldapEntry.getDN());
        outId.setRepositoryId(reposId);
        return outId;
    }

    /**
     * Set the appropriate value for the specified property.
     *
     * @param entity
     * @param attr
     * @param prop
     * @param ldapAttr
     * @throws WIMException
     */
    @SuppressWarnings("unchecked")
    private void setPropertyValue(Entity entity, Attribute attr, String propName, LdapAttribute ldapAttr) throws WIMException {
        String dataType = entity.getDataType(propName);
        boolean isMany = entity.isMultiValuedProperty(propName);

        String syntax = LDAP_ATTR_SYNTAX_STRING;

        if (ldapAttr != null) {
            syntax = ldapAttr.getSyntax();
            if (tc.isEventEnabled()) {
                Tr.event(tc, "ldapAttr " + ldapAttr + " syntax is " + syntax);
            }
        }

        try {

            if (isMany) {
                for (NamingEnumeration<?> enu = attr.getAll(); enu.hasMoreElements();) {
                    Object ldapValue = enu.nextElement();
                    if (ldapValue != null) {
                        entity.set(propName, processPropertyValue(entity, propName, dataType, syntax, ldapValue));
                    }
                }
            } else {
                Object ldapValue = attr.get();
                if (ldapValue != null) {
                    entity.set(propName, processPropertyValue(entity, propName, dataType, syntax, ldapValue));
                }
            }

        } catch (NamingException e) {
            if (tc.isEventEnabled()) {
                Tr.event(tc, "Unexpected on " + propName + " with dataType " + dataType, e);
            }
            String msg = Tr.formatMessage(tc, WIMMessageKey.NAMING_EXCEPTION, WIMMessageHelper.generateMsgParms(e.toString(true)));
            throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, msg, e);
        } catch (ClassCastException ce) {
            if (tc.isEventEnabled()) {
                Tr.event(tc, "Failed to cast property " + propName + " to " + dataType, ce);
            }
            if (tc.isErrorEnabled())
                Tr.error(tc, WIMMessageKey.INVALID_PROPERTY_DATA_TYPE, WIMMessageHelper.generateMsgParms(propName));
        } catch (ArrayStoreException ae) {
            if (tc.isEventEnabled()) {
                Tr.event(tc, "Unexpected on " + propName + " with dataType " + dataType, ae);
            }
            if (tc.isErrorEnabled())
                Tr.error(tc, WIMMessageKey.INVALID_PROPERTY_DATA_TYPE, WIMMessageHelper.generateMsgParms(propName));
        }

    }

    /**
     * Process the value of a property.
     *
     * @param entity The entity to process the value for.
     * @param propName The property name to process.
     * @param dataType The data type of the property.
     * @param syntax The syntax for the property.
     * @param ldapValue The value from the LDAP server.
     * @return The processed value.
     * @throws WIMException If there was an issue processing the property's value.
     */
    private Object processPropertyValue(Entity entity, String propName, final String dataType, final String syntax, Object ldapValue) throws WIMException {
        if (DATA_TYPE_STRING.equals(dataType)) {
            boolean octet = LDAP_ATTR_SYNTAX_OCTETSTRING.equalsIgnoreCase(syntax);
            return getString(octet, ldapValue);
        } else if (DATA_TYPE_DATE_TIME.equals(dataType)) {
            return getDateString(ldapValue);
        } else if (DATA_TYPE_DATE.equals(dataType)) {
            return getDateObject(ldapValue);
        } else if (DATA_TYPE_INT.equals(dataType)) {
            return Integer.parseInt(ldapValue.toString());
        } else if (DATA_TYPE_IDENTIFIER_TYPE.equals(dataType)) {
            try {
                String stringValue = (String) ldapValue;
                LdapEntry ldapEntry = iLdapConn.getEntityByIdentifier(stringValue, null, null, null, null, false, false);
                return createIdentiferFromLdapEntry(ldapEntry);
            } catch (WIMException we) {
                if (WIMMessageKey.LDAP_ENTRY_NOT_FOUND.equalsIgnoreCase(we.getMessageKey())) {
                    String msg = Tr.formatMessage(tc, WIMMessageKey.INVALID_PROPERTY_VALUE, WIMMessageHelper.generateMsgParms(propName, entity.getIdentifier().getExternalName()));
                    throw new WIMSystemException(WIMMessageKey.INVALID_PROPERTY_VALUE, msg);
                } else {
                    throw we;
                }
            }
        } else if (DATA_TYPE_BASE_64_BINARY.equals(dataType)) {
            return ldapValue;
        } else if (DATA_TYPE_LANG_TYPE.equals(dataType)) {
            LangType lang = new LangType();
            lang.setValue(String.valueOf(ldapValue));
            return lang;
        } else if (DATA_TYPE_BOOLEAN.equals(dataType)) {
            return Boolean.parseBoolean(ldapValue.toString());
        } else if (DATA_TYPE_LONG.equals(dataType)) { //PI05723
            return Long.parseLong(ldapValue.toString());
        } else {
            if (tc.isEventEnabled()) {
                Tr.event(tc, "Datatype for " + propName + " was null, process without casting");
            }
            return ldapValue;
        }
    }

    /**
     * Method to get the Groups for the given entity.
     *
     * @param entity
     * @param ldapEntry
     * @param grpMbrshipCtrl
     * @throws WIMException
     */
    private void getGroups(Entity entity, LdapEntry ldapEntry, GroupMembershipControl grpMbrshipCtrl) throws WIMException {
        if (grpMbrshipCtrl == null) {
            return;
        }

        int level = grpMbrshipCtrl.getLevel();
        List<String> propNames = grpMbrshipCtrl.getProperties();
        String[] bases = null;
        List<String> searchBases = grpMbrshipCtrl.getSearchBases();
        int size = searchBases.size();
        String[] grpBases = iLdapConfigMgr.getGroupSearchBases();
        if (size == 0) {
            // New:: Default search bases to top level nodes if no group level search bases are defined
            if (grpBases.length != 0)
                bases = grpBases;
            else
                bases = iLdapConfigMgr.getTopLdapNodes();
        } else {
            List<String> baseList = new ArrayList<String>(size);
            for (int i = 0; i < size; i++) {
                String uName = searchBases.get(i);
                String dn = getDN(uName, null, null, true, false);
                baseList.add(dn);
            }
            bases = baseList.toArray(new String[0]);
        }
        bases = NodeHelper.getTopNodes(bases);

        if (iLdapConfigMgr.isRacf()) {
            getGroupsByMembershipRacf(entity, ldapEntry, bases, level, propNames, null);
        } else if (iLdapConfigMgr.getMembershipAttribute() != null) {
            getGroupsByMembership(entity, ldapEntry, bases, level, propNames, null);
        } else {
            // If operational attribute "ibm-allGroups" is specified in groupMemberIdMap, then get groups using operational attr "ibm-allGroups"
            if (LdapConstants.IDS_LDAP_SERVER.equalsIgnoreCase(iLdapConfigMgr.getLdapType()) && iLdapConfigMgr.isLdapOperationalAttributeSet())
                getGroupsByOperationalAttribute(entity, ldapEntry, bases, level, propNames);
            else {
                getGroupsByMember(entity, ldapEntry, bases, level, propNames, null);
            }
        }
    }

    /**
     * @param entity
     * @param ldapEntry
     * @param bases
     * @param level
     * @param propNames
     * @param object
     * @throws WIMException
     */
    private boolean getGroupsByMembershipRacf(Entity entity, LdapEntry ldapEntry, String[] bases, int level, List<String> propNames, String groupDN) throws WIMException {
        final String METHODNAME = "getGroupsByMembershipRacf";
        boolean isInGrp = false;

        String mbrshipAttrName = LdapConstants.LDAP_ATTR_RACF_CONNECT_GROUP_NAME;
        Attribute mbrshipAttr = ldapEntry.getAttributes().get(mbrshipAttrName);
        if (mbrshipAttr == null || mbrshipAttr.size() == 0) {
            String dn = ldapEntry.getDN();

            String[] attrIds = new String[0];
            attrIds[0] = mbrshipAttrName;

            List<String> entityTypes = new ArrayList<String>(1);
            entityTypes.add(ldapEntry.getType());

            Attributes attrs = iLdapConn.getAttributesByUniqueName(dn, attrIds, entityTypes);

            mbrshipAttr = attrs.get(mbrshipAttrName);
        }

        if (mbrshipAttr != null && mbrshipAttr.size() > 0) {
            NamingEnumeration<?> enm;
            try {
                enm = mbrshipAttr.getAll();
            } catch (NamingException e) {
                throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, Tr.formatMessage(
                                                                                              tc,
                                                                                              WIMMessageKey.NAMING_EXCEPTION,
                                                                                              WIMMessageHelper.generateMsgParms(e.toString(true))));
            }

            List<String> supportedProps = iLdapConfigMgr.getSupportedProperties(SchemaConstants.DO_GROUP, propNames);
            List<String> groupTypes = iLdapConfigMgr.getGroupTypes();

            while (enm.hasMoreElements()) {
                String dn = (String) enm.nextElement();

                //Remove spaces from DN. if DN contain space like in dn, cn=g1-10, ou=users,dc=rtp,dc=raleigh,dc=ibm,dc=com
                dn = LdapHelper.getValidDN(dn);
                if (groupDN != null && groupDN.equalsIgnoreCase(dn)) {
                    isInGrp = true;
                    return isInGrp;
                }

                if (!LdapHelper.isUnderBases(dn, bases)) {
                    continue;
                }

                LdapEntry grpEntry = null;
                try {
                    grpEntry = iLdapConn.getEntityByIdentifier(dn, null, null, groupTypes, supportedProps, false, false);
                } catch (EntityNotFoundException e) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, METHODNAME + " Group " + dn + " is not found and ignored.");
                    }
                    continue;
                }
                if (!iLdapConfigMgr.isGroup(grpEntry.getType())) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, METHODNAME + " Ldap entry " + dn + " is not a Group.");
                    }
                    continue;
                }
                if (entity != null) {
                    createEntityFromLdapEntry(entity, SchemaConstants.DO_GROUP, grpEntry, supportedProps);
                }
            }
        }
        return isInGrp;
    }

    /**
     * @param entity
     * @param ldapEntry
     * @param bases
     * @param level
     * @param propNames
     * @param object
     */
    private void getGroupsByOperationalAttribute(Entity entity, LdapEntry ldapEntry, String[] bases, int level, List<String> propNames) throws WIMException {
        String filter = "(objectclass=*)";
        List<String> grpTypes = iLdapConfigMgr.getGroupTypes();

        // Get the list of supported properties
        List<String> supportedProps = iLdapConfigMgr.getSupportedProperties(SchemaConstants.DO_GROUP, propNames);

        SearchResult result = iLdapConn.searchByOperationalAttribute(ldapEntry.getDN(), filter, grpTypes, supportedProps, LdapConstants.LDAP_ATTR_IBM_ALL_GROUP);

        if (result != null) {
            Attribute attribute = result.getAttributes().get(LDAP_ATTR_IBM_ALL_GROUP);
            if (attribute != null) {
                try {
                    NamingEnumeration<?> groups = attribute.getAll();
                    while (groups.hasMore()) {
                        if (entity != null) {
                            String groupDN = String.valueOf(groups.next());
                            LdapEntry grpEntry = iLdapConn.getEntityByIdentifier(groupDN, null, null, iLdapConfigMgr.getGroupTypes(), propNames, false, false);
                            createEntityFromLdapEntry(entity, SchemaConstants.DO_GROUP, grpEntry, supportedProps);
                        }
                    }
                } catch (NamingException e) {
                    throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, Tr.formatMessage(
                                                                                                  tc,
                                                                                                  WIMMessageKey.NAMING_EXCEPTION,
                                                                                                  WIMMessageHelper.generateMsgParms(e.toString(true))));
                }
            }
        }
    }

    private String getDN(String uniqueName, String entityType, Attributes attrs, boolean queryLdap, boolean translate) throws WIMException {
        final String METHODNAME = "getDN";

        String dn = null;
        String ldapNode = iLdapConfigMgr.getLdapNode(uniqueName);
        if (ldapNode != null) {
            return ldapNode;
        }
        uniqueName = iLdapConfigMgr.switchToLdapNode(uniqueName);
        if ((translate || iLdapConfigMgr.needTranslateRDN())
            && iLdapConfigMgr.needTranslateRDN(entityType)) {
            try {
                if (entityType != null) {
                    LdapEntity ldapEntity = iLdapConfigMgr.getLdapEntity(entityType);
                    if (ldapEntity != null && attrs != null) {
                        String[] rdnName = LdapHelper.getRDNAttributes(uniqueName);
                        String[][] rdnWIMProps = ldapEntity.getWIMRDNProperties();
                        String[][] rdnAttrs = ldapEntity.getRDNAttributes();
                        Attribute[] rdnAttributes = new Attribute[rdnWIMProps.length];
                        String[] rdnAttrValues = new String[rdnWIMProps.length];
                        for (int i = 0; i < rdnWIMProps.length; i++) {
                            String[] rdnProp = rdnWIMProps[i];
                            boolean isRDN = true;
                            for (int j = 0; j < rdnProp.length; j++) {
                                if (!rdnProp[j].equalsIgnoreCase(rdnName[j])) {
                                    isRDN = false;
                                }
                            }
                            if (isRDN) {
                                String[] rdnAttr = rdnAttrs[i];
                                for (int k = 0; k < rdnAttr.length; k++) {
                                    rdnAttributes[k] = attrs.get(rdnAttr[k]);
                                    if (rdnAttributes[k] == null && !queryLdap) {
                                        throw new MissingMandatoryPropertyException(WIMMessageKey.MISSING_MANDATORY_PROPERTY, Tr.formatMessage(
                                                                                                                                               tc,
                                                                                                                                               WIMMessageKey.MISSING_MANDATORY_PROPERTY,
                                                                                                                                               WIMMessageHelper.generateMsgParms(rdnProp[k])));
                                    }
                                    rdnAttrValues[k] = (String) rdnAttributes[k].get();
                                }
                                dn = LdapHelper.replaceRDN(uniqueName, rdnAttr, rdnAttrValues);
                            }
                        }
                    }
                }
                if (dn == null && queryLdap) {
                    Attributes reAttrs = iLdapConn.getAttributesByUniqueName(uniqueName, new String[0], null);
                    Attribute dnAttr = reAttrs.get(LDAP_DN);
                    dn = (String) dnAttr.get();
                }
            } catch (NamingException e) {
                throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, Tr.formatMessage(
                                                                                              tc,
                                                                                              WIMMessageKey.NAMING_EXCEPTION,
                                                                                              WIMMessageHelper.generateMsgParms(e.toString(true))));
            }
        }
        if (dn == null) {
            dn = uniqueName;
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, METHODNAME + " Translated DN: " + dn);
            }
        }
        return dn;
    }

    private boolean getGroupsByMembership(Entity entity, LdapEntry ldapEntry, String[] bases, int level,
                                          List<String> propNames, String groupDN) throws WIMException {
        final String METHODNAME = "getGroupsByMembership";

        boolean nested = (level == 0 && iLdapConfigMgr.getMembershipAttributeScope() == LDAP_DIRECT_GROUP_MEMBERSHIP);
        // set nested to true if recursiveSearch in config is set true
        if (!nested && (iLdapConfigMgr.isRecursiveSearch()))
            nested = true;

        boolean isInGrp = false;

        List<String> supportedProps = iLdapConfigMgr.getSupportedProperties(SchemaConstants.DO_GROUP, propNames);

        String mbrshipAttrName = iLdapConfigMgr.getMembershipAttribute();
        Attribute mbrshipAttr = ldapEntry.getAttributes().get(mbrshipAttrName);
        try {
            if (mbrshipAttr == null || (mbrshipAttr.size() == 1 && mbrshipAttr.get(0) == null)) {
                if (LdapConstants.IDS_LDAP_SERVER.equalsIgnoreCase(iLdapConfigMgr.getLdapType()) &&
                    mbrshipAttrName != null && mbrshipAttrName.equalsIgnoreCase(LDAP_ATTR_IBM_ALL_GROUP)) {
                    isInGrp = false;
                } else {
                    // This member do not have membership attr, use member attr to look up.
                    isInGrp = getGroupsByMember(entity, ldapEntry, bases, level, supportedProps, groupDN);
                }

                return isInGrp;
            } else if (mbrshipAttr.size() == 0) {
                // No groups contain this member
                isInGrp = false;
                return isInGrp;
            }

            Map<String, Attribute> DNMbrshipMap = null;
            if (nested) {
                DNMbrshipMap = new HashMap<String, Attribute>();
            }

            Set<String> groupsToDo = new HashSet<String>();
            List<String> groupTypes = iLdapConfigMgr.getGroupTypes();

            NamingEnumeration<?> enm = mbrshipAttr.getAll();
            while (enm.hasMoreElements()) {
                String dn = (String) enm.nextElement();
                //Remove spaces from DN. if DN contain space like in dn, cn=g1-10, ou=users,dc=rtp,dc=raleigh,dc=ibm,dc=com
                dn = LdapHelper.getValidDN(dn);
                if (groupDN != null && groupDN.equalsIgnoreCase(dn)) {
                    isInGrp = true;
                    return isInGrp;
                }
                if (!LdapHelper.isUnderBases(dn, bases)) {
                    continue;
                }
                List<String> grpTypes = iLdapConfigMgr.getGroupTypes();
                Set<String> entityType = new HashSet<String>(groupTypes);
                String entityTypeFilter = iLdapConfigMgr.getEntityTypesFilter(entityType);
                LdapEntry grpEntry = null;
                Set<LdapEntry> grpEntries = null;
                try {
                    grpEntries = iLdapConn.searchEntities(dn, entityTypeFilter, null, SearchControls.OBJECT_SCOPE, grpTypes,
                                                          supportedProps, true, false);
                    if (grpEntries != null) {
                        Iterator<LdapEntry> itr = grpEntries.iterator();
                        while (itr.hasNext())
                            grpEntry = itr.next();
                    }
                    //grpEntry = iLdapConn.getEntityByIdentifier(dn, null, null, groupTypes, supportedProps, nested, false);
                } catch (EntityNotFoundException e) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, METHODNAME + " Group " + dn + " is not found and ingored.");
                    }
                    continue;
                }
                if (grpEntry != null && !iLdapConfigMgr.isGroup(grpEntry.getType())) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, METHODNAME + " Ldap entry " + dn + " is not a Group.");
                    }
                    continue;
                }
                if (grpEntry != null && entity != null) {
                    createEntityFromLdapEntry(entity, SchemaConstants.DO_GROUP, grpEntry, supportedProps);
                }
                if (grpEntry != null && nested) {
                    String key = dn.toLowerCase();
                    DNMbrshipMap.put(key, grpEntry.getAttributes().get(mbrshipAttrName));
                    groupsToDo.add(key);
                }
            }

            // Find dynamic groups
            // dyna grp DN to LdapEntry map.
            Map<String, LdapEntry> dynaGrpMap = null;

            // dyna grp DN to URLs map
            Map<String, LdapURL[]> dynaGrpURLsMap = null;

            // If membership attribute does not include dynamic groups, need to search dynamic groups.
            boolean findDynaGrp = iLdapConfigMgr.getMembershipAttributeScope() != LDAP_ALL_GROUP_MEMBERSHIP
                                  && iLdapConfigMgr.supportDynamicGroup();
            if (findDynaGrp) {
                dynaGrpMap = iLdapConn.getDynamicGroups(bases, supportedProps, true);
                dynaGrpURLsMap = new HashMap<String, LdapURL[]>(dynaGrpMap.size());
                Set<Map.Entry<String, LdapEntry>> dynaGrpMapEntrySet = dynaGrpMap.entrySet();
                for (Map.Entry<String, LdapEntry> dynaEntry : dynaGrpMapEntrySet) {
                    String dynaGrpDn = dynaEntry.getKey();
                    String dynaGrpKey = dynaGrpDn.toLowerCase();
                    LdapEntry dynaGrpEntry = dynaEntry.getValue();
                    Attributes attrs = dynaGrpEntry.getAttributes();
                    Attribute dynaMbrAttr = attrs.get(iLdapConfigMgr.getDynamicMemberAttribute(attrs.get(LDAP_ATTR_OBJECTCLASS)));
                    if (dynaMbrAttr != null) {
                        LdapURL[] ldapURLs = LdapHelper.getLdapURLs(dynaMbrAttr);
                        if (ldapURLs != null && ldapURLs.length > 0) {
                            dynaGrpURLsMap.put(dynaGrpDn, ldapURLs);
                            if (!DNMbrshipMap.containsKey(dynaGrpKey)
                                && iLdapConn.isMemberInURLQuery(ldapURLs, ldapEntry.getDN())) {
                                if (groupDN != null && groupDN.equalsIgnoreCase(dynaGrpDn)) {
                                    isInGrp = true;
                                    return isInGrp;
                                }

                                if (entity != null) {
                                    createEntityFromLdapEntry(entity, SchemaConstants.DO_GROUP, dynaGrpEntry, supportedProps);
                                }
                                if (nested) {
                                    DNMbrshipMap.put(dynaGrpKey, dynaGrpEntry.getAttributes().get(mbrshipAttrName));
                                    groupsToDo.add(dynaGrpKey);

                                }
                            }
                        }
                    }
                }
            }

            if (!nested && groupDN != null) {
                return isInGrp;
            }
            // groupToDo size is > 0, means nested
            while (groupsToDo.size() > 0) {
                Set<String> nextGroups = new HashSet<String>();
                for (String dn : groupsToDo) {
                    Attribute grpMbrshipAttr = DNMbrshipMap.get(dn.toLowerCase());
                    if (grpMbrshipAttr != null) {
                        NamingEnumeration<?> grpEnum = grpMbrshipAttr.getAll();
                        while (grpEnum.hasMoreElements()) {
                            String grpDn = (String) grpEnum.nextElement();
                            if (grpDn != null) {
                                if (groupDN != null && groupDN.equalsIgnoreCase(grpDn)) {
                                    isInGrp = true;
                                    return isInGrp;
                                }
                                if (!LdapHelper.isUnderBases(grpDn, bases)) {
                                    continue;
                                }
                                String grpKey = grpDn.toLowerCase();
                                // If this group is already known, ignore it.
                                if (DNMbrshipMap.containsKey(grpKey)) {
                                    continue;
                                }
                                LdapEntry grpEntry = null;
                                Set<LdapEntry> grpEntries = null;
                                List<String> grpTypes = iLdapConfigMgr.getGroupTypes();
                                Set<String> entityType = new HashSet<String>(groupTypes);
                                String entityTypeFilter = iLdapConfigMgr.getEntityTypesFilter(entityType);
                                try {
                                    grpEntries = iLdapConn.searchEntities(grpDn, entityTypeFilter, null, SearchControls.OBJECT_SCOPE, grpTypes,
                                                                          supportedProps, true, false);
                                    if (grpEntries != null) {
                                        Iterator<LdapEntry> itr = grpEntries.iterator();
                                        while (itr.hasNext())
                                            grpEntry = itr.next();
                                    }
                                    //grpEntry = iLdapConn.getEntityByIdentifier(grpDn, null, null, groupTypes, supportedProps,nested, false);
                                } catch (EntityNotFoundException e) {
                                    if (tc.isDebugEnabled()) {
                                        Tr.debug(tc, METHODNAME + " Group " + grpDn + " is not found and ignored.");
                                    }
                                    continue;
                                }
                                if (grpEntry != null && !iLdapConfigMgr.isGroup(grpEntry.getType())) {
                                    if (tc.isDebugEnabled()) {
                                        Tr.debug(tc, METHODNAME + " Ldap entry " + grpDn + " is not a Group.");
                                    }
                                    continue;
                                }
                                if (grpEntry != null && entity != null) {
                                    createEntityFromLdapEntry(entity, SchemaConstants.DO_GROUP, grpEntry, supportedProps);
                                }
                                if (grpEntry != null && nested) {
                                    DNMbrshipMap.put(grpKey, grpEntry.getAttributes().get(mbrshipAttrName));
                                    if (!groupsToDo.contains(grpKey)) {
                                        nextGroups.add(grpKey);
                                    }
                                }
                            }
                        }
                    }
                    // Find dynamic groups
                    if (findDynaGrp) {
                        Set<Map.Entry<String, LdapURL[]>> dynaGrpUrlEntrySet = dynaGrpURLsMap.entrySet();
                        for (Map.Entry<String, LdapURL[]> dynaUrlEntry : dynaGrpUrlEntrySet) {
                            String dynaGrpDn = dynaUrlEntry.getKey();
                            String dynaGrpKey = dynaGrpDn.toLowerCase();
                            if (!DNMbrshipMap.containsKey(dynaGrpKey)
                                && iLdapConn.isMemberInURLQuery(dynaUrlEntry.getValue(), dn)) {
                                if (groupDN != null && groupDN.equalsIgnoreCase(dynaGrpDn)) {
                                    isInGrp = true;
                                    return isInGrp;
                                }

                                LdapEntry dynaGrpEntry = dynaGrpMap.get(dynaGrpDn);
                                if (entity != null) {
                                    createEntityFromLdapEntry(entity, SchemaConstants.DO_GROUP, dynaGrpEntry, supportedProps);
                                }
                                DNMbrshipMap.put(dynaGrpKey, dynaGrpEntry.getAttributes().get(mbrshipAttrName));
                                if (!groupsToDo.contains(dynaGrpKey)) {
                                    nextGroups.add(dynaGrpKey);
                                }
                            }
                        }
                    }
                }
                groupsToDo = nextGroups;
            }
        } catch (NamingException e) {
            throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, Tr.formatMessage(
                                                                                          tc,
                                                                                          WIMMessageKey.NAMING_EXCEPTION,
                                                                                          WIMMessageHelper.generateMsgParms(e.toString(true))));
        }

        return isInGrp;
    }

    private boolean getGroupsByMember(Entity entity, LdapEntry ldapEntry, String[] bases, int level, List<String> propNames, String groupDN) throws WIMException {
        boolean nested = (level == 0) && !iLdapConfigMgr.isMemberAttributesNestedScope();
        // Set nested to true if recursiveSearch in config is set true
        if (!nested && (iLdapConfigMgr.isRecursiveSearch()))
            nested = true;

        boolean isInGrp = false;
        String filter = iLdapConfigMgr.getGroupMemberFilter(ldapEntry.getDN());
        List<String> grpTypes = iLdapConfigMgr.getGroupTypes();
        Set<String> groupsToDo = new HashSet<String>();
        Set<String> allGroups = new HashSet<String>();

        // Get the list of supported properties
        List<String> supportedProps = iLdapConfigMgr.getSupportedProperties(SchemaConstants.DO_GROUP, propNames);

        // Search member attributes for static groups
        for (int i = 0, n = bases.length; i < n; i++) {
            Set<LdapEntry> grpEntries = iLdapConn.searchEntities(bases[i], filter, null, SearchControls.SUBTREE_SCOPE, grpTypes,
                                                                 supportedProps, false, false);
            for (LdapEntry grpEntry : grpEntries) {
                if (entity != null) {
                    createEntityFromLdapEntry(entity, SchemaConstants.DO_GROUP, grpEntry, supportedProps);
                }
                String dn = grpEntry.getDN();
                //Remove spaces from DN. if DN contain space like in dn, cn=g1-10, ou=users,dc=rtp,dc=raleigh,dc=ibm,dc=com
                dn = LdapHelper.getValidDN(dn);
                if (groupDN != null && groupDN.equalsIgnoreCase(dn)) {
                    isInGrp = true;
                    return isInGrp;
                }
                if (nested) {
                    String key = dn.toLowerCase();
                    groupsToDo.add(key);
                    allGroups.add(key);
                }
            }
        }
        // Find dynamic groups
        // dyna grp DN to LdapEntry map.
        Map<String, LdapEntry> dynaGrpMap = null;
        // dyna grp DN to URLs map
        Map<String, LdapURL[]> dynaGrpURLsMap = null;
        // If all member attrs includes dynamic members, no need to search dynamic groups.
        boolean findDynaGrp = !iLdapConfigMgr.isMemberAttributesAllScope() && iLdapConfigMgr.supportDynamicGroup();
        if (findDynaGrp) {
            dynaGrpMap = iLdapConn.getDynamicGroups(bases, supportedProps, false);
            dynaGrpURLsMap = new HashMap<String, LdapURL[]>(dynaGrpMap.size());
            Set<Map.Entry<String, LdapEntry>> dynaGrpEntrySet = dynaGrpMap.entrySet();
            for (Map.Entry<String, LdapEntry> entry : dynaGrpEntrySet) {
                String dynaGrpDn = entry.getKey();
                String dynaGrpKey = dynaGrpDn.toLowerCase();
                LdapEntry dynaGrpEntry = entry.getValue();
                Attributes attrs = dynaGrpEntry.getAttributes();
                Attribute dynaMbrAttr = attrs.get(iLdapConfigMgr.getDynamicMemberAttribute(attrs.get(LDAP_ATTR_OBJECTCLASS)));
                if (dynaMbrAttr != null) {
                    LdapURL[] ldapURLs = LdapHelper.getLdapURLs(dynaMbrAttr);
                    if (ldapURLs != null && ldapURLs.length > 0) {
                        dynaGrpURLsMap.put(dynaGrpDn, ldapURLs);
                        if (!allGroups.contains(dynaGrpKey)
                            && iLdapConn.isMemberInURLQuery(ldapURLs, ldapEntry.getDN())) {
                            if (groupDN != null && groupDN.equalsIgnoreCase(dynaGrpDn)) {
                                isInGrp = true;
                                return isInGrp;
                            }

                            if (entity != null) {
                                createEntityFromLdapEntry(entity, SchemaConstants.DO_GROUP, dynaGrpEntry, supportedProps);
                            }
                            if (nested) {
                                groupsToDo.add(dynaGrpKey);
                                allGroups.add(dynaGrpKey);
                            }
                        }
                    }
                }
            }
        }

        if (!nested && groupDN != null) {
            return isInGrp;
        }

        while (groupsToDo.size() > 0) {
            Set<String> nextGroups = new HashSet<String>();
            for (String dn : groupsToDo) {
                filter = iLdapConfigMgr.getGroupMemberFilter(dn);
                for (int i = 0, n = bases.length; i < n; i++) {
                    Set<LdapEntry> grpEntries = iLdapConn.searchEntities(bases[i], filter, null, SearchControls.SUBTREE_SCOPE,
                                                                         grpTypes, supportedProps, false, false);
                    for (LdapEntry grpEntry : grpEntries) {
                        String grpDn = grpEntry.getDN();
                        if (groupDN != null && groupDN.equalsIgnoreCase(grpDn)) {
                            isInGrp = true;
                            return isInGrp;
                        }

                        String grpKey = grpDn.toLowerCase();
                        if (!allGroups.contains(grpKey)) {
                            if (entity != null) {
                                createEntityFromLdapEntry(entity, SchemaConstants.DO_GROUP, grpEntry, supportedProps);
                            }
                            allGroups.add(grpKey);
                            if (!groupsToDo.contains(grpKey)) {
                                nextGroups.add(grpKey);
                            }
                        }
                    }
                }
                // Find dynamic groups
                if (findDynaGrp) {
                    Set<Map.Entry<String, LdapURL[]>> dynaGrpURLsEntrySet = dynaGrpURLsMap.entrySet();
                    for (Map.Entry<String, LdapURL[]> urlEntry : dynaGrpURLsEntrySet) {
                        String dynaGrpDn = urlEntry.getKey();
                        String dynaGrpKey = dynaGrpDn.toLowerCase();
                        if (!allGroups.contains(dynaGrpKey)
                            && iLdapConn.isMemberInURLQuery(urlEntry.getValue(), dn)) {
                            LdapEntry dynaGrpEntry = dynaGrpMap.get(dynaGrpDn);
                            if (groupDN != null && groupDN.equalsIgnoreCase(dynaGrpDn)) {
                                isInGrp = true;
                                return isInGrp;
                            }

                            if (entity != null) {
                                createEntityFromLdapEntry(entity, SchemaConstants.DO_GROUP, dynaGrpEntry, supportedProps);
                            }
                            allGroups.add(dynaGrpKey);
                            if (!groupsToDo.contains(dynaGrpKey)) {
                                nextGroups.add(dynaGrpKey);
                            }
                        }
                    }
                }
            }
            groupsToDo = nextGroups;
        }
        return isInGrp;
    }

    private void getMembers(Entity entity, LdapEntry ldapEntry, GroupMemberControl grpMbrCtrl) throws WIMException {
        if (grpMbrCtrl == null) {
            return;
        }

        int level = grpMbrCtrl.getLevel();
        List<String> propNames = grpMbrCtrl.getProperties();
        List<String> mbrTypes = getEntityTypes(grpMbrCtrl);
        String[] bases = getBases(grpMbrCtrl, mbrTypes);

        if (!iLdapConfigMgr.isDefaultMbrAttr() || iLdapConfigMgr.isRacf()) {
            getMembersByMember(entity, ldapEntry, bases, level, propNames, mbrTypes);
        } else if (iLdapConfigMgr.getMembershipAttribute() != null) {
            getMembersByMembership(entity, ldapEntry, bases, level, propNames, mbrTypes);
        } else {
            getMembersByMember(entity, ldapEntry, bases, level, propNames, mbrTypes);
        }
    }

    private List<String> getEntityTypes(SearchControl ctrl) throws WIMException {
        String searchExpr = ctrl.getExpression();

        // Gets the entity types of the members.
        Set<String> mbrTypes = new HashSet<String>();
        if (searchExpr != null && searchExpr.trim().length() > 0) {
            WIMXPathInterpreter parser = new WIMXPathInterpreter(new StringReader(searchExpr));
            try {
                parser.parse(null);
            } catch (ParseException e) {
                throw new SearchControlException(WIMMessageKey.INVALID_SEARCH_EXPRESSION, Tr.formatMessage(
                                                                                                           tc,
                                                                                                           WIMMessageKey.INVALID_SEARCH_EXPRESSION,
                                                                                                           WIMMessageHelper.generateMsgParms(searchExpr)));
            } catch (TokenMgrError e) {
                throw new SearchControlException(WIMMessageKey.INVALID_SEARCH_EXPRESSION, Tr.formatMessage(
                                                                                                           tc,
                                                                                                           WIMMessageKey.INVALID_SEARCH_EXPRESSION,
                                                                                                           WIMMessageHelper.generateMsgParms(searchExpr)));
            }
            List<String> mbrTypeList = parser.getEntityTypes();
            mbrTypes.addAll(mbrTypeList);
        } else {
            List<LdapEntity> ldapEntities = iLdapConfigMgr.getLdapEntities();
            for (int i = 0; i < ldapEntities.size(); i++) {
                mbrTypes.add(ldapEntities.get(i).getName());
            }
        }
        return new ArrayList<String>(mbrTypes);
    }

    private String[] getBases(SearchControl ctrl, List<String> mbrTypes) throws WIMException {
        String[] bases = null;
        List<String> searchBaseList = ctrl.getSearchBases();
        int size = searchBaseList.size();

        if (size > 0) {
            bases = searchBaseList.toArray(new String[0]);
            bases = NodeHelper.getTopNodes(bases);
            for (int i = 0; i < bases.length; i++) {
                String uName = bases[i];

                String dn = getDN(uName, null, null, true, false);

                bases[i] = dn;
                //At this point bases is an array of nameInRepository. Get top level nodes to avoid overlapping nameInRepositories.
                bases = NodeHelper.getTopNodes(bases);
            }
        }
        // If no search bases is specified, use the search bases of the returned types.
        else {
            List<String> searchBases = new ArrayList<String>();
            if (mbrTypes == null) {
                bases = iLdapConfigMgr.getTopLdapNodes();
            } else {
                for (int i = 0; i < mbrTypes.size(); i++) {
                    LdapEntity ldapEntity = iLdapConfigMgr.getLdapEntity(mbrTypes.get(i));
                    if (ldapEntity != null) {
                        searchBases.addAll(ldapEntity.getSearchBaseList());
                    } else {
                        bases = iLdapConfigMgr.getTopLdapNodes();
                        break;
                    }
                }
                if (bases == null) {
                    bases = searchBases.toArray(new String[0]);
                    bases = NodeHelper.getTopNodes(bases);
                }
            }
        }

        return bases;
    }

    private void getMembersByMembership(Entity entity, LdapEntry ldapEntry, String[] bases, int level, List<String> propNames, List<String> mbrTypes) throws WIMException {
        boolean nested = (level == 0 && iLdapConfigMgr.getMembershipAttributeScope() == LDAP_DIRECT_GROUP_MEMBERSHIP);
        // set nested to true if recursiveSearch in config is set true
        if (!nested && (iLdapConfigMgr.isRecursiveSearch()))
            nested = true;

        String mbrshipAttrName = iLdapConfigMgr.getMembershipAttribute();
        boolean returnGrp = iLdapConfigMgr.containGroup(mbrTypes);
        //Map for avoiding duplicate entries
        HashMap<String, Object> mbrMap = new HashMap<String, Object>();

        List<String> searchBaseList = new ArrayList<String>();
        for (int i = 0; i < bases.length; i++) {
            searchBaseList.add(bases[i]);
        }

        // Formulate members filter.
        StringBuffer memberFilter = new StringBuffer("(&(" + mbrshipAttrName + "={0})");

        // find search base.
        if (mbrTypes.size() > 1 || (!returnGrp && nested)) {
            memberFilter.append("(|");
        }
        for (String entityType : mbrTypes) {
            LdapEntity ldapEntity = iLdapConfigMgr.getLdapEntity(entityType);
            memberFilter.append(ldapEntity.getSearchFilter());
        }

        // Need to return group when nested.
        if (!returnGrp && nested) {
            LdapEntity grpEntity = iLdapConfigMgr.getLdapEntity(SchemaConstants.DO_GROUP);
            memberFilter.append(grpEntity.getSearchFilter());
            searchBaseList.addAll(grpEntity.getSearchBaseList());

            // Add Groups as a type to be returned
            mbrTypes.add(SchemaConstants.DO_GROUP);
        }
        if (mbrTypes.size() > 1 || (!returnGrp && nested)) {
            memberFilter.append(")");
        }
        memberFilter.append(")");
        Object[] filterArgs = {
                                ldapEntry.getDN()
        };

        String[] searchBases = {};
        searchBases = searchBaseList.toArray(searchBases);
        searchBases = NodeHelper.getTopNodes(searchBases);

        Set<String> groupsToDo = new HashSet<String>();
        Set<String> requestedGrps = new HashSet<String>();
        for (int i = 0; i < searchBases.length; i++) {
            Set<LdapEntry> entities = iLdapConn.searchEntities(searchBases[i], memberFilter.toString(), filterArgs,
                                                               SearchControls.SUBTREE_SCOPE, mbrTypes, propNames, false, false);
            for (LdapEntry mbrEntity : entities) {
                String dn = mbrEntity.getDN();
                //Remove spaces from DN. if DN contain space like in dn, cn=g1-10, ou=users,dc=rtp,dc=raleigh,dc=ibm,dc=com
                dn = LdapHelper.getValidDN(dn);
                if (iLdapConfigMgr.isGroup(mbrEntity.getType())) {
                    if (nested) {
                        String key = dn.toLowerCase();
                        groupsToDo.add(key);
                        requestedGrps.add(key);
                    }
                    if (!returnGrp) {
                        continue;
                    }
                }
                // Dont create and add entity if its already present in mbrMap to avoid duplications
                if ((LdapHelper.isUnderBases(dn, bases)) && (!mbrMap.containsKey((mbrEntity.getUniqueName())))) {
                    createEntityFromLdapEntry(entity, SchemaConstants.DO_MEMBERS, mbrEntity, propNames);
                }
                mbrMap.put(mbrEntity.getUniqueName(), null);
            }
        }
        if (nested) {
            while (groupsToDo.size() > 0) {
                Set<String> nextGroups = new HashSet<String>();
                for (String grpDN : groupsToDo) {
                    filterArgs[0] = grpDN;
                    for (int i = 0; i < searchBases.length; i++) {
                        Set<LdapEntry> entities = iLdapConn.searchEntities(searchBases[i], memberFilter.toString(), filterArgs,
                                                                           SearchControls.SUBTREE_SCOPE, mbrTypes, propNames, false, false);
                        for (LdapEntry mbrEntity : entities) {
                            String dn = mbrEntity.getDN();
                            //Remove spaces from DN. if DN contain space like in dn, cn=g1-10, ou=users,dc=rtp,dc=raleigh,dc=ibm,dc=com
                            dn = LdapHelper.getValidDN(dn);
                            String key = dn.toLowerCase();
                            if (!requestedGrps.contains(key) && iLdapConfigMgr.isGroup(mbrEntity.getType())) {
                                if (!groupsToDo.contains(key)) {
                                    nextGroups.add(key);
                                }
                                requestedGrps.add(key);
                                if (!returnGrp) {
                                    continue;
                                }
                            }
                            // Dont create and add entity if its already present in mbrMap to avoid duplications
                            if ((LdapHelper.isUnderBases(dn, bases)) && (!mbrMap.containsKey((mbrEntity.getUniqueName())))) {
                                createEntityFromLdapEntry(entity, SchemaConstants.DO_MEMBERS, mbrEntity, propNames);
                            }
                            mbrMap.put(mbrEntity.getUniqueName(), null);
                        }
                    }
                }
                groupsToDo = nextGroups;
            }
        }
    }

    @FFDCIgnore(EntityNotFoundException.class)
    private void getMembersByMember(Entity entity, LdapEntry ldapEntry, String[] bases, int level, List<String> propNames, List<String> mbrTypes) throws WIMException {
        final String METHODNAME = "getMembersByMember";

        // Retrieve group member (including dynamic members if supported)from previous call.
        Attributes attrs = ldapEntry.getAttributes();
        Attribute[] memberAttrs = iLdapConfigMgr.getGroupMemberAttrs(attrs, attrs.get(LDAP_ATTR_OBJECTCLASS));
        Attribute memberAttr = null;
        if (memberAttrs != null && memberAttrs.length > 0)
            memberAttr = memberAttrs[0];

        List<String> supportedProps = iLdapConfigMgr.getSupportedProperties(SchemaConstants.DO_PERSON_ACCOUNT, propNames);
        //  HashMap<String, Object> mbrMapNested = new HashMap<String, Object>();
        HashMap<String, Object> mbrMap = new HashMap<String, Object>();

        if (iLdapConfigMgr.supportDynamicGroup()) {
            String dynaMbrAttr = iLdapConfigMgr.getDynamicMemberAttribute(attrs.get(LDAP_ATTR_OBJECTCLASS));
            if (dynaMbrAttr != null) {
                List<String> dynaMemberDNs = getDynamicMembers(attrs.remove(dynaMbrAttr));
                if (dynaMemberDNs != null && dynaMemberDNs.size() > 0) {
                    if (memberAttr == null) {
                        String mbrAttrName = null;
                        if (attrs.get(LDAP_ATTR_OBJECTCLASS) != null) {
                            mbrAttrName = iLdapConfigMgr.getDynamicMemberAttribute(attrs.get(LDAP_ATTR_OBJECTCLASS));
                        }
                        memberAttr = new BasicAttribute(mbrAttrName);
                    }
                    for (String dynaMemberDN : dynaMemberDNs) {
                        memberAttr.add(dynaMemberDN);
                    }
                }
            }
        }

        if (memberAttr == null) {
            return;
        } else {
            if (memberAttrs == null) {
                // Add memberAttr to memberAttrs
                memberAttrs = new Attribute[1];
                memberAttrs[0] = memberAttr;
            }
        }

        boolean nested = (level == 0) && !iLdapConfigMgr.isMemberAttributesNestedScope();
        // set nested to true if recursiveSearch in config is set true
        if (!nested && (iLdapConfigMgr.isRecursiveSearch()))
            nested = true;

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, METHODNAME + " nested = " + nested);
        }

        // Store all members which are groups and its groupMemberAttrs.
        Map<String, Attribute> grpDNAttrs = new HashMap<String, Attribute>();

        // Create a copy of the list of types sent by the user and add "GROUP" to is.
        ArrayList<String> mbrTypesWithGroup = new ArrayList<String>();
        if (mbrTypes != null)
            mbrTypesWithGroup.addAll(mbrTypes);

        // Include group only if the custom property is set.
        if (nested) {
            if (!mbrTypesWithGroup.contains(SchemaConstants.DO_GROUP))
                mbrTypesWithGroup.add(SchemaConstants.DO_GROUP);
        }
        String entityTypeFilter = null;
        HashSet<String> entityType = null;

        if (mbrTypes != null) {
            entityType = new HashSet<String>(mbrTypesWithGroup);
            entityTypeFilter = iLdapConfigMgr.getEntityTypesFilter(entityType);
        }

        // Store the immediate members of this group.
        try {
            if (memberAttrs != null) {
                ArrayList<String> RDNCheckType = new ArrayList<String>();

                for (int i = 0; i < memberAttrs.length; i++) {
                    memberAttr = memberAttrs[i];
                    for (NamingEnumeration<?> enu = memberAttr.getAll(); enu.hasMoreElements();) {
                        String dn = (String) enu.next();
                        //Remove spaces from DN. if DN contain space like in dn, cn=g1-10, ou=users,dc=rtp,dc=raleigh,dc=ibm,dc=com
                        dn = LdapHelper.getValidDN(dn);
                        if (iLdapConfigMgr.isDummyMember(dn) || !LdapHelper.isUnderBases(dn, bases)
                            || !startWithSameRDN(dn, mbrTypes, nested)) {
                            continue;
                        }
                        LdapEntry mbrEntity = null;
                        Set<LdapEntry> mbrEntries = null;
                        try {
                            mbrEntries = iLdapConn.searchEntities(dn, entityTypeFilter, null, SearchControls.OBJECT_SCOPE, mbrTypesWithGroup,
                                                                  supportedProps, false, true);
                            if (mbrEntries != null && mbrEntries.size() > 0) {
                                Iterator<LdapEntry> itr = mbrEntries.iterator();
                                while (itr.hasNext())
                                    mbrEntity = itr.next();
                            }
                            //mbrEntity = iLdapConn.getEntityByIdentifier(dn, null, null, mbrTypesWithGroup, propNames, false, nested);
                        } catch (EntityNotFoundException e) {
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, METHODNAME + " Group member " + dn + " is not found and ignored.");
                            continue;
                        }
                        if (mbrEntity != null) {
                            String mbrType = mbrEntity.getType();

                            // Check the Entity Type matches the configured RDN
                            if (mbrType != null) {
                                RDNCheckType.clear();
                                RDNCheckType.add(mbrType);
                                if (!startWithSameRDN(dn, RDNCheckType, nested))
                                    continue;
                            }

                            if (LdapHelper.isEntityTypeInList(mbrType, mbrTypes)) {
                                List<String> supportedProps1 = iLdapConfigMgr.getSupportedProperties(mbrType, propNames);

                                if (!mbrMap.containsKey((mbrEntity.getUniqueName())))
                                    createEntityFromLdapEntry(entity, SchemaConstants.DO_MEMBERS, mbrEntity, supportedProps1);

                                mbrMap.put(mbrEntity.getUniqueName(), null);
                                //createEntityFromLdapEntry(entity, SchemaConstants.DO_MEMBERS, mbrEntity, supportedProps1);
                            }

                            if (nested && iLdapConfigMgr.isGroup(mbrType)) {
                                Attributes mbrAttrs = mbrEntity.getAttributes();
                                // Cache member attrs for nested call
                                Attribute[] mbrAttr = iLdapConfigMgr.getGroupMemberAttrs(mbrAttrs, mbrAttrs.get(LDAP_ATTR_OBJECTCLASS));
                                if (mbrAttr != null) {
                                    for (int loop = 0; loop < mbrAttr.length; loop++)
                                        grpDNAttrs.put(dn.toLowerCase(), mbrAttr[loop]);
                                }
                            }
                        }
                    }
                }
            }

            if (nested) {
                // Store the groups need to do nested call.
                Set<String> groupsToDo = new HashSet<String>(grpDNAttrs.keySet());
                while (groupsToDo.size() > 0) {
                    Set<String> nextGroups = new HashSet<String>();
                    for (String grpDN : groupsToDo) {
                        String grpKey = grpDN.toLowerCase();
                        memberAttr = grpDNAttrs.get(grpKey);
                        if (memberAttr == null) {
                            continue;
                        }
                        for (NamingEnumeration<?> enu = memberAttr.getAll(); enu.hasMoreElements();) {
                            String dn = (String) enu.next();
                            //Remove spaces from DN. if DN contain space like in dn, cn=g1-10, ou=users,dc=rtp,dc=raleigh,dc=ibm,dc=com
                            dn = LdapHelper.getValidDN(dn);
                            if (dn != null) {
                                String key = dn.toLowerCase();
                                if (iLdapConfigMgr.isDummyMember(dn) || !LdapHelper.isUnderBases(dn, bases)
                                    || !startWithSameRDN(dn, mbrTypes, nested) || grpDNAttrs.containsKey(key)) {
                                    continue;
                                }
                                LdapEntry mbrEntity = null;
                                Set<LdapEntry> mbrEntries = null;
                                try {
                                    mbrEntries = iLdapConn.searchEntities(dn, entityTypeFilter, null, SearchControls.OBJECT_SCOPE, mbrTypesWithGroup,
                                                                          supportedProps, false, true);
                                    if (mbrEntries != null && mbrEntries.size() > 0) {
                                        Iterator<LdapEntry> itr = mbrEntries.iterator();
                                        while (itr.hasNext())
                                            mbrEntity = itr.next();
                                    }

                                    /*
                                     * mbrEntity = iLdapConn.getEntityByIdentifier(dn, null, null, mbrTypesWithGroup, propNames, false,
                                     * nested);
                                     */
                                } catch (EntityNotFoundException e) {
                                    if (tc.isDebugEnabled()) {
                                        Tr.debug(tc, METHODNAME + " Group member " + dn + " is not found and ignored.");
                                    }
                                    continue;
                                }

                                if (mbrEntity != null) {
                                    String mbrType = mbrEntity.getType();
                                    if (LdapHelper.isEntityTypeInList(mbrType, mbrTypes)) {
                                        List<String> supportedProps2 = iLdapConfigMgr.getSupportedProperties(mbrType, propNames);

                                        if (!mbrMap.containsKey((mbrEntity.getUniqueName())))
                                            createEntityFromLdapEntry(entity, SchemaConstants.DO_MEMBERS, mbrEntity, supportedProps2);

                                        mbrMap.put(mbrEntity.getUniqueName(), null);
                                    }
                                    if (nested && iLdapConfigMgr.isGroup(mbrType)) {
                                        Attributes mbrAttrs = mbrEntity.getAttributes();
                                        // Cache member attrs for nested call
                                        Attribute[] mbrAttr = iLdapConfigMgr.getGroupMemberAttrs(mbrAttrs,
                                                                                                 mbrAttrs.get(LDAP_ATTR_OBJECTCLASS));
                                        if (mbrAttr != null) {
                                            for (int loop = 0; loop < mbrAttr.length; loop++)
                                                grpDNAttrs.put(dn.toLowerCase(), mbrAttr[loop]);
                                        }
                                        if (!groupsToDo.contains(key)) {
                                            nextGroups.add(key);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    groupsToDo = nextGroups;
                }
            }
        } catch (NamingException e) {
            throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, Tr.formatMessage(
                                                                                          tc,
                                                                                          WIMMessageKey.NAMING_EXCEPTION,
                                                                                          WIMMessageHelper.generateMsgParms(e.toString(true))));
        }
    }

    @Trivial
    private boolean startWithSameRDN(String dn, List<String> mbrTypes, boolean nested) {
        dn = dn.toLowerCase();
        boolean containGrp = false;
        for (int i = 0; i < mbrTypes.size(); i++) {
            String mbrType = mbrTypes.get(i);
            if (iLdapConfigMgr.isGroup(mbrType)) {
                containGrp = true;
            }

            if (iLdapConfigMgr.getLdapEntity(mbrType).startWithSameRDN(dn)) {
                return true;
            }
        }
        if (nested && !containGrp) {
            if (iLdapConfigMgr.getLdapEntity(SchemaConstants.DO_GROUP).startWithSameRDN(dn)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Method to get the list of dynamic members from the given Group Member URL.
     *
     * @param groupMemberURLs
     * @return
     * @throws WIMException
     */
    private List<String> getDynamicMembers(Attribute groupMemberURLs) throws WIMException {
        final String METHODNAME = "getDynamicMembers";

        List<String> memberDNs = new ArrayList<String>();
        if (groupMemberURLs != null) {
            try {
                for (NamingEnumeration<?> enu = groupMemberURLs.getAll(); enu.hasMoreElements();) {
                    String ldapurlStr = (String) (enu.nextElement());
                    if (ldapurlStr != null) {
                        LdapURL ldapURL = new LdapURL(ldapurlStr);
                        if (ldapURL.parsedOK()) {
                            int searchScope = SearchControls.OBJECT_SCOPE;
                            String scopeBuf = ldapURL.get_scope();
                            if (scopeBuf != null) {
                                if (scopeBuf.compareToIgnoreCase("base") == 0) {
                                    searchScope = SearchControls.OBJECT_SCOPE;
                                } else if (scopeBuf.compareToIgnoreCase("one") == 0) {
                                    searchScope = SearchControls.ONELEVEL_SCOPE;
                                } else if (scopeBuf.compareToIgnoreCase("sub") == 0) {
                                    searchScope = SearchControls.SUBTREE_SCOPE;
                                }
                            }
                            String searchFilter = ldapURL.get_filter();
                            if (searchFilter == null) {
                                searchFilter = "(objectClass=*)";
                            }

                            String searchBase = ldapURL.get_dn();
                            String[] attributesToReturn = ldapURL.get_attributes();

                            for (NamingEnumeration<?> nenu = iLdapConn.search(searchBase, searchFilter, searchScope,
                                                                              attributesToReturn); nenu.hasMoreElements();) {
                                javax.naming.directory.SearchResult thisEntry = (javax.naming.directory.SearchResult) nenu.nextElement();
                                if (thisEntry == null) {
                                    continue;
                                }
                                String dynaMbrDN = LdapHelper.prepareDN(thisEntry.getName(), searchBase);
                                memberDNs.add(dynaMbrDN);
                            }
                        }
                    } else {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, METHODNAME + " LDAP URL=null.");
                        }
                    }
                }
            } catch (NamingException e) {
                throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, Tr.formatMessage(
                                                                                              tc,
                                                                                              WIMMessageKey.NAMING_EXCEPTION,
                                                                                              WIMMessageHelper.generateMsgParms(e.toString(true))));
            }
        }
        return memberDNs;
    }

    /**
     * Method to get the list of descendants.
     *
     * @param entity
     * @param ldapEntry
     * @param descCtrl
     * @throws WIMException
     */
    private void getDescendants(Entity entity, LdapEntry ldapEntry, DescendantControl descCtrl) throws WIMException {
        if (descCtrl == null) {
            return;
        }

        List<String> propNames = descCtrl.getProperties();
        int level = descCtrl.getLevel();
        List<String> descTypes = getEntityTypes(descCtrl);
        String[] bases = getBases(descCtrl, descTypes);
        boolean treeView = descCtrl.isSetTreeView();
        int scope = SearchControls.ONELEVEL_SCOPE;
        if (level == 0 && !treeView) {
            scope = SearchControls.SUBTREE_SCOPE;
        }
        Set<String> descToDo = new HashSet<String>();
        Map<String, Entity> descendants = new HashMap<String, Entity>();
        Set<LdapEntry> descEntries = iLdapConn.searchEntities(ldapEntry.getDN(), "objectClass=*", null, scope, descTypes,
                                                              propNames, false, false);
        for (LdapEntry descEntry : descEntries) {
            String descType = descEntry.getType();
            String descDn = descEntry.getDN();
            Entity descendant = null;
            if (LdapHelper.isUnderBases(descDn, bases) && descTypes.contains(descType)) {
                descendant = createEntityFromLdapEntry(entity, SchemaConstants.DO_CHILDREN, descEntry, propNames);
            } else if (treeView) {
                descendant = createEntityFromLdapEntry(entity, SchemaConstants.DO_CHILDREN, descEntry, null);
            }
            if (treeView) {
                descToDo.add(descDn);
                descendants.put(descDn, descendant);
            }
        }
        if (treeView) {
            while (descToDo.size() > 0) {
                Set<String> nextDescs = new HashSet<String>();
                for (String dn : descToDo) {
                    Entity parent = descendants.get(dn);
                    descEntries = iLdapConn.searchEntities(dn, "objectClass=*", null, scope, descTypes, propNames,
                                                           false, false);
                    for (LdapEntry descEntry : descEntries) {
                        String descType = descEntry.getType();
                        String descDn = descEntry.getDN();
                        Entity descendant = null;
                        if (descTypes.contains(descType)) {
                            descendant = createEntityFromLdapEntry(parent, SchemaConstants.DO_CHILDREN, descEntry, propNames);
                        } else if (treeView) {
                            descendant = createEntityFromLdapEntry(parent, SchemaConstants.DO_CHILDREN, descEntry, null);
                        }
                        if (!descToDo.contains(descDn)) {
                            nextDescs.add(descDn);
                            descendants.put(descDn, descendant);
                        }
                    }
                }
                descToDo = nextDescs;
            }
        }
    }

    private List<String> getEntityTypes(DescendantControl ctrl) throws WIMException {
        String searchExpr = ctrl.getExpression();

        // Gets the entity types of the members.
        Set<String> mbrTypes = new HashSet<String>();
        if (searchExpr != null && searchExpr.trim().length() > 0) {
            WIMXPathInterpreter parser = new WIMXPathInterpreter(new StringReader(searchExpr));
            try {
                parser.parse(null);
            } catch (ParseException e) {
                throw new SearchControlException(WIMMessageKey.INVALID_SEARCH_EXPRESSION, Tr.formatMessage(
                                                                                                           tc,
                                                                                                           WIMMessageKey.INVALID_SEARCH_EXPRESSION,
                                                                                                           WIMMessageHelper.generateMsgParms(searchExpr)));
            } catch (TokenMgrError e) {
                throw new SearchControlException(WIMMessageKey.INVALID_SEARCH_EXPRESSION, Tr.formatMessage(
                                                                                                           tc,
                                                                                                           WIMMessageKey.INVALID_SEARCH_EXPRESSION,
                                                                                                           WIMMessageHelper.generateMsgParms(searchExpr)));
            }
            List<String> mbrTypeList = parser.getEntityTypes();
            mbrTypes.addAll(mbrTypeList);
        } else {
            List<LdapEntity> ldapEntities = iLdapConfigMgr.getLdapEntities();
            for (int i = 0; i < ldapEntities.size(); i++) {
                mbrTypes.add(ldapEntities.get(i).getName());
            }
        }
        return new ArrayList<String>(mbrTypes);
    }

    private String[] getBases(DescendantControl ctrl, List<String> mbrTypes) throws WIMException {
        String[] bases = null;
        List<String> searchBaseList = ctrl.getSearchBases();
        int size = searchBaseList.size();

        if (size > 0) {
            bases = searchBaseList.toArray(new String[0]);
            bases = NodeHelper.getTopNodes(bases);
            for (int i = 0; i < bases.length; i++) {
                String uName = bases[i];

                String dn = getDN(uName, null, null, true, false);

                bases[i] = dn;
                //At this point bases is an array of nameInRepository. Get top level nodes to avoid overlapping nameInRepositories.
                bases = NodeHelper.getTopNodes(bases);

            }
        }
        // If no search bases is specified, use the search bases of the returned types.
        else {
            List<String> searchBases = new ArrayList<String>();
            if (mbrTypes == null) {
                bases = iLdapConfigMgr.getTopLdapNodes();

            } else {

                for (int i = 0; i < mbrTypes.size(); i++) {
                    LdapEntity ldapEntity = iLdapConfigMgr.getLdapEntity(mbrTypes.get(i));
                    if (ldapEntity != null) {
                        searchBases.addAll(ldapEntity.getSearchBaseList());
                    } else {
                        bases = iLdapConfigMgr.getTopLdapNodes();
                        break;
                    }
                }
                if (bases == null) {
                    bases = searchBases.toArray(new String[0]);
                    bases = NodeHelper.getTopNodes(bases);
                }
            }
        }

        return bases;
    }

    /**
     * Method to get the ancestors of the given entity.
     *
     * @param entity
     * @param ldapEntry
     * @param ancesCtrl
     * @throws WIMException
     */
    private void getAncestors(Entity entity, LdapEntry ldapEntry, AncestorControl ancesCtrl) throws WIMException {
        if (ancesCtrl == null) {
            return;
        }

        List<String> propNames = ancesCtrl.getProperties();
        int level = ancesCtrl.getLevel();
        List<String> ancesTypes = getEntityTypes(ancesCtrl);
        String[] bases = getBases(ancesCtrl, ancesTypes);

        String dn = ldapEntry.getDN();
        List<String> ancestorDns = iLdapConn.getAncestorDNs(dn, level);
        Entity parentEntity = entity;
        for (int i = 0; i < ancestorDns.size(); i++) {
            String ancesDn = ancestorDns.get(i);
            if (ancesDn.length() == 0) {
                continue;
            }
            if (LdapHelper.isUnderBases(ancesDn, bases)) {
                LdapEntry ancesEntry = iLdapConn.getEntityByIdentifier(ancesDn, null, null, ancesTypes, propNames,
                                                                       false, false);
                String ancesType = ancesEntry.getType();
                Entity ancestor = null;
                if (ancesTypes.contains(ancesType)) {
                    ancestor = createEntityFromLdapEntry(parentEntity, SchemaConstants.DO_PARENT, ancesEntry, propNames);
                } else {
                    ancestor = createEntityFromLdapEntry(parentEntity, SchemaConstants.DO_PARENT, ancesEntry, null);
                }
                parentEntity = ancestor;
            }
        }
    }

    private boolean isMemberInGroup(Entity inEntity, LdapEntry inEntry, int level) throws WIMException {
        List<Group> grps = inEntity.getGroups();

        LdapEntry memberEntry = null;
        String grpDN = null;
        if (grps.size() > 0) {
            memberEntry = inEntry;
            Group grp = grps.get(0);
            LdapEntry grpEntry = iLdapConn.getEntityByIdentifier(grp.getIdentifier(), null, null, false, false);
            if (!iLdapConfigMgr.isGroup(grpEntry.getType())) {
                throw new InvalidEntityTypeException(WIMMessageKey.ENTITY_IS_NOT_A_GROUP, Tr.formatMessage(
                                                                                                           tc,
                                                                                                           WIMMessageKey.ENTITY_IS_NOT_A_GROUP,
                                                                                                           WIMMessageHelper.generateMsgParms(grpEntry.getUniqueName())));
            }
            grpDN = grpEntry.getDN();
        } else if (iLdapConfigMgr.isGroup(inEntry.getType())) {
            List<Entity> mbrs = ((Group) inEntity).getMembers();
            if (mbrs.size() > 0) {
                Entity mbr = mbrs.get(0);
                memberEntry = iLdapConn.getEntityByIdentifier(mbr.getIdentifier(), null, null, true, false);
                grpDN = inEntry.getDN();
            } else {
                return false;
            }
        } else {
            return false;
        }

        String[] grpBases = iLdapConfigMgr.getGroupSearchBases();
        if (iLdapConfigMgr.getMembershipAttribute() != null) {
            if (iLdapConfigMgr.isRacf())
                return getGroupsByMembershipRacf(null, memberEntry, grpBases, level, null, grpDN);
            else
                return getGroupsByMembership(null, memberEntry, grpBases, level, null, grpDN);
        } else {
            return getGroupsByMember(null, memberEntry, grpBases, level, null, grpDN);
        }
    }

    /**
     * return the specified realms from the root object of the input data graph
     *
     * @param root
     * @return
     */
    public static Set<String> getSpecifiedRealms(Root root) {
        Set<String> result = new HashSet<String>();
        List<Context> contexts = root.getContexts();
        for (Context context : contexts) {
            String key = context.getKey();
            if (key != null && key.equals(SchemaConstants.PROP_REALM)) {
                String value = (String) context.getValue();
                result.add(value);
            }
        }
        return result;
    }

    /**
     * return the customProperty from the root object of the input data graph
     *
     * @param root
     * @param propertyName
     * @return
     */
    public static String getContextProperty(Root root, String propertyName) {
        String result = "";
        List<Context> contexts = root.getContexts();
        for (Context context : contexts) {
            String key = context.getKey();
            if (key != null && key.equals(propertyName)) {
                result = (String) context.getValue();
                break;
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private LdapSearchControl getLdapSearchControl(SearchControl searchControl, boolean isSearchBaseSetByClient, boolean ignoreDNBaseSearch) throws WIMException {
        final String METHODNAME = "getLdapSearchControl";
        List<String> propNames = searchControl.getProperties();
        int countLimit = searchControl.getCountLimit();
        int timeLimit = searchControl.getTimeLimit();
        boolean returnSubTypes = searchControl.isReturnSubType();

        String searchExpr = searchControl.getExpression();

        // Gets the entity types of the members.
        Set<String> entityTypes = new HashSet<String>();
        List<String> entityTypeList = null;
        String filter = null;
        boolean pNameSearch = false;
        String pNameBase = null;

        if (searchExpr != null && searchExpr.trim().length() > 0) {
            WIMXPathInterpreter parser = new WIMXPathInterpreter(new StringReader(searchExpr));
            try {
                XPathNode node = parser.parse(null);
                List<String> typeList = parser.getEntityTypes();
                if (returnSubTypes) {
                    for (int i = 0; i < typeList.size(); i++) {
                        String entityType = typeList.get(i);
                        entityTypes.add(entityType);
                        List<String> subTypes = iLdapConfigMgr.getLdapSubEntityTypes(entityType);
                        entityTypes.addAll(subTypes);
                    }
                } else {
                    entityTypes.addAll(typeList);
                }

                entityTypeList = new ArrayList<String>(entityTypes);
                if (node != null) {
                    HashMap propNodeMap = new HashMap();
                    Iterator<PropertyNode> iter = node.getPropertyNodes(propNodeMap);
                    while (iter.hasNext()) {
                        PropertyNode pNode = iter.next();
                        String propName = pNode.getName();
                        if (propName.equals(SchemaConstants.PROP_PRINCIPAL_NAME)) {
                            if (propNodeMap.size() > 1) {
                                String msg = Tr.formatMessage(tc, WIMMessageKey.CANNOT_SEARCH_PRINCIPAL_NAME_WITH_OTHER_PROPS, (Object) null);
                                throw new SearchControlException(WIMMessageKey.CANNOT_SEARCH_PRINCIPAL_NAME_WITH_OTHER_PROPS, msg);
                            } else {
                                pNameSearch = true;
                                break;
                            }
                        }
                    }
                }
                if (pNameSearch) {
                    if (node.getNodeType() != XPathNode.NODE_PROPERTY) {
                        String msg = Tr.formatMessage(tc, WIMMessageKey.CANNOT_SEARCH_PRINCIPAL_NAME_WITH_OTHER_PROPS, (Object) null);
                        throw new SearchControlException(WIMMessageKey.CANNOT_SEARCH_PRINCIPAL_NAME_WITH_OTHER_PROPS, msg);
                    }
                    PropertyNode pNameNode = (PropertyNode) node;
                    String value = (String) pNameNode.getValue();
                    String uName = UniqueNameHelper.getValidUniqueName(value);
                    if (uName == null || ignoreDNBaseSearch) {
                        filter = getPrincipalNameFilter(value);
                    } else {
                        pNameBase = getDN(uName, SchemaConstants.DO_PERSON_ACCOUNT, null, true, false);
                    }

                } else {
                    filter = getSearchFilter(entityTypes, node);
                }
            } catch (ParseException e) {
                throw new SearchControlException(WIMMessageKey.INVALID_SEARCH_EXPRESSION, Tr.formatMessage(
                                                                                                           tc,
                                                                                                           WIMMessageKey.INVALID_SEARCH_EXPRESSION,
                                                                                                           WIMMessageHelper.generateMsgParms(searchExpr)));
            } catch (TokenMgrError e) {
                throw new SearchControlException(WIMMessageKey.INVALID_SEARCH_EXPRESSION, Tr.formatMessage(
                                                                                                           tc,
                                                                                                           WIMMessageKey.INVALID_SEARCH_EXPRESSION,
                                                                                                           WIMMessageHelper.generateMsgParms(searchExpr)));
            }
        } else {
            List<LdapEntity> ldapEntities = iLdapConfigMgr.getLdapEntities();
            for (int i = 0; i < ldapEntities.size(); i++) {
                entityTypes.add(ldapEntities.get(i).getName());
            }
        }
        if (filter != null) {
            filter = "(&" + iLdapConfigMgr.getEntityTypesFilter(entityTypes) + filter + ")";
        } else {
            filter = iLdapConfigMgr.getEntityTypesFilter(entityTypes);
        }

        // Use entity search base if configured, unless overridden by client
        if (entityTypeList != null && entityTypeList.size() > 0) {
            boolean isEntityBaseConfigured = isEntitySearchBaseConfigured(entityTypeList);
            if (isEntityBaseConfigured && !isSearchBaseSetByClient) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, METHODNAME + " Entity search base is configured, so realm search base will be ignored");
                searchControl.getSearchBases().clear();
                if (tc.isDebugEnabled())
                    Tr.debug(tc, METHODNAME + " Search base has been changed to entity search base");
            }
        }
        if (isSearchBaseSetByClient)
            if (tc.isDebugEnabled())
                Tr.debug(tc, METHODNAME + " Search base explicitly set by client, so configured search bases will be ignored");

        String[] bases = getBases(searchControl, entityTypeList);
        if (pNameBase != null) {
            if (!LdapHelper.isUnderBases(pNameBase, bases)) {
                bases = new String[0];
            } else {
                bases = new String[1];
                bases[0] = pNameBase;
            }
        }

        // Get the search time out configured at ldap registry level
        int iTimeLimit = iLdapConn.getSearchTimeout();
        // set the lowest of iTimeLimit and timeLimit in search control
        if (timeLimit <= 0)
            timeLimit = iTimeLimit;
        else if (iTimeLimit < timeLimit)
            timeLimit = iTimeLimit;

        LdapSearchControl ldapSearchCtrl = new LdapSearchControl(bases, entityTypeList, filter, propNames, countLimit, timeLimit);
        if (pNameBase != null) {
            ldapSearchCtrl.setScope(SearchControls.OBJECT_SCOPE);
        }
        return ldapSearchCtrl;
    }

    private String getPrincipalNameFilter(String principalName) {
        List<String> loginAttrs = iLdapConfigMgr.getLoginAttributes();
        principalName = principalName.replace("\"\"", "\""); // Unescape escaped XPath quotation marks
        principalName = principalName.replace("''", "'"); // Unescape escaped XPath apostrophes
        principalName = Rdn.escapeValue(principalName);
        principalName = principalName.replace("(", "\\("); // Escape paren for LDAP filter.
        principalName = principalName.replace(")", "\\)"); // Escape paren for LDAP filter.
        StringBuffer filter = new StringBuffer();
        if (loginAttrs != null) {
            if (loginAttrs.size() > 1) {
                filter.append("(|");
            }
            for (int i = 0; i < loginAttrs.size(); i++) {
                filter.append("(" + loginAttrs.get(i) + "=" + principalName + ")");
            }
            if (loginAttrs.size() > 1) {
                filter.append(")");
            }
        }
        return filter.toString();
    }

    private String getSearchFilter(Set<String> entityTypes, XPathNode node) throws WIMException {
        String filter = null;
        StringBuffer propsFilter = new StringBuffer();
        if (node != null) {
            LdapXPathTranslateHelper helper = new LdapXPathTranslateHelper(entityTypes, iLdapConfigMgr);
            helper.genSearchString(propsFilter, node);

            // make sure that query is surrounded by parenthesis because it's going to be and'ed
            if (propsFilter.length() > 0
                && (propsFilter.charAt(0) != '(' || propsFilter.charAt(propsFilter.length() - 1) != ')')) {
                propsFilter.insert(0, '(');
                propsFilter.append(')');
            }

            filter = propsFilter.toString();
        }

        return filter;
    }

    /**
     * Checks if a search base is configured for the entity types.
     *
     * @param mbrTypes Entity types
     * @return
     */
    private boolean isEntitySearchBaseConfigured(List<String> mbrTypes) {
        String METHODNAME = "isEntitySearchBaseConfigured(mbrTypes)";
        boolean isConfigured = false;
        if (mbrTypes != null) {
            for (String mbrType : mbrTypes) {
                LdapEntity ldapEntity = iLdapConfigMgr.getLdapEntity(mbrType);
                if (ldapEntity != null && ldapEntity.isSearchBaseConfigured()) {
                    isConfigured = true;
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, METHODNAME + " Search base is explicitly configured for "
                                     + mbrType + ": " + ldapEntity.getSearchBaseList());
                }
            }
        }
        return isConfigured;
    }

    /**
     * Find the LdapEntry mapped to the Certificate.
     *
     * @param certs
     * @param srchCtrl
     * @return
     * @throws WIMException
     */
    @FFDCIgnore({ EntityNotFoundException.class, com.ibm.websphere.security.CertificateMapNotSupportedException.class,
                  com.ibm.websphere.security.CertificateMapFailedException.class })
    private LdapEntry mapCertificate(X509Certificate[] certs, LdapSearchControl srchCtrl) throws WIMException {
        LdapEntry result = null;

        String dn = null;
        String filter = null;
        String certMapMode = iLdapConfigMgr.getCertificateMapMode();

        if (ConfigConstants.CONFIG_VALUE_CERT_NOT_SUPPORTED_MODE.equalsIgnoreCase(certMapMode)) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Certificate authentication has been disabled for this LDAP registry.");
            }
            String msg = Tr.formatMessage(tc, WIMMessageKey.LDAP_REGISTRY_CERT_IGNORED);
            throw new CertificateMapNotSupportedException(WIMMessageKey.LDAP_REGISTRY_CERT_IGNORED, msg);
        } else if (ConfigConstants.CONFIG_VALUE_CUSTOM_MODE.equalsIgnoreCase(certMapMode)) {
            String mapping;
            try {
                X509CertificateMapper mapper = iCertificateMapperRef.get();
                if (mapper == null) {
                    String msg = Tr.formatMessage(tc, WIMMessageKey.LDAP_REGISTRY_MAPPER_NOT_BOUND);
                    throw new CertificateMapFailedException(WIMMessageKey.LDAP_REGISTRY_MAPPER_NOT_BOUND, msg);
                }

                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Using custom X.509 certificate mapper: " + mapper.getClass());
                }

                mapping = mapper.mapCertificate(certs);
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "The custom X.509 certificate mapper returned the following mapping: " + mapping);
                }
            } catch (com.ibm.websphere.security.CertificateMapNotSupportedException e) {
                String msg = Tr.formatMessage(tc, WIMMessageKey.LDAP_REGISTRY_CUSTOM_MAPPER_NOT_SUPPORTED);
                throw new CertificateMapNotSupportedException(WIMMessageKey.LDAP_REGISTRY_CUSTOM_MAPPER_NOT_SUPPORTED, msg, e);
            } catch (com.ibm.websphere.security.CertificateMapFailedException e) {
                String msg = Tr.formatMessage(tc, WIMMessageKey.LDAP_REGISTRY_CUSTOM_MAPPER_FAILED);
                throw new CertificateMapFailedException(WIMMessageKey.LDAP_REGISTRY_CUSTOM_MAPPER_FAILED, msg, e);
            }

            /*
             * The mapper should return some value.
             */
            if (mapping == null || mapping.trim().isEmpty()) {
                String msg = Tr.formatMessage(tc, WIMMessageKey.LDAP_REGISTRY_INVALID_MAPPING);
                throw new CertificateMapFailedException(WIMMessageKey.LDAP_REGISTRY_INVALID_MAPPING, msg);
            }

            /*
             * If in the form of a distinguished name
             */
            dn = LdapHelper.getValidDN(mapping);
            if (dn == null) {
                filter = mapping;
            }
        } else if (ConfigConstants.CONFIG_VALUE_FILTER_DESCRIPTOR_MODE.equalsIgnoreCase(certMapMode)) {
            filter = iLdapConfigMgr.getCertificateLDAPFilter(certs[0]).trim();
        } else {
            dn = LdapHelper.getValidDN(certs[0].getSubjectX500Principal().getName());
        }

        /*
         * Try and validate the user with the LDAP server.
         */
        if (dn != null) {
            /*
             * We have a distinguished name. Search for the user directly.
             */
            try {
                result = iLdapConn.getEntityByIdentifier(dn, null, null, null, srchCtrl.getPropertyNames(), false,
                                                         false);
            } catch (EntityNotFoundException e) {
                /* User not found in this repository. */
            }
        } else {
            /*
             * We have a search filter. Search over all the base entries.
             */
            String[] searchBases = srchCtrl.getBases();
            int countLimit = srchCtrl.getCountLimit();
            int timeLimit = srchCtrl.getTimeLimit();
            List<String> entityTypes = srchCtrl.getEntityTypes();
            List<String> propNames = srchCtrl.getPropertyNames();
            int scope = srchCtrl.getScope();

            int count = 0;
            for (int i = 0; i < searchBases.length; i++) {
                try {
                    Set<LdapEntry> ldapEntries = iLdapConn.searchEntities(searchBases[i], filter, null, scope, entityTypes,
                                                                          propNames, false, false, countLimit, timeLimit);
                    if (ldapEntries.size() > 1) {
                        //Uncomment this when MULTIPLE_PRINCIPALS_FOUND is added to LdapUtilMesssages.nlsprops
                        /*
                         * if(tc.isErrorEnabled()){
                         * Tr.error(tc, WIMMessageKey.MULTIPLE_PRINCIPALS_FOUND, WIMMessageHelper.generateMsgParms(sFilter));
                         * }
                         */
                        String msg = Tr.formatMessage(tc, WIMMessageKey.MULTIPLE_PRINCIPALS_FOUND, WIMMessageHelper.generateMsgParms(filter));
                        throw new CertificateMapFailedException(WIMMessageKey.MULTIPLE_PRINCIPALS_FOUND, msg);
                    } else if (ldapEntries.size() == 1) {
                        if (count == 0) {
                            result = ldapEntries.iterator().next();
                        }
                        count++;
                        if (count > 1) {
                            //Uncomment this when MULTIPLE_PRINCIPALS_FOUND is added to LdapUtilMesssages.nlsprops
                            /*
                             * if(tc.isErrorEnabled()){
                             * Tr.error(tc, WIMMessageKey.MULTIPLE_PRINCIPALS_FOUND, WIMMessageHelper.generateMsgParms(sFilter));
                             * }
                             */
                            String msg = Tr.formatMessage(tc, WIMMessageKey.MULTIPLE_PRINCIPALS_FOUND, WIMMessageHelper.generateMsgParms(filter));
                            throw new CertificateMapFailedException(WIMMessageKey.MULTIPLE_PRINCIPALS_FOUND, msg);
                        }
                    }
                } catch (EntityNotFoundException e) {
                    /* User not found in this search base. */
                    continue;
                }
            }
        }

        return result;
    }

    @FFDCIgnore(javax.naming.AuthenticationException.class)
    private void authenticateWithPassword(String dn, byte[] pwd, String principalName) throws WIMException {
        try {
            TimedDirContext ctx = null;
            // Check if user wants to bind to LDAP server with input principal name. If not, default
            // behavior is to bind using user DN.
            if (iLdapConfigMgr.isSetUsePrincipalNameForLogin())
                ctx = iLdapConn.getContextManager().createDirContext(principalName, pwd);
            else
                ctx = iLdapConn.getContextManager().createDirContext(dn, pwd);
            ctx.close();
        }

        catch (javax.naming.AuthenticationNotSupportedException e) {
            throw new AuthenticationNotSupportedException(WIMMessageKey.AUTHENTICATE_NOT_SUPPORTED, Tr.formatMessage(
                                                                                                                     tc,
                                                                                                                     WIMMessageKey.AUTHENTICATE_NOT_SUPPORTED,
                                                                                                                     WIMMessageHelper.generateMsgParms(reposId, e.toString(true))));
        } catch (javax.naming.AuthenticationException e) {
            throw new PasswordCheckFailedException(WIMMessageKey.PASSWORD_CHECKED_FAILED, Tr.formatMessage(
                                                                                                           tc,
                                                                                                           WIMMessageKey.PASSWORD_CHECKED_FAILED,
                                                                                                           WIMMessageHelper.generateMsgParms(principalName, e.toString(true))));
        } catch (NameNotFoundException e) {
            throw new EntityNotFoundException(WIMMessageKey.LDAP_ENTRY_NOT_FOUND, Tr.formatMessage(
                                                                                                   tc,
                                                                                                   WIMMessageKey.LDAP_ENTRY_NOT_FOUND,
                                                                                                   WIMMessageHelper.generateMsgParms(dn, e.toString(true))));
        } catch (NamingException e) {
            throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, Tr.formatMessage(
                                                                                          tc,
                                                                                          WIMMessageKey.NAMING_EXCEPTION,
                                                                                          WIMMessageHelper.generateMsgParms(e.toString(true))));
        }
    }

    @Override
    public String getRealm() {
        return reposRealm;
    }

    /**
     * Delete the entity (Person/Group) specified by the Identifier data object.
     *
     * @param root The object containing the indentifier of objects to be deleted.
     * @throws Exception
     * @throws RemoteException
     */
    @Override
    public Root delete(Root root) throws WIMException {
        Map<String, Control> ctrlMap = ControlsHelper.getControlMap(root);

        DeleteControl deleteCtrl = (DeleteControl) ctrlMap.get(SchemaConstants.DO_DELETE_CONTROL);

        boolean delDesc = false;
        if (deleteCtrl != null) {
            delDesc = deleteCtrl.isDeleteDescendants();
        }

        Entity inEntity = root.getEntities().get(0);
        IdentifierType inId = inEntity.getIdentifier();

        LdapEntry ldapEntry = iLdapConn.getEntityByIdentifier(inId, null, null, false, false);

        Root outRoot = new Root();

        List<LdapEntry> delEntries = deleteAll(ldapEntry, delDesc);
        for (int i = 0; i < delEntries.size(); i++) {
            LdapEntry delEntry = delEntries.get(i);
            createEntityFromLdapEntry(outRoot, SchemaConstants.DO_ENTITIES, delEntry, null);
            updateGroupMember(ldapEntry.getDN(), null);
            // Invalidate Attributes Cache.
            iLdapConn.invalidateAttributes(delEntry.getDN(), delEntry.getExtId(), delEntry.getUniqueName());
        }
        // Invalidate Names Cache
        iLdapConn.invalidateSearchCache();

        return outRoot;
    }

    /**
     * Delete the descendants of the specified ldap entry.
     *
     * @param ldapEntry
     * @param delDescendant
     * @return
     * @throws WIMException
     */
    private List<LdapEntry> deleteAll(LdapEntry ldapEntry, boolean delDescendant) throws WIMException {
        String dn = ldapEntry.getDN();
        List<LdapEntry> delEntries = new ArrayList<LdapEntry>();
        List<LdapEntry> descs = getDescendants(dn, SearchControls.ONELEVEL_SCOPE);
        if (descs.size() > 0) {
            if (delDescendant) {
                for (int i = 0; i < descs.size(); i++) {
                    LdapEntry descEntry = descs.get(i);
                    delEntries.addAll(deleteAll(descEntry, true));
                }
            } else {
                throw new EntityHasDescendantsException(WIMMessageKey.ENTITY_HAS_DESCENDENTS, Tr.formatMessage(tc, WIMMessageKey.ENTITY_HAS_DESCENDENTS,
                                                                                                               WIMMessageHelper.generateMsgParms(dn)));
            }
        }

        /* Call the preexit function */
        deletePreExit(dn);

        List<String> grpList = getGroups(dn);
        iLdapConn.getContextManager().destroySubcontext(dn);
        delEntries.add(ldapEntry);
        for (int i = 0; i < grpList.size(); i++) {
            String grpDN = grpList.get(i);
            iLdapConn.invalidateAttributes(grpDN, null, null);
        }
        return delEntries;
    }

    /**
     * This method is called just before an LDAP entry is deleted.
     *
     * @param ldapDN LDAP DN
     */
    protected void deletePreExit(String ldapDN) throws WIMException {}

    /**
     * Get all the descendants of the given DN.
     *
     * @param DN
     * @param level
     * @return
     * @throws WIMException
     */
    private List<LdapEntry> getDescendants(String DN, int level) throws WIMException {
        int scope = SearchControls.ONELEVEL_SCOPE;
        if (level == 0) {
            scope = SearchControls.SUBTREE_SCOPE;
        }

        List<LdapEntry> descendants = new ArrayList<LdapEntry>();

        Set<LdapEntry> ldapEntries = iLdapConn.searchEntities(DN, "objectClass=*", null, scope, null, null, false, false);
        for (Iterator<LdapEntry> iter = ldapEntries.iterator(); iter.hasNext();) {
            LdapEntry entry = iter.next();
            descendants.add(entry);
        }
        return descendants;
    }

    /**
     * Get the groups that contain the specified DN as its member.
     *
     * @param dn
     * @return
     * @throws WIMException
     */
    private List<String> getGroups(String dn) throws WIMException {
        List<String> grpList = new ArrayList<String>();
        String filter = iLdapConfigMgr.getGroupMemberFilter(dn);
        String[] searchBases = iLdapConfigMgr.getGroupSearchBases();
        for (int i = 0; i < searchBases.length; i++) {
            String searchBase = searchBases[i];
            NamingEnumeration<SearchResult> nenu = iLdapConn.search(searchBase, filter, SearchControls.SUBTREE_SCOPE,
                                                                    LDAP_ATTR_OBJECTCLASS_ARRAY);
            while (nenu.hasMoreElements()) {
                SearchResult thisEntry = nenu.nextElement();
                if (thisEntry == null) {
                    continue;
                }
                String entryName = thisEntry.getName();
                if (entryName == null || entryName.trim().length() == 0) {
                    continue;
                }
                grpList.add(LdapHelper.prepareDN(entryName, searchBase));
            }
        }
        return grpList;
    }

    /**
     * Update the Group member
     *
     * @param oldDN
     * @param newDN
     * @throws WIMException
     */
    private void updateGroupMember(String oldDN, String newDN) throws WIMException {
        if (!iLdapConfigMgr.updateGroupMembership()) {
            return;
        }
        String filter = iLdapConfigMgr.getGroupMemberFilter(oldDN);
        String[] mbrAttrs = iLdapConfigMgr.getMemberAttributes();
        Map<String, ModificationItem[]> mbrAttrMap = new HashMap<String, ModificationItem[]>(mbrAttrs.length);
        for (int i = 0; i < mbrAttrs.length; i++) {
            String mbrAttr = mbrAttrs[i];
            ModificationItem removeAttr = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, new BasicAttribute(mbrAttr, oldDN));
            ModificationItem[] modifAttrs = null;
            if (newDN != null) {
                ModificationItem addAttr = new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute(mbrAttr, newDN));
                modifAttrs = new ModificationItem[2];
                modifAttrs[0] = addAttr;
                modifAttrs[1] = removeAttr;
            } else {
                modifAttrs = new ModificationItem[1];
                modifAttrs[0] = removeAttr;
            }
            mbrAttrMap.put(mbrAttr.toLowerCase(), modifAttrs);
        }
        String[] searchBases = iLdapConfigMgr.getGroupSearchBases();
        for (int i = 0; i < searchBases.length; i++) {
            String searchBase = searchBases[i];
            NamingEnumeration<SearchResult> nenu = iLdapConn.search(searchBase, filter, SearchControls.SUBTREE_SCOPE,
                                                                    LDAP_ATTR_OBJECTCLASS_ARRAY);
            while (nenu.hasMoreElements()) {
                SearchResult thisEntry = nenu.nextElement();
                if (thisEntry == null) {
                    continue;
                }
                String entryName = thisEntry.getName();
                if (entryName == null || entryName.trim().length() == 0) {
                    continue;
                }
                String DN = LdapHelper.prepareDN(entryName, searchBase);
                Attributes attrs = thisEntry.getAttributes();
                String[] thisMbrAttrs = iLdapConfigMgr.getMemberAttribute(attrs.get(LDAP_ATTR_OBJECTCLASS));
                if (thisMbrAttrs != null) {
                    for (int j = 0; j < thisMbrAttrs.length; j++) {
                        ModificationItem[] attrsTobeModify = mbrAttrMap.get(thisMbrAttrs[j].toLowerCase());
                        if (attrsTobeModify != null) {
                            try {
                                iLdapConn.modifyAttributes(DN, attrsTobeModify);
                            } catch (Exception e) {
                                if (tc.isDebugEnabled()) {
                                    Tr.debug(tc, "Updating group " + DN + " for " + oldDN + " failed due to: " + e.toString());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public Root create(Root root) throws WIMException {
        // Only create first entity
        Entity entity = root.getEntities().get(0);
        String typeName = entity.getTypeName();
        Root outRoot = new Root();

        IdentifierType id = entity.getIdentifier();
        // entity.unset(DO_IDENTIFIER);

        // If external name is specified, directly use it to create LDAP entry. Unique name is ingored.
        String dn = id.getExternalName();
        String uniqueName = id.getUniqueName();

        LdapEntity ldapEntity = iLdapConfigMgr.getLdapEntity(typeName);
        if (ldapEntity == null) {
            throw new EntityTypeNotSupportedException(WIMMessageKey.ENTITY_TYPE_NOT_SUPPORTED, Tr.formatMessage(tc, WIMMessageKey.ENTITY_TYPE_NOT_SUPPORTED,
                                                                                                                WIMMessageHelper.generateMsgParms(typeName)));
        }
        String qEntityType = ldapEntity.getName();
        // defect 116261 if principalName is set, throw UpdatePropertyException
        if (iLdapConfigMgr.isPersonAccount(qEntityType)) {
            if (entity.get(SchemaConstants.PROP_PRINCIPAL_NAME) != null) {
                throw new UpdatePropertyException(WIMMessageKey.CAN_NOT_UPDATE_PROPERTY_IN_REPOSITORY, Tr.formatMessage(tc, WIMMessageKey.CAN_NOT_UPDATE_PROPERTY_IN_REPOSITORY,
                                                                                                                        WIMMessageHelper.generateMsgParms(SchemaConstants.PROP_PRINCIPAL_NAME,
                                                                                                                                                          reposId)));
            }
            if (entity.get(SchemaConstants.PROP_REALM) != null) {
                throw new UpdatePropertyException(WIMMessageKey.CAN_NOT_UPDATE_PROPERTY_IN_REPOSITORY, Tr.formatMessage(tc, WIMMessageKey.CAN_NOT_UPDATE_PROPERTY_IN_REPOSITORY,
                                                                                                                        WIMMessageHelper.generateMsgParms(SchemaConstants.PROP_REALM,
                                                                                                                                                          reposId)));
            }
        }
        List<String> propertyNames = Entity.getPropertyNames(entity.getTypeName());
        Attributes attrs = new BasicAttributes();
        List<Group> groups = null;
        List<Entity> members = null;

        if (propertyNames != null) {
            for (String propertyName : propertyNames) {
                if (entity.isMandatory(propertyName) && !entity.isSet(propertyName)) {
                    throw new MissingMandatoryPropertyException(WIMMessageKey.MISSING_MANDATORY_PROPERTY, Tr.formatMessage(tc, WIMMessageKey.MISSING_MANDATORY_PROPERTY,
                                                                                                                           WIMMessageHelper.generateMsgParms(propertyName)));
                }
                if (entity.isSet(propertyName)) {
                    if (entity.isPersistentProperty(propertyName)) {
                        Set<Attribute> attrSet = getAttribute(entity, null, propertyName, ldapEntity);
                        for (Iterator<Attribute> iter = attrSet.iterator(); iter.hasNext();) {
                            attrs.put(iter.next());
                        }
                    } else {
                        if (SchemaConstants.DO_MEMBERS.equals(propertyName)) {
                            members = ((Group) entity).getMembers();
                        }
                        // Assgin the entity to groups.
                        else if (SchemaConstants.DO_GROUPS.equals(propertyName)) {
                            groups = entity.getGroups();
                        }
                    }
                }
            }
        }

        if (dn == null) {
            dn = getDN(uniqueName, qEntityType, attrs, false, true);
        }
        // Add object class attr
        Attribute objClsAttr = ldapEntity.getObjectClassAttribute(dn);
        attrs.put(objClsAttr);

        // Generate exId if it is set to true
        String extIdName = ldapEntity.getExtId();
        String extId = null;
        LdapAttribute extIdAttr = iLdapConfigMgr.getAttribute(extIdName);
        if (extIdAttr != null && extIdAttr.isWIMGenerate()) {
            extId = UniqueIdGenerator.newUniqueId();
            attrs.put(new BasicAttribute(extIdName, extId));
        }
        boolean isGroup = iLdapConfigMgr.isGroup(qEntityType);

        // Add member attribute if it is group.
        if (members != null && members.size() > 0) {
            if (!isGroup) {
                throw new InvalidEntityTypeException(WIMMessageKey.ENTITY_IS_NOT_A_GROUP, Tr.formatMessage(tc, WIMMessageKey.ENTITY_IS_NOT_A_GROUP,
                                                                                                           WIMMessageHelper.generateMsgParms(uniqueName)));
            }
            // Use the first configured member attribute
            String mbrAttrName = iLdapConfigMgr.getMemberAttribute(objClsAttr)[0];
            if (members.size() > 0) {
                Attribute attr = new BasicAttribute(mbrAttrName);
                for (int j = 0; j < members.size(); j++) {
                    IdentifierType mbrId = members.get(j).getIdentifier();
                    LdapEntry ldapEntry = iLdapConn.getEntityByIdentifier(mbrId, null, null, false, false);
                    String mbrDN = ldapEntry.getDN();
                    attr.add(mbrDN);
                }
                attrs.put(attr);
            }
        } else if (isGroup) {
            // Use the first configured member attribute
            String mbrAttrName = iLdapConfigMgr.getMemberAttribute(objClsAttr)[0];
            // Add dummy member
            String dummyMbr = iLdapConfigMgr.getDummyMember(mbrAttrName);
            if (dummyMbr != null) {
                Attribute attr = new BasicAttribute(mbrAttrName, dummyMbr);
                attrs.put(attr);
            }
        }

        // Check if attributes have default value.
        Set<String> valueAttrs = iLdapConfigMgr.getAttributesWithDefaultValue();
        for (Iterator<String> iter = valueAttrs.iterator(); iter.hasNext();) {
            String valueAttr = iter.next();
            LdapAttribute ldapAttr = iLdapConfigMgr.getLdapAttribute(valueAttr);
            if (ldapAttr != null) {
                Object defaultValue = ldapAttr.getDefaultValue(qEntityType);
                if (defaultValue != null && !LdapHelper.inAttributes(valueAttr, attrs)) {
                    attrs.put(new BasicAttribute(ldapAttr.getName(), defaultValue));
                }
            }
        }

        // Check if attributes have default attribute.
        Set<String> attrAttrs = iLdapConfigMgr.getAttributesWithDefaultAttribute();
        for (Iterator<String> iter = attrAttrs.iterator(); iter.hasNext();) {
            String attrAttr = iter.next();
            LdapAttribute ldapAttr = iLdapConfigMgr.getLdapAttribute(attrAttr);
            if (ldapAttr != null) {
                String defaultAttr = ldapAttr.getDefaultAttribute(qEntityType);
                if (defaultAttr != null && !LdapHelper.inAttributes(attrAttr, attrs)) {
                    Attribute attr = attrs.get(defaultAttr);
                    if (attr != null) {
                        attrs.put(LdapHelper.cloneAttribute(ldapAttr.getName(), attr));
                    }
                }
            }
        }
        Attribute accCtrl = null;
        if (iLdapConfigMgr.getLdapType().equals("AD2003")) {
            // Active Directory 2003 does not allow userAccountControl when creating.
            accCtrl = attrs.remove(LDAP_ATTR_USER_ACCOUNT_CONTROL);
        }
        DirContext subCtx = iLdapConn.getContextManager().createSubcontext(dn, attrs);
        try {
            subCtx.close();
        } catch (NamingException e) {
            throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, Tr.formatMessage(tc, WIMMessageKey.NAMING_EXCEPTION, WIMMessageHelper.generateMsgParms(e.toString(true))));
        }

        if (accCtrl != null) {
            Attributes accCtrlAttrs = new BasicAttributes();
            accCtrlAttrs.put(accCtrl);
            try {
                iLdapConn.modifyAttributes(dn, DirContext.REPLACE_ATTRIBUTE, accCtrlAttrs);
            } catch (NamingException e) {
                throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, Tr.formatMessage(tc, WIMMessageKey.NAMING_EXCEPTION,
                                                                                              WIMMessageHelper.generateMsgParms(e.toString(true))));
            }
        }

        if (groups != null) {
            updateGroups(dn, groups, Service.ASSIGN_MODE);
        }

        if (extId == null) {
            extId = getExtId(dn, extIdName, null);
        }
        // Invalidate Names Cache
        iLdapConn.invalidateSearchCache();

        // Construct return data graph
        Entity returnEntity = new Entity();
        outRoot.getEntities().add(returnEntity);

        IdentifierType returnId = new IdentifierType();
        returnEntity.setIdentifier(returnId);
        if (iLdapConfigMgr.needTranslateRDN(typeName) && !iLdapConfigMgr.needTranslateRDN()) {
            returnId.setUniqueName(iLdapConfigMgr.switchToNode(dn));
        } else {
            returnId.setUniqueName(uniqueName);
        }
        returnId.setExternalName(dn);
        returnId.setExternalId(extId);
        return outRoot;
    }

    @SuppressWarnings("unchecked")
    private Set<Attribute> getAttribute(Entity entity, Object values, String propertyName, LdapEntity ldapEntity) throws WIMException {
        Map<String, Attribute> attrMap = new HashMap<String, Attribute>();

        // Check if the property is supported or not
        List<String> props = new ArrayList<String>(1);
        props.add(propertyName);
        List<String> supportedProp = iLdapConfigMgr.getSupportedProperties(ldapEntity, props);
        if (supportedProp == null || supportedProp.size() == 0)
            return new HashSet<Attribute>();

        String attrName = iLdapConfigMgr.getAttributeName(ldapEntity, propertyName);
        String dataType = entity.getDataType(propertyName);
        LdapAttribute ldapAttr = iLdapConfigMgr.getLdapAttribute(attrName);
        String ldapType = iLdapConfigMgr.getLdapType();
        List<Object> attrValues = null;
        if (entity.get(propertyName) instanceof List) {
            if (values != null) {
                attrValues = (List<Object>) values;
            } else {
                attrValues = (List<Object>) entity.get(propertyName);
            }
        } else if (SchemaConstants.PROP_PASSWORD.equalsIgnoreCase(propertyName)) {
            attrValues = new ArrayList<Object>(1);
            attrValues.add(new String((byte[]) entity.get(propertyName)));
        } else {
            attrValues = new ArrayList<Object>(1);
            if (values != null) {
                attrValues.add(values);
            } else {
                attrValues.add(entity.get(propertyName));
            }
        }

        for (int i = 0; i < attrValues.size(); i++) {
            String newAttrName = attrName;
            Object value = attrValues.get(i);
            Object ldapValue = null;
            if (SchemaConstants.DATA_TYPE_STRING.equals(dataType)) {
                ldapValue = LdapHelper.getStringLdapValue(value, ldapAttr, ldapType);
            } else if (SchemaConstants.DATA_TYPE_DATE_TIME.equals(dataType)) {
                ldapValue = LdapHelper.getDateLdapValue(value, ldapAttr, ldapType);
            } else if (SchemaConstants.DATA_TYPE_IDENTIFIER_TYPE.equals(dataType)) {
                if (value != null) {
                    IdentifierType id = (IdentifierType) value;
                    try {
                        LdapEntry ldapEntry = iLdapConn.getEntityByIdentifier(id, null, null, false, false);
                        ldapValue = ldapEntry.getDN();
                    } catch (EntityNotFoundException e) {
                        e.getMessage();
                        throw new InvalidPropertyValueException(WIMMessageKey.INVALID_PROPERTY_VALUE, Tr.formatMessage(tc, WIMMessageKey.INVALID_PROPERTY_VALUE,
                                                                                                                       WIMMessageHelper.generateMsgParms(propertyName,
                                                                                                                                                         entity.getTypeName())));
                    }
                }
            } else if (SchemaConstants.DATA_TYPE_BASE_64_BINARY.equals(dataType)) {
                if (ldapAttr != null
                    && LdapConstants.LDAP_ATTR_SYNTAX_UNICODEPWD.equalsIgnoreCase(ldapAttr.getSyntax())) {
                    // The the LDAP attribute is unicodePwd, need to convert it to special byte array format.
                    ldapValue = LdapHelper.encodePassword(new String((byte[]) value));
                } else {
                    ldapValue = value;
                }
            } else if (SchemaConstants.DATA_TYPE_INT.equals(dataType)) {
                ldapValue = LdapHelper.getIntLdapValue(value, ldapAttr, ldapType);
            } else if (SchemaConstants.DATA_TYPE_LANG_TYPE.equals(dataType)) {
                LangType langDO = (LangType) value;
                ldapValue = langDO.getValue();
            } else if (SchemaConstants.DATA_TYPE_BOOLEAN.equals(dataType)) {
                /*
                 * Some servers (OpenLDAP, Micorosoft Active Directory, among others) won't accept lower-case Boolean values
                 * according to RFC 4517, section 3.3.3, where the ABNF is: Boolean = "TRUE" / "FALSE"
                 */
                ldapValue = value.toString().toUpperCase();
            } else {
                ldapValue = value.toString();
            }
            if (ldapValue != null) {
                Attribute attr = attrMap.get(newAttrName);
                if (attr == null) {
                    attr = new BasicAttribute(newAttrName);
                    attrMap.put(newAttrName, attr);
                }
                attr.add(ldapValue);
            }
        }

        Set<Attribute> attrSet = new HashSet<Attribute>(attrMap.values());
        return attrSet;
    }

    @FFDCIgnore({ NoSuchAttributeException.class, OperationNotSupportedException.class,
                  AttributeInUseException.class, NameAlreadyBoundException.class })
    private void updateGroups(String mbrDn, List<Group> groups, int modifiedMode) throws WIMException {
        final String METHODNAME = "updateGroups(String, List, int)";
        if (groups.size() > 0) {
            String mbrAttr = null;
            List<String> grpTypes = iLdapConfigMgr.getGroupTypes();
            for (Group group : groups) {
                IdentifierType id = group.getIdentifier();
                LdapEntry ldapEntry = iLdapConn.getEntityByIdentifier(id, grpTypes, null, false, false);
                if (!iLdapConfigMgr.isGroup(ldapEntry.getType())) {
                    throw new InvalidEntityTypeException(WIMMessageKey.ENTITY_IS_NOT_A_GROUP, Tr.formatMessage(tc, WIMMessageKey.ENTITY_IS_NOT_A_GROUP,
                                                                                                               WIMMessageHelper.generateMsgParms(id.getUniqueName())));
                }
                String dn = ldapEntry.getDN();
                // there are multiple mbr attrs, need always retrieve object classes
                if (mbrAttr == null) {
                    Attributes attrs = ldapEntry.getAttributes();
                    Attribute objClsAttr = attrs.get(LDAP_ATTR_OBJECTCLASS);
                    mbrAttr = iLdapConfigMgr.getMemberAttribute(objClsAttr)[0];
                }

                Attributes grpMbrAttrs = new BasicAttributes();
                Attribute grpMbrAttr = new BasicAttribute(mbrAttr, mbrDn);
                grpMbrAttrs.put(grpMbrAttr);
                try {
                    if (Service.UNASSIGN_MODE == modifiedMode) {
                        try {
                            iLdapConn.modifyAttributes(dn, DirContext.REMOVE_ATTRIBUTE, grpMbrAttrs);
                        } catch (NoSuchAttributeException e) {
                            if (tc.isDebugEnabled()) {
                                Tr.debug(tc, METHODNAME + " Entity " + mbrDn + " is already not a member of group " + dn
                                             + ":" + e.toString(true));
                            }
                        } catch (OperationNotSupportedException e) {
                            if (tc.isDebugEnabled()) {
                                Tr.debug(tc, METHODNAME + " Entity " + mbrDn + " is already not a member of group " + dn
                                             + ":" + e.toString(true));
                            }
                        }

                    } else if (Service.REPLACE_ASSIGN_MODE == modifiedMode) {
                        try {
                            iLdapConn.modifyAttributes(dn, DirContext.REPLACE_ATTRIBUTE, grpMbrAttrs);
                        } catch (NoSuchAttributeException e) {
                            if (tc.isDebugEnabled()) {
                                Tr.debug(tc, METHODNAME + " Entity " + mbrDn + " is already not a member of group " + dn
                                             + ":" + e.toString(true));
                            }

                        } catch (OperationNotSupportedException e) {
                            if (tc.isDebugEnabled()) {
                                Tr.debug(tc, METHODNAME + " Entity " + mbrDn + " is already not a member of group " + dn
                                             + ":" + e.toString(true));
                            }
                        }
                    } else {
                        // Default is assign mode
                        try {
                            iLdapConn.modifyAttributes(dn, DirContext.ADD_ATTRIBUTE, grpMbrAttrs);
                        } catch (AttributeInUseException e) {
                            if (tc.isDebugEnabled()) {
                                Tr.debug(tc, METHODNAME + " Entity " + mbrDn + " is already a member of group " + dn
                                             + ":" + e.toString(true));
                            }
                        } catch (NameAlreadyBoundException e) {
                            if (tc.isDebugEnabled()) {
                                Tr.debug(tc, METHODNAME + " Entity " + mbrDn + " is already a member of group" + dn
                                             + ":" + e.toString(true));
                            }
                        }
                    }
                } catch (NamingException e) {
                    throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, Tr.formatMessage(tc, WIMMessageKey.NAMING_EXCEPTION,
                                                                                                  WIMMessageHelper.generateMsgParms(e.toString(true))));
                }
                iLdapConn.invalidateAttributes(dn, ldapEntry.getExtId(), ldapEntry.getUniqueName());
            }
        }
    }

    private String getExtId(String dn, String extIdName, Attributes attrs) throws WIMException {
        if (LDAP_DN.equalsIgnoreCase(extIdName)) {
            return LdapHelper.toUpperCase(dn);
        }
        Attribute extIdAttr = null;
        if (attrs != null) {
            extIdAttr = attrs.get(extIdName);
        }
        if (extIdAttr == null) {
            // Go to LDAP server retrieve
            String[] attrIds = { extIdName };
            attrs = iLdapConn.checkAttributesCache(dn, attrIds);
            extIdAttr = attrs.get(extIdName);
        }

        if (extIdAttr == null) {
            return LdapHelper.toUpperCase(dn);
        } else if (extIdAttr.size() > 1) {
            throw new WIMSystemException(WIMMessageKey.EXT_ID_HAS_MULTIPLE_VALUES, Tr.formatMessage(tc, WIMMessageKey.EXT_ID_HAS_MULTIPLE_VALUES,
                                                                                                    WIMMessageHelper.generateMsgParms(extIdAttr)));
        }
        LdapAttribute ldapAttr = iLdapConfigMgr.getAttribute(extIdName);
        try {
            Object val = extIdAttr.get();
            if (ldapAttr != null && LDAP_ATTR_SYNTAX_OCTETSTRING.equalsIgnoreCase(ldapAttr.getSyntax())) {
                return LdapHelper.getOctetString((byte[]) val);
            } else if ((ldapAttr != null) && (LDAP_ATTR_SYNTAX_GUID.equalsIgnoreCase(ldapAttr.getSyntax()))) {
                return LdapHelper.convertToDashedString((byte[]) val);
            } else {
                return val.toString();
            }
        } catch (NamingException e) {
            throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, Tr.formatMessage(tc, WIMMessageKey.NAMING_EXCEPTION,
                                                                                          WIMMessageHelper.generateMsgParms(e.toString(true))));
        }
    }

    @Override
    public Root update(Root root) throws WIMException {
        final String METHODNAME = "update";

        Map<String, Control> ctrlMap = ControlsHelper.getControlMap(root);
        CacheControl cacheCtrl = (CacheControl) ctrlMap.get(SchemaConstants.DO_CACHE_CONTROL);

        // If a cache control is passed, the cache should be cleared
        if (cacheCtrl != null) {
            String cacheMode = cacheCtrl.getMode();
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, METHODNAME + " Cache Control is passed with mode " + cacheMode);
            }

            // If the mode is 'clearAll', then invalidate the caches
            if (cacheMode != null && SchemaConstants.CACHE_MODE_CLEARALL.equalsIgnoreCase(cacheMode)) {
                // Invalidate the attributes cache
                iLdapConn.invalidateAttributeCache();

                // Invalidate the search cache
                iLdapConn.invalidateSearchCache();

                // Log this clearAll call
                String uniqueName = getCallerUniqueName();
                if (tc.isWarningEnabled())
                    Tr.warning(tc, WIMMessageKey.CLEAR_ALL_CLEAR_CACHE_MODE,
                               WIMMessageHelper.generateMsgParms(reposId, cacheMode, uniqueName));
            }
            // If the mode is 'clearEntity', then invalidate the attribute cache for the DN
            else if (cacheMode != null && SchemaConstants.CACHE_MODE_CLEAR_ENTITY.equalsIgnoreCase(cacheMode)) {
                // Clear only the first entity
                Entity inEntity = root.getEntities().get(0);

                // Get the identifier
                IdentifierType id = inEntity.getIdentifier();

                // Get the external name
                String externalName = id.getExternalName();

                // Get the external Id
                String extId = id.getExternalId();

                // Get the uniqueName
                String uniqueName = id.getUniqueName();

                // Invalidate attibutes
                iLdapConn.invalidateAttributes(externalName, extId, uniqueName);

                // Invalidate the search cache
                iLdapConn.invalidateSearchCache();
            } else if (tc.isWarningEnabled()) {
                Tr.warning(tc, WIMMessageKey.UNKNOWN_CLEAR_CACHE_MODE,
                           WIMMessageHelper.generateMsgParms(reposId, cacheMode));
            }

            return null;
        }

        Root outRoot = updateEntity(root);

        return outRoot;
    }

    private Root updateEntity(Root root) throws WIMException {
        final String METHODNAME = "updateEntity";

        Root outRoot = new Root();

        // Only update first entity
        Entity inEntity = root.getEntities().get(0);
        String inEntityType = inEntity.getTypeName();

        // If principalName is set, throw UpdatePropertyException
        if (inEntity.getSuperTypes().contains(SchemaConstants.DO_LOGIN_ACCOUNT) && inEntity.isSet(SchemaConstants.PROP_PRINCIPAL_NAME)) {
            throw new UpdatePropertyException(WIMMessageKey.CAN_NOT_UPDATE_PROPERTY_IN_REPOSITORY, Tr.formatMessage(tc, WIMMessageKey.CAN_NOT_UPDATE_PROPERTY_IN_REPOSITORY,
                                                                                                                    WIMMessageHelper.generateMsgParms(SchemaConstants.PROP_PRINCIPAL_NAME,
                                                                                                                                                      reposId)));
        }

        List<String> inEntityTypes = new ArrayList<String>(1);
        inEntityTypes.add(inEntityType);
        IdentifierType inId = inEntity.getIdentifier();
        LdapEntry ldapEntry = iLdapConn.getEntityByIdentifier(inId, inEntityTypes, null, false, false);

        // If external name is specified, directly use it to create LDAP entry. Unique name is ingored.
        String dn = ldapEntry.getDN();
        String extId = ldapEntry.getExtId();
        String uniqueName = ldapEntry.getUniqueName();
        String outEntityType = ldapEntry.getType();

        LdapEntity ldapEntity = iLdapConfigMgr.getLdapEntity(outEntityType);
        Map<String, Control> ctrlMap = ControlsHelper.getControlMap(root);
        GroupMemberControl grpMbrCtrl = (GroupMemberControl) ctrlMap.get(SchemaConstants.DO_GROUP_MEMBER_CONTROL);
        GroupMembershipControl grpMbrshipCtrl = (GroupMembershipControl) ctrlMap.get(SchemaConstants.DO_GROUP_MEMBERSHIP_CONTROL);

        List<String> propertyNames = Entity.getPropertyNames(inEntityType);
        List<ModificationItem> modItemList = new ArrayList<ModificationItem>();
        List<Group> groups = null;
        List<Entity> members = null;

        String[] rdnAttrNames = LdapHelper.getRDNAttributes(dn);
        String[] rdnAttrValues = null;
        for (String propertyName : propertyNames) {
            boolean isSet = inEntity.isSet(propertyName);
            boolean isUnset = inEntity.isUnset(propertyName);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, METHODNAME + " Property '" + propertyName + "': Set? " + isSet + ", Unset? " + isUnset);
            }
            if (isSet || isUnset) {
                if (inEntity.isPersistentProperty(propertyName)) {
                    Set<Attribute> attrSet = null;

                    if (isSet) {
                        attrSet = getAttribute(inEntity, null, propertyName, ldapEntity);
                    } else if (isUnset) {
                        String attrName = iLdapConfigMgr.getAttributeName(ldapEntity, propertyName);

                        /*
                         * Don't unset RDN attributes.
                         */
                        if (!isRDN(attrName, rdnAttrNames)) {
                            Attribute attr = new BasicAttribute(attrName);
                            attrSet = new HashSet<Attribute>();
                            attrSet.add(attr);
                        }
                    }

                    /*
                     * Was there an LDAP attribute mapped to the WIM property? There is nothing
                     * to update if there was not.
                     */
                    if (attrSet != null) {
                        for (Attribute attr : attrSet) {
                            if (tc.isDebugEnabled()) {
                                Tr.debug(tc, METHODNAME + " Property '" + propertyName + "': Attribute: " + attr);
                            }

                            boolean isRdnAttr = false;
                            for (int j = 0; j < rdnAttrNames.length; j++) {
                                if (attr.getID().equalsIgnoreCase(rdnAttrNames[j])) {
                                    if (rdnAttrValues == null) {
                                        rdnAttrValues = new String[rdnAttrNames.length];
                                    }
                                    try {
                                        rdnAttrValues[j] = (String) attr.get();
                                    } catch (NamingException e) {
                                        throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, Tr.formatMessage(tc, WIMMessageKey.NAMING_EXCEPTION,
                                                                                                                      WIMMessageHelper.generateMsgParms(e.toString(true))));
                                    } catch (NoSuchElementException nse) {
                                        nse.getMessage();
                                        throw new UpdatePropertyException(WIMMessageKey.MISSING_MANDATORY_PROPERTY, Tr.formatMessage(tc, WIMMessageKey.MISSING_MANDATORY_PROPERTY,
                                                                                                                                     WIMMessageHelper.generateMsgParms(attr.getID())));
                                    }
                                    isRdnAttr = true;
                                }
                            }

                            /*
                             * Dont set or unset RDN attributes.
                             */
                            if (!isRdnAttr) {
                                if (isSet) {
                                    // If attribute was set by caller then replace it
                                    if (tc.isDebugEnabled()) {
                                        Tr.debug(tc, METHODNAME + " The following LDAP attribute is to be set: " + attr);
                                    }
                                    modItemList.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, attr));
                                } else if (isUnset) {
                                    // If attribute was unset by caller then remove it
                                    if (tc.isDebugEnabled()) {
                                        Tr.debug(tc, METHODNAME + " The following LDAP attribute is to be unset: " + attr);
                                    }
                                    modItemList.add(new ModificationItem(DirContext.REMOVE_ATTRIBUTE, attr));
                                }
                            }
                        }
                    }
                } else {
                    if (SchemaConstants.DO_MEMBERS.equals(propertyName)) {
                        members = ((Group) inEntity).getMembers();
                    }
                    // Assign the entity to groups.
                    else if (SchemaConstants.DO_GROUPS.equals(propertyName)) {
                        groups = inEntity.getGroups();
                    }
                }
            }
        }

        boolean updateMbr = false;
        int grpMbrMode = Service.ASSIGN_MODE;
        // Add member attribute if it is group.
        if (members != null && members.size() > 0) {
            if (!iLdapConfigMgr.isGroup(outEntityType)) {
                throw new InvalidEntityTypeException(WIMMessageKey.ENTITY_IS_NOT_A_GROUP, Tr.formatMessage(tc, WIMMessageKey.ENTITY_IS_NOT_A_GROUP,
                                                                                                           WIMMessageHelper.generateMsgParms(ldapEntry.getUniqueName())));
            }
            String[] mbrAttrNames = iLdapConfigMgr.getMemberAttribute(ldapEntry.getAttributes().get(LDAP_ATTR_OBJECTCLASS));

            // Use the first configured attribute
            Attribute attr = new BasicAttribute(mbrAttrNames[0]);

            if (grpMbrCtrl != null) {
                grpMbrMode = grpMbrCtrl.getModifyMode();
            }

            for (Entity member : members) {
                IdentifierType mbrId = member.getIdentifier();
                LdapEntry mbrEntry = iLdapConn.getEntityByIdentifier(mbrId, null, null, false, false);
                String mbrDN = mbrEntry.getDN();
                iLdapConn.invalidateAttributes(mbrDN, mbrEntry.getExtId(), mbrEntry.getUniqueName());
                attr.add(mbrDN);
            }
            if (Service.UNASSIGN_MODE == grpMbrMode) {
                modItemList.add(new ModificationItem(DirContext.REMOVE_ATTRIBUTE, attr));
                // Add a default member
                String dummyMbr = iLdapConfigMgr.getDummyMember(mbrAttrNames[0]);
                if (dummyMbr != null) {
                    Attribute dummyMbrAttr = new BasicAttribute(mbrAttrNames[0], dummyMbr);
                    modItemList.add(new ModificationItem(DirContext.ADD_ATTRIBUTE, dummyMbrAttr));
                }
            } else if (Service.REPLACE_ASSIGN_MODE == grpMbrMode) {
                modItemList.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, attr));
            } else {
                modItemList.add(new ModificationItem(DirContext.ADD_ATTRIBUTE, attr));
            }
            updateMbr = true;
        }

        if (modItemList.size() > 0) {
            ModificationItem[] items = {};
            items = modItemList.toArray(items);
            try {
                iLdapConn.modifyAttributes(dn, items);
            } catch (AttributeInUseException e) {
                if (updateMbr && Service.ASSIGN_MODE == grpMbrMode) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, METHODNAME + " Entity is already a member of group "
                                     + dn + ":" + e.toString(true));
                    }
                } else {
                    throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, Tr.formatMessage(tc, WIMMessageKey.NAMING_EXCEPTION,
                                                                                                  WIMMessageHelper.generateMsgParms(e.toString(true))));
                }
            } catch (NoSuchAttributeException e) {
                if (updateMbr && Service.UNASSIGN_MODE == grpMbrMode) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, METHODNAME + " Entity is already not a member of group "
                                     + dn + ":" + e.toString(true));
                    }
                } else {
                    throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, Tr.formatMessage(tc, WIMMessageKey.NAMING_EXCEPTION,
                                                                                                  WIMMessageHelper.generateMsgParms(e.toString(true))));
                }
            } catch (OperationNotSupportedException e) {
                if (updateMbr && Service.UNASSIGN_MODE == grpMbrMode) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, METHODNAME + " Entity is already not a member of group "
                                     + dn + ":" + e.toString(true));
                    }
                } else {
                    throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, Tr.formatMessage(tc, WIMMessageKey.NAMING_EXCEPTION,
                                                                                                  WIMMessageHelper.generateMsgParms(e.toString(true))));
                }
            } catch (NamingException e) {
                throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, Tr.formatMessage(tc, WIMMessageKey.NAMING_EXCEPTION,
                                                                                              WIMMessageHelper.generateMsgParms(e.toString(true))));
            }
        }
        String newDn = null;
        boolean renamed = false;
        if (rdnAttrValues != null) {
            newDn = LdapHelper.replaceRDN(dn, rdnAttrNames, rdnAttrValues);
            if (!newDn.equalsIgnoreCase(dn)) {
                iLdapConn.rename(dn, newDn);
                updateGroupMember(dn, newDn);
                // Re-read to update
                List<String> entityTypes = new ArrayList<String>(1);
                entityTypes.add(outEntityType);
                ldapEntry = iLdapConn.getEntityByIdentifier(newDn, null, null, entityTypes, null, false, false);
                renamed = true;
            }
        }

        if (groups != null) {
            int grpMbrshipMode = Service.ASSIGN_MODE;
            if (grpMbrshipCtrl != null) {
                grpMbrshipMode = grpMbrshipCtrl.getModifyMode();
            }
            updateGroups(renamed ? newDn : dn, groups, grpMbrshipMode);
        }

        iLdapConn.invalidateAttributes(dn, extId, uniqueName);

        // Invalidate Names Cache
        iLdapConn.invalidateSearchCache();

        createEntityFromLdapEntry(outRoot, SchemaConstants.DO_ENTITIES, ldapEntry, null);
        return outRoot;
    }

    /**
     * @param sFilter
     * @param doPersonAccount
     */
    private String setAttributeNamesInFilter(String sFilter, String entityType) {
        // Find the attribute operators and identify the attributes in the expression
        int index = -1;
        int attrIndex = -1;
        String name = null;
        String attributeName = null;
        LdapEntity ldapEntity = iLdapConfigMgr.getLdapEntity(entityType);

        // While there is another operator
        while (sFilter.indexOf("~=", index) > -1 ||
               sFilter.indexOf(">=", index) > -1 ||
               sFilter.indexOf("<=", index) > -1 ||
               sFilter.indexOf("=", index) > -1) {

            if (sFilter.indexOf("~=", index) > -1) {
                index = sFilter.indexOf("~=", index);
            } else if (sFilter.indexOf(">=", index) > -1) {
                index = sFilter.indexOf(">=", index);
            } else if (sFilter.indexOf("<=", index) > -1) {
                index = sFilter.indexOf("<=", index);
            } else if (sFilter.indexOf("=", index) > -1) {
                index = sFilter.indexOf("=", index);
            }

            attrIndex = index;
            while (attrIndex > -1 && sFilter.charAt(attrIndex) != '(') {
                attrIndex--;
            }

            name = sFilter.substring(attrIndex + 1, index);
            attributeName = iLdapConfigMgr.getAttributeName(ldapEntity, name);
            sFilter = sFilter.substring(0, attrIndex + 1) + attributeName + sFilter.substring(index);

            index = sFilter.indexOf(')', index);
            if (index == -1)
                index = sFilter.length() - 1;
        }

        return sFilter;
    }

    /**
     * @param propertyName
     * @param rdnAttrNames
     * @return
     */
    private boolean isRDN(String attrName, String[] rdnAttrNames) {
        if (rdnAttrNames == null || attrName == null)
            return false;

        for (int i = 0; i < rdnAttrNames.length; i++) {
            if (attrName.equalsIgnoreCase(rdnAttrNames[i]))
                return true;
        }

        return false;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, target = "(id=unbound)")
    protected void setCertificateMapper(X509CertificateMapper mapper) {
        iCertificateMapperRef.set(mapper);
    }

    protected void unsetCertificateMapper(X509CertificateMapper mapper) {
        iCertificateMapperRef.compareAndSet(mapper, null);
    }
}
