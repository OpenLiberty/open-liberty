/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.translator.document;

import java.io.IOException;
import java.io.Reader;
import java.util.StringTokenizer;

import com.ibm.ws.jsp.Constants;
import com.ibm.ws.jsp.JspCoreException;

public class JspEncodingScanner
{
	// 221083 - pageEncoding overrides charset regardless of the order in which they're found.  Also,
	//			scan all page directives until pageEncoding is found, or end of page directives.
	protected String pageEncoding = null;
    protected String charset = null;
    protected boolean jspRootFound = false;
    protected Reader jspReader = null;
    protected int charCount = 0;

    public JspEncodingScanner(Reader jspReader) {
        this.jspReader = jspReader;    
    }
    
    public boolean scan() throws JspCoreException {
        boolean scanOk = true;
        try {            
            jspReader.mark(Constants.DEFAULT_BUFFER_SIZE);
            int character = 0;
            boolean endFound = false;
            boolean saveChars = false;
            StringBuffer chars = new StringBuffer();

            while (endFound == false) {
                character = readCharacter();
                
                if (charCount < Constants.DEFAULT_BUFFER_SIZE-1) {
                    switch (character) {
                        case -1: {
                            endFound = true;
                            break;    
                        }
                        
                        case '<': {
                            int nextChar = readCharacter();
                            int nextNextChar = readCharacter();
                            if (nextChar == '%' && nextNextChar == '@') {
                                scanPageDirective();
                                if (pageEncoding != null) // 221083
                                    endFound = true;    
                            }
                            // begin 221074: code was checking inside of commented 
                            // out directives for pageEncoding and contentType
                            else if (nextChar == '%' && nextNextChar == '-') {
                            	int nextNextNextChar = readCharacter();
                            	if(nextNextNextChar == '-')
                                {
                                    // PQ99398: change ignoreComment to return boolean
                            		if ( ignoreComment () )
                                        endFound = true;
                            	}
                            	else{
                                    saveChars = true;    
                                    chars.append((char)character);
                                    chars.append((char)nextChar);
                                    chars.append((char)nextNextChar);
                                    chars.append((char)nextNextNextChar);
                            	}
                            }
                        	//end 221074: code was checking inside of commented out directives for pageEncoding and contentType
                            
                            else {
                                saveChars = true;    
                                chars.append((char)character);
                                chars.append((char)nextChar);
                                chars.append((char)nextNextChar);
                            }
                            break;	
                        }
                        
                        case '>': {
                            if (saveChars) {
                                saveChars = false;
                                if (chars.toString().startsWith("<jsp:root"))
                                    jspRootFound = true;                            
                                chars.delete(0, chars.length());
                            }
                        }
                        
                        default : {
                            if (saveChars)
                                chars.append((char)character);
                        }
                    }
                }
                else {
                    endFound = true;
                }
            }
        }
        catch (IOException e) {
            throw new JspCoreException(e);
        }
        finally {
            try {
                jspReader.reset();
            }
            catch (IOException e) {
                scanOk = false;
            }
        }
        return scanOk;
    }
    
    public String getEncoding() {
        return (pageEncoding!=null?pageEncoding:charset); // 221083
    }
    
    public boolean jspRootFound() {
        return jspRootFound; 
    }

    protected void scanPageDirective() throws JspCoreException, IOException {
        boolean endFound = false;
        
        StringBuffer directive = new StringBuffer();
        StringBuffer name = new StringBuffer();
        StringBuffer value = new StringBuffer();

        boolean inValue = false;
        boolean inDirective = true;
        
        int prev = 0;
        int character = 0;
        
        while (endFound == false) {
            prev = character;
            character = readCharacter();

            switch (character) {
                case -1: {
                    throw new JspCoreException("jsp.error.end.of.file.reached");
                }
                
                case '<': {
                    if (inValue)
                        value.append("&lt;");
                    else if (inDirective)
                        throw new JspCoreException("jsp.error.invalid.jsp.syntax", new Object[] {directive.toString()});
                    else                                                         
                        throw new JspCoreException("jsp.error.invalid.jsp.syntax", new Object[] {name.toString()});
                    break;                        
                }
                
                case '>': {
                    if (prev == '%')
                        endFound = true;
                    else {
                        if (inValue)
                            value.append("&gt;");
                        else if (!inDirective)
                            name.append("&gt;");
                    }
                    break; 
                }

                case '\r':
                {
                    break;
                }
                
                case '=': {
                    if (inValue) 
                        value.append((char)character);
                    break;
                }
                // PK00433: Treat new line and tab as equivalent to space
                case '\n':
                case '\t': 
                case ' ': 
                {
                    if (inDirective && directive.toString().trim().length() > 0) 
                    {
                        inDirective = false;
                    }
                    else if (inValue) {
                        value.append((char)character);
                    }
                    break;
                }

                case '\'':
                case '\"': {
                    if (inValue) {
                        if (directive.toString().equals("page") ||
                            directive.toString().equals("tag")) {
                            if (name.toString().equals("pageEncoding")) {
                                pageEncoding = value.toString(); // 221083
                            }
                            else if (name.toString().equals("contentType")) {
                        		// begin 221074: this code was working per the specification but regressed behavior of 5.0. 
                            	// changed to search for charset without the leading ; being immediately 1 or zero characters before charset.
                               /* if (value.indexOf("; charset=") != -1 ||		// defect 219340 change from && to ||
                                    value.indexOf(";charset=") != -1) {
                                    encoding = value.substring(value.indexOf("charset=")+8);	// defect 219340 change to 8 instead of 9
                                }
                                */
                            	
                            	if( (value!= null) && (value.indexOf(";") != -1 )){
                            		StringTokenizer st = new StringTokenizer (value.toString(), ";");
                            		while (st.hasMoreTokens()){
                            			String token = st.nextToken();
                                    	if (token.indexOf("charset=") != -1){
                                    		charset = value.substring(value.indexOf("charset=")+8).trim(); // 221083
                                    	}
                            		}
                            	}
                        		//end 221074: this code was working per the specification but regressed behavior of 5.0. 
                            }
                        }
                        name.delete(0, name.length());
                        value.delete(0, value.length());
                        inValue = false;
                    }
                    else {
                        inValue = true;
                    }
                    break;
                }
                default: {
                    if (inValue)
                        value.append((char)character);
                    else if (inDirective)
                        directive.append((char)character);
                    else
                        name.append((char)character);
                    break;
                }
            }
        }
    }
    
    private int readCharacter() throws IOException {
        int character = jspReader.read();
        charCount++;        
        return character;
    }
	// begin 221074: code was added to ignore comments when searching for page directive. 
    private boolean ignoreComment( ) throws IOException
    {
    	StringBuffer comment = new StringBuffer();
        int prev = 0;
        int character = 0;
        boolean endFound = false;
        boolean eofFound = false;
        while( endFound == false )
        {
            prev = character;
            character = readCharacter();
            if (character != -1) 
            {
                comment.append( (char) character );
                if ( character == '>' && prev =='%' )
                {
                   String fullComment = comment.toString(); 
                   if (fullComment.endsWith("--%>"))
                   {
                     endFound = true;
                   }
                }
            }
            else 
            {
              eofFound = endFound = true;
            }
        } // end "while"
        return eofFound;
    }
    // end 221074: code was added to ignore comments when searching for page directive.
    
}
