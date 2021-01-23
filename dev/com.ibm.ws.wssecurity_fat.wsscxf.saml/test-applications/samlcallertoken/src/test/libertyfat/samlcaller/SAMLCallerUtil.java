/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package test.libertyfat.caller;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.MessageFactory;
import java.io.StringReader;
import java.util.ArrayList;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
//import com.ibm.wsspi.security.token.AttributeNameConstants;

import javax.security.auth.Subject;
import java.security.Principal;
import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.cred.WSCredential;

public class SAMLCallerUtil {

    static Boolean debug = true ;
    
    static public SOAPMessage invoke(SOAPMessage request, String strResponse, String strMsgID) {
        SOAPMessage response = null;
        String PrincipalUserID = "";
        try {
            System.out.println( strMsgID + " gets a client request");
            //SOAPBody sb = request.getSOAPBody();
            //System.out.println("Incoming SOAPBody: " + sb);
            StringReader respMsg = new StringReader( strResponse );
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
            WSCredential credential = getCredential() ;
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
        } catch(Exception e ){
            e.printStackTrace();
        }
        System.out.println("Returning realm: " + realm);
        return realm;
    }

    static public String getPrincipalUserID(){
        try{
            if (debug) {System.out.println("In getPrincipalUserID"); } ;
            
            String caller_princ    =  WSSubject.getCallerPrincipal();
            Subject caller_subject =  WSSubject.getRunAsSubject();
         
            System.out.println("WSSubject.getCallerPrincipal():" + caller_princ + ":" + caller_subject);
            if( caller_princ == null ){
                System.out.println("caller_princ IS null");
                Set caller_set = caller_subject.getPrincipals();
                Object[] objects = caller_set.toArray();
                if( objects.length > 0){
                	if( objects[0] instanceof Principal){
                		Principal principal = (Principal)objects[0];
                		caller_princ = principal.getName();
                	}
                	
                } else{
                	caller_princ = objects[0].toString();                	
                }

            }
            if (debug) {System.out.println("Returning principal: " + caller_princ) ;} ;
            return caller_princ;
        } catch(Exception e ){
            e.printStackTrace();
        }
        return null;
    }
    
    static public String getSubject(){
        try{
        
            Subject caller_subject = WSSubject.getRunAsSubject();
            System.out.println("WSSubject.getRunAsSubject():" + caller_subject);
            return caller_subject.toString();
            
        } catch(Exception e ){
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
        return credential ;
    }

}
