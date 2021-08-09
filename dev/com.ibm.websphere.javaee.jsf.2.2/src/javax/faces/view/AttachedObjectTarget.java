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

import java.util.List;

import javax.faces.component.UIComponent;

/**
 * @since 2.0
 */
public interface AttachedObjectTarget
{
    /**
     * The key in the value set of the <em>composite component</em> <code>BeanDescriptor</code>, the value for which is
     * a <code>List&lt;AttachedObjectTarget&gt;</code>.
     */
    public static final String ATTACHED_OBJECT_TARGETS_KEY = "javax.faces.view.AttachedObjectTargets";

    public String getName();

    public List<UIComponent> getTargets(UIComponent topLevelComponent);
}
