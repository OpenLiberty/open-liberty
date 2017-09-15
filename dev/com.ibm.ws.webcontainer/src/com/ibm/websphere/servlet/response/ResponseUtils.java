/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.servlet.response;
/**
 * @ibm-api
 * ResponseUtils contains publicly available utilities for working with
 * response data
 */
public class ResponseUtils
{
    /**
     * Searches the passed in String for any characters that could be
     * used in a cross site scripting attack (<, >, +, &, ", ', (, ), %, ;)
     * and converts them to their browser equivalent name or code specification.
     *
     * @param iString contains the String to be encoded
     *
     * @return an encoded String
     */
    public static String encodeDataString(String iString)
    {
        if (iString == null)
            return "";
        
        int strLen = iString.length(), i;

        if (strLen < 1)
            return iString;

        // convert any special chars to their browser equivalent specification
        StringBuffer retString = new StringBuffer(strLen * 2);

	for (i = 0; i < strLen; i++)
        {
	    switch (iString.charAt(i))
            {
                case '<':
                    retString.append("&lt;");
	       	    break;

	       	case '>':
                    retString.append("&gt;");
	       	    break;

	       	case '&':
                    retString.append("&amp;");
	       	    break;

	       	case '\"':
                    retString.append("&quot;");
	       	    break;

	       	case '+':
                    retString.append("&#43;");
	       	    break;

	       	case '(':
                    retString.append("&#40;");
	       	    break;

	       	case ')':
                    retString.append("&#41;");
	       	    break;

	       	case '\'':
                    retString.append("&#39;");
	       	    break;

	       	case '%':
                    retString.append("&#37;");
	       	    break;

	       	case ';':
                    retString.append("&#59;");
	       	    break;

	       	default:
	       	    retString.append(iString.charAt(i));
	       	    break;
	    }
	}
			
	return retString.toString();
    }
}

