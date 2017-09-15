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

import javax.faces.view.BehaviorHolderAttachedObjectHandler;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletTag;

/**
 * @since 2.0
 */
@JSFFaceletTag
public class BehaviorHandler extends FaceletsAttachedObjectHandler implements BehaviorHolderAttachedObjectHandler
{
    private String behaviorId;
    private TagAttribute event;
    private TagHandlerDelegate helper;
    
    /**
     * @param config
     */
    public BehaviorHandler(BehaviorConfig config)
    {
        super(config);
        
        behaviorId = config.getBehaviorId();
        event = getAttribute ("event");
    }
    
    public String getBehaviorId()
    {
        return behaviorId;
    }
    
    public TagAttribute getEvent()
    {
        return event;
    }

    public String getEventName ()
    {
        if (event == null)
        {
            return null;
        }
        
        return event.getValue();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected TagHandlerDelegate getTagHandlerDelegate()
    {
        if (helper == null)
        {
            // Spec seems to indicate that the helper is created here, as opposed to other Handler
            // instances, where it's presumably a new instance for every getter call.
            
            this.helper = delegateFactory.createBehaviorHandlerDelegate (this);
        }
        return helper;
    }
}
