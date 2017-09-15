/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.myfaces.config.element;

import java.io.Serializable;
import java.util.Collection;

import javax.el.ValueExpression;
import javax.faces.context.FacesContext;

/**
 * @author Manfred Geiler (latest modification by $Author: struberg $)
 * @version $Revision: 1188686 $ $Date: 2011-10-25 14:59:52 +0000 (Tue, 25 Oct 2011) $
 */
public abstract class ManagedBean implements Serializable
{
    // <!ELEMENT managed-bean (description*, display-name*, icon*, managed-bean-name, managed-bean-class,
    // managed-bean-scope, (managed-property* | map-entries | list-entries))>

    public static final int INIT_MODE_NO_INIT = 0;
    public static final int INIT_MODE_PROPERTIES = 1;
    public static final int INIT_MODE_MAP = 2;
    public static final int INIT_MODE_LIST = 3;

    public abstract String getDescription();
    public abstract String getManagedBeanName();
    public abstract String getManagedBeanClassName();
    public abstract Class<?> getManagedBeanClass();
    public abstract String getManagedBeanScope();
    
    public abstract int getInitMode();

    /**
     * @return Iterator over {@link ManagedProperty} entries
     */
    public abstract Collection<? extends ManagedProperty> getManagedProperties();

    public abstract MapEntries getMapEntries();

    public abstract ListEntries getListEntries();
    
    /**
     * Is the value of managed-bean-scope a EL ValueExpression?
     * @since 2.0
     * @return
     */
    public abstract boolean isManagedBeanScopeValueExpression();
    
    /**
     * Returns the ValueExpression for managed-bean-scope
     * or null, if managed-bean-scope is literal.
     * @param facesContext
     * @since 2.0
     * @return
     */
    public abstract ValueExpression getManagedBeanScopeValueExpression(FacesContext facesContext);
    
    /**
     * Returns the value of the eager attribute. Default is false.
     * @since 2.0
     * @return
     */
    public abstract String getEager();
    
}
