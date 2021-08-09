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
package org.apache.myfaces.view.jsp;

/**
 * An Exception that indicates that the user uses a
 * facelets-only feature on a JSP.
 * 
 * NOTE: this class must not extend FacesException, because
 * otherwise its message won't be displayed by the ExceptionHandler.
 * Thus it directly extends Exception.
 * 
 * @author Jakob Korherr (latest modification by $Author: jakobk $)
 * @version $Revision: 934200 $ $Date: 2010-04-14 21:21:02 +0000 (Wed, 14 Apr 2010) $
 * 
 * @since 2.0
 */
public class FaceletsOnlyException extends Exception
{

    private static final long serialVersionUID = 4268633427284543647L;

    public FaceletsOnlyException(String message, Throwable cause)
    {
        super(message, cause);
    }
    
}
