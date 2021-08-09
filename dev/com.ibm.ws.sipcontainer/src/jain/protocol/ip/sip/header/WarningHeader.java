/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jain.protocol.ip.sip.header;

import jain.protocol.ip.sip.SipParseException;

/**
 * <p>
 * This interface represents the Warning response-header.
 * WarningHeader is used to carry additional information about
 * the status of a Response. WarningHeaders are sent
 * with Responses and a Response may carry more
 * than one WarningHeader.
 * </p><p>
 * The text should be in a natural language that is most likely
 * to be intelligible to the human user receiving the Response.
 * This decision can be based on any available knowledge, such as the
 * location of the cache or user, the AcceptLanguageHeader in a
 * Request, or the ContentLanguageHeader in a Response.
 * </p><p>
 * Any server may add WarningHeaders to a Response. Proxy servers
 * must place additional WarningHeaders before any AuthorizationHeaders.
 * Within that constraint, WarningHeaders must be added after any
 * existing WarningHeaders not covered by a signature. A proxy server
 * must not delete any WarningHeader that it received with a
 * Response.
 * </p><p>
 * When multiple WarningHeaders are attached to a Response, the user
 * agent should display as many of them as possible, in the order that
 * they appear in the Response. If it is not possible to display all of
 * the warnings, the user agent first displays warnings that appear
 * early in the Response.
 * </p><p>
 * The warning code consists of three digits. A first digit of "3"
 * indicates warnings specific to SIP (1xx and 2xx have been taken by HTTP/1.1).
 * This is a list of the
 * currently-defined "warn-code"s, each with a recommended warning text
 * in English, and a description of its meaning. Note that these warnings
 * describe failures induced by the session description.
 * </p><p>
 * Warning codes 300 through 329 are reserved for indicating problems with
 * keywords in the session description, 330 through 339 are warnings
 * related to basic network services requested in the session
 * description, 370 through 379 are warnings related to quantitative QoS
 * parameters requested in the session description, and 390 through 399
 * are miscellaneous warnings that do not fall into one of the above
 * categories.
 * </p>
 * Warning code constants are defined in this interface.
 *
 * @version 1.0
 *
 */
public interface WarningHeader extends Header
{
    
    /**
     * Gets agent of WarningHeader
     * @return agent of WarningHeader
     */
    public String getAgent();
    
    /**
     * Gets text of WarningHeader
     * @return text of WarningHeadert
     */
    public String getText();
    
    /**
     * Sets code of WarningHeader
     * @param <var>code</var> code
     * @throws SipParseException if code is not accepted by implementation
     */
    public void setCode(int code)
                 throws SipParseException;
    
    /**
     * Sets agent host of WarningHeader
     * @param <var>host</var> agent host
     * @throws IllegalArgumentException if host is null
     * @throws SipParseException if host is not accepted by implementation
     */
    public void setAgent(String host)
                 throws IllegalArgumentException,SipParseException;
    
    /**
     * Sets text of WarningHeader
     * @param <var>text</var> text
     * @throws IllegalArgumentException if text is null
     * @throws SipParseException if text is not accepted by implementation
     */
    public void setText(String text)
                 throws IllegalArgumentException,SipParseException;
    
    /**
     * Gets code of WarningHeader
     * @return code of WarningHeader
     */
    public int getCode();
    
    /**
     * One or more network protocols
     * contained in the session description are not available.
     */
    public final static int INCOMPATIBLE_NETWORK_PROTOCOL = 300;
    
    /**
     * One or more network address
     * formats contained in the session description are not available.
     */
    public final static int INCOMPATIBLE_NETWORK_ADDRESS_FORMATS = 301;
    
    /**
     * The warning text can include arbitrary
     * information to be presented to a human user, or logged. A system
     * receiving this warning MUST NOT take any automated action.
     */
    public final static int MISCELLANEOUS_WARNING = 399;
    
    /**
     * The site where the user is located does
     * not support multicast.
     */
    public final static int MULTICAST_NOT_AVAILABLE = 330;
    
    /**
     * The site where the user is located does
     * not support unicast communication (usually due to the presence
     * of a firewall).
     */
    public final static int UNICAST_NOT_AVAILABLE = 331;
    
    ////////////////////////////////////////////////////////
    
    /**
     * Name of WarningHeader
     */
    public final static String name = "Warning";
    
    /**
     * One or more of the media attributes in
     * the session description are not supported.
     */
    public final static int ATTRIBUTE_NOT_UNDERSTOOD = 306;
    
    /**
     * A parameter other
     * than those listed above was not understood.
     */
    public final static int SESSION_DESCRIPTION_PARAMETER_NOT_UNDERSTOOD = 307;
    
    /**
     * One or more bandwidth measurement
     * units contained in the session description were not understood.
     */
    public final static int INCOMPATIBLE_BANDWIDTH_UNITS = 303;
    
    /**
     * The bandwidth specified in the session
     * description or defined by the media exceeds that known to be
     * available.
     */
    public final static int INSUFFICIENT_BANDWIDTH = 370;
    
    /**
     * One or more media types contained in
     * the session description are not available.
     */
    public final static int MEDIA_TYPE_NOT_AVAILABLE = 304;
    
    /**
     * One or more media formats contained in
     * the session description are not available.
     */
    public final static int INCOMPATIBLE_MEDIA_FORMAT = 305;
    
    /**
     * One or more transport protocols
     * described in the session description are not available.
     */
    public final static int INCOMPATIBLE_TRANSPORT_PROTOCOL = 302;
}
