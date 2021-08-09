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
package org.apache.myfaces.shared.resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ValueExpression;
import javax.faces.application.Resource;
import javax.faces.context.FacesContext;
import javax.faces.event.ExceptionQueuedEvent;
import javax.faces.event.ExceptionQueuedEventContext;

import org.apache.myfaces.shared.util.io.DynamicPushbackInputStream;

public class ValueExpressionFilterInputStream extends InputStream
{
    private PushbackInputStream delegate;
    private String libraryName;
    private String resourceName;
    private String contractName;
    
    public ValueExpressionFilterInputStream(InputStream in, String libraryName, String resourceName)
    {
        super();
        delegate = new DynamicPushbackInputStream(in,300);
        this.libraryName = libraryName;
        this.resourceName = resourceName;
        this.contractName = null;
    }
    
    public ValueExpressionFilterInputStream(InputStream in, Resource resource)
    {
        super();
        delegate = new DynamicPushbackInputStream(in,300);
        this.libraryName = resource.getLibraryName();
        this.resourceName = resource.getResourceName();
        this.contractName = (resource instanceof ContractResource) ? 
                ((ContractResource)resource).getContractName() : null;
    }

    @Override
    public int read() throws IOException
    {
        int c1 = delegate.read();
        
        if (c1 == -1)
        {
            return -1;
        }
        
        if ( ((char)c1) == '#')
        {
            int c2 = delegate.read();
            if (c2 == -1)
            {
                return -1;
            }
            if (((char)c2) == '{')
            {
                //It is a value expression. We need
                //to look for a occurrence of } to 
                //extract the expression and evaluate it,
                //the result should be unread.
                List<Integer> expressionList = new ArrayList<Integer>();
                int c3 = delegate.read();
                while ( c3 != -1 && ((char)c3) != '}' )
                {
                    expressionList.add(c3);
                    c3 = delegate.read();
                }
                
                if (c3 == -1)
                {
                    //get back the data, because we can't
                    //extract any value expression
                    for (int i = 0; i < expressionList.size(); i++)
                    {
                        delegate.unread(expressionList.get(i));
                    }
                    delegate.unread(c2);
                    return c1;
                }
                else
                {
                    //EL expression found. Evaluate it and pushback
                    //the result into the stream
                    FacesContext context = FacesContext.getCurrentInstance();
                    ELContext elContext = context.getELContext();
                    try
                    {
                        if (libraryName != null)
                        {
                            ResourceELUtils.saveResourceLibraryForResolver(context, libraryName);
                        }
                        if (contractName != null)
                        {
                            ResourceELUtils.saveResourceContractForResolver(context, contractName);
                        }
                        ValueExpression ve = context.getApplication().
                            getExpressionFactory().createValueExpression(
                                    elContext,
                                    "#{"+convertToExpression(expressionList)+"}",
                                    String.class);
                        String value = (String) ve.getValue(elContext);
                        
                        for (int i = value.length()-1; i >= 0 ; i--)
                        {
                            delegate.unread((int) value.charAt(i));
                        }
                    }
                    catch(ELException e)
                    {
                        ExceptionQueuedEventContext equecontext = new ExceptionQueuedEventContext (
                                context, e, null);
                        context.getApplication().publishEvent (context, ExceptionQueuedEvent.class, equecontext);
                        
                        Logger log = Logger.getLogger(ResourceImpl.class.getName());
                        if (log.isLoggable(Level.SEVERE))
                        {
                            log.severe("Cannot evaluate EL expression " + convertToExpression(expressionList)
                                    + " in resource " + (libraryName == null?"":libraryName) + ":" + 
                                    (resourceName == null?"":resourceName));
                        }
                        
                        delegate.unread(c3);
                        for (int i = expressionList.size()-1; i >= 0; i--)
                        {
                            delegate.unread(expressionList.get(i));
                        }
                        delegate.unread(c2);
                        return c1;
                    }
                    finally
                    {
                        if (libraryName != null)
                        {
                            ResourceELUtils.removeResourceLibraryForResolver(context);
                        }
                        if (contractName != null)
                        {
                            ResourceELUtils.removeResourceContractForResolver(context);
                        }
                    }
                    
                    //read again
                    return delegate.read();
                }
            }
            else
            {
                delegate.unread(c2);
                return c1;
            }
        }
        else
        {
            //just continue
            return c1;
        }
    }
    
    private String convertToExpression(List<Integer> expressionList)
    {
        char[] exprArray = new char[expressionList.size()];
        
        for (int i = 0; i < expressionList.size(); i++)
        {
            exprArray[i] = (char) expressionList.get(i).intValue();
        }
        return String.valueOf(exprArray);
    }

    @Override
    public void close() throws IOException
    {
        delegate.close();
    }
}
