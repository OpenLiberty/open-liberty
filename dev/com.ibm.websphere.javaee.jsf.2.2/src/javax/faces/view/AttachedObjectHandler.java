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
package javax.faces.view;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

/**
 * The abstract base interface for a handler representing an <em>attached object</em> in a PDL page. Subinterfaces are
 * provided for the common attached objects that expose {@link javax.faces.convert.Converter Converter},
 * {@link javax.faces.validator.Validator Validator}, {@link javax.faces.event.ValueChangeEvent ValueChangeEvent}, and
 * {@link javax.faces.event.ActionListener ActionListener} for use by <em>page authors</em>.
 * 
 * @since 2.0
 */
public interface AttachedObjectHandler
{
    /**
     * Take the argument <code>parent</code> and apply this attached <code>object</code> to it. The action taken varies
     * with class that implements one of the subinterfaces of this interface.
     * 
     * @param context
     *            The <code>FacesContext</code> for this request
     * @param parent
     *            The <code>UIComponent</code> to which this particular attached object must be applied.
     */
    public void applyAttachedObject(FacesContext context, UIComponent parent);

    /**
     * Return the value of the "for" attribute specified by the <em>page author</em> on the tag for this
     * <code>AttachedObjectHandler</code>.
     * 
     * @return the value of the "for" attribute specified by the <em>page author</em> on the tag for this
     *         <code>AttachedObjectHandler</code>.
     */
    public String getFor();
}
