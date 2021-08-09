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

import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;



/**
 * @author Manfred Geiler (latest modification by $Author: struberg $)
 * @author Anton Koinov

 * @version $Revision: 1188686 $ $Date: 2011-10-25 14:59:52 +0000 (Tue, 25 Oct 2011) $
 */
public abstract class ManagedProperty implements Serializable
{
    // <!ELEMENT managed-property (description*, display-name*, icon*,
    // property-name, property-class?, (map-entries|null-value|value|list-entries))>

    public static final int TYPE_UNKNOWN = 0;
    public static final int TYPE_MAP = 1;
    public static final int TYPE_NULL = 2;
    public static final int TYPE_VALUE = 3;
    public static final int TYPE_LIST = 4;

    public abstract String getPropertyName();
    public abstract String getPropertyClass();

    public abstract int getType();
    public abstract MapEntries getMapEntries();
    public abstract ListEntries getListEntries();
    public abstract Object getRuntimeValue(FacesContext facesContext);
    public abstract boolean isValueReference();
    public abstract ValueBinding getValueBinding(FacesContext facesContext);
}
