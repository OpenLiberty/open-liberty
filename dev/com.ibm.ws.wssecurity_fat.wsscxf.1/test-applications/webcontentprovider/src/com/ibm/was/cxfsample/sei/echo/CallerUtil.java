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

package com.ibm.was.cxfsample.sei.echo;

import java.util.Set;

import com.ibm.websphere.security.auth.WSSubject;
import javax.security.auth.Subject;
import java.security.Principal;

public class CallerUtil {

    static public String getPrincipalUserID(){
        try{
            String caller_princ    =  WSSubject.getCallerPrincipal();
            Subject caller_subject =  WSSubject.getRunAsSubject();

            System.out.println("WSSubject.getCallerPrincipal():" + caller_princ + ":" + caller_subject);
            if( caller_princ == null ){
                Set caller_set = caller_subject.getPrincipals();
                Object[] objects = caller_set.toArray();
                if( objects.length > 0){
                    if( objects[0] instanceof Principal){
                        Principal principal = (Principal)objects[0];
                        caller_princ = principal.getName();
                    }
                } else{
                    caller_princ = objects[ 0].toString();                  
                }

            }
            return caller_princ;
        } catch(Exception e ){
            e.printStackTrace();
        }
        return null;
    }
}
