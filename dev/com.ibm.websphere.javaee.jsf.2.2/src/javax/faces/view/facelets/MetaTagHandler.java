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
package javax.faces.view.facelets;

/**
 * A base tag for wiring state to an object instance based on rules populated at the time of creating a MetaRuleset.
 */
public abstract class MetaTagHandler extends TagHandler
{
    private Class _lastType = Object.class;

    private Metadata _mapper;

    public MetaTagHandler(TagConfig config)
    {
        super(config);
    }

    /**
     * Extend this method in order to add your own rules.
     * 
     * @param type
     * @return
     * 
     * FIXME: EG _ GENERIC
     */
    protected abstract MetaRuleset createMetaRuleset(Class type);

    /**
     * Invoking/extending this method will cause the results of the created MetaRuleset to auto-wire state to 
     * the passed instance.
     * 
     * @param ctx
     * @param instance
     */
    protected void setAttributes(FaceletContext ctx, Object instance)
    {
        if (instance != null)
        {
            Class<?> type = instance.getClass();
            if (_mapper == null || !_lastType.equals(type))
            {
                _lastType = type;
                _mapper = createMetaRuleset(type).finish();
            }
            
            _mapper.applyMetadata(ctx, instance);
        }
    }
}
