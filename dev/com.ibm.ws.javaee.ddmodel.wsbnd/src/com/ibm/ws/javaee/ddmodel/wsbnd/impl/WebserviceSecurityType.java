/*******************************************************************************
 * Copyright (c) 2013,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.wsbnd.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ibm.ws.javaee.dd.common.SecurityRole;
import com.ibm.ws.javaee.dd.web.common.LoginConfig;
import com.ibm.ws.javaee.dd.web.common.SecurityConstraint;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.common.SecurityRoleType;
import com.ibm.ws.javaee.ddmodel.web.common.LoginConfigType;
import com.ibm.ws.javaee.ddmodel.web.common.SecurityConstraintType;
import com.ibm.ws.javaee.ddmodel.wsbnd.WebserviceSecurity;

/*
 <xsd:complexType name="webserviceSecurityType">
 <xsd:choice minOccurs="0" maxOccurs="unbounded">
 <xsd:element name="security-constraint" type="javaee:security-constraintType"/>
 <xsd:element name="login-config" type="javaee:login-configType"/>
 <xsd:element name="security-role" type="javaee:security-roleType"/>
 </xsd:choice>
 </xsd:complexType>
 */
public class WebserviceSecurityType extends DDParser.ElementContentParsable implements WebserviceSecurity {

    private final ArrayList<SecurityConstraintType> securityConstraints = new ArrayList<SecurityConstraintType>();

    private final ArrayList<SecurityRoleType> securityRoles = new ArrayList<SecurityRoleType>();

    private LoginConfigType loginConfigType;

    @Override
    public List<SecurityConstraint> getSecurityConstraints() {
        return securityConstraints.isEmpty() ? Collections.emptyList(): new ArrayList<SecurityConstraint>(securityConstraints);
    }

    @Override
    public List<SecurityRole> getSecurityRoles() {
        return securityRoles.isEmpty() ? Collections.emptyList() : new ArrayList<SecurityRole>(securityRoles);
    }

    @Override
    public LoginConfig getLoginConfig() {
        return loginConfigType;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if (SECURITY_CONSTRAINT_ELEMENT_NAME.equals(localName)) {
            SecurityConstraintType securityConstraintType = new SecurityConstraintType();
            parser.parse(securityConstraintType);
            securityConstraints.add(securityConstraintType);
            return true;
        } else if (SECURITY_ROLE_ELEMENT_NAME.equals(localName)) {
            SecurityRoleType securityRoleType = new SecurityRoleType();
            parser.parse(securityRoleType);
            securityRoles.add(securityRoleType);
            return true;
        } else if (LOGIN_CONFIG_ELEMENT_NAME.equals(localName)) {
            loginConfigType = new LoginConfigType();
            parser.parse(loginConfigType);
            return true;
        }

        return false;
    }

    @Override
    public void describe(Diagnostics diag) {
        diag.append("[" + SECURITY_CONSTRAINT_ELEMENT_NAME + "<");
        String prefix = "";
        for (SecurityConstraintType securityConstraint : securityConstraints) {
            diag.append(prefix);
            securityConstraint.describe(diag);
            prefix = ",";
        }
        diag.append(">]");

        diag.append("[" + SECURITY_ROLE_ELEMENT_NAME + "<");
        prefix = "";
        for (SecurityRoleType securityRole : securityRoles) {
            diag.append(prefix);
            securityRole.describe(diag);
            prefix = ",";
        }
        diag.append(">]");

        diag.append("[" + LOGIN_CONFIG_ELEMENT_NAME + "<");
        if (null != loginConfigType) {
            loginConfigType.describe(diag);
        }
        diag.append(">]");

    }
}
