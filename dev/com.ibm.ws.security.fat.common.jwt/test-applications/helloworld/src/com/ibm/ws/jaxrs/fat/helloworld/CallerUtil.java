/*******************************************************************************
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jaxrs.fat.helloworld;

import java.io.StringReader;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import javax.security.auth.Subject;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.websphere.security.cred.WSCredential;
// import com.ibm.wsspi.security.token.AttributeNameConstants;
import com.ibm.wsspi.security.token.SingleSignonToken;

@SuppressWarnings("rawtypes")
public class CallerUtil {

    static Boolean debug = true;

    static public SOAPMessage invoke(SOAPMessage request, String strResponse, String strMsgID) {
        SOAPMessage response = null;
        String PrincipalUserID = "";
        try {
            System.out.println(strMsgID + " gets a client request");
            StringReader respMsg = new StringReader(strResponse);
            Source src = new StreamSource(respMsg);
            MessageFactory factory = MessageFactory.newInstance();
            response = factory.createMessage();
            response.getSOAPPart().setContent(src);
            response.saveChanges();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return response;
    }

    static public String getRealmName() {

        System.out.println("In getRealmName");

        String realm = "";
        try {

            WSCredential credential = getCredential();
            if (credential != null) {
                return credential.getRealmName();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Returning realm: " + realm);
        return realm;
    }

    static public String getAccessId() {
        System.out.println("In getAccessId");
        String accessId = null;
        try {
            WSCredential credential = getCredential();
            if (credential != null) {
                return credential.getAccessId();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Returning accessId: " + accessId);
        return accessId;
    }

    static public String getSecurityName() {
        System.out.println("In getSecurityName");
        String securityName = null;
        try {
            WSCredential credential = getCredential();
            if (credential != null) {
                return credential.getSecurityName();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Returning securityName: " + securityName);
        return securityName;
    }

    static public String getPrincipalUserID() {
        try {
            if (debug) {
                System.out.println("In getPrincipalUserID");
            }
            ;

            String caller_princ = WSSubject.getCallerPrincipal();
            Subject caller_subject = WSSubject.getRunAsSubject();

            System.out.println("WSSubject.getCallerPrincipal():" + caller_princ + ":" + caller_subject);
            if (caller_princ == null) {
                System.out.println("caller_princ IS null");
                Set caller_set = caller_subject.getPrincipals();
                Object[] objects = caller_set.toArray();
                if (objects.length > 0) {
                    if (objects[0] instanceof Principal) {
                        Principal principal = (Principal) objects[0];
                        caller_princ = principal.getName();
                    }

                } else {
                    caller_princ = objects[0].toString();
                }

            }
            if (debug) {
                System.out.println("Returning principal: " + caller_princ);
            }
            ;
            return caller_princ;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static public String getSubject() {
        try {

            Subject caller_subject = WSSubject.getRunAsSubject();
            System.out.println("WSSubject.getRunAsSubject():" + caller_subject);
            return caller_subject.toString();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String getGroups() {

        System.out.println("In getGroups");

        String grps = null;
        try {
            WSCredential credential = null;
            Subject subject = null;
            ArrayList groups = new ArrayList();
            try {
                subject = WSSubject.getRunAsSubject();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            if (subject != null) {
                java.util.Set publicCreds = subject.getPublicCredentials();

                if (publicCreds != null && publicCreds.size() > 0) {
                    java.util.Iterator publicCredIterator = publicCreds
                            .iterator();

                    while (publicCredIterator.hasNext()) {
                        Object cred = publicCredIterator.next();

                        if (cred != null && cred instanceof WSCredential) {
                            credential = (WSCredential) cred;
                        }
                    }
                }
            } else {
                System.out.println("subject is null");
            }

            if (credential != null) {
                groups = credential.getGroupIds();
                if (groups == null) {
                    System.out.println("Groups are null");
                } else {
                    for (Object g : groups) {
                        System.out.println("Group is: " + g.toString());
                    }
                }
            } else {
                System.out.println("credential is null");
            }
            if (groups != null && groups.size() > 0) {
                grps = groups.toString();
            } else {
                System.out.println("groups is null");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Returning groups: " + grps);
        return grps;
    }

    private static WSCredential getCredential() {

        WSCredential credential = null;
        try {
            Subject subject = null;
            try {
                subject = WSSubject.getRunAsSubject();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            if (subject != null) {
                java.util.Set publicCreds = subject.getPublicCredentials();

                if (publicCreds != null && publicCreds.size() > 0) {
                    java.util.Iterator publicCredIterator = publicCreds
                            .iterator();

                    while (publicCredIterator.hasNext()) {
                        Object cred = publicCredIterator.next();

                        if (cred != null && cred instanceof WSCredential) {
                            credential = (WSCredential) cred;
                        }
                    }
                }
            } else {
                System.out.println("subject is null");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return credential;
    }

    /**
     * Print the various programmatic API values we care about.
     *
     * @param req
     * @param writer
     */
    public static void printProgrammaticApiValues(StringBuffer sb) {

        try {
            // Get the CallerSubject
            Subject callerSubject = WSSubject.getCallerSubject();
            writeLine(sb, "callerSubject: " + callerSubject);

            // Get the public credential from the CallerSubject
            if (callerSubject != null) {
                WSCredential callerCredential = callerSubject.getPublicCredentials(WSCredential.class).iterator().next();
                if (callerCredential != null) {
                    writeLine(sb, "WSCredential SecurityName=" + callerCredential.getSecurityName());
                    writeLine(sb, "WSCredential RealmName=" + callerCredential.getRealmName());
                    writeLine(sb, "WSCredential RealmSecurityName=" + callerCredential.getRealmSecurityName());
                    writeLine(sb, "WSCredential UniqueSecurityName=" + callerCredential.getUniqueSecurityName());
                    writeLine(sb, "WSCredential groupIds=" + callerCredential.getGroupIds());
                    writeLine(sb, "WSCredential accessId=" + callerCredential.getAccessId());
                } else {
                    writeLine(sb, "WSCredential=null");
                }
            } else {
                writeLine(sb, "WSCredential=null");
            }

            //            String customCacheKey = null;

        } catch (NoClassDefFoundError ne) {
            // For OSGI App testing (EBA file), we expect this exception for all packages that are not public
            writeLine(sb, "NoClassDefFoundError for SubjectManager: " + ne);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * "Writes" the msg out to the client. This actually appends the msg
     * and a line delimiters to the running StringBuffer. This is necessary
     * because if too much data is written to the PrintWriter before the
     * logic is done, a flush() may get called and lock out changes to the
     * response.
     *
     * @param sb
     *            Running StringBuffer
     * @param msg
     *            Message to write
     */
    public static void writeLine(StringBuffer sb, String msg) {
        sb.append(msg + "\n");
    }

    /**
     * Gets the SSO token from the subject.
     *
     * @param subject
     *            {@code null} is not supported.
     * @return
     */
    private static SingleSignonToken getSSOToken(Subject subject) {
        SingleSignonToken ssoToken = null;
        Set<SingleSignonToken> ssoTokens = subject.getPrivateCredentials(SingleSignonToken.class);
        Iterator<SingleSignonToken> ssoTokensIterator = ssoTokens.iterator();
        if (ssoTokensIterator.hasNext()) {
            ssoToken = ssoTokensIterator.next();
        }
        return ssoToken;
    }

}
