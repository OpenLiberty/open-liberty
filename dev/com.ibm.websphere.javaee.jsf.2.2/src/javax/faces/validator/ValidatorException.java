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
package javax.faces.validator;

import java.util.Collection;

import javax.faces.FacesException;
import javax.faces.application.FacesMessage;

/**
 * see Javadoc of <a href="http://java.sun.com/javaee/javaserverfaces/1.2/docs/api/index.html">JSF Specification</a>
 */
public class ValidatorException
        extends FacesException
{
    private static final long serialVersionUID = 5965885122446047949L;
    private FacesMessage _facesMessage;
    private Collection<FacesMessage> _facesMessages;

    public ValidatorException(Collection<FacesMessage> messages)
    {
        super(facesMessagesToString(messages));
        _facesMessages = messages;
    }
    
    public ValidatorException(Collection<FacesMessage> messages, Throwable cause)
    {
        super(facesMessagesToString(messages),cause);
        _facesMessages = messages;
    }
    
    public ValidatorException(FacesMessage message)
    {
        super(facesMessageToString(message));
        _facesMessage = message;
    }

    public ValidatorException(FacesMessage message,
                              Throwable cause)
    {
        super(facesMessageToString(message), cause);
        _facesMessage = message;
    }

    public FacesMessage getFacesMessage()
    {
        return _facesMessage;
    }
    
    public Collection<FacesMessage> getFacesMessages()
    {
        return _facesMessages;
    }

    private static String facesMessageToString(FacesMessage message)
    {
        String summary = message.getSummary();
        String detail = message.getDetail();
        
        if (summary != null)
        {
            if (detail != null)
            {
                return summary + ": " + detail;
            }
            
            return summary;
        }
        
        return detail != null ? detail : "";
    }
    
    private static String facesMessagesToString(Collection<FacesMessage> messages)
    {
        if (messages == null || messages.isEmpty())
        {
            return "";
        }
        StringBuilder sb = new StringBuilder();

        String separator = "";
        for (FacesMessage message : messages)
        {
            if (message != null)
            {
                String summary = message.getSummary();
                String detail = message.getDetail();
                
                if (summary != null)
                {
                    sb.append(separator);
                    sb.append(summary);
                    if (detail != null)
                    {
                        sb.append(": ");
                        sb.append(detail);
                    }
                    separator = ", ";
                }
            }
        }
        return sb.toString();
    }
}
