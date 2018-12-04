/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity.config.wim;

import javax.xml.bind.annotation.XmlElement;

import com.ibm.websphere.simplicity.config.ConfigElement;

/**
 * Configuration for the following nested elements:
 *
 * <ul>
 * <li>ldapRegistry --> groupConfiguration</li>
 * </ul>
 */
public class GroupProperties extends ConfigElement {

    private DynamicMemberAttribute dynamicMemberAttribute;
    private MemberAttribute memberAttribute;
    private MembershipAttribute membershipAttribute;

    /**
     * @return the dynamicMemberAttribute
     */
    public DynamicMemberAttribute getDynamicMemberAttribute() {
        return dynamicMemberAttribute;
    }

    /**
     * @return the memberAttribute
     */
    public MemberAttribute getMemberAttribute() {
        return memberAttribute;
    }

    /**
     * @return the membershipAttribute
     */
    public MembershipAttribute getMembershipAttribute() {
        return membershipAttribute;
    }

    /**
     * @param dynamicMemberAttribute the dynamicMemberAttribute to set
     */
    @XmlElement(name = "dynamicMemberAttribute")
    public void setDynamicMemberAttribute(DynamicMemberAttribute dynamicMemberAttribute) {
        this.dynamicMemberAttribute = dynamicMemberAttribute;
    }

    /**
     * @param memberAttribute the memberAttribute to set
     */
    @XmlElement(name = "memberAttribute")
    public void setMemberAttribute(MemberAttribute memberAttribute) {
        this.memberAttribute = memberAttribute;
    }

    /**
     * @param membershipAttribute the membershipAttribute to set
     */
    @XmlElement(name = "membershipAttribute")
    public void setMembershipAttribute(MembershipAttribute membershipAttribute) {
        this.membershipAttribute = membershipAttribute;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append(getClass().getSimpleName()).append("{ ");

        if (dynamicMemberAttribute != null) {
            sb.append("dynamicMemberAttribute=\"").append(dynamicMemberAttribute).append("\" ");;
        }
        if (memberAttribute != null) {
            sb.append("memberAttribute=\"").append(memberAttribute).append("\" ");;
        }
        if (membershipAttribute != null) {
            sb.append("membershipAttribute=\"").append(membershipAttribute).append("\" ");;
        }

        sb.append("}");

        return sb.toString();
    }
}
