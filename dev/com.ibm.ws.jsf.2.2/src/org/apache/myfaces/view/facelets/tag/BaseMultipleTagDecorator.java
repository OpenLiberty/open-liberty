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

/**
 *
 * @author Leonardo Uribe
 */
public class BaseMultipleTagDecorator implements TagDecorator
{
    private final TagDecorator defaultTagDecorator;

    private final TagDecorator compositeTagDecorator;

    public BaseMultipleTagDecorator(TagDecorator defaultTagDecorator, TagDecorator normalTagDecorator)
    {
        this.defaultTagDecorator = defaultTagDecorator;
        this.compositeTagDecorator = normalTagDecorator;
    }
    
    public Tag decorate(Tag tag)
    {
        // The default tag decorator is special, because a non null return value does not
        // stop processing.
        Tag processedTag = this.defaultTagDecorator.decorate(tag);
        // If a not null value is returned, pass the processedTag, otherwise pass the default one.
        return compositeTagDecorator.decorate(processedTag != null ? processedTag : tag);
    }
}
