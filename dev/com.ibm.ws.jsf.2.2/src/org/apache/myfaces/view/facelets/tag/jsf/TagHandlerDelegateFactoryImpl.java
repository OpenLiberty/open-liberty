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
package org.apache.myfaces.view.facelets.tag.jsf;

import javax.faces.view.facelets.BehaviorHandler;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.ConverterHandler;
import javax.faces.view.facelets.TagHandlerDelegate;
import javax.faces.view.facelets.TagHandlerDelegateFactory;
import javax.faces.view.facelets.ValidatorHandler;

/**
 * @author Leonardo Uribe (latest modification by $Author: lu4242 $)
 * @version $Revision: 799963 $ $Date: 2009-08-02 00:02:41 +0000 (Sun, 02 Aug 2009) $
 *
 * @since 2.0
 */
public class TagHandlerDelegateFactoryImpl extends TagHandlerDelegateFactory
{

    @Override
    public TagHandlerDelegate createBehaviorHandlerDelegate(
            BehaviorHandler owner)
    {
        return new BehaviorTagHandlerDelegate(owner);
    }

    @Override
    public TagHandlerDelegate createComponentHandlerDelegate(
            ComponentHandler owner)
    {
        return new ComponentTagHandlerDelegate(owner);
    }

    @Override
    public TagHandlerDelegate createConverterHandlerDelegate(
            ConverterHandler owner)
    {
        return new ConverterTagHandlerDelegate(owner);
    }

    @Override
    public TagHandlerDelegate createValidatorHandlerDelegate(
            ValidatorHandler owner)
    {
        return new ValidatorTagHandlerDelegate(owner);
    }

}
