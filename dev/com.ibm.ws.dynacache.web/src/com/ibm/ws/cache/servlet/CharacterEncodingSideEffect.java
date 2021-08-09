/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.servlet;

import com.ibm.ws.ffdc.FFDCFilter;
import javax.servlet.http.HttpServletResponse;

public class CharacterEncodingSideEffect
    implements ResponseSideEffect
{

    public String toString()
    {
        StringBuffer sb = new StringBuffer("Character Encoding side effect: \n\t");
        sb.append("Character Encoding: ").append(characterEncoding).append("\n");
        return sb.toString();
    }

    public CharacterEncodingSideEffect(String charEnc)
    {
        characterEncoding = null;
        characterEncoding = charEnc;
    }

    public void performSideEffect(HttpServletResponse response)
    {
        try
        {
            if(response instanceof CacheProxyResponse)
            {
                CacheProxyResponse cpr = (CacheProxyResponse)response;
                if(!cpr._gotWriter && !cpr._gotOutputStream && !cpr.getResponse().isCommitted())
                    cpr.setCharacterEncoding(characterEncoding);
            }
        }
        catch(IllegalStateException ex)
        {
            FFDCFilter.processException(ex, "com.ibm.ws.cache.servlet.ContentTypeSideEffect.performSideEffect", "71", this);
        }
    }

    private static final long serialVersionUID = 0xf8bdef22fd0be42aL;
    private String characterEncoding;
}

