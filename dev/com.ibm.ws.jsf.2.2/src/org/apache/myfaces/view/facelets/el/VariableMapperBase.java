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
package org.apache.myfaces.view.facelets.el;

import javax.el.VariableMapper;

/**
 * Defines an interface to detect when an EL expression has been
 * resolved by a facelets variable mapper and in that way allow cache it
 * if it is possible. This class should be implemented by any 
 * "facelets contextual" variable mapper.
 * 
 * @since 2.0.8
 * @author Leonardo Uribe
 *
 */
public abstract class VariableMapperBase extends VariableMapper
{

    /**
     * Check if a variable has been resolved by this variable mapper
     * or any parent "facelets contextual" variable mapper.
     * 
     * @return
     */
    public abstract boolean isAnyFaceletsVariableResolved();

    /**
     * Indicates an expression will be resolved, so preparations
     * should be done to detect if a contextual variable has been resolved.
     */
    public abstract void beforeConstructELExpression();

    /**
     * Cleanup all initialization done.
     */
    public abstract void afterConstructELExpression();
}
