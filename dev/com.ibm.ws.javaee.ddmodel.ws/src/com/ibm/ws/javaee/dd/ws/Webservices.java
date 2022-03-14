/*******************************************************************************
 * Copyright (c) 2012, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.dd.ws;

import java.util.List;

import com.ibm.ws.javaee.dd.DeploymentDescriptor;
import com.ibm.ws.javaee.dd.common.DescriptionGroup;

public interface Webservices extends DeploymentDescriptor, DescriptionGroup {
    String DD_SHORT_NAME = "webservices.xml";

    String WEB_DD_NAME = "WEB-INF/webservices.xml";
    String EJB_DD_NAME = "META-INF/webservices.xml";

    String[] DD_NAMES = { WEB_DD_NAME, EJB_DD_NAME };

    int VERSION_1_1 = 11; // J2EE 1.4
    int VERSION_1_2 = 12; // Java EE 5
    int VERSION_1_3 = 13; // Java EE 6
    int VERSION_1_4 = 14; // Java EE 7, Java EE 8
    int VERSION_2_0 = 20; // Jakarta 9
    // No Jakarta 10 yet

    int[] VERSIONS = {
            VERSION_1_1, VERSION_1_2, VERSION_1_3, VERSION_1_4,
            VERSION_2_0
    };
    
    String VERSION_1_1_STR = "1.1";    
    String VERSION_1_2_STR = "1.2";
    String VERSION_1_3_STR = "1.3";
    String VERSION_1_4_STR = "1.4";
    String VERSION_2_0_STR = "2.0";

    //

    String getVersion();

    List<WebserviceDescription> getWebServiceDescriptions();
    
    //
    
    // http://www.oracle.com/webfolder/technetwork/jsc/xml/ns/j2ee/j2ee_web_services_1_1.xsd
    //
    // <webservices xmlns="http://java.sun.com/xml/ns/j2ee"
    //              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    //              xsi:schemaLocation="http://java.sun.com/xml/ns/j2ee
    //                  http://www.ibm.com/webservices/xsd/j2ee_web_services_1_1.xsd"
    //              version="1.1">
    
    // http://www.oracle.com/webfolder/technetwork/jsc/xml/ns/javaee/javaee_web_services_1_2.xsd
    //
    // <webservices xmlns="http://java.sun.com/xml/ns/javaee"
    //              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    //              xsi:schemaLocation="http://java.sun.com/xml/ns/javaee
    //                  http://java.sun.com/xml/ns/javaee/javaee_web_services_1_2.xsd"
    //              version="1.2">
    
    // http://www.oracle.com/webfolder/technetwork/jsc/xml/ns/javaee/javaee_web_services_1_3.xsd
    //
    // <webservices xmlns="http://java.sun.com/xml/ns/javaee"
    //              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    //              xsi:schemaLocation="http://java.sun.com/xml/ns/javaee
    //                  http://java.sun.com/xml/ns/javaee/javaee_web_services_1_3.xsd"
    //              version="1.3">

    // http://www.oracle.com/webfolder/technetwork/jsc/xml/ns/javaee/javaee_web_services_1_4.xsd
    //
    // <webservices xmlns="http://xmlns.jcp.org/xml/ns/javaee"
    //              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    //              xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee
    //                  http://xmlns.jcp.org/xml/ns/javaee/javaee_web_services_1_4.xsd"
    //              version="1.4">
    
    // https://jakarta.ee/xml/ns/jakartaee/jakartaee_web_services_2_0.xsd
    //
    // <webservices xmlns="https://jakarta.ee/xml/ns/jakartaee"
    //              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    //              xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee
    //                  https://jakarta.ee/xml/ns/jakartaee/jakartaee_web_services_2_0.xsd"
    //              version="2.0">        
}
