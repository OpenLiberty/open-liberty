/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.rules;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;

/**
 * @author Amir Perlman, Dec 25, 2003
 *
 * Wrapper for phone number represented as a String. Overides the equals 
 * function for providing appropriate phone matching. Phone numbers are compared 
 * by digits only, other characters are ignored. 
 */
public class PhoneComparison
{
    /**
     * Phone number wrapped by this class.  
     */
    private String m_phone;
    /**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(PhoneComparison.class);

    /**
     * Construct a new phone comparator for the given number. 
     */
    public PhoneComparison(String phone)throws IllegalArgumentException 
    {
        boolean init = false;
        if(phone!=null)
        {
            init = true;
	        char[] a = phone.toLowerCase().toCharArray();
	        int i = 0;
	        for (; i < a.length&& init; i++) {
	            if (Character.isLetter(a[i])) {
	                int numValue = Character.getNumericValue(a[i]);
	                if (numValue > 13 || numValue < 10) {
	                    init = false;
	                 }
	            }
	        }
        }
        if(init == false){
            throw new IllegalArgumentException(
                                               "PhoneComparison cannot be initialized for phoneNum = "
                                               + phone);
        }
        
        m_phone = phone;
    }
    
    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return m_phone;
    }

    /**
     * Compares phone numbers by matching digits only. 
     * Any non digit separators are ignored in comparison.
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public final boolean equals(Object other,boolean ignoreCase)
    {      
        if (null == m_phone || other == null)
        {
            return false;
        }
        boolean rc = true;

        char[] a = m_phone.toLowerCase().toCharArray();
        char[] b = other.toString().toLowerCase().toCharArray();
        int j = 0;
        char c;

        //Anat : According to rfc2806 -	"tel" URL scheme 
        //can contain dtmf-digit = "*" / "#" / "A" / "B" / "C" / "D"
        //so need to copmare them as well 
        for (int i = 0; i < a.length; i++)
        {
            if (Character.isLetterOrDigit(a[i]))
            {
                //Init c with a non digit value
                c = ' ';
                for (; j < b.length; j++)
                {
                    if (Character.isLetterOrDigit(b[j]))
                    {
                        c = b[j];
                        j++;
                        break;
                    }
                }

                if (c != a[i])
                {
                    rc = false;
                    break;
                }
            }
        }

        return rc;

    }
    
    /**
     * Checks if phone number containes requested string
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public final boolean contains(String other,boolean ignoreCase)
    {
       return m_phone.toLowerCase().indexOf(other.toLowerCase()) > -1;
    }
}
