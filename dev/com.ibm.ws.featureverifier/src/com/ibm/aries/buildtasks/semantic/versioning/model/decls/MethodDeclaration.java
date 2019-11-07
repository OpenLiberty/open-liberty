/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.aries.buildtasks.semantic.versioning.model.decls;

import java.lang.reflect.Modifier;

public class MethodDeclaration extends GenericDeclaration
{
    private final String desc;
    private final String[] exceptions;

    public MethodDeclaration(int access, String name, String desc, String signature, String[] exceptions) {
        super(access, name, signature);
        this.desc = desc;
        this.exceptions = exceptions;
    }

    public String toXML() {
        StringBuilder sb = new StringBuilder();
        sb.append("       <methoddecl>\n");
        sb.append("         <name>" + escapeXML(getName()) + "</name>\n");
        sb.append("         <access>" + getRawAccess() + "</access>\n");
        sb.append("         <desc>" + escapeXML(getDesc()) + "</desc>\n");
        if (getSignature() != null) {
            sb.append("         <signature>" + escapeXML(getSignature()) + "</signature>\n");
        }
        if (exceptions != null && exceptions.length > 0) {
            sb.append("         <exceptions>\n");
            for (String e : exceptions) {
                sb.append("            <exception>" + e + "</exception>\n");
            }
            sb.append("         </exceptions>\n");
        }
        sb.append("       </methoddecl>\n");
        return sb.toString();
    }

    public String getDesc()
    {
        return desc;
    }

    public boolean isAbstract() {
        return Modifier.isAbstract(getAccess());
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        //int result = super.hashCode();
        int result = prime + ((desc == null) ? 0 : desc.hashCode());
        result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;

        if (!(obj instanceof MethodDeclaration)) {
            throw new RuntimeException("MethodDeclaration.equals not written to handle non same type");
        }

        //if (getClass() != obj.getClass()) return false;
        MethodDeclaration other = (MethodDeclaration) obj;
        if (desc == null) {
            if (other.desc != null)
                return false;
        } else if (!desc.equals(other.desc))
            return false;
        if (getName() == null) {
            if (other.getName() != null)
                return false;
        } else if (!getName().equals(other.getName()))
            return false;

        return true;
    }

}
