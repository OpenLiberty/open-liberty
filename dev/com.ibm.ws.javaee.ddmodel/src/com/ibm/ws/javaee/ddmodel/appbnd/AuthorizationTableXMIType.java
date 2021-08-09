/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.appbnd;

import com.ibm.ws.javaee.dd.app.Application;
import com.ibm.ws.javaee.dd.appbnd.SpecialSubject;
import com.ibm.ws.javaee.dd.common.SecurityRole;
import com.ibm.ws.javaee.ddmodel.CrossComponentReferenceType;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.StringType;

/**
 * Manual implementation of the authorizationTable XMI element. The contents of
 * this object and {@link RunAsMapXMIType} are merged by
 * the {@link ApplicationBndType#finish} method to form the result of
 * the {@link com.ibm.ws.javaee.dd.appbnd.ApplicationBnd#getSecurityRoles} method.
 */
public class AuthorizationTableXMIType extends DDParser.ElementContentParsable {
    static class AuthorizationXMIType extends DDParser.ElementContentParsable {
        DDParser.ParsableList<UserType> users;
        DDParser.ParsableList<GroupType> groups;
        DDParser.ParsableList<SpecialSubjectXMIType> specialSubjects;
        StringType roleName;
        CrossComponentReferenceType role;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("users".equals(localName)) {
                UserType user = new UserType(true);
                parser.parse(user);
                addUser(user);
                return true;
            }
            if ("groups".equals(localName)) {
                GroupType group = new GroupType(true);
                parser.parse(group);
                addGroup(group);
                return true;
            }
            if ("specialSubjects".equals(localName)) {
                SpecialSubjectXMIType specialSubject = new SpecialSubjectXMIType();
                parser.parse(specialSubject);
                addSpecialSubject(specialSubject);
                return true;
            }
            if ("role".equals(localName)) {
                role = new CrossComponentReferenceType("role", Application.class);
                parser.parse(role);
                SecurityRole referent = role.resolveReferent(parser, SecurityRole.class);
                if (referent == null) {
                    DDParser.unresolvedReference("role", role.getReferenceString());
                } else {
                    roleName = parser.parseString(referent.getRoleName());
                }
                return true;
            }
            return false;
        }

        private void addUser(UserType user) {
            if (users == null) {
                users = new DDParser.ParsableList<UserType>();
            }
            users.add(user);
        }

        private void addGroup(GroupType group) {
            if (groups == null) {
                groups = new DDParser.ParsableList<GroupType>();
            }
            groups.add(group);
        }

        private void addSpecialSubject(SpecialSubjectXMIType specialSubject) {
            if (specialSubjects == null) {
                specialSubjects = new DDParser.ParsableList<SpecialSubjectXMIType>();
            }
            specialSubjects.add(specialSubject);
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            diag.describeIfSet("users", users);
            diag.describeIfSet("groups", groups);
            diag.describeIfSet("specialSubjects", specialSubjects);
            diag.describeIfSet("role", role);
        }
    }

    static class SpecialSubjectXMIType extends SpecialSubjectType {
        @Override
        public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws ParseException {
            if (nsURI == null) {
                if ("name".equals(localName) || "accessId".equals(localName)) {
                    // Allowed but ignored.
                    return true;
                }
            } else if ("http://www.omg.org/XMI".equals(nsURI)) {
                if ("type".equals(localName)) {
                    String type = parser.parseStringAttributeValue(index).getValue();
                    int typeIndex = type.lastIndexOf(':');
                    String typePrefix = type.substring(0, typeIndex);
                    String typeNSURI = parser.getNamespaceURI(typePrefix);
                    // Allow erroneous undeclared "applicationbnd" prefix for
                    // Liberty backwards compatibility.  tWAS will fail.
                    if (typeIndex != -1 && ("applicationbnd.xmi".equals(typeNSURI) || (typeNSURI == null && typePrefix.equals("applicationbnd")))) {
                        String typeName = type.substring(typeIndex + 1);
                        if ("AllAuthenticatedInTrustedRealms".equals(typeName)) {
                            this.type = SpecialSubject.Type.ALL_AUTHENTICATED_IN_TRUSTED_REALMS;
                        } else if ("AllAuthenticatedUsers".equals(typeName)) {
                            this.type = SpecialSubject.Type.ALL_AUTHENTICATED_USERS;
                        } else if ("Everyone".equals(typeName)) {
                            this.type = SpecialSubject.Type.EVERYONE;
                        } else if ("Server".equals(typeName)) {
                            this.type = SpecialSubject.Type.SERVER;
                        }
                        return this.type != null;
                    }
                }
            }

            // Do not delegate to super.handleAttribute.
            return false;
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            diag.describeEnum("xmi:type", type);
        }
    }

    DDParser.ParsableList<AuthorizationXMIType> authorizations;

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if ("authorizations".equals(localName)) {
            AuthorizationXMIType authorization = new AuthorizationXMIType();
            parser.parse(authorization);
            addAuthorization(authorization);
            return true;
        }
        return false;
    }

    private void addAuthorization(AuthorizationXMIType authorization) {
        if (this.authorizations == null) {
            this.authorizations = new DDParser.ParsableList<AuthorizationXMIType>();
        }
        this.authorizations.add(authorization);
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        diag.describeIfSet("authorizations", authorizations);
    }
}
