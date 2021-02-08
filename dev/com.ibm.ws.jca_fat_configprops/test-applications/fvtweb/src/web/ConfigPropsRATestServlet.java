/*******************************************************************************
 * Copyright (c) 2012, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import java.util.Map;

import javax.naming.InitialContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import componenttest.app.FATServlet;

public class ConfigPropsRATestServlet extends FATServlet {
    private static final long serialVersionUID = 7709282314904580334L;

    /**
     * Config properties defined in the ManagedConnectionFactory @ConfigProperty annotation override config properties from the ResourceAdapter entry in wlp-ra.xml
     *
     * @param request  HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testMCFAnnotationOverridesRAWLPExtension(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        @SuppressWarnings("unchecked")
        Map<String, Object> map1 = (Map<String, Object>) new InitialContext().lookup("eis/map1");
        Object value;

        // We don't currently support primitives.
//        value = map1.get("propInt1_Anno");
//        if (!Integer.valueOf(111).equals(value))
//            throw new Exception("unexpected value for propInt1_Anno: " + value);

        value = map1.get("propInteger2_Anno");
        if (!Integer.valueOf(222).equals(value))
            throw new Exception("unexpected value for propInteger2_Anno: " + value);

        value = map1.get("propString_Anno");
        if (!"MCF Annotation Default Value".equals(value))
            throw new Exception("unexpected value for propString_Anno: " + value);
    }

    /**
     * Config properties defined in the ManagedConnectionFactory deployment descriptor override config properties defined on the ManagedConnectionFactory @ConfigProperty annotation
     *
     * @param request  HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testMCFDDOverridesMCFAnnotation(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        @SuppressWarnings("unchecked")
        Map<String, Object> map1 = (Map<String, Object>) new InitialContext().lookup("eis/map1");
        Object value;

        // We don't currently support primitives.
//        value = map1.get("propInt1_DD");
//        if (!Integer.valueOf(1111).equals(value))
//            throw new Exception("unexpected value for propInt1_DD: " + value);

        value = map1.get("propInteger2_DD");
        if (!Integer.valueOf(2222).equals(value))
            throw new Exception("unexpected value for propInteger2_DD: " + value);

        value = map1.get("propString_DD");
        if (!"MCF Deployment Descriptor Default Value".equals(value))
            throw new Exception("unexpected value for propString_DD: " + value);
    }

    /**
     * The config properties of the MCF JavaBean only, that are not defined anywhere else
     *
     * @param request  HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testMCFJavaBean(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        @SuppressWarnings("unchecked")
        Map<String, Object> map1 = (Map<String, Object>) new InitialContext().lookup("eis/map1");
        Object value;

        // We don't currently support primitives.
//        value = map1.get("propInt1");
//        if (!Integer.valueOf(11).equals(value))
//            throw new Exception("unexpected value for propInt1: " + value);

        value = map1.get("propInteger2");
        if (!Integer.valueOf(22).equals(value))
            throw new Exception("unexpected value for propInteger2: " + value);

        value = map1.get("propString");
        if (!"MCF JavaBean Default Value".equals(value))
            throw new Exception("unexpected value for propString: " + value);
    }

    /**
     * Config properties defined on the ResourceAdapter @ConfigProperty annotation override config properties of the ResourceAdapter JavaBean
     *
     * @param request  HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testRAAnnotationOverridesRAJavaBean(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        @SuppressWarnings("unchecked")
        Map<String, Object> map1 = (Map<String, Object>) new InitialContext().lookup("eis/map1");
        Object value;

        // We don't currently support primitives.
//        value = map1.get("propInt1RA_Anno");
//        if (!Integer.valueOf(100).equals(value))
//            throw new Exception("unexpected value for propInt1RA_Anno: " + value);

        value = map1.get("propInteger2RA_Anno");
        if (!Integer.valueOf(200).equals(value))
            throw new Exception("unexpected value for propInteger2RA_Anno: " + value);

        value = map1.get("propStringRA_Anno");
        if (!"RA Annotation Default Value".equals(value))
            throw new Exception("unexpected value for propStringRA_Anno: " + value);
    }

    /**
     * Config properties defined in the ResourceAdapter deployment descriptor entry override config properties of the ResourceAdapter @ConfigProperty annotation
     *
     * @param request  HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testRADeploymentDescriptorOverridesRAAnnotation(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        @SuppressWarnings("unchecked")
        Map<String, Object> map1 = (Map<String, Object>) new InitialContext().lookup("eis/map1");
        Object value;

        // We don't currently support primitives.
//        value = map1.get("propInt1RA_DD");
//        if (!Integer.valueOf(1000).equals(value))
//            throw new Exception("unexpected value for propInt1RA_DD: " + value);

        value = map1.get("propInteger2RA_DD");
        if (!Integer.valueOf(2000).equals(value))
            throw new Exception("unexpected value for propInteger2RA_DD: " + value);

        value = map1.get("propStringRA_DD");
        if (!"RA Deployment Descriptor Default Value".equals(value))
            throw new Exception("unexpected value for propStringRA_DD: " + value);
    }

    /**
     * Config properties defined on the ResourceAdapter JavaBean override config properties of the ManagedConnectionFactory JavaBean
     *
     * @param request  HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testRAJavaBeanOverridesMCFJavaBean(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        @SuppressWarnings("unchecked")
        Map<String, Object> map1 = (Map<String, Object>) new InitialContext().lookup("eis/map1");
        Object value;

        // We don't currently support primitives.
//        value = map1.get("propInt1RA");
//        if (!Integer.valueOf(0).equals(value))
//            throw new Exception("unexpected value for propInt1RA: " + value);

        value = map1.get("propInteger2RA");
        if (!Integer.valueOf(20).equals(value))
            throw new Exception("unexpected value for propInteger2RA: " + value);

        value = map1.get("propStringRA");
        if (!"RA JavaBean Default Value".equals(value))
            throw new Exception("unexpected value for propStringRA: " + value);
    }

    /**
     * Config properties defined in the ResourceAdapter wlp-ra.xml override config properties of the ResourceAdapter deployment descriptor entry
     *
     * @param request  HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testRAWLPExtensionOverridesRADeploymentDescriptor(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        @SuppressWarnings("unchecked")
        Map<String, Object> map1 = (Map<String, Object>) new InitialContext().lookup("eis/map1");
        Object value;

        // We don't currently support primitives.
//        value = map1.get("propInt1RA_WLP");
//        if (!Integer.valueOf(10000).equals(value))
//            throw new Exception("unexpected value for propInt1RA_WLP: " + value);

        value = map1.get("propInteger2RA_WLP");
        if (!Integer.valueOf(20000).equals(value))
            throw new Exception("unexpected value for propInteger2RA_WLP: " + value);

        value = map1.get("propStringRA_WLP");
        if (!"RA wlp-ra.xml Default Value".equals(value))
            throw new Exception("unexpected value for propStringRA_WLP: " + value);
    }

    /**
     * Config properties defined in server.xml override config properties of the ManagedConnectionFactory wlp-ra.xml
     *
     * @param request  HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testServerXMLOverridesWLPExtension(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        @SuppressWarnings("unchecked")
        Map<String, Object> map2 = (Map<String, Object>) new InitialContext().lookup("eis/map2");
        Object value;

        // We don't currently support primitives.
//        value = map2.get("propInt1_WLP");
//        if (!Integer.valueOf(12345).equals(value))
//            throw new Exception("unexpected value for propInt1_WLP: " + value);

        value = map2.get("propInteger2_WLP");
        if (!Integer.valueOf(12345).equals(value))
            throw new Exception("unexpected value for propInteger2_WLP: " + value);

        value = map2.get("propString_WLP");
        if (!"server.xml default value".equals(value))
            throw new Exception("unexpected value for propString_WLP: " + value);
    }

    /**
     * Config properties defined in the ManagedConnectionFactory wlp-ra.xml override config properties of the ManagedConnectionFactory deployment descriptor entry
     *
     * @param request  HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testWLPExtensionOverridesMCFDeploymentDescriptor(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        @SuppressWarnings("unchecked")
        Map<String, Object> map1 = (Map<String, Object>) new InitialContext().lookup("eis/map1");
        Object value;

        // We don't currently support primitives.
//        value = map1.get("propInt1_WLP");
//        if (!Integer.valueOf(11111).equals(value))
//            throw new Exception("unexpected value for propInt1_WLP: " + value);

        value = map1.get("propInteger2_WLP");
        if (!Integer.valueOf(22222).equals(value))
            throw new Exception("unexpected value for propInteger2_WLP: " + value);

        value = map1.get("propString_WLP");
        if (!"MCF wlp-ra.xml Default Value".equals(value))
            throw new Exception("unexpected value for propString_WLP: " + value);
    }
}
