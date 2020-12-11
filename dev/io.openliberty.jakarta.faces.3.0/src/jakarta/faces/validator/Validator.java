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
package jakarta.faces.validator;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import java.util.EventListener;

/**
 * see Javadoc of <a href="http://java.sun.com/javaee/javaserverfaces/1.2/docs/api/index.html">JSF Specification</a>
 */
public interface Validator<T> extends EventListener
{
    /**
     * @deprecated
     */
    @Deprecated
    public static final String NOT_IN_RANGE_MESSAGE_ID = "jakarta.faces.validator.NOT_IN_RANGE";

    public void validate(FacesContext context, UIComponent component, T value) throws ValidatorException;
}
