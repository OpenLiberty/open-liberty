/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
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

import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.websphere.security.cred.WSCredential;
// import com.ibm.wsspi.security.token.AttributeNameConstants;
import com.ibm.wsspi.security.token.SingleSignonToken;

public class CallerUtil {

    static Boolean debug = true;

    static public SOAPMessage invoke(SOAPMessage request, String strResponse, String strMsgID) {
        SOAPMessage response = null;
        String PrincipalUserID = "";
        try {
            System.out.println(strMsgID + " gets a client request");
            //SOAPBody sb = request.getSOAPBody();
            //System.out.println("Incoming SOAPBody: " + sb);
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

            //            Subject sub = WSSubject.getRunAsSubject();
            //            java.util.Set publicCreds = sub.getPublicCredentials();
            //            if (publicCreds != null) {
            //
            //            }
            WSCredential credential = getCredential();
            if (credential != null) {
                return credential.getRealmName();
            }
            //            credential.getRealmName() ;
            //
            //            String[] properties = { /*AttributeNameConstants.WSCREDENTIAL_REALM*/"com.ibm.wsspi.security.cred.realm" };
            //            SubjectHelper subjectHelper = new SubjectHelper();
            //
            //            Hashtable<String, ?> customProperties = subjectHelper.getHashtableFromSubject(sub, properties);
            //            if (customProperties != null) {
            //                realm = (String) customProperties.get("com.ibm.wsspi.security.cred.realm");
            //                System.out.println("Realm from the subject : " + realm + ":" + sub);;
            //            }
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
                //subject = WSSubject.getCallerSubject();
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
                //                        grps = new String[groups.size()];
                //                        groups.toArray(grps);
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
        //        writeLine(sb, "getAuthType: " + req.getAuthType());
        //        writeLine(sb, "getRemoteUser: " + req.getRemoteUser());
        //        writeLine(sb, "getUserPrincipal: " + req.getUserPrincipal());
        //
        //        if (req.getUserPrincipal() != null) {
        //            writeLine(sb, "getUserPrincipal().getName(): "
        //                          + req.getUserPrincipal().getName());
        //        }
        //        writeLine(sb, "isUserInRole(Employee): "
        //                      + req.isUserInRole("Employee"));
        //        writeLine(sb, "isUserInRole(Manager): " + req.isUserInRole("Manager"));
        //        String role = req.getParameter("role");
        //        if (role == null) {
        //            writeLine(sb, "You can customize the isUserInRole call with the follow paramter: ?role=name");
        //        }
        //        writeLine(sb, "isUserInRole(" + role + "): " + req.isUserInRole(role));
        //
        //        Cookie[] cookies = req.getCookies();
        //        writeLine(sb, "Getting cookies");
        //        if (cookies != null && cookies.length > 0) {
        //            for (int i = 0; i < cookies.length; i++) {
        //                writeLine(sb, "cookie: " + cookies[i].getName() + " value: "
        //                              + cookies[i].getValue());
        //            }
        //        }
        //        writeLine(sb, "getRequestURL: " + req.getRequestURL().toString());

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

            // getInvocationSubject for RunAs tests
            //            Subject runAsSubject = WSSubject.getRunAsSubject();
            //            writeLine(sb, "RunAs subject: " + runAsSubject);

            // Check for cache key for hashtable login test. Will return null otherwise
            String customCacheKey = null;
            //            if (callerSubject != null) {
            //                String[] properties = { AttributeNameConstants.WSCREDENTIAL_CACHE_KEY };
            //                SubjectHelper subjectHelper = new SubjectHelper();
            //                Hashtable<String, ?> customProperties = subjectHelper.getHashtableFromSubject(callerSubject, properties);
            //                if (customProperties != null) {
            //                    customCacheKey = (String) customProperties.get(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY);
            //                }
            //                if (customCacheKey == null) {
            //                    SingleSignonToken ssoToken = getSSOToken(callerSubject);
            //                    if (ssoToken != null) {
            //                        String[] attrs = ssoToken.getAttributes(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY);
            //                        if (attrs != null && attrs.length > 0) {
            //                            customCacheKey = attrs[0];
            //                        }
            //                    }
            //                }
            //            }
            //            writeLine(sb, "customCacheKey: " + customCacheKey);

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
        sb.append(msg + System.getProperty("line.separator"));
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

    public static String getAccessTokenFromSubject() {
        Subject callerSubject;
        Set<Object> privateCreds = null;
        try {
            callerSubject = WSSubject.getCallerSubject();
            privateCreds = callerSubject.getPrivateCredentials();
        } catch (WSSecurityException e1) {
            e1.printStackTrace();
            return null;
        }
        String access_token = null;
        for (Object privateCred : privateCreds) {
            try {
                String fString = privateCred.toString();
                if (fString != null && fString.contains("access_token")) {
                    int start = fString.indexOf("{");
                    int end = fString.lastIndexOf("}");
                    String[] parts = fString.substring(start + 1, end).split(",");
                    for (String part : parts) {
                        if (part != null && part.contains("access_token")) {
                            access_token = part.trim().substring(13);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return access_token;

    }
}
