/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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

import com.ibm.ws.javaee.ddmodel.DDParser;

public class SecurityRoleType extends com.ibm.ws.javaee.ddmodel.DDParser.ElementContentParsable implements com.ibm.ws.javaee.dd.appbnd.SecurityRole {
    com.ibm.ws.javaee.ddmodel.StringType name;
    DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.appbnd.UserType, com.ibm.ws.javaee.dd.appbnd.User> user;
    DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.appbnd.GroupType, com.ibm.ws.javaee.dd.appbnd.Group> group;
    DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.appbnd.SpecialSubjectType, com.ibm.ws.javaee.dd.appbnd.SpecialSubject> special_subject;
    com.ibm.ws.javaee.ddmodel.appbnd.RunAsType run_as;

    @Override
    public java.lang.String getName() {
        return name != null ? name.getValue() : null;
    }

    @Override
    public java.util.List<com.ibm.ws.javaee.dd.appbnd.User> getUsers() {
        if (user != null) {
            return user.getList();
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public java.util.List<com.ibm.ws.javaee.dd.appbnd.Group> getGroups() {
        if (group != null) {
            return group.getList();
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public java.util.List<com.ibm.ws.javaee.dd.appbnd.SpecialSubject> getSpecialSubjects() {
        if (special_subject != null) {
            return special_subject.getList();
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public com.ibm.ws.javaee.dd.appbnd.RunAs getRunAs() {
        return run_as;
    }

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws DDParser.ParseException {
        if (nsURI == null) {
            if ("name".equals(localName)) {
                this.name = parser.parseStringAttributeValue(index);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws DDParser.ParseException {
        if ("user".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.appbnd.UserType user = new com.ibm.ws.javaee.ddmodel.appbnd.UserType();
            parser.parse(user);
            this.addUser(user);
            return true;
        }
        if ("group".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.appbnd.GroupType group = new com.ibm.ws.javaee.ddmodel.appbnd.GroupType();
            parser.parse(group);
            this.addGroup(group);
            return true;
        }
        if ("special-subject".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.appbnd.SpecialSubjectType special_subject = new com.ibm.ws.javaee.ddmodel.appbnd.SpecialSubjectType();
            parser.parse(special_subject);
            this.addSpecialSubject(special_subject);
            return true;
        }
        if ("run-as".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.appbnd.RunAsType run_as = new com.ibm.ws.javaee.ddmodel.appbnd.RunAsType();
            parser.parse(run_as);
            this.run_as = run_as;
            return true;
        }
        return false;
    }

    void addUser(com.ibm.ws.javaee.ddmodel.appbnd.UserType user) {
        if (this.user == null) {
            this.user = new DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.appbnd.UserType, com.ibm.ws.javaee.dd.appbnd.User>();
        }
        this.user.add(user);
    }

    void addGroup(com.ibm.ws.javaee.ddmodel.appbnd.GroupType group) {
        if (this.group == null) {
            this.group = new DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.appbnd.GroupType, com.ibm.ws.javaee.dd.appbnd.Group>();
        }
        this.group.add(group);
    }

    void addSpecialSubject(com.ibm.ws.javaee.ddmodel.appbnd.SpecialSubjectType special_subject) {
        if (this.special_subject == null) {
            this.special_subject = new DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.appbnd.SpecialSubjectType, com.ibm.ws.javaee.dd.appbnd.SpecialSubject>();
        }
        this.special_subject.add(special_subject);
    }

    @Override
    public void describe(com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics diag) {
        diag.describeIfSet("name", name);
        diag.describeIfSet("user", user);
        diag.describeIfSet("group", group);
        diag.describeIfSet("special-subject", special_subject);
        diag.describeIfSet("run-as", run_as);
    }
}
