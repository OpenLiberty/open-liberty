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
package org.apache.myfaces.shared.component;

import javax.faces.context.FacesContext;
import javax.faces.component.UIComponent;

/**
 * With findComponent - you get a component, but this component might
 * not be prepared to actually have the correct context information. This
 * is important for e.g. DataTables. They'll need to prepare the component
 * with the current row-state to make sure that the method is executed
 * correctly.
 */
public interface ExecuteOnCallback
{
    Object execute(FacesContext context, UIComponent component);
}
