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

import javax.faces.view.facelets.Tag;
import javax.faces.view.facelets.TagDecorator;

import org.apache.myfaces.view.facelets.util.ParameterCheck;

/**
 * A TagDecorator that is composed of 1 or more TagDecorator instances. It uses the chain of responsibility pattern to
 * stop processing if any of the TagDecorators return a value other than null.
 * 
 * @author Jacob Hookom
 * @version $Id: CompositeTagDecorator.java 1187701 2011-10-22 12:21:54Z bommel $
 */
public final class CompositeTagDecorator implements TagDecorator
{

    private final TagDecorator[] decorators;

    public CompositeTagDecorator(TagDecorator[] decorators)
    {
        ParameterCheck.notNull("decorators", decorators);
        this.decorators = decorators;
    }

    /**
     * Uses the chain of responsibility pattern to stop processing if any of the TagDecorators return a value other than
     * null.
     * 
     * @see javax.faces.view.facelets.TagDecorator#decorate(javax.faces.view.facelets.Tag)
     */
    public Tag decorate(Tag tag)
    {
        Tag t = null;
        for (int i = 0; i < this.decorators.length; i++)
        {
            t = this.decorators[i].decorate(tag);
            if (t != null)
            {
                return t;
            }
        }
        return tag;
    }

}

