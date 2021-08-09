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
package org.apache.myfaces.view.facelets.tag;

import javax.faces.view.facelets.MetaRuleset;
import javax.faces.view.facelets.MetaTagHandler;
import javax.faces.view.facelets.TagConfig;

import org.apache.myfaces.view.facelets.util.ParameterCheck;

/**
 * A base tag for wiring state to an object instance based on rules populated at the time of creating a MetaRuleset.
 * 
 * @author Jacob Hookom
 * @version $Id: MetaTagHandlerImpl.java 1187701 2011-10-22 12:21:54Z bommel $
 */
public abstract class MetaTagHandlerImpl extends MetaTagHandler
{
    public MetaTagHandlerImpl(TagConfig config)
    {
        super(config);
    }

    /**
     * Extend this method in order to add your own rules.
     * 
     * @param type
     * @return
     */
    protected MetaRuleset createMetaRuleset(Class type)
    {
        ParameterCheck.notNull("type", type);
        
        return new MetaRulesetImpl(this.tag, type);
    }
}
