/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.genericbnf.internal;

import com.ibm.wsspi.genericbnf.GenericKeys;

/**
 * Class for return codes on the various parsing methods.
 * 
 */
public class TokenCodes extends GenericKeys {

    /**
     * Constructor of a token code object
     * 
     * @param s
     * @param o
     */
    public TokenCodes(String s, int o) {
        super(s, o);
    }

    /** Return code representing "needing more data" */
    public static final TokenCodes TOKEN_RC_MOREDATA = new TokenCodes("Need_more_data", 0);
    /** Return code representing "search delimiter found" */
    public static final TokenCodes TOKEN_RC_DELIM = new TokenCodes("Delimiter_found", 1);
    /** Return code representing "CRLF found while searching" */
    public static final TokenCodes TOKEN_RC_CRLF = new TokenCodes("CRLF_found", 2);
    /** Return code representing "search arg not found" */
    public static final TokenCodes TOKEN_RC_NOTFOUND = new TokenCodes("Not found", 3);

}
