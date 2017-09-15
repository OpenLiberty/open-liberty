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

/**
 * A PDL handler that exposes {@link javax.faces.validator.Validator Validator} or
 * {@link javax.faces.event.ValueChangeListener ValueChangeListener} to a <em>page author</em>. The default
 * implementation of Facelets must provide an implemention of this in the handler for the
 * <code>&lt;f:validator&gt;</code> (and any tags for any of the standard validators) and
 * <code>&lt;f:valueChangeListener&gt;</code> tags.
 * 
 * @since 2.0
 */
public interface EditableValueHolderAttachedObjectHandler extends ValueHolderAttachedObjectHandler
{

}
