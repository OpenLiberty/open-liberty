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
package org.apache.myfaces.view.facelets.tag.ui;

import org.apache.myfaces.view.facelets.component.UIRepeat;
import org.apache.myfaces.view.facelets.tag.AbstractTagLibrary;

/**
 * @author Jacob Hookom
 * @version $Id: LegacyUILibrary.java 1539908 2013-11-08 02:54:51Z lu4242 $
 */
public final class LegacyUILibrary extends AbstractTagLibrary
{

    public final static String NAMESPACE = "http://xmlns.jcp.org/jsf/facelets";
    public final static String ALIAS_NAMESPACE = "http://java.sun.com/jsf/facelets";

    public final static LegacyUILibrary INSTANCE = new LegacyUILibrary();

    public LegacyUILibrary()
    {
        super(NAMESPACE, ALIAS_NAMESPACE);

        this.addComponent("component", ComponentRef.COMPONENT_TYPE, null, ComponentRefHandler.class);

        this.addComponent("fragment", ComponentRef.COMPONENT_TYPE, null, ComponentRefHandler.class);

        this.addTagHandler("define", DefineHandler.class);

        this.addTagHandler("insert", InsertHandler.class);

        this.addTagHandler("include", LegacyIncludeHandler.class);

        this.addTagHandler("composition", LegacyCompositionHandler.class);

        this.addTagHandler("param", LegacyParamHandler.class);

        this.addTagHandler("decorate", LegacyDecorateHandler.class);

        this.addComponent("repeat", UIRepeat.COMPONENT_TYPE, null, RepeatHandler.class);

        this.addComponent("debug", UIDebug.COMPONENT_TYPE, null);
    }
}
