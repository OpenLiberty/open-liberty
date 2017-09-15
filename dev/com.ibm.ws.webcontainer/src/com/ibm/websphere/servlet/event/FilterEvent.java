/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.servlet.event;


import java.util.EventObject;
import javax.servlet.FilterConfig;

public class FilterEvent extends EventObject
{

    /**
     * FilterEvent contructor.
     * @param source the object that triggered this event.
     * @param filterConfig the filter's FilterConfig.
     */
 public FilterEvent(Object source, FilterConfig filterConfig)
 {
     super(source);
     _filterConfig = filterConfig;
 }

 /**
  * Return the name of the Filter that this event is associated with.
  */
 public String getFilterName()
 {
     return _filterConfig.getFilterName();
 }

 /**
  * Return the FilterConfig that this event is associated with.
  */
 public FilterConfig getFilterConfig()
 {
     return _filterConfig;
 }

 private FilterConfig _filterConfig;
 private static final long serialVersionUID = 1L;
}
