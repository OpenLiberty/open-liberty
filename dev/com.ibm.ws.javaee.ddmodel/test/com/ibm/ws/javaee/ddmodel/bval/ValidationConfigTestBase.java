/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.bval;

import org.jmock.Expectations;

import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.javaee.dd.bval.ValidationConfig;
import com.ibm.ws.javaee.ddmodel.DDTestBase;

public class ValidationConfigTestBase extends DDTestBase {
    
    protected static ValidationConfigEntryAdapter createValidationConfigAdapter() {
        return new ValidationConfigEntryAdapter();    
    }

    protected ValidationConfig parse(String xml) throws Exception {
        return parse(xml, !NOT_BVAL_XML, null);
    }
    
    protected ValidationConfig parse(String xml, String altMessage, String... messages) throws Exception {
        return parse(xml, !NOT_BVAL_XML, altMessage, messages);
    }

    public static final boolean NOT_BVAL_XML = true;

    protected ValidationConfig parse(String ddText, boolean notBvalXml) throws Exception {
        return parse(ddText, notBvalXml, null);
    }

    protected ValidationConfig parse(String ddText, boolean notBvalXml,
                                     String altMessage, String... messages) throws Exception {

        String appPath = null;
        String modulePath = "/root/wlp/usr/servers/server1/apps/ejbJar.jar";
        String fragmentPath = null;
        String ddPath = "META-INF/validation.xml";

        ModuleInfo moduleInfo;
        if ( !notBvalXml ) {
            moduleInfo = mockery.mock(ModuleInfo.class, "moduleInfo" + mockId++);
            mockery.checking(new Expectations() {
                {
                    allowing(moduleInfo).getName();
                    will(returnValue("thisModule"));
                }
            });
        } else {
            moduleInfo = null;
        }

        return parse(appPath, modulePath, fragmentPath,
                     ddText, createValidationConfigAdapter(), ddPath,
                     null, null,
                     ModuleInfo.class, moduleInfo,
                     altMessage, messages);
    }

    protected static final String validationConfigTail =
            "</validation-config>";
    
    protected static String processBody(String body) {
        if ( body == null ) {
            return "";
        } else if ( body.isEmpty() ) {
            return "";
        } else {
            return body + "\n";
        }
    }

    //
    
    protected ValidationConfig parseNoVersion(String body) throws Exception {
        return parse( validationConfigNoVersion(body) );
    }
    
    protected ValidationConfig parseNoVersion() throws Exception {
        return parse( validationConfigNoVersion() );
    }    

    protected ValidationConfig parse10(String body) throws Exception {
        return parse( validationConfig10(body) );
    }
    
    protected ValidationConfig parse10() throws Exception {
        return parse( validationConfig10() );
    }    
    
    protected ValidationConfig parse11(String body) throws Exception {
        return parse( validationConfig11(body) );
    }
    
    protected ValidationConfig parse11() throws Exception {
        return parse( validationConfig11() );
    }        
    
    //

    protected static String validationConfigNoVersion() {
        return validationConfigNoVersion(null);
    }

    protected static String validationConfig10() {
        return validationConfig10(null);
    }
    
    protected static String validationConfig11() {
        return validationConfig11(null);
    }

    //

    protected static String validationConfigNoVersion(String body) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
               "<validation-config" + "\n" +
                   " xmlns=\"http://jboss.org/xml/ns/javax/validation/configuration\"" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                   " xsi:schemaLocation=\"http://jboss.org/xml/ns/javax/validation/configuration validation-configuration-1.0.xsd\"" +
               ">" + "\n" +
               processBody(body) +
               validationConfigTail;
    }

    protected static String validationConfig10(String body) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
               "<validation-config" +
                   " xmlns=\"http://jboss.org/xml/ns/javax/validation/configuration\"" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                   " xsi:schemaLocation=\"http://jboss.org/xml/ns/javax/validation/configuration validation-configuration-1.1.xsd\"" +
                   " version=\"1.0\"" +
               ">" + "\n" +
               processBody(body) +
               validationConfigTail;
    }

    protected static String validationConfig11(String body) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
               "<validation-config" +
                   " xmlns=\"http://jboss.org/xml/ns/javax/validation/configuration\"" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                   " xsi:schemaLocation=\"http://jboss.org/xml/ns/javax/validation/configuration validation-configuration-1.1.xsd\"" +
                   " version=\"1.1\"" +
               ">" + "\n" +
               processBody(body) +
               validationConfigTail;
    }
    
    //

    protected static String notValidationConfig() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
               "<not-validation-config>" + "\n" +
               "</not-validation-config>";
    }    

    //

    protected static String validationConfig11NoNamespace() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
                "<validation-config" +
                   // " xmlns=\"http://jboss.org/xml/ns/javax/validation/configuration\"" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                   " xsi:schemaLocation=\"http://jboss.org/xml/ns/javax/validation/configuration validation-configuration-1.1.xsd\"" +
                   " version=\"1.1\"" +
               ">" + "\n" +
               validationConfigTail;
    }
    
    protected static String validationConfig11NoSchemaInstance() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
                "<validation-config" +
                   " xmlns=\"http://jboss.org/xml/ns/javax/validation/configuration\"" +
                   // " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                   " xsi:schemaLocation=\"http://jboss.org/xml/ns/javax/validation/configuration validation-configuration-1.1.xsd\"" +
                   " version=\"1.1\"" +
               ">" + "\n" +
               validationConfigTail;
    }
    
    protected static String validationConfig11NoSchemaLocation() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
                "<validation-config" +
                   " xmlns=\"http://jboss.org/xml/ns/javax/validation/configuration\"" +
                   // " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                   " xsi:schemaLocation=\"http://jboss.org/xml/ns/javax/validation/configuration validation-configuration-1.1.xsd\"" +
                   " version=\"1.1\"" +
               ">" + "\n" +
               validationConfigTail;
    }
    
    protected static String validationConfig11NoXSI() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
                "<validation-config" +
                   " xmlns=\"http://jboss.org/xml/ns/javax/validation/configuration\"" +
                   // " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                   // " xsi:schemaLocation=\"http://jboss.org/xml/ns/javax/validation/configuration validation-configuration-1.1.xsd\"" +
                   " version=\"1.1\"" +
               ">" + "\n" +
               validationConfigTail;
    }

    protected static String validationConfigNamespaceOnly() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
               "<validation-config xmlns=\"http://jboss.org/xml/ns/javax/validation/configuration\">" + "\n" +
               validationConfigTail;
    }    
    protected static String validationConfigVersion10Only() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
               "<validation-config version=\"1.0\">" + "\n" +
               validationConfigTail;
    }    
    
    protected static String validationConfigVersion11Only() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
               "<validation-config version=\"1.1\">" + "\n" +
               validationConfigTail;
    }        
    
    protected static String validationConfigVersion12Only() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
               "<validation-config version=\"1.2\">" + "\n" +
               validationConfigTail;
    }            
}
