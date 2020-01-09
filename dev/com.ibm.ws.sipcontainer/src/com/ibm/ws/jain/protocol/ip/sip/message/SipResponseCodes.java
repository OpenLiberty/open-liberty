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
package com.ibm.ws.jain.protocol.ip.sip.message;

import java.util.NoSuchElementException;

/**
 * This Class holds all response Codes for SIP messages
 * 
 * @version 1.0
 */
public final class SipResponseCodes 
{
	private SipResponseCodes(){}
	
 	/**
 	 * Sip Messages Codes
 	 */
 	
 	public static final int  INFO_TRYING	 		= 100;
	public static final int  INFO_RINGING 	 		= 180;
	public static final int  INFO_CALL_FORWARDING 		= 181;
    public static final int  INFO_QUEUED 	 		= 182;
    public static final int  INFO_SESSION_PROGRESS		= 183; 

    public static final int  OK  			= 200; 
    public static final int  ACCEPTED  			= 202; 

    public static final int REDIRECT_MULTIPLE_CHOICES  		= 300;
    public static final int REDIRECT_MOVED_PERMANENTLY 		= 301;
    public static final int REDIRECT_MOVED_TEMPORARILY 	 	= 302;
    public static final int REDIRECT_USE_PROXY	       	= 305;
    public static final int REDIRECT_ALTERNATIVE_SERVICE 	= 380;

    public static final int CLIENT_BAD_REQUEST         	= 400; 
    public static final int CLIENT_UNAUTHORIZED        	= 401;
    public static final int CLIENT_PAYMENT_REQUIRED    	= 402  ; 
    public static final int CLIENT_FORBIDDEN           	= 403 ; 
    public static final int CLIENT_NOT_FOUND           	= 404  ;  
    public static final int CLIENT_METHOD_NOT_ALLOWED  	= 405;
    public static final int CLIENT_NOT_ACCEPTABLE  		= 406;
    public static final int CLIENT_PROXY_AUTHENTICATION_REQUIRED = 407; 
    public static final int CLIENT_REQUEST_TIMEOUT     	= 408;
    public static final int CLIENT_CONFLICT             	= 409;
    public static final int CLIENT_GONE                	= 410;
    public static final int CLIENT_LENGTH_REQUIRED     	= 411;
    public static final int CLIENT_CONDITIONAL_REQUEST_FAILED = 412;
    public static final int CLIENT_REQUEST_ENTITY_TOO_LARGE = 413;
    public static final int CLIENT_REQUEST_URI_TOO_LARGE 	= 414;
    public static final int CLIENT_UNSUPPORTED_MEDIA_TYPE 	= 415;
    public static final int CLIENT_UNSUPPORTED_URI_SCHEME = 416;
    public static final int CLIENT_UNKNOWN_RESOURCE_PRIORITY = 417;
    public static final int CLIENT_BAD_EXTENSION          	= 420;
    public static final int CLIENT_EXTENSION_REQUIRED = 421;
    public static final int CLIENT_SESSION_INTERVAL_TOO_SMALL = 422;
    public static final int CLIENT_INTERVAL_TOO_BRIEF = 423;
    public static final int CLIENT_BAD_LOCATION_INFORMATION = 424;
    public static final int CLIENT_USE_IDENTITY_HEADER = 428;
    public static final int CLIENT_PROVIDE_REFERER_IDENTITY = 429;
    public static final int CLIENT_ANONYMILY_DISALLOWED = 433;    
    public static final int CLIENT_BAD_IDENTITY_INFO = 436;
    public static final int CLIENT_UNSUPPORTED_CERTIFICATE = 437;
    public static final int CLIENT_INVALID_IDENTITY_HEADER = 438;
    public static final int CLIENT_TEMPORARILY_UNAVAILABLE 	= 480;
    public static final int CLIENT_CALL_OR_TRANSACTION_DOES_NOT_EXIST = 481;
    public static final int CLIENT_LOOP_DETECTED         = 482;
    public static final int CLIENT_TOO_MANY_HOPS         = 483;
    public static final int CLIENT_ADDRESS_INCOMPLETE    = 484;
    public static final int CLIENT_AMBIGUOUS             = 485;
    public static final int CLIENT_BUSY_HERE             = 486;
    public static final int CLIENT_REQUEST_CANCELLED     = 487;
    public static final int CLIENT_REQUEST_TERMINATED		   = 487;//Alias
    public static final int CLIENT_NOT_ACCEPTABLE_HERE   = 488;
    public static final int CLIENT_BAD_EVENT = 489;
	
    public static final int CLIENT_REQUEST_PENDING = 491;
    public static final int CLIENT_UNDECIPHERABLE = 493;
    public static final int CLIENT_SECURITY_AGREEMENT_REQUIRED = 494;

    public static final int SERVER_INTERNAL_FAILURE        = 500 ;  
    public static final int SERVER_NOT_IMPLEMENTED          = 501;
    public static final int SERVER_BAD_GATEWAY              = 502;
    public static final int SERVER_SERVICE_UNAVAILABLE      = 503;
    public static final int SERVER_GATEWAY_TIMEOUT          = 504;
    public static final int SERVER_SIP_VERSION_NOT_SUPPORTED = 505;
    public static final int SC_MESSAGE_TOO_LARGE 			= 513;
    
    public static final int SERVER_PRECONDITION_FAILURE = 580;

    public static final int GLOBAL_BUSY_EVERYWHERE         = 600;  
    public static final int GLOBAL_DECLINE                 = 603;
    public static final int GLOBAL_DOES_NOT_EXIST_ANYWHERE = 604;
    public static final int GLOBAL_NOT_ACCEPTABLE         = 606;
        
    /**
     *  get the Text that represents the response code
     */
    public static final String getResponseCodeText( int responseCode )
    	throws NoSuchElementException
    	{
    		String retval;
    		
    		switch( responseCode )
    		{
                    case INFO_TRYING:
                        retval = "Trying";
                        break ;

                    case INFO_RINGING:
                        retval = "Ringing";
                        break ;
                    
                    case INFO_CALL_FORWARDING:
                        retval = "Call Is Being Forwarded";
                        break ;
                        
                    case INFO_QUEUED:
                        retval = "Queued" ;
                        break ;

                    case INFO_SESSION_PROGRESS:
                        retval = "Session Progress" ;
                        break ;

                    /* OK */
                        
                    case OK:
                        retval = "OK" ;
                        break ;
                        
                    case ACCEPTED:
                        retval = "ACCEPTED" ;
                        break ;
                        
                    /* Redirec codes */
                        
                    case REDIRECT_MULTIPLE_CHOICES:
                        retval = "Multiple Choices" ;
                        break ;

                    case REDIRECT_MOVED_PERMANENTLY:
                        retval = "Moved Permanently" ;
                        break ;                        
                
                    case REDIRECT_MOVED_TEMPORARILY:
                        retval = "Moved Temporarily" ;
                        break ; 
                        
                    case REDIRECT_USE_PROXY:
                        retval = "Use Proxy" ;
                        break ;
                        
                    case REDIRECT_ALTERNATIVE_SERVICE:
                        retval = "Alternative Service" ;
                        break ;
                        
                    /* Client codes */    
                        
                     case CLIENT_BAD_REQUEST:
                        retval = "Bad Request" ;
                        break ;
                        
                     case CLIENT_UNAUTHORIZED:
                        retval = "Unauthorized" ;
                        break ;
                        
                     case CLIENT_PAYMENT_REQUIRED:
                        retval = "Payment Required" ;
                        break ;
                        
                     case CLIENT_FORBIDDEN:
                        retval = "Forbidden" ;
                        break ;
                        
                     case CLIENT_NOT_FOUND:
                        retval = "Not Found" ;
                        break ;
                        
                     case CLIENT_METHOD_NOT_ALLOWED:
                        retval = "Method Not Allowed" ;
                        break ;
                        
                     case CLIENT_NOT_ACCEPTABLE:
                        retval = "Not Acceptable" ;
                        break ;
                        
                     case CLIENT_PROXY_AUTHENTICATION_REQUIRED:
                        retval = "Proxy Authentication Required" ;
                        break ;
                        
                     case CLIENT_REQUEST_TIMEOUT:
                        retval = "Request Timeout" ;
                        break ;
                        
                     case CLIENT_CONFLICT:
                        retval = "Conflict" ;
                        break ;
                        
                     case CLIENT_GONE:
                        retval = "Gone" ;
                        break ;
                        
                     case CLIENT_LENGTH_REQUIRED:
                        retval = "Length Required" ;
                        break ;
                        
                     case CLIENT_CONDITIONAL_REQUEST_FAILED:
                    	 retval = "Conditional Request Failed";
                    	 break;
                    	 
                     case CLIENT_REQUEST_ENTITY_TOO_LARGE:
                        retval = "Request Entity Too Large" ;
                        break ;
                        
                     case CLIENT_REQUEST_URI_TOO_LARGE:
                        retval = "Request-URI Too Long" ;
                        break ;
                        
                     case CLIENT_UNSUPPORTED_MEDIA_TYPE:
                        retval = "Unsupported Media Type" ;
                        break ;
                        
                     case CLIENT_UNSUPPORTED_URI_SCHEME:
                     	retval = "Unsupported URI Scheme";
                     	break;
                     	
                     case CLIENT_UNKNOWN_RESOURCE_PRIORITY:
                    	 retval = "Unknown Resource-Priority";
                    	 break;
                     
                     case CLIENT_BAD_EXTENSION:
                        retval = "Bad Extension" ;
                        break ;
                        
                     case CLIENT_SESSION_INTERVAL_TOO_SMALL:
                    	 retval = "Session Interval Too Small" ;
                    	 break ;
                        
                     case CLIENT_EXTENSION_REQUIRED:
                     	retval = "Extension Required";
                     	break;
                     
                     case CLIENT_INTERVAL_TOO_BRIEF:
                     	retval = "Interval Too Brief";
                     	break;
                     	
                     case CLIENT_BAD_LOCATION_INFORMATION:
                    	 retval = "Bad Location Information";
                    	 break;
                    	 
                     case CLIENT_USE_IDENTITY_HEADER:
                    	 retval = "Use Identity Header";
                    	 break;
                    	 
                     case CLIENT_PROVIDE_REFERER_IDENTITY:
                    	 retval = "Provide Referrer Identity";
                    	 break;
                    	 
                     case CLIENT_ANONYMILY_DISALLOWED:
                    	 retval = "Anonymity Disallowed";
                    	 break;
                    	 
                     case CLIENT_BAD_IDENTITY_INFO:
                    	 retval = "Bad Identity-Info";
                    	 break;
                    	 
                     case CLIENT_UNSUPPORTED_CERTIFICATE:
                    	 retval = "Unsupported Certificate";
                    	 break;
                    	 
                     case CLIENT_INVALID_IDENTITY_HEADER:
                    	 retval = "Invalid Identity Header";
                    	 break;
                    	 
                     case CLIENT_TEMPORARILY_UNAVAILABLE:
                        retval = "Temporarily Unavailable" ;
                        break ;
                        
                     case CLIENT_CALL_OR_TRANSACTION_DOES_NOT_EXIST:
                        retval = "Call/Transaction Does Not Exist" ;
                        break ;
                        
                     case CLIENT_LOOP_DETECTED:
                        retval = "Loop Detected" ;
                        break ;
                        
                     case CLIENT_TOO_MANY_HOPS:
                        retval = "Too Many Hops" ;
                        break ;
                        
                     case CLIENT_ADDRESS_INCOMPLETE:
                        retval = "Address Incomplete" ;
                        break ;
                        
                     case CLIENT_AMBIGUOUS:
                        retval = "Ambiguous" ;
                        break ;
                        
                     case CLIENT_BUSY_HERE:
                        retval = "Busy Here" ;
                        break ;
                        
                     case CLIENT_REQUEST_TERMINATED:
                        retval = "Request Terminated" ;
                        break ;
                        
                     case CLIENT_NOT_ACCEPTABLE_HERE:
                        retval = "Not Acceptable Here" ;
                        break ;
                     
                     case CLIENT_REQUEST_PENDING:
                    	 retval = "Request Pending";
                    	 break;
                    	 
                     case CLIENT_UNDECIPHERABLE:
                     	retval = "Undecipherable";
                     	break;
                     	
                     case CLIENT_SECURITY_AGREEMENT_REQUIRED:
                    	 retval = "Security Agreement Required";
                    	 break;
                    	 
                     case CLIENT_BAD_EVENT:
                    	 retval = "Bad Event";
                    	 break;
                        
                     /* Server codes */   
                        
                     case SERVER_INTERNAL_FAILURE:
                        retval = "Server Internal Error" ;
                        break ;
                        
                     case SERVER_NOT_IMPLEMENTED:
                        retval = "Not Implemented" ;
                        break ;
                        
                     case SERVER_BAD_GATEWAY:
                        retval = "Bad Gateway" ;
                        break ;
                        
                     case SERVER_SERVICE_UNAVAILABLE:
                        retval = "Service Unavailable" ;
                        break ;
                        
                      case SERVER_GATEWAY_TIMEOUT:
                        retval = "Server Time-out" ;
                        break ;
                        
                      case SERVER_SIP_VERSION_NOT_SUPPORTED:
                        retval = "Version Not Supported" ;
                        break ;

                      case SC_MESSAGE_TOO_LARGE:
                    	retval = "Message Too Large" ;
                    	break ;
                    	
                      case SERVER_PRECONDITION_FAILURE:
                    	  retval = "Precondition Failure";
                    	  break;

                    	  /* Global codes */  
                        
                      case GLOBAL_BUSY_EVERYWHERE:
                        retval = "Busy Everywhere" ;
                        break ;
                        
                      case GLOBAL_DECLINE:
                        retval = "Decline" ;
                        break ;
                         
                      case GLOBAL_DOES_NOT_EXIST_ANYWHERE:
                        retval = "Does Not Exist Anywhere" ;
                        break ;
                        
                      case GLOBAL_NOT_ACCEPTABLE:
                        retval = "Not Acceptable" ;
                        break ;
                        
					default:
						// unrecognized response codes treatment per 3261 8.1.3.2
						switch (responseCode / 100) {
						case 1:
							retval = "Session Progress";
							break;
						case 2:
							retval = "OK";
							break;
						case 3:
							retval = "Multiple Choices";
							break;
						case 4:
							retval = "Bad Request";
							break;
						case 5:
							retval = "Server Internal Error";
							break;
						case 6:
							retval = "Busy Everywhere";
							break;
						default:
							retval = "Bad Request" ;
						}
						break ;    					    		}
    		
    		return retval;
    			
    	} 
    	
		public static boolean isProvionalResponse( int reasonCode )
		{
			return reasonCode >= 100 && reasonCode < 200; 
		}
		
		public static boolean isRedirectResponse( int reasonCode )
		{
			return reasonCode >= 300 && reasonCode < 400; 
		}
	
		public static boolean isFinalResponse( int reasonCode )
		{
			return reasonCode >= 200 && reasonCode < 700; 
		}
	
		public static boolean isNonOkFinalResponse( int reasonCode )
		{
			return reasonCode >= 300 && reasonCode < 700; 
		}
	
		public static boolean isOKFinalResponse( int reasonCode )
		{
				return reasonCode >= 200 && reasonCode < 300; 
		}
		
		public static boolean isClientFailResponse( int reasonCode )
		{
				return reasonCode >= 400 && reasonCode < 400; 
		}    	
}
