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
package com.ibm.ws.sip.container.pmi;

public interface ResponseModuleInterface{
    
//  Constants
	static final int INBOUND_OTHER = 1000;
	// Informational status codes - 1xx
	static final int INBOUND_TRYING = 1100;
    static final int INBOUND_RINGING = 1180;
    static final int INBOUND_CALL_BEING_FORWARDED = 1181;
    static final int INBOUND_CALL_QUEUED = 1182;
    static final int INBOUND_SESSION_PROGRESS = 1183;
    // Success status codes - 2xx
    static final int INBOUND_OK = 1200;
    static final int INBOUND_ACCEPTED = 1202;
    static final int INBOUND_NO_NOTIFICATION = 1204;
    // Redirection status codes - 3xx
    static final int INBOUND_MULTIPLE_CHOICES = 1300;
    static final int INBOUND_MOVED_PERMANENTLY = 1301;
    static final int INBOUND_MOVED_TEMPORARILY = 1302;
    static final int INBOUND_USE_PROXY = 1305;
    static final int INBOUND_ALTERNATIVE_SERVICE = 1380;
    // Client failure - 4xx
    static final int INBOUND_BAD_REQUEST = 1400;
    static final int INBOUND_UNAUTHORIZED = 1401;
    static final int INBOUND_PAYMENT_REQUIRED = 1402;
    static final int INBOUND_FORBIDDEN = 1403;
    static final int INBOUND_NOT_FOUND = 1404;
    static final int INBOUND_METHOD_NOT_ALLOWED = 1405;
    static final int INBOUND_NOT_ACCEPTABLE = 1406;
    static final int INBOUND_PROXY_AUTHENTICATION_REQUIRED = 1407;
    static final int INBOUND_REQUEST_TIMEOUT = 1408;
    static final int INBOUND_CONFLICT = 1409;
    static final int INBOUND_GONE = 1410;
    static final int INBOUND_CONDITIONAL_REQUEST_FAILED = 1412;
    static final int INBOUND_REQUEST_ENTITY_TOO_LARGE = 1413;
    static final int INBOUND_REQUEST_URI_TOO_LONG = 1414;
    static final int INBOUND_UNSUPPORTED_MEDIA_TYPE = 1415;
    static final int INBOUND_UNSUPPORTED_URI_SCHEME = 1416;
    static final int INBOUND_UNKNOWN_RESOURCE_PRIORITY = 1417;
    static final int INBOUND_BAD_EXTENSION = 1420;
    static final int INBOUND_EXTENSION_REQUIRED = 1421;
    static final int INBOUND_SESSION_INTERVAL_TOO_SMALL = 1422;
    static final int INBOUND_INTERVAL_TOO_BRIEF = 1423;    
    static final int INBOUND_BAD_LOCATION_INFORMATION = 1424;
    static final int INBOUND_USE_IDENTITY_HEADER = 1428;
    static final int INBOUND_PROVIDE_REFERRER_IDENTITY = 1429;
    static final int INBOUND_ANONYMILY_DISALLOWED = 1433;
    static final int INBOUND_BAD_IDENTITY_INFO = 1436;
    static final int INBOUND_UNSUPPORTED_CERTIFICATE = 1437;
    static final int INBOUND_INVALID_IDENTITY_HEADER = 1438;
    
    static final int INBOUND_TEMPORARLY_UNAVAILABLE = 1480;
    static final int INBOUND_CALL_LEG_DONE = 1481;
    static final int INBOUND_LOOP_DETECTED = 1482;
    static final int INBOUND_TOO_MANY_HOPS = 1483;
    static final int INBOUND_ADDRESS_INCOMPLETE = 1484;
    static final int INBOUND_AMBIGUOUS = 1485;
    static final int INBOUND_BUSY_HERE = 1486;
    static final int INBOUND_REQUEST_TERMINATED = 1487;
    static final int INBOUND_NOT_ACCEPTABLE_HERE = 1488;
    static final int INBOUND_BAD_EVENT = 1489;
    static final int INBOUND_REQUEST_PENDING = 1491;
    static final int INBOUND_UNDECIPHERABLE = 1493;
    static final int INBOUND_SECURITY_AGREEMENT_REQUIRED = 1494;
    // Server failure - 5xx
    static final int INBOUND_SERVER_INTERNAL_ERROR = 1500;
    static final int INBOUND_NOT_IMPLEMENTED = 1501;
    static final int INBOUND_BAD_GATEWAY = 1502;
    static final int INBOUND_SERVICE_UNAVAILABLE = 1503;
    static final int INBOUND_SERVER_TIMEOUT = 1504;
    static final int INBOUND_VERSION_NOT_SUPPORTED = 1505;
    static final int INBOUND_MESSAGE_TOO_LARGE = 1513;
    // Global failure - 6xx
    static final int INBOUND_BUSY_EVERYWHERE = 1600;
    static final int INBOUND_DECLINE = 1603;
    static final int INBOUND_DOES_NOT_EXIT_ANYWHERE = 1604;
    static final int INBOUND_NOT_ACCEPTABLE_ANYWHERE = 1606;
    
    static final int OUTBOUND_OTHER = 2000;
    //  Informational status codes - 1xx
	static final int OUTBOUND_TRYING = 2100;
    static final int OUTBOUND_RINGING = 2180;
    static final int OUTBOUND_CALL_BEING_FORWARDED = 2181;
    static final int OUTBOUND_CALL_QUEUED = 2182;
    static final int OUTBOUND_SESSION_PROGRESS = 2183;
    // Success status codes - 2xx
    static final int OUTBOUND_OK = 2200;
    static final int OUTBOUND_ACCEPTED = 2202;
    static final int OUTBOUND_NO_NOTIFICATION = 2204;
    // Redirection status codes - 3xx
    static final int OUTBOUND_MULTIPLE_CHOICES = 2300;
    static final int OUTBOUND_MOVED_PERMANENTLY = 2301;
    static final int OUTBOUND_MOVED_TEMPORARILY = 2302;
    static final int OUTBOUND_USE_PROXY = 2305;
    static final int OUTBOUND_ALTERNATIVE_SERVICE = 2380;
    // Client failure - 4xx
    static final int OUTBOUND_BAD_REQUEST = 2400;
    static final int OUTBOUND_UNAUTHORIZED = 2401;
    static final int OUTBOUND_PAYMENT_REQUIRED = 2402;
    static final int OUTBOUND_FORBIDDEN = 2403;
    static final int OUTBOUND_NOT_FOUND = 2404;
    static final int OUTBOUND_METHOD_NOT_ALLOWED = 2405;
    static final int OUTBOUND_NOT_ACCEPTABLE = 2406;
    static final int OUTBOUND_PROXY_AUTHENTICATION_REQUIRED = 2407;
    static final int OUTBOUND_REQUEST_TIMEOUT = 2408;
    static final int OUTBOUND_CONFLICT = 2409;
    static final int OUTBOUND_GONE = 2410;
    static final int OUTBOUND_CONDITIONAL_REQUEST_FAILED = 2412;
    static final int OUTBOUND_REQUEST_ENTITY_TOO_LARGE = 2413;
    static final int OUTBOUND_REQUEST_URI_TOO_LONG = 2414;
    static final int OUTBOUND_UNSUPPORTED_MEDIA_TYPE = 2415;
    static final int OUTBOUND_UNSUPPORTED_URI_SCHEME = 2416;
    static final int OUTBOUND_UNKNOWN_RESOURCE_PRIORITY = 2417;
    static final int OUTBOUND_BAD_EXTENSION = 2420;
    static final int OUTBOUND_EXTENSION_REQUIRED = 2421;
    static final int OUTBOUND_SESSION_INTERVAL_TOO_SMALL = 2422;
    static final int OUTBOUND_INTERVAL_TOO_BRIEF = 2423;    
    static final int OUTBOUND_BAD_LOCATION_INFORMATION = 2424;
    static final int OUTBOUND_USE_IDENTITY_HEADER = 2428;
    static final int OUTBOUND_PROVIDE_REFERRER_IDENTITY = 2429;
    static final int OUTBOUND_ANONYMILY_DISALLOWED = 2433;
    static final int OUTBOUND_BAD_IDENTITY_INFO = 2436;
    static final int OUTBOUND_UNSUPPORTED_CERTIFICATE = 2437;
    static final int OUTBOUND_INVALID_IDENTITY_HEADER = 2438;
    
    static final int OUTBOUND_TEMPORARLY_UNAVAILABLE = 2480;
    static final int OUTBOUND_CALL_LEG_DONE = 2481;
    static final int OUTBOUND_LOOP_DETECTED = 2482;
    static final int OUTBOUND_TOO_MANY_HOPS = 2483;
    static final int OUTBOUND_ADDRESS_INCOMPLETE = 2484;
    static final int OUTBOUND_AMBIGUOUS = 2485;
    static final int OUTBOUND_BUSY_HERE = 2486;
    static final int OUTBOUND_REQUEST_TERMINATED = 2487;
    static final int OUTBOUND_NOT_ACCEPTABLE_HERE = 2488;
    static final int OUTBOUND_BAD_EVENT = 2489;
    static final int OUTBOUND_REQUEST_PENDING = 2491;
    static final int OUTBOUND_UNDECIPHERABLE = 2493;
    static final int OUTBOUND_SECURITY_AGREEMENT_REQUIRED = 2494;
    // Server failure - 5xx
    static final int OUTBOUND_SERVER_INTERNAL_ERROR = 2500;
    static final int OUTBOUND_NOT_IMPLEMENTED = 2501;
    static final int OUTBOUND_BAD_GATEWAY = 2502;
    static final int OUTBOUND_SERVICE_UNAVAILABLE = 2503;
    static final int OUTBOUND_SERVER_TIMEOUT = 2504;
    static final int OUTBOUND_VERSION_NOT_SUPPORTED = 2505;
    static final int OUTBOUND_MESSAGE_TOO_LARGE = 2513;
    // Global failure - 6xx
    static final int OUTBOUND_BUSY_EVERYWHERE = 2600;
    static final int OUTBOUND_DECLINE = 2603;
    static final int OUTBOUND_DOES_NOT_EXIT_ANYWHERE = 2604;
    static final int OUTBOUND_NOT_ACCEPTABLE_ANYWHERE = 2606;
       
    /**
     * Update number of Inbound Response
     * @param code The Response code
     */
    public void incrementInResponse(int code);
    
    /**
     * Update number of Outbound Response
     * @param code The Response code
     */
    public void incrementOutResponse(int code);
    
    /**
     * Update counters that were countered till now
     *
     */
    public void updateCounters();
    
    /**
     * Unregister module 
     */
    public void destroy();
}
