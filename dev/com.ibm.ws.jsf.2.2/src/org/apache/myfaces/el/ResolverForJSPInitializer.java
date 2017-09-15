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
package org.apache.myfaces.el;

import java.util.Iterator;

import javax.faces.FactoryFinder;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.faces.lifecycle.LifecycleFactory;

import org.apache.myfaces.el.unified.ELResolverBuilder;

/**
 * The class will initialize the resolver for JSP
 * 
 * @author Mathias Broekelmann (latest modification by $Author: lu4242 $)
 * @version $Revision: 1237538 $ $Date: 2012-01-30 03:43:13 +0000 (Mon, 30 Jan 2012) $
 */
public final class ResolverForJSPInitializer implements PhaseListener
{
    private final ELResolverBuilder _resolverBuilder;
    private volatile boolean initialized;
    private final javax.el.CompositeELResolver _resolverForJSP;

    public ResolverForJSPInitializer(final ELResolverBuilder resolverBuilder,
                                     final javax.el.CompositeELResolver resolverForJSP)
    {
        _resolverBuilder = resolverBuilder;
        _resolverForJSP = resolverForJSP;
    }

    public void beforePhase(final PhaseEvent event)
    {
        if (!initialized)
        {
            synchronized(this)
            {
                if (!initialized)
                {
                    _resolverBuilder.build(_resolverForJSP);

                    LifecycleFactory factory = (LifecycleFactory) 
                            FactoryFinder.getFactory(FactoryFinder.LIFECYCLE_FACTORY);
                    for (Iterator<String> iter = factory.getLifecycleIds(); iter.hasNext();)
                    {
                        factory.getLifecycle(iter.next()).removePhaseListener(this);
                    }
                    
                    initialized = true;
                }
            }
        }
    }

    public void afterPhase(final PhaseEvent event)
    {
    }

    public PhaseId getPhaseId()
    {
        return PhaseId.ANY_PHASE;
    }

}
