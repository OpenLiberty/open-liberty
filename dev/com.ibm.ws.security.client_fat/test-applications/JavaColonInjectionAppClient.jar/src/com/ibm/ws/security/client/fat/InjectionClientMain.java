/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.client.fat;


import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.mail.MailSessionDefinition;
import javax.mail.MailSessionDefinitions;

import com.ibm.ws.security.client.fat.view.MySimpleInjectionBeanRemote;

@MailSessionDefinitions({
    @MailSessionDefinition(
            from="smtp@testserver.com", 
            user="imap@testserver.com", 
            password="imapPa$$word4U2C",
            host="localhost",
            name="java:module/env/myMailSession",
            storeProtocol="imap",
            description="module scoped mailSession",
            properties={"mail.imap.host=localhost", "mail.imap.port=7890", "scope=module"}),
    @MailSessionDefinition(
            from="smtp@testserver.com", 
            user="imap@testserver.com", 
            password="imapPa$$word4U2C",
            host="localhost",
            name="java:comp/env/myMailSession",
            storeProtocol="imap",
            description="comp scoped mailSession",
            properties={"mail.imap.host=localhost", "mail.imap.port=7890", "scope=comp"})
})
public class InjectionClientMain {

    private final static String NAME_EJB_GLOBAL = "java:global/JavaColonInjectionApp/JavaColonInjectionAppEJB/MySimpleInjectionBean!com.ibm.ws.security.client.fat.view.MySimpleInjectionBeanRemote";
    
    @Resource(lookup="java:global/env/globalEnvEntry")
    public static String injectedGlobalEnvEntry;
    
      
    @EJB(lookup=NAME_EJB_GLOBAL)
    public static MySimpleInjectionBeanRemote injectedGlobalEjb;
   
     
    /**
     * @param args arguments
     */
    public static void main(String[] args) {
        System.out.println("main - entry");
        // env entries:
        if ("I am global".equals(injectedGlobalEnvEntry)) System.out.println("injectGlobal_env-PASSED");
        
         
        // ejbs:
        if (checkEJB(injectedGlobalEjb)) System.out.println("injectGlobal_EJB-PASSED");

        System.out.println("main - exit");
    }
    
    private static boolean checkEJB(MySimpleInjectionBeanRemote ejb) {
        boolean b;
        try {
            b = ejb != null && ejb.add(4, 8) == 12;
        } catch (Exception ex) {
            ex.printStackTrace();
            b = false;
        }
        return b;
    }
  
 
}
