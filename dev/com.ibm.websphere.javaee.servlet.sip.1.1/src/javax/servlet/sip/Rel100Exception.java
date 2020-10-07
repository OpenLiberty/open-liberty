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

import javax.servlet.ServletException;

/**
 * Indicates that a provisional response cannot be sent reliably.
 * 
 * <p>This is thrown by the container when an application requested
 * that a provisional response be sent reliably (using the 100rel
 * extension defined in RFC 3262) but one or more of the conditions
 * for using 100rel is not satisfied:
 * <ul>
 * <li>the status code of the response is not in the range 101-199
 * <li>the request was not an INVITE
 * <li>the UAC did not indicate support for the 100rel extension in the request
 * <li>the container doesn't support the 100rel extension
 * </ul>
 * 
 * <p>The actual reason why {@link SipServletResponse#sendReliably}
 * failed can be discovered through {@link #getReason()}.
 */
public class Rel100Exception extends ServletException {


    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
     * Reason code indicating that the UAC didn't indicate support for the 
     * reliable responses extension in the request. 
     */
    public static final int NO_REQ_SUPPORT = 2;

    /**
     * Reason code indicating that {@link SipServletResponse#createPrack()}  was 
     * invoked on a provisional response that is not reliable. 
     */
    public static final int NOT_100rel = 4;

    /**
     * Reason code indicating that {@link SipServletResponse#sendReliably}
     * was invoked on a final or a 100 response.
     */
    public static final int NOT_1XX        = 0;

    /**
     * Reason code indicating that {@link SipServletResponse#sendReliably}
     * was invoked for a response to a non-INVITE request.
     */
    public static final int NOT_INVITE     = 1;
    
    /**
     * Reason code indicating that the container does not support
     * reliable provisional response.
     */
    public static final int NOT_SUPPORTED  = 3;

    /** The reason code. */
    private int reason;

    /**
     * Constructs a new <code>Rel100Exception</code> with the specified
     * error reason.
     * 
     * @param reason one of <code>NOT_1XX</code>, <code>NOT_INVITE</code>,
     *      <code>NO_UAC_SUPPORT</code>, <code>NOT_SUPPORTED</code>
     */
    public Rel100Exception(int reason) {
        this.reason = reason;
    }

    /**
     * Returns message phrase suitable for the reason integer code.
     * 
     * @return descriptive message phrase
     */
    public String getMessage() {
        switch (reason) {
        case NOT_1XX:
            return "Response not a non-100 1xx";
        case NOT_INVITE:
            return "Response is not for an INVITE";
        case NO_REQ_SUPPORT:
            return "UAC didn't indicate support for the reliable responses extension in the request";
        case NOT_SUPPORTED:
            return "100rel not supported by the container";
        case NOT_100rel:
        	return "SipServletResponse.createPrack()  was invoked on a provisional response that is not reliable";
        default:
            return "Failed to send response reliably";
        }
    }
    
    /**
     * Returns an integer code indicating the specific reason why this
     * exception was thrown.
     * 
     * @return one of <code>NOT_1XX</code>, <code>NOT_INVITE</code>,
     *      <code>NO_UAC_SUPPORT</code>, <code>NOT_SUPPORTED</code>
     */
    public int getReason() {
        return reason;
    }
}
