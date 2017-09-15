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
package com.ibm.ws.javaee.dd.jsp;

import java.util.List;

/**
 * Represents the jsp-configType type from the jsp XSD.
 */
public interface JSPConfig {

    /**
     * @return &lt;taglib> as a read-only list
     */
    List<Taglib> getTaglibs();

    /**
     * @return &lt;jsp-property-group> as a read-only list
     */
    List<JSPPropertyGroup> getJSPPropertyGroups();

}
