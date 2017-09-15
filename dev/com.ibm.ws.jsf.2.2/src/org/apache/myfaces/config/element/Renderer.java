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
package org.apache.myfaces.config.element;

import java.io.Serializable;

/**
 * @author Manfred Geiler (latest modification by $Author: struberg $)
 * @version $Revision: 1188686 $ $Date: 2011-10-25 14:59:52 +0000 (Tue, 25 Oct 2011) $
 */
public abstract class Renderer implements Serializable
{
    // <!ELEMENT renderer (description*, display-name*, icon*, component-family,
    // renderer-type, renderer-class, attribute*, renderer-extension*)>

    public abstract String getComponentFamily();
    public abstract String getRendererType();
    public abstract String getRendererClass();

}
