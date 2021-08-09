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
package com.ibm.ws.javaee.ddmetadata.generator.appbnd;

import java.io.File;
import java.io.PrintWriter;

import com.ibm.ws.javaee.ddmetadata.generator.ModelInterfaceImplClassGenerator;
import com.ibm.ws.javaee.ddmetadata.model.ModelInterfaceType;

public class ApplicationBndModelInterfaceImplClassGenerator extends ModelInterfaceImplClassGenerator {
    public ApplicationBndModelInterfaceImplClassGenerator(File destdir, ModelInterfaceType type) {
        super(destdir, type);
    }

    @Override
    protected void writeFieldsExtra(PrintWriter out, String indent) {
        out.append(indent).append("    AuthorizationTableXMIType authorizationTable;").println();
        out.append(indent).append("    RunAsMapXMIType runAsMap;").println();
    }

    @Override
    protected void writeExtra(PrintWriter out, String indent) {
        out.println();
        out.append(indent).append("    private SecurityRoleType getSecurityRole(com.ibm.ws.javaee.ddmodel.StringType name, java.util.Map<String, SecurityRoleType> roleMap) {").println();
        out.append(indent).append("        String nameValue = name == null ? null : name.getValue();").println();
        out.append(indent).append("        SecurityRoleType role = roleMap.get(nameValue);").println();
        out.append(indent).append("        if (role == null) {").println();
        out.append(indent).append("            role = new SecurityRoleType();").println();
        out.append(indent).append("            role.name = name;").println();
        out.append(indent).append("            roleMap.put(nameValue, role);").println();
        out.append(indent).append("            if (security_role == null) {").println();
        out.append(indent).append("                security_role = new DDParser.ParsableListImplements<SecurityRoleType, com.ibm.ws.javaee.dd.appbnd.SecurityRole>();").println();
        out.append(indent).append("            }").println();
        out.append(indent).append("            security_role.add(role);").println();
        out.append(indent).append("        }").println();
        out.append(indent).append("        return role;").println();
        out.append(indent).append("    }").println();
    }

    @Override
    protected boolean isHandleChildExtraNeeded() {
        return true;
    }

    @Override
    protected void writeHandleChildExtra(PrintWriter out, String indent) {
        out.append(indent).append("        if (xmi && \"authorizationTable\".equals(localName)) {").println();
        out.append(indent).append("            authorizationTable = new AuthorizationTableXMIType();").println();
        out.append(indent).append("            parser.parse(authorizationTable);").println();
        out.append(indent).append("            return true;").println();
        out.append(indent).append("        }").println();
        out.append(indent).append("        if (xmi && \"runAsMap\".equals(localName)) {").println();
        out.append(indent).append("            runAsMap = new RunAsMapXMIType();").println();
        out.append(indent).append("            parser.parse(runAsMap);").println();
        out.append(indent).append("            return true;").println();
        out.append(indent).append("        }").println();
    }

    @Override
    protected boolean isXMISuperHandleChild() {
        return false;
    }

    @Override
    protected boolean isFinishExtraNeeded() {
        return true;
    }

    @Override
    protected void writeFinishExtra(PrintWriter out, String indent) {
        out.println();
        out.append(indent).append("        if (xmi) {").println();
        out.append(indent).append("            java.util.Map<String, SecurityRoleType> roleMap = new java.util.HashMap<String, SecurityRoleType>();").println();
        out.append(indent).append("            if (authorizationTable != null && authorizationTable.authorizations != null) {").println();
        out.append(indent).append("                for (AuthorizationTableXMIType.AuthorizationXMIType auth : authorizationTable.authorizations) {").println();
        out.append(indent).append("                    SecurityRoleType role = getSecurityRole(auth.roleName, roleMap);").println();
        out.append(indent).append("                    if (auth.users != null) {").println();
        out.append(indent).append("                        for (UserType user : auth.users) {").println();
        out.append(indent).append("                            role.addUser(user);").println();
        out.append(indent).append("                        }").println();
        out.append(indent).append("                    }").println();
        out.append(indent).append("                    if (auth.groups != null) {").println();
        out.append(indent).append("                        for (GroupType group : auth.groups) {").println();
        out.append(indent).append("                            role.addGroup(group);").println();
        out.append(indent).append("                        }").println();
        out.append(indent).append("                    }").println();
        out.append(indent).append("                    if (auth.specialSubjects != null) {").println();
        out.append(indent).append("                        for (SpecialSubjectType specialSubject : auth.specialSubjects) {").println();
        out.append(indent).append("                            role.addSpecialSubject(specialSubject);").println();
        out.append(indent).append("                        }").println();
        out.append(indent).append("                    }").println();
        out.append(indent).append("                }").println();
        out.append(indent).append("            }").println();
        out.append(indent).append("            if (runAsMap != null && runAsMap.runAsBindings != null) {").println();
        out.append(indent).append("                for (RunAsMapXMIType.RunAsBindingXMIType runAs : runAsMap.runAsBindings) {").println();
        out.append(indent).append("                    SecurityRoleType role = getSecurityRole(runAs.securityRoleName, roleMap);").println();
        out.append(indent).append("                    role.run_as = runAs.authData;").println();
        out.append(indent).append("                }").println();
        out.append(indent).append("            }").println();
        out.append(indent).append("        }").println();
    }
}
