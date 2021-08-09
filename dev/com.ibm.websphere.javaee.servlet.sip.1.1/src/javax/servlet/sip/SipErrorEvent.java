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
package javax.servlet.sip;

import java.util.EventObject;

/**
 * Events of this type are sent to objects implementing the
 * {@link SipErrorListener} interface when an error occurs which
 * is related to the applications processing of a SIP transaction.
 */
public class SipErrorEvent extends EventObject {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -4393226611841330754L;
	
	private transient SipServletRequest request = null;
    private transient SipServletResponse response = null;
    
    /**
     * Constructs a new <code>SipErrorEvent</code>.
     * 
     * @param request  the request the error relates to
     * @param response the response the error relates to
     */
    public SipErrorEvent(SipServletRequest request,
                         SipServletResponse response)
    {
        super(request);
        this.request = request;
        this.response = response;
    }
    
    /**
     * Returns the request object associated with this
     * <code>SipErrorEvent</code>.
     * 
     * @return request object associated with this <code>SipErrorEvent</code>
     */
    public SipServletRequest getRequest() {
        return request;
    }
    
    /**
     * Returns the response object associated with this
     * <code>SipErrorEvent</code>.
     * 
     * @return response object associated with this <code>SipErrorEvent</code>
     */
    public SipServletResponse getResponse() {
        return response;
    }
}
