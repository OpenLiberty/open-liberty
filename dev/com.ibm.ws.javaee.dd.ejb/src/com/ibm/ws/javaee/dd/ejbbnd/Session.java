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
package com.ibm.ws.javaee.dd.ejbbnd;

import java.util.List;

import com.ibm.ws.javaee.ddmetadata.annotation.DDAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.DDAttributeType;
import com.ibm.ws.javaee.ddmetadata.annotation.DDElement;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIAttribute;

/**
 * Represents &lt;session>.
 */
public interface Session extends EnterpriseBean {

    /**
     * @return &lt;Interface>, or empty list if unspecified
     */
    @DDElement(name = "interface")
    List<Interface> getInterfaces();

    /**
     * @return simple-binding-name="..." , or null if unspecified
     */
    @DDAttribute(name = "simple-binding-name", type = DDAttributeType.String)
    @DDXMIAttribute(name = "jndiName")
    String getSimpleBindingName();

    /**
     * @return component-id="..." , or null if unspecified
     */
    @DDAttribute(name = "component-id", type = DDAttributeType.String)
    String getComponentID();

    /**
     * @return remote-home-binding-name="..." , or null if unspecified
     */
    @DDAttribute(name = "remote-home-binding-name", type = DDAttributeType.String)
    String getRemoteHomeBindingName();

    /**
     * @return local-home-binding-name="..." , or null if unspecified
     */
    @DDAttribute(name = "local-home-binding-name", type = DDAttributeType.String)
    String getLocalHomeBindingName();

}
