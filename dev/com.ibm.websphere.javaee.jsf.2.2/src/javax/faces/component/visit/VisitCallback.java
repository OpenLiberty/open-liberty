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
package javax.faces.component.visit;

import javax.faces.component.UIComponent;

/**
 * <p>A simple callback interface that enables 
 * taking action on a specific UIComponent (either facet or child) during 
 * a component tree visit.</p>
 *
 * @since 2.0
 */
public interface VisitCallback
{
    /**
     * <p>This method is called during component tree visits by 
     * {@link VisitContext#invokeVisitCallback VisitContext.invokeVisitCallback()} 
     * to visit the specified component.  At the point in time when this 
     * method is called, the argument {@code target} is guaranteed
     * to be in the proper state with respect to its ancestors in the
     * View.</p>
     *
     * @param context the {@link VisitContext} for this tree visit.
     *
     * @param target the {@link UIComponent} to visit
     *
     * @return a {@link VisitResult} that indicates whether to continue
     *   visiting the component's subtree, skip visiting the component's
     *   subtree or end the visit.
     */
    public VisitResult visit(VisitContext context, UIComponent target);
}