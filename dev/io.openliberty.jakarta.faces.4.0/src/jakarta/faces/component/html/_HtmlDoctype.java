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
package jakarta.faces.component.html;

import jakarta.faces.component.Doctype;
import jakarta.faces.component.UIOutput;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFComponent;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFProperty;

/**
 * @since 2.1
 */
@JSFComponent(name="h:doctype", clazz = "jakarta.faces.component.html.HtmlDoctype",
        defaultRendererType="jakarta.faces.Doctype",template=true)
abstract class _HtmlDoctype extends UIOutput implements Doctype
{

    static public final String COMPONENT_FAMILY = "jakarta.faces.Output";
    static public final String COMPONENT_TYPE = "jakarta.faces.OutputDoctype";

    @JSFProperty
    @Override
    public abstract String getPublic();

    @JSFProperty
    @Override
    public abstract String getRootElement(); 

    @JSFProperty
    @Override
    public abstract String getSystem(); 

}
