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
package org.apache.myfaces.shared.renderkit.html.util;

import java.util.Map;
import javax.faces.context.FacesContext;

public class SharedStringBuilder
{
    public static StringBuilder get(String stringBuilderKey)
    {
        return get(FacesContext.getCurrentInstance(), stringBuilderKey);
    }

    // TODO checkstyle complains; does this have to lead with __ ?
    public static StringBuilder get(FacesContext facesContext, String stringBuilderKey)
    {
        Map<Object, Object> attributes = facesContext.getAttributes();

        StringBuilder sb = (StringBuilder) attributes.get(stringBuilderKey);

        if (sb == null)
        {
            sb = new StringBuilder();
            attributes.put(stringBuilderKey, sb);
        }
        else
        {

            // clear out the stringBuilder by setting the length to 0
            sb.setLength(0);
        }

        return sb;
    }
    
    public static StringBuilder get(FacesContext facesContext, String stringBuilderKey, int initialSize)
    {
        Map<Object, Object> attributes = facesContext.getAttributes();

        StringBuilder sb = (StringBuilder) attributes.get(stringBuilderKey);

        if (sb == null)
        {
            sb = new StringBuilder(initialSize);
            attributes.put(stringBuilderKey, sb);
        }
        else
        {

            // clear out the stringBuilder by setting the length to 0
            sb.setLength(0);
        }

        return sb;
    }
}
