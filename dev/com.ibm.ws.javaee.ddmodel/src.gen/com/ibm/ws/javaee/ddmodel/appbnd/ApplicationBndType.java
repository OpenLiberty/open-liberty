/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
// NOTE: This is a generated file. Do not edit it directly.
package com.ibm.ws.javaee.ddmodel.appbnd;

import com.ibm.ws.javaee.dd.appbnd.ApplicationBnd;
import com.ibm.ws.javaee.dd.appbnd.Profile;
import com.ibm.ws.javaee.dd.appbnd.SecurityRole;
import com.ibm.ws.javaee.ddmodel.CrossComponentReferenceType;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.commonbnd.RefBindingsGroupType;

public class ApplicationBndType 
    extends RefBindingsGroupType
    implements ApplicationBnd, DDParser.RootParsable {

    public ApplicationBndType(String ddPath) {
        this(ddPath, !IS_XMI);
    }

    public static final boolean IS_XMI = true;

    public ApplicationBndType(String ddPath, boolean xmi) {
        super(xmi);
        this.deploymentDescriptorPath = ddPath;
    }

    private final String deploymentDescriptorPath;
    private DDParser.ComponentIDMap idMap;
    private CrossComponentReferenceType xmiRef;
    com.ibm.ws.javaee.ddmodel.StringType version;
    DDParser.ParsableListImplements<SecurityRoleType, SecurityRole> security_role;
    DDParser.ParsableListImplements<ProfileType, Profile> profile;
    com.ibm.ws.javaee.ddmodel.commonbnd.JASPIRefType jaspi_ref;
    com.ibm.ws.javaee.ddmodel.StringType appName;
    AuthorizationTableXMIType authorizationTable;
    RunAsMapXMIType runAsMap;

    @Override
    public java.lang.String getVersion() {
        return xmi ? "XMI" : version != null ? version.getValue() : null;
    }

    @Override
    public java.util.List<com.ibm.ws.javaee.dd.appbnd.SecurityRole> getSecurityRoles() {
        if (security_role != null) {
            return security_role.getList();
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public java.util.List<com.ibm.ws.javaee.dd.appbnd.Profile> getProfiles() {
        if (profile != null) {
            return profile.getList();
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public com.ibm.ws.javaee.dd.commonbnd.JASPIRef getJASPIRef() {
        return jaspi_ref;
    }

    @Override
    public String getDeploymentDescriptorPath() {
        return deploymentDescriptorPath;
    }

    @Override
    public Object getComponentForId(String id) {
        return idMap.getComponentForId(id);
    }

    @Override
    public String getIdForComponent(Object ddComponent) {
        return idMap.getIdForComponent(ddComponent);
    }

    /**
     * Override: Ensure that the version is assigned.
     * 
     * Use the version computed by the parser to ensure
     * the local version variable is assigned. 
     */
    @Override
    public void finish(DDParser parser) throws DDParser.ParseException {
        super.finish(parser);

        if ( version == null ) {
            version = parser.parseToken( parser.getDottedVersionText() );            
        }
        this.idMap = parser.idMap;

        if (xmi) {
            java.util.Map<String, SecurityRoleType> roleMap = new java.util.HashMap<String, SecurityRoleType>();
            if (authorizationTable != null && authorizationTable.authorizations != null) {
                for (AuthorizationTableXMIType.AuthorizationXMIType auth : authorizationTable.authorizations) {
                    SecurityRoleType role = getSecurityRole(auth.roleName, roleMap);
                    if (auth.users != null) {
                        for (UserType user : auth.users) {
                            role.addUser(user);
                        }
                    }
                    if (auth.groups != null) {
                        for (GroupType group : auth.groups) {
                            role.addGroup(group);
                        }
                    }
                    if (auth.specialSubjects != null) {
                        for (SpecialSubjectType specialSubject : auth.specialSubjects) {
                            role.addSpecialSubject(specialSubject);
                        }
                    }
                }
            }
            if (runAsMap != null && runAsMap.runAsBindings != null) {
                for (RunAsMapXMIType.RunAsBindingXMIType runAs : runAsMap.runAsBindings) {
                    SecurityRoleType role = getSecurityRole(runAs.securityRoleName, roleMap);
                    role.run_as = runAs.authData;
                }
            }
        } else {
            if ( version == null ) {
                String versionText;
                switch ( parser.version ) {
                case ApplicationBnd.VERSION_1_2:
                    versionText = ApplicationBnd.VERSION_1_2_STR;
                    break;
                case ApplicationBnd.VERSION_1_3:
                    versionText = ApplicationBnd.VERSION_1_3_STR;
                    break;
                case ApplicationBnd.VERSION_1_4:
                    versionText = ApplicationBnd.VERSION_1_4_STR;
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported application bindings version [ " + parser.version + " ]");
                }
                version = parser.parseToken(versionText);
            }
        }
    }

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    private SecurityRoleType getSecurityRole(com.ibm.ws.javaee.ddmodel.StringType name, java.util.Map<String, SecurityRoleType> roleMap) {
        String nameValue = name == null ? null : name.getValue();
        SecurityRoleType role = roleMap.get(nameValue);
        if (role == null) {
            role = new SecurityRoleType();
            role.name = name;
            roleMap.put(nameValue, role);
            if (security_role == null) {
                security_role = new DDParser.ParsableListImplements<SecurityRoleType, com.ibm.ws.javaee.dd.appbnd.SecurityRole>();
            }
            security_role.add(role);
        }
        return role;
    }

    @Override
    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws DDParser.ParseException {
        if (nsURI == null) {
            if (!xmi && "version".equals(localName)) {
                this.version = parser.parseStringAttributeValue(index);
                return true;
            }
            if (xmi && "appName".equals(localName)) {
                this.appName = parser.parseStringAttributeValue(index);
                return true;
            }
        }
        if (xmi && "http://www.omg.org/XMI".equals(nsURI)) {
            if ("version".equals(localName)) {
                // Allowed but ignored.
                return true;
            }
        }
        return super.handleAttribute(parser, nsURI, localName, index);
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws DDParser.ParseException {
        if (xmi && "application".equals(localName)) {
            xmiRef = new com.ibm.ws.javaee.ddmodel.CrossComponentReferenceType("application", com.ibm.ws.javaee.dd.app.Application.class);
            parser.parse(xmiRef);
            return true;
        }
        if (!xmi && "security-role".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.appbnd.SecurityRoleType security_role = new com.ibm.ws.javaee.ddmodel.appbnd.SecurityRoleType();
            parser.parse(security_role);
            this.addSecurityRole(security_role);
            return true;
        }
        if (!xmi && "profile".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.appbnd.ProfileType profile = new com.ibm.ws.javaee.ddmodel.appbnd.ProfileType();
            parser.parse(profile);
            this.addProfile(profile);
            return true;
        }
        if (!xmi && "jaspi-ref".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.commonbnd.JASPIRefType jaspi_ref = new com.ibm.ws.javaee.ddmodel.commonbnd.JASPIRefType();
            parser.parse(jaspi_ref);
            this.jaspi_ref = jaspi_ref;
            return true;
        }
        if (xmi && "authorizationTable".equals(localName)) {
            authorizationTable = new AuthorizationTableXMIType();
            parser.parse(authorizationTable);
            return true;
        }
        if (xmi && "runAsMap".equals(localName)) {
            runAsMap = new RunAsMapXMIType();
            parser.parse(runAsMap);
            return true;
        }
        return !xmi && super.handleChild(parser, localName);
    }

    void addSecurityRole(com.ibm.ws.javaee.ddmodel.appbnd.SecurityRoleType security_role) {
        if (this.security_role == null) {
            this.security_role = new DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.appbnd.SecurityRoleType, com.ibm.ws.javaee.dd.appbnd.SecurityRole>();
        }
        this.security_role.add(security_role);
    }

    void addProfile(com.ibm.ws.javaee.ddmodel.appbnd.ProfileType profile) {
        if (this.profile == null) {
            this.profile = new DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.appbnd.ProfileType, com.ibm.ws.javaee.dd.appbnd.Profile>();
        }
        this.profile.add(profile);
    }

    @Override
    public void describe(com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics diag) {
        diag.describeIfSet("application", xmiRef);
        diag.describeIfSet("version", version);
        diag.describeIfSet("appName", appName);
        diag.describeIfSet("security-role", security_role);
        diag.describeIfSet("profile", profile);
        diag.describeIfSet("jaspi-ref", jaspi_ref);
    }

    @Override
    public void describe(StringBuilder sb) {
        DDParser.Diagnostics diag = new DDParser.Diagnostics(idMap, sb);
        diag.describe(toTracingSafeString(), this);
    }
}
