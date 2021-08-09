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


/**
 * A utility class providing additional support for
 * converged HTTP/SIP applications and converged J2EE/SIP applications.
 *
 * <p>This class can be accessed through the <code>ServletContext</code>
 * parameter named <code>javax.servlet.sip.SipSessionsUtil</code> or it can be injected
 * using the @Resource annotation.
 *
 * @since 1.1
 */
public interface SipSessionsUtil {
	
  /**
   * Returns the SipApplicationSession for a given applicationSessionId. 
   * The applicationSessionId String is the same as that obtained through 
   * SipApplicationSession.getId(). The method shall return the Application 
   * Session only if the queried application session belongs to the application 
   * from where this method is invoked. As an example if there exists a SIP 
   * Application with some Java EE component like a Message Driven Bean, bundled 
   * in the same application archive file (.war), then if the id of 
   * SipApplicationSession is known to the MDB it can get a reference to the 
   * SipApplicationSession object using this method. If this MDB were in a different 
   * application then it would not possible for it to access the SipApplicationSession. 
   * The method returns null in case the container does not find the SipApplicationSession 
   * instance matching the ID.
   *   
   * @param applicationSessionId - the SipApplicationSession's id 
   * 
   * @return SipApplicationSession object or a null if it is not found 
   * 
   * @throws java.lang.NullPointerException - if the applicationSessionId is null.
   * 
   * @see SipApplicationSession#getId()
   */
  public SipApplicationSession getApplicationSessionById(java.lang.String applicationSessionId);	
	
  /**
   * Returns the SipApplicationSession for a given session applicationSessionKey. 
   * The applicationSessionKey String is the same as that supplied to 
   * {@link SipFactory#createApplicationSessionByKey(String)}. The method shall return the Application 
   * Session only if the queried application session belongs to the application from where 
   * this method is invoked. The method returns null in case the container does not 
   * find the SipApplicationSession instance matching the applicationSessionKey.
   *  
   * @param applicationSessionKey - session applicationSessionKey of the SipApplicationSession
   * @param create - controls whether new session should be created upon lookup failure 
   * 
   * @throws NullPointerException - if the applicationSessionKey is null.
   * 
   * @return SipApplicationSession object or a null if it is not found and create 
   * 		 is set to false. If create  is true, create a new SipApplicationSession 
   * 		 with the given applicationSessionKey
   */
  public SipApplicationSession getApplicationSessionByKey(String applicationSessionKey,
          boolean create) throws NullPointerException;
  
  /**
   * Returns related SipSession. This method is helpful when the application code 
   * wants to carry out session join or replacement as described by RFC 3911 and 
   * RFC 3891 respectively.
   * 
   * The association is made implicitly by the container implementation. An example 
   * is shown below.
   *
   * @Resource
   * SipSessionsUtil sipSessionsUtil;
   * protected void doInvite(SipServletRequest req) {
   *    SipSession joining = req.getSession(true);
   *    SipSession beingJoined = sipSessionsUtil.getCorrespondingSipSession(
   *	                            joining,"Join");
   *	   [...]
   *	 }
   *
   * @param session - one of the two related SIP sessions. For example, it can be the 
   * 				  joining session or the replacing session.
   * @param headerName - the header name through which the association is made. 
   * 					For example, for RFC 3911, it is Join, for RFC 3891, it is Replaces
   * @return SipSession related to the supplied session. For RFC 3911, 
   *         if joining session is passed in, the session being joined is returned. 
   *         For RFC 3891, if the replacing session is passed in, the session being 
   *         replaced is returned. If none is found, this method returns null.
   */
  SipSession getCorrespondingSipSession(SipSession session,
          java.lang.String headerName);

}
