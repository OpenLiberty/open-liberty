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
package org.apache.myfaces.shared.application;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

public class NavigationUtils
{
    /**
     * Evaluate all EL expressions found as parameters and return a map that can be used for 
     * redirect or render bookmark links
     * 
     * @param parameters parameter map retrieved from NavigationCase.getParameters()
     * @return
     */
    public static Map<String, List<String> > getEvaluatedNavigationParameters(
            FacesContext facesContext, 
            Map<String, List<String> > parameters)
    {
        Map<String,List<String>> evaluatedParameters = null;
        if (parameters != null && parameters.size() > 0)
        {
            evaluatedParameters = new HashMap<String, List<String>>();
            for (Map.Entry<String, List<String>> pair : parameters.entrySet())
            {
                boolean containsEL = false;
                for (String value : pair.getValue())
                {
                    if (_isExpression(value))
                    {
                        containsEL = true;
                        break;
                    }
                }
                if (containsEL)
                {
                    evaluatedParameters.put(pair.getKey(), 
                            _evaluateValueExpressions(facesContext, pair.getValue()));
                }
                else
                {
                    evaluatedParameters.put(pair.getKey(), pair.getValue());
                }
            }
        }
        else
        {
            evaluatedParameters = parameters;
        }
        return evaluatedParameters;
    }
    
    /**
     * Checks the Strings in the List for EL expressions and evaluates them.
     * Note that the returned List will be a copy of the given List, because
     * otherwise it will have unwanted side-effects.
     * @param values
     * @return
     */
    private static List<String> _evaluateValueExpressions(FacesContext context, List<String> values)
    {
        // note that we have to create a new List here, because if we
        // change any value on the given List, it will be changed in the
        // NavigationCase too and the EL expression won't be evaluated again
        List<String> target = new ArrayList<String>(values.size());
        for (String value : values)
        {
            if (_isExpression(value))
            {
                // evaluate the ValueExpression
                value = context.getApplication().evaluateExpressionGet(context, value, String.class);
            }
            target.add(value);
        }
        return target;
    }
    
    private static boolean _isExpression(String text)
    {
        return text.indexOf("#{") != -1;
    }

}
