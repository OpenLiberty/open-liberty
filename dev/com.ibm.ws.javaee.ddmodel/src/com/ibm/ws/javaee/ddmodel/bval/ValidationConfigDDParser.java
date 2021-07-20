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

import com.ibm.websphere.ras.Tr;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.javaee.dd.bval.ValidationConfig;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

public class ValidationConfigDDParser extends DDParser {
    public static final String NAMESPACE_JBOSS_VALIDATION_CONFIG =
        "http://jboss.org/xml/ns/javax/validation/configuration";

    public ValidationConfigDDParser(Container ddRootContainer, Entry ddEntry)
        throws ParseException {
        super(ddRootContainer, ddEntry, "validation-config");
    }

    /**
     * Parse the specified configuration element.
     * 
     * If the root element name is not "validation-config", the
     * document is assumed to not be for validation configuration,
     * and null is returned. This is different from other parsers,
     * which would throw a parse exception if the root element
     * name was not valid.  The intent is to ignore documents
     * which have the name "validation.xml" but which are not
     * for use by bean validation.
     * 
     * If the root element name is "validation-config", the document
     * is assumed to be for validation configuration.  The version
     * and namespace values must exactly match the bean validation
     * schemas.  This is intentionally more strict than other parsers, 
     * since we are guarding against accidentally using documents
     * not intended for bean validation.
     *
     * @return The parsed validation configuration.  Null if the
     *     root element name is incorrect.
     *
     * @throws ParseException Thrown if parsing encountered an error.
     */
    @Override
    public ValidationConfigType parse() throws ParseException {
        super.parseRootElement();
        return (ValidationConfigType) rootParsable;        
    }

    @Override
    protected ValidationConfigType createRootParsable() throws ParseException {
        if ( !"validation-config".equals(rootElementLocalName) ) {
            warning( unexpectedRootElement("validation-config") );
            return null;
        }
        
        // TODO: In other parsers, if a version is provided,
        //       the namespace is ignored.  Here, since we
        //       are on guard for accidentally using a file
        //       not intended for bean validation, we require
        //       that the correct namespace be provided.
        if ( namespace == null ) {
            throw new ParseException( missingDescriptorNamespace(NAMESPACE_JBOSS_VALIDATION_CONFIG) );                            
        } else if ( !NAMESPACE_JBOSS_VALIDATION_CONFIG.equals(namespace) ) {
            throw new ParseException( unsupportedDescriptorNamespace(namespace) );                
        }

        // "1.0" is NOT accepted as the version attribute.  The version
        // attribute must either be entirely absent, or must be "1.1".
        String versionAttr = getAttributeValue("", "version");
        if ( versionAttr == null ) {
            version = ValidationConfig.VERSION_1_0; // JavaEE 6
        } else if ( ValidationConfig.VERSION_1_1_STR.equals(versionAttr) ) {
            version = ValidationConfig.VERSION_1_1; // JavaEE 7
        } else {
            throw new ParseException( unsupportedDescriptorVersion(versionAttr) );
        }

        return new ValidationConfigType( getDeploymentDescriptorPath() );
    }

    /**
     * Override for bean validation: Bean validation emits a warning
     * instead of throwing an exception, and displays a bean validation
     * specific message.
     * 
     * @param expectedRootElementName The expected root element name.
     * 
     * @return A bean validation specific message for the unexpected root
     *     element.
     */
    @Override
    protected String unexpectedRootElement(String expectedRootElementName) {
        // New messages:
        //
        // unexpected.root.element.bval
        // CWWKC2271W: Ignoring bean validation configuration file {0} of module {1}.
        // At line number {2}, the root element is {3} but should be {4}.
        //
        // The validation configuration file does not match the validation configuration
        // schemas (validation-configuration-1.0.xsd and validation-configuration-1.1.xsd).
        // Either the configuration file was not intended for use for bean validation,
        // or the configuration file is malformed.  The configuration file is being
        // ignored.  The BeanValidationService will not create a ValidatorFactory.
        //
        // If the validation configuration file is not intended for bean validation,
        // the warning message can be ignored.  If the validation configuration file
        // is intended for bean validation, the warning message should not be ignored,
        // as the bean validation service is not running as intended.  To diagnose the
        // problem, enable bean validation runtime trace, stop and restart the server,
        // and examine trace output for unexpected exceptions relating to bean validation.
        // For example, "Unexpected exception when trying to unmarshall the validation.xml
        // file."  Examine the exception stack trace for debugging assistance.
        
        // Old messages:

        // BVKEY_NOT_A_BEAN_VALIDATION_XML=
        // CWWKC2271W: A validation.xml file was found for the {0} module.
        // However, that validation.xml file is not configured for validation;
        // therefore this XML file is ignored.
        
        // BVKEY_NOT_A_BEAN_VALIDATION_XML.explanation=
        // The BeanValidationService cannot create a ValidatorFactory because
        // of an error parsing the validation.xml file with the
        // validation-configuration-1.0.xsd schema file. This error may indicate
        // that the validation.xml file is not intended for use by the Bean Validation
        // runtime; therefore, this XML file is ignored.

        // BVKEY_NOT_A_BEAN_VALIDATION_XML.useraction=
        // If the validation.xml file is not for creating a ValidatorFactory instance,
        // then ignore the message. However if the validation.xml file is intended to
        // create a ValidatorFactory instance, then a problem exists.  Enable Bean
        // Validation run-time trace in the application server, stop the application,
        // restart the application, and examine the trace output file for a trace event
        // such as: Unexpected exception when trying to unmarshall the validation.xml
        // file. Examine the exception stack trace for debugging assistance.
        
        return Tr.formatMessage(tc, "unexpected.root.element.bval",
                describeEntry(), getModuleName(), getLineNumber(),
                rootElementLocalName, expectedRootElementName);
    }        
    
    public String getModuleName() {
        ModuleInfo moduleInfo = cacheGet(ModuleInfo.class);
        if ( moduleInfo != null ) {
            return moduleInfo.getName();

        } else {
            Entry rootEntry;
            try {
                rootEntry = rootContainer.adapt(Entry.class);
            } catch ( UnableToAdaptException e ) {
                // FFDC, but otherwise ignore.
                rootEntry = null;
            }
            if ( rootEntry != null ) {
                return rootEntry.getPath().substring(1);
            } else {
                return getSimpleName(rootContainer);
            }
        }
    }    
}
