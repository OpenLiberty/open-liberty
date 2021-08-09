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
package org.apache.myfaces.view.facelets;

public enum ELExpressionCacheMode
{
    /**
     * Does not cache expressions.
     */
    noCache,
    
    /**
     * Does not cache expressions on these cases:
     * - A c:set tag with only var and value properties was used on the current page.
     * - ui:param was used on the current template context
     * - Inside user tags
     * - An expression uses a variable resolved through VariableMapper
     */
    strict,

    /**
     * Does not cache expressions on these cases:
     * - ui:param was used on the current template context
     * - Inside user tags
     * - An expression uses a variable resolved through VariableMapper
     * 
     * In this case, c:set is assumed to be always on the parent node.
     */
    allowCset,
    
    /**
     * Does not cache expressions on these cases:
     * - Inside user tags
     * - An expression uses a variable resolved through VariableMapper
     * 
     * Note if ui:param is used, each template call should define the same
     * param count, even if only just a few are used. 
     */
    always,
    
    /**
     * Does not cache expressions on these cases:
     * - An expression uses a variable resolved through VariableMapper
     * 
     * It uses an alternate FaceletCache that implements AbstractFaceletCache
     * contract and force recompile the facelet in case additional ui:param
     * instances are detected.
     * 
     */
    alwaysRecompile,
}
