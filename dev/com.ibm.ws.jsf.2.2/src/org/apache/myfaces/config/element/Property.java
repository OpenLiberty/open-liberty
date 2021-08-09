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
import java.util.Collection;

/**
 * @author Martin Marinschek
 * @version $Revision: 1188686 $ $Date: 2011-10-25 14:59:52 +0000 (Tue, 25 Oct 2011) $
 *
     The "property" element represents a JavaBean property of the Java class
     represented by our parent element.

     Property names must be unique within the scope of the Java class
     that is represented by the parent element, and must correspond to
     property names that will be recognized when performing introspection
     against that class via java.beans.Introspector.

    <!ELEMENT property        (description*, display-name*, icon*, property-name, property-class,
default-value?, suggested-value?, property-extension*)>

 *          <p/>
 */
public abstract class Property implements Serializable
{

    public abstract Collection<? extends String> getDescriptions();

    public abstract Collection<? extends String> getDisplayNames();

    public abstract Collection<? extends String> getIcons();

    public abstract String getPropertyName();

    public abstract String getPropertyClass();

    public abstract String getDefaultValue();

    public abstract String getSuggestedValue();

    public abstract Collection<? extends String> getPropertyExtensions();

}
