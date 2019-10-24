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

public class FieldDeclaration extends GenericDeclaration
{
    private final String desc;
    private final Object value;

    public FieldDeclaration(int access, String name, String desc, String signature, Object value) {
        super(access, name, signature);
        this.desc = desc;
        this.value = value;
        // System.out.println("   - Field decl of "+name+" access "+access+" signature "+signature);
    }

    public String toXML() {
        StringBuilder sb = new StringBuilder();
        sb.append("         <fielddecl>\n");
        sb.append("           <name>" + escapeXML(getName()) + "</name>\n");
        sb.append("           <access>" + getRawAccess() + "</access>\n");
        sb.append("           <desc>" + escapeXML(getDesc()) + "</desc>\n");
        if (getSignature() != null) {
            sb.append("           <signature>" + escapeXML(getSignature()) + "</signature>\n");
        }
        //value.. ouch.. we'll encode the string value.. currently the getter is only 
        //        used to obtain the serialUUID (long), so we can rebuild that from string.
        if (getValue() != null) {
            //value can contain some real nasty chars.. so we'll render it as hex ;p 

            byte[] a = getValue().toString().getBytes();
            StringBuilder sb2 = new StringBuilder(a.length * 2);
            for (byte b : a)
                sb2.append(String.format("%02x", b & 0xff));

            sb.append("           <value>" + escapeXML(sb2.toString()) + "</value>\n");
        }
        sb.append("         </fielddecl>\n");
        return sb.toString();
    }

    public String getDesc()
    {
        return desc;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
            return false;
        if (this == obj)
            return true;
        if (getClass() != obj.getClass())
            return false;
        FieldDeclaration other = (FieldDeclaration) obj;
        if (getName() == null) {
            if (other.getName() != null)
                return false;
        } else if (!getName().equals(other.getName()))
            return false;
        return true;
    }

}
