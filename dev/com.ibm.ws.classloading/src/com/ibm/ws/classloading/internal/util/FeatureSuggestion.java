/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.internal.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.kernel.service.util.JavaInfo;

public class FeatureSuggestion {
    
    private static final TraceComponent tc = Tr.register(FeatureSuggestion.class);
    
    private static final String FEATURE_JAXB = "jaxb-2.2";
    private static final String FEAUTRE_CORBA = "corba-2.4";
    private static final String FEATURE_JDBC = "jdbc-4.0";
    private static final String FEAUTRE_JAXWS = "jaxws-2.2";
    
    private static final Map<String, String> pkgToFeature = new HashMap<>();
    private static final Set<String> suggestedFeatures = new HashSet<>();
    
    static {
        // Packages from java.activation
        pkgToFeature.put("javax.activation", FEATURE_JAXB);

        // Packages from java.corba module
        pkgToFeature.put("javax.activity", FEAUTRE_CORBA);
        pkgToFeature.put("javax.rmi", FEAUTRE_CORBA);
        pkgToFeature.put("javax.rmi.CORBA", FEAUTRE_CORBA);
        pkgToFeature.put("org.omg.CORBA", FEAUTRE_CORBA);
        pkgToFeature.put("org.omg.CORBA_2_3", FEAUTRE_CORBA);
        pkgToFeature.put("org.omg.CORBA_2_3.portable", FEAUTRE_CORBA);
        pkgToFeature.put("org.omg.CORBA.DynAnyPackage", FEAUTRE_CORBA);
        pkgToFeature.put("org.omg.CORBA.ORBPackage", FEAUTRE_CORBA);
        pkgToFeature.put("org.omg.CORBA.portable", FEAUTRE_CORBA);
        pkgToFeature.put("org.omg.CORBA.TypeCodePackage", FEAUTRE_CORBA);
        pkgToFeature.put("org.omg.CosNaming", FEAUTRE_CORBA);
        pkgToFeature.put("org.omg.CosNaming.NamingContextExtPackage", FEAUTRE_CORBA);
        pkgToFeature.put("org.omg.CosNaming.NamingContextPackage", FEAUTRE_CORBA);
        pkgToFeature.put("org.omg.Dynamic", FEAUTRE_CORBA);
        pkgToFeature.put("org.omg.DynamicAny", FEAUTRE_CORBA);
        pkgToFeature.put("org.omg.DynamicAny.DynAnyFactoryPackage", FEAUTRE_CORBA);
        pkgToFeature.put("org.omg.DynamicAny.DynAnyPackage", FEAUTRE_CORBA);
        pkgToFeature.put("org.omg.IOP", FEAUTRE_CORBA);
        pkgToFeature.put("org.omg.IOP.CodecFactoryPackage", FEAUTRE_CORBA);
        pkgToFeature.put("org.omg.IOP.CodecPackage", FEAUTRE_CORBA);
        pkgToFeature.put("org.omg.Messaging", FEAUTRE_CORBA);
        pkgToFeature.put("org.omg.PortableInterceptor", FEAUTRE_CORBA);
        pkgToFeature.put("org.omg.PortableInterceptor.ORBInitInfoPackage", FEAUTRE_CORBA);
        pkgToFeature.put("org.omg.PortableServer", FEAUTRE_CORBA);
        pkgToFeature.put("org.omg.PortableServer.CurrentPackage", FEAUTRE_CORBA);
        pkgToFeature.put("org.omg.PortableServer.POAManagerPackage", FEAUTRE_CORBA);
        pkgToFeature.put("org.omg.PortableServer.POAPackage", FEAUTRE_CORBA);
        pkgToFeature.put("org.omg.PortableServer.portable", FEAUTRE_CORBA);
        pkgToFeature.put("org.omg.PortableServer.ServantLocatorPackage", FEAUTRE_CORBA);
        pkgToFeature.put("org.omg.SendingContext", FEAUTRE_CORBA);
        pkgToFeature.put("org.omg.stub.java.rmi", FEAUTRE_CORBA);

        // Packages from java.transaction module
        pkgToFeature.put("javax.transaction", FEATURE_JDBC);

        // Packages from java.xml.bind module
        pkgToFeature.put("javax.xml.bind", FEATURE_JAXB);
        pkgToFeature.put("javax.xml.bind.annotation", FEATURE_JAXB);
        pkgToFeature.put("javax.xml.bind.annotation.adapters", FEATURE_JAXB);
        pkgToFeature.put("javax.xml.bind.attachment", FEATURE_JAXB);
        pkgToFeature.put("javax.xml.bind.helpers", FEATURE_JAXB);
        pkgToFeature.put("javax.xml.bind.util", FEATURE_JAXB);

        // Packages from java.xml.ws module
        pkgToFeature.put("javax.jws", FEAUTRE_JAXWS);
        pkgToFeature.put("javax.jws.soap", FEAUTRE_JAXWS);
        pkgToFeature.put("javax.xml.soap", FEAUTRE_JAXWS);
        pkgToFeature.put("javax.xml.ws", FEAUTRE_JAXWS);
        pkgToFeature.put("javax.xml.ws.handler", FEAUTRE_JAXWS);
        pkgToFeature.put("javax.xml.ws.handler.soap", FEAUTRE_JAXWS);
        pkgToFeature.put("javax.xml.ws.http", FEAUTRE_JAXWS);
        pkgToFeature.put("javax.xml.ws.soap", FEAUTRE_JAXWS);
        pkgToFeature.put("javax.xml.ws.spi", FEAUTRE_JAXWS);
        pkgToFeature.put("javax.xml.ws.spi.http", FEAUTRE_JAXWS);
        pkgToFeature.put("javax.xml.ws.wsaddressing", FEAUTRE_JAXWS);
    }
    
    public static ClassNotFoundException getExceptionWithSuggestion(ClassNotFoundException original) {
        String suggestedFeature = getFeatureSuggestion(original.getMessage());
        if (suggestedFeature == null)
            return original;

        String warnMsg = Tr.formatMessage(tc, "cls.classloader.suggested.feature", original.getMessage(), suggestedFeature);
        ClassNotFoundException toThrow = new ClassNotFoundException(warnMsg, original);
        if (!suggestedFeatures.contains(suggestedFeature)) {
            suggestedFeatures.add(suggestedFeature);
            // TODO: Temporarily disable the FFDC and downgrade to INFO message
            Tr.info(tc, warnMsg);
            //FFDCFilter.processException(toThrow, "com.ibm.ws.classloading.internal.util.FeatureSuggestion", "106");
        }
        return toThrow;
    }

    private static String getFeatureSuggestion(String name) {
        // Currently feature suggestions only apply to Java SE APIs removed in JDK 9
        if (JavaInfo.majorVersion() < 9)
            return null;
        
        if (name == null)
            return null;
        
        if (!name.startsWith("javax.") &&
            !name.startsWith("org.omg."))
            return null;
        
        // Weld will try to load javax.xml.ws.WebServiceRef to detect if JAX-WS API is available,
        // so don't issue a feature suggestion for this specific class
        // TODO: Figure out a more generic solution for internal components that do try-loads using the AppCL
        if (name.equals("javax.xml.ws.WebServiceRef"))
            return null;

        String pkg = name.substring(0, name.lastIndexOf('.'));
        return pkgToFeature.get(pkg);
    }

}
