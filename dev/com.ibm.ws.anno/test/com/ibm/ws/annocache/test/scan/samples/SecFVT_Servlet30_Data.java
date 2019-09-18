/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.annocache.test.scan.samples;

import com.ibm.ws.annocache.classsource.specification.internal.ClassSourceImpl_Specification_Direct_EJB;
import com.ibm.ws.annocache.classsource.specification.internal.ClassSourceImpl_Specification_Direct_WAR;
import com.ibm.ws.annocache.test.scan.Test_Base;
import com.ibm.ws.annocache.test.utils.TestLocalization;
import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl_Factory;

public class SecFVT_Servlet30_Data {
    public static final String EAR_NAME = "secfvt_servlet30.ear";
    public static final String EAR_SIMPLE_NAME = "secfvt_servlet30";

    public static final String WAR_ANNMIXED_NAME = "Servlet30AnnMixed.war";
    public static final String WAR_ANNMIXED_SIMPLE_NAME = "Servlet30AnnMixed";
    public static final String WAR_ANNPURE_NAME = "Servlet30AnnPure.war";
    public static final String WAR_ANNPURE_SIMPLE_NAME = "Servlet30AnnPure";
    public static final String WAR_ANNWEBXML_NAME = "Servlet30AnnWebXML.war";
    public static final String WAR_ANNWEBXML_SIMPLE_NAME = "Servlet30AnnWebXML";
    public static final String WAR_DYNCONFLICT_NAME = "Servlet30DynConflict.war";
    public static final String WAR_DYNCONFLICT_SIMPLE_NAME = "Servlet30DynConflict";
    public static final String WAR_DYNPURE_NAME = "Servlet30DynPure.war";
    public static final String WAR_DYNPURE_SIMPLE_NAME = "Servlet30DynPure";
    public static final String WAR_INHERIT_NAME = "Servlet30Inherit.war";
    public static final String WAR_INHERIT_SIMPLE_NAME = "Servlet30Inherit";
    public static final String WAR_API_NAME = "Servlet30api.war";
    public static final String WAR_API_SIMPLE_NAME = "Servlet30api";
    public static final String WAR_APIFL_NAME = "Servlet30apiFL.war";
    public static final String WAR_APIFL_SIMPLE_NAME = "Servlet30apiFL";

    public static final String EJBJAR_NAME = "SecFVTS1EJB.jar";
    public static final String EJBJAR_SIMPLE_NAME = "SecFVTS1EJB";

    public static ClassSourceImpl_Specification_Direct_WAR createClassSourceSpecification_WAR(
        ClassSourceImpl_Factory classSourceFactory,
        String warSimpleName, String warName) {

        ClassSourceImpl_Specification_Direct_WAR warSpecification =
            classSourceFactory.newWARDirectSpecification(EAR_SIMPLE_NAME, warSimpleName, Test_Base.JAVAEE_MOD_CATEGORY_NAME);

        warSpecification.setModulePath(TestLocalization.putIntoData(EAR_NAME + '/', warName) + '/');

        warSpecification.setRootClassLoader( SecFVT_Servlet30_Data.class.getClassLoader() );

        return warSpecification;
    }

    public static ClassSourceImpl_Specification_Direct_EJB createClassSourceSpecification_EJBJar(
    	ClassSourceImpl_Factory classSourceFactory) {

        ClassSourceImpl_Specification_Direct_EJB ejbSpecification =
            classSourceFactory.newEJBDirectSpecification(EAR_SIMPLE_NAME, EJBJAR_SIMPLE_NAME, Test_Base.JAVAEE_MOD_CATEGORY_NAME);

        ejbSpecification.setModulePath(TestLocalization.putIntoData(EAR_NAME + '/', EJBJAR_NAME) + '/');

        ejbSpecification.setRootClassLoader( SecFVT_Servlet30_Data.class.getClassLoader() );

        return ejbSpecification;
    }
}
