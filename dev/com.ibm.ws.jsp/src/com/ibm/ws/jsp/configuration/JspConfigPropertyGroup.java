/*******************************************************************************
 * Copyright (c) 1997, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.descriptor.JspPropertyGroupDescriptor;

import com.ibm.ws.javaee.dd.jsp.JSPPropertyGroup;

public interface JspConfigPropertyGroup extends JspPropertyGroupDescriptor {
   
    /**
     * This method is under EE10's JspPropertyGroupDescriptor, but we are using the older servlet APIs to build 
     * this bundle. This method has been added under JspConfigPropertyGroup instead. 
     * It will only be used when Pages 3.1 is enabled. 
     * Will the use of an unknown identifier in Expression Language within a Jakarta Server Pages page trigger an error for this group?
     * @return {@code true} if an error will be triggered, otherwise {@code false}
     * @since Servlet 6.0 / Pages 3.1
     */
    public String getErrorOnELNotFound();

}
