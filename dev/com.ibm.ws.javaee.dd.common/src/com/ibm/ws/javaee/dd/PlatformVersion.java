/*******************************************************************************
 * Copyright (c) 2014, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.dd;

public interface PlatformVersion {
    // 1.2
    // 1.3
    // 1.4, http://java.sun.com/xml/ns/j2ee
    // 5,   http://java.sun.com/xml/ns/javaee
    // 6,   http://java.sun.com/xml/ns/javaee
    // 7,   http://xmlns.jcp.org/xml/ns/javaee
    // 8,   http://xmlns.jcp.org/xml/ns/javaee  // last javax
    // 9,   https://jakarta.ee/xml/ns/jakartaee // jakarta EE 9
    // 10,  https://jakarta.ee/xml/ns/jakartaee // jakarta EE 10
    // 11,  https://jakarta.ee/xml/ns/jakartaee // jakarta EE 11

    String NAMESPACE_SUN_J2EE   = "http://java.sun.com/xml/ns/j2ee";
    String NAMESPACE_SUN_JAVAEE = "http://java.sun.com/xml/ns/javaee";
    String NAMESPACE_JCP_JAVAEE = "http://xmlns.jcp.org/xml/ns/javaee";
    String NAMESPACE_JAKARTA    = "https://jakarta.ee/xml/ns/jakartaee";

    String VERSION_1_2_STR  =  "1.2";
    String VERSION_1_3_STR  =  "1.3";
    String VERSION_1_4_STR  =  "1.4";
    String VERSION_5_0_STR  =  "5.0";
    String VERSION_6_0_STR  =  "6.0";
    String VERSION_7_STR    =  "7";
    String VERSION_7_0_STR  =  "7.0";
    String VERSION_8_0_STR  =  "8.0";
    String VERSION_9_STR    =  "9";
    String VERSION_9_0_STR  =  "9.0";
    String VERSION_10_STR   = "10";
    String VERSION_10_0_STR = "10.0";
    String VERSION_11_STR   = "11";
    String VERSION_11_0_STR = "11.0";

    int VERSION_1_2_INT  =  12;
    int VERSION_1_3_INT  =  13;
    int VERSION_1_4_INT  =  14;
    int VERSION_5_0_INT  =  50;
    int VERSION_6_0_INT  =  60;
    int VERSION_7_0_INT  =  70;
    int VERSION_8_0_INT  =  80;
    int VERSION_9_0_INT  =  90;
    int VERSION_10_0_INT = 100;
    int VERSION_11_0_INT = 110;
    
    //
    
    public static String getDottedVersionText(int version) {
        switch ( version ) {
            case 0:   return null;
            case 9:   return "0.9";
            case 10:  return "1.0";
            case 11:  return "1.1";
            case 12:  return "1.2";
            case 13:  return "1.3";
            case 14:  return "1.4";
            case 15:  return "1.5";
            case 16:  return "1.6";
            case 17:  return "1.7";
            case 20:  return "2.0";
            case 21:  return "2.1";
            case 22:  return "2.2";
            case 23:  return "2.3";
            case 24:  return "2.4";
            case 25:  return "2.5";
            case 30:  return "3.0";
            case 31:  return "3.1";
            case 32:  return "3.2";
            case 40:  return "4.0";
            case 50:  return "5.0";
            case 60:  return "6.0";
            case 61:  return "6.1";
            case 70:  return "7.0";
            case 80:  return "8.0";
            case 90:  return "9.0";
            case 100: return "10.0";
            case 110: return "11.0";
            default:  throw new IllegalArgumentException("Unknown schema version [ " + version + " ]");
        }
    }

    public static String getVersionText(int version) {
        switch ( version ) {
            case 0:   return null;
            case 11:  return "1.1";
            case 12:  return "1.2";
            case 13:  return "1.3";
            case 14:  return "1.4";
            case 15:  return "1.5";
            case 16:  return "1.6";
            case 17:  return "1.7";
            case 20:  return "2.0";
            case 21:  return "2.1";
            case 22:  return "2.2";
            case 23:  return "2.3";
            case 24:  return "2.4";
            case 25:  return "2.5";
            case 30:  return "3.0";
            case 31:  return "3.1";
            case 32:  return "3.2";
            case 40:  return "4";
            case 50:  return "5";
            case 60:  return "6";
            case 61:  return "6.1";
            case 70:  return "7";
            case 80:  return "8";
            case 90:  return "9";
            case 100: return "10";
            case 110: return "11";
            default:  throw new IllegalArgumentException("Unknown schema version [ " + version + " ]");
        }
    }
    
    public static int getVersionInt(String version) {
        switch ( version ) {
            case "0.9" : return 9 ;
            case "1.0" : return 10 ;
            case "1.1" : return 11 ;
            case "1.2" : return 12 ;
            case "1.3" : return 13 ;
            case "1.4" : return 14 ;
            case "1.5" : return 15 ;
            case "1.6" : return 16 ;
            case "1.7" : return 17 ;
            case "2.0" : return 20 ;
            case "2.1" : return 21 ;
            case "2.2" : return 22 ;
            case "2.3" : return 23 ;
            case "2.4" : return 24 ;
            case "2.5" : return 25 ;
            case "3.0" : return 30 ;
            case "3.1" : return 31 ;
            case "3.2" : return 32 ;
            case "4.0" : return 40 ;
            case "5.0" : return 50 ;
            case "6.0" : return 60 ;
            case "6.1" : return 61 ;
            case "7.0" : return 70 ;
            case "8.0" : return 80 ;
            case "9.0" : return 90 ;
            case "10.0": return 100 ;
            case "11.0": return 110 ;
            default:  throw new IllegalArgumentException("Unknown schema version [ " + version + " ]");
            
        }
    }
}
