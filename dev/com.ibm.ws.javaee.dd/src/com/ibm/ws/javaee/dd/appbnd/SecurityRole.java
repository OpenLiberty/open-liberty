/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.dd.appbnd;

import java.util.List;

import com.ibm.ws.javaee.ddmetadata.annotation.DDAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.DDAttributeType;
import com.ibm.ws.javaee.ddmetadata.annotation.DDElement;
import com.ibm.ws.javaee.ddmetadata.annotation.DDIdAttribute;

/**
 * Represents &lt;security-role>.
 */
@DDIdAttribute
public interface SecurityRole {

    /**
     * @return name="..." attribute value
     */
    @DDAttribute(name = "name", type = DDAttributeType.String)
    String getName();

    /**
     * @return &lt;user> as a read-only list
     */
    @DDElement(name = "user")
    List<User> getUsers();

    /**
     * @return &lt;group> as a read-only list
     */
    @DDElement(name = "group")
    List<Group> getGroups();

    /**
     * @return &lt;special-subject> as a read-only list
     */
    @DDElement(name = "special-subject")
    List<SpecialSubject> getSpecialSubjects();

    /**
     * @return &lt;run-as>, or null if unspecified
     */
    @DDElement(name = "run-as")
    RunAs getRunAs();

}
