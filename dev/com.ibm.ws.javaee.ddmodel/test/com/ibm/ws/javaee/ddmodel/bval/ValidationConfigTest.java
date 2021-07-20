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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import com.ibm.ws.javaee.dd.bval.DefaultValidatedExecutableTypes.ExecutableTypeEnum;
import com.ibm.ws.javaee.dd.bval.ExecutableValidation;
import com.ibm.ws.javaee.dd.bval.Property;
import com.ibm.ws.javaee.dd.bval.ValidationConfig;

public class ValidationConfigTest extends ValidationConfigTestBase {
    @Test
    public void testVersionMissing() throws Exception {
        int versionId = parseNoVersion().getVersionID();
        Assert.assertEquals("Version should be 1.0", ValidationConfig.VERSION_1_0, versionId);
    }

    @Test
    public void testVersion10() throws Exception {
        parse( validationConfig10(),
               UNSUPPORTED_DESCRIPTOR_VERSION_ALT_MESSAGE,
               UNSUPPORTED_DESCRIPTOR_VERSION_MESSAGES );
    }

    @Test
    public void testVersion11() throws Exception {
        int versionId = parse11().getVersionID();
        Assert.assertEquals("Version should be 1.1", ValidationConfig.VERSION_1_1, versionId);
    }

    @Test
    public void testNotBValValidationXML() throws Exception {
        ValidationConfig config = parse(notValidationConfig(), NOT_BVAL_XML);
        assertNull("Non-bean Validation xml should be null", config);
    }

    @Test
    public void testUnknownElement() throws Exception {
        parse( validationConfigNoVersion(
                   "<not-part-of-validation-config>x</not-part-of-validation-config>"),
               "unexpected.child.element", "CWWKC2259E");
    }
    
    //

    @Test
    public void testVersion11NoNamespace() throws Exception {
        parse( validationConfig11NoNamespace(),
               MISSING_DESCRIPTOR_NAMESPACE_ALT_MESSAGE,
               MISSING_DESCRIPTOR_NAMESPACE_ALT_MESSAGES );
    }

    @Test
    public void testVersion11NoSchemaInstance() throws Exception {
        parse( validationConfig11NoSchemaInstance(),
               "xml.error",
               "CWWKC2272E", "ejbJar.jar : META-INF/validation.xml" );        
    }

    @Test
    public void testVersion11NoSchemaLocation() throws Exception {
        parse( validationConfig11NoSchemaLocation(),
               "xml.error",
               "CWWKC2272E", "ejbJar.jar : META-INF/validation.xml" );
    }

    @Test
    public void testVersion11NoXSI() throws Exception {
        int versionId = parse(validationConfig11NoXSI()).getVersionID();
        Assert.assertEquals("Version should be 1.1", ValidationConfig.VERSION_1_1, versionId);
    }

    @Test
    public void testNamespaceOnly() throws Exception {
        int versionId = parse(validationConfigNamespaceOnly()).getVersionID();
        Assert.assertEquals("Version should be 1.0", ValidationConfig.VERSION_1_0, versionId);
    }

    @Test
    public void testVersion10Only() throws Exception {
        parse( validationConfigVersion10Only(),
               MISSING_DESCRIPTOR_NAMESPACE_ALT_MESSAGE,
               MISSING_DESCRIPTOR_NAMESPACE_ALT_MESSAGES );
    }

    @Test
    public void testVersion11Only() throws Exception {
        parse( validationConfigVersion11Only(),
               MISSING_DESCRIPTOR_NAMESPACE_ALT_MESSAGE,
               MISSING_DESCRIPTOR_NAMESPACE_ALT_MESSAGES );
    }

    @Test
    public void testVersion12Only() throws Exception {
        parse( validationConfigVersion12Only(),
               MISSING_DESCRIPTOR_NAMESPACE_ALT_MESSAGE,
               MISSING_DESCRIPTOR_NAMESPACE_ALT_MESSAGES );
    }

    //
    
    @Test
    public void testGetDefaultProvider() throws Exception {
        String defaultProvider = parseNoVersion().getDefaultProvider();
        assertEquals("Default provider should be null", null, defaultProvider);

        defaultProvider = parseNoVersion("<default-provider></default-provider>")
                .getDefaultProvider();
        assertEquals("Default provider should be empty", "", defaultProvider);

        defaultProvider = parseNoVersion("<default-provider>provider.class.Name</default-provider>")
                .getDefaultProvider();
        assertEquals("Incorrect default provider", "provider.class.Name", defaultProvider);

        parse( validationConfig10("<default-provider>provider.class.Name</default-provider>"),
               UNSUPPORTED_DESCRIPTOR_VERSION_ALT_MESSAGE,
               UNSUPPORTED_DESCRIPTOR_VERSION_MESSAGES );              

        defaultProvider = parse11("<default-provider>provider.class.Name</default-provider>")
                .getDefaultProvider();
        assertEquals("Incorrect default provider", "provider.class.Name", defaultProvider);
    }

    @Test
    public void testGetMessageInterpolator() throws Exception {
        String messageInterpolator = parseNoVersion().getMessageInterpolator();
        assertEquals("Message interpolator should be null", null, messageInterpolator);

        messageInterpolator = parseNoVersion("<message-interpolator></message-interpolator>")
                .getMessageInterpolator();
        assertEquals("Message interpolator should be empty", "", messageInterpolator);

        messageInterpolator = parseNoVersion("<message-interpolator>provider.class.Name1</message-interpolator>")
                .getMessageInterpolator();
        assertEquals("Incorrect message interpolator", "provider.class.Name1", messageInterpolator);
    }

    @Test
    public void testGetTraversableResolver() throws Exception {
        String traversableResolver = parseNoVersion().getTraversableResolver();
        assertEquals("Traversable resolver should be null", null, traversableResolver);

        traversableResolver =
                parseNoVersion("<traversable-resolver></traversable-resolver>")
                    .getTraversableResolver();
        assertEquals("Traversable resolver should be empty", "", traversableResolver);

        traversableResolver =
                parseNoVersion("<traversable-resolver>provider.class.Name2</traversable-resolver>")
                    .getTraversableResolver();
        assertEquals("Incorrect traversable resolver", "provider.class.Name2", traversableResolver);
    }

    @Test
    public void testGetConstraintValidatorFactory() throws Exception {
        String constraintValidatorFactory = parseNoVersion().getConstraintValidatorFactory();
        assertEquals("Constraint validator factory should be null", null, constraintValidatorFactory);

        constraintValidatorFactory =
                parseNoVersion("<constraint-validator-factory></constraint-validator-factory>")
                    .getConstraintValidatorFactory();
        assertEquals("Constraint validator factory should be empty", "", constraintValidatorFactory);

        constraintValidatorFactory =
                parseNoVersion("<constraint-validator-factory>provider.class.Name3</constraint-validator-factory>")
                    .getConstraintValidatorFactory();
        assertEquals("Incorrect constraint validator factory", "provider.class.Name3", constraintValidatorFactory);
    }

    @Test
    public void testGetParameterNameProvider() throws Exception {
        String parameterNameProvider = parseNoVersion().getParameterNameProvider();
        assertEquals("Parameter name provider should be null", null, parameterNameProvider);

        parameterNameProvider = parseNoVersion("<parameter-name-provider></parameter-name-provider>")
                .getParameterNameProvider();
        assertEquals("Parameter name provider should be empty", "", parameterNameProvider);

        parameterNameProvider = parseNoVersion("<parameter-name-provider>provider.class.Name4</parameter-name-provider>")
                .getParameterNameProvider();
        assertEquals("Incorrect parameter name provider", "provider.class.Name4", parameterNameProvider);
    }

    @Test
    public void testGetDefaultValidatedExecutableTypes() throws Exception {
        // no executable-validation specified
        ExecutableValidation validation = parse11().getExecutableValidation();
        assertNull("executable-validation should be null", validation);

        // enabled NOT specified and one executable-type specified
        validation = parse11("<executable-validation>" +
                                 "<default-validated-executable-types>" +
                                     "<executable-type>NONE</executable-type>" +
                                 "</default-validated-executable-types>" +
                             "</executable-validation>").getExecutableValidation();
        assertTrue("Default executable validation should be enabled", validation.getEnabled());

        List<ExecutableTypeEnum> list = validation.getDefaultValidatedExecutableTypes().getExecutableTypes();
        assertEquals("executable types list should be of size 1", 1, list.size());
        assertEquals("the only executable-type should be NONE", ExecutableTypeEnum.NONE, list.get(0));

        // enabled specified false and two executable-type's specified
        validation = parse11("<executable-validation enabled=\"false\">" +
                                 "<default-validated-executable-types>" +
                                     "<executable-type>NONE</executable-type>" +
                                     "<executable-type>GETTER_METHODS</executable-type>" +
                                 "</default-validated-executable-types>" +
                             "</executable-validation>").getExecutableValidation();
        assertFalse("executable validation should false", validation.getEnabled());

        list = validation.getDefaultValidatedExecutableTypes().getExecutableTypes();
        assertEquals("executable types list should be of size 2", 2, list.size());
        assertEquals("the executable-type should be NONE", ExecutableTypeEnum.NONE, list.get(0));
        assertEquals("the executable-type should be GETTER_METHODS",
                     ExecutableTypeEnum.GETTER_METHODS, list.get(1));

        // enabled specified true and NO executable-type's specified, but 
        // default-validated-executable-types specified
        validation = parse11("<executable-validation enabled=\"true\">" +
                                 "<default-validated-executable-types>" +
                                 "</default-validated-executable-types>" +
                             "</executable-validation>").getExecutableValidation();
        assertTrue("executable validation should true", validation.getEnabled());

        list = validation.getDefaultValidatedExecutableTypes().getExecutableTypes();
        assertEquals("executable types list should be of size 0", 0, list.size());

        // NO executable-type's specified and default-validated-executable-types not specified
        validation = parse11("<executable-validation>" +
                             "</executable-validation>").getExecutableValidation();
        assertTrue("default executable validation should true", validation.getEnabled());

        assertNull("default-validated-executable-types should be null",
                   validation.getDefaultValidatedExecutableTypes());

        // provide executable-type that isn't recognized
        parse( validationConfig11(
                   "<executable-validation>" +
                       "<default-validated-executable-types>" +
                           "<executable-type>NON</executable-type>" +
                       "</default-validated-executable-types>" +
                   "</executable-validation>"),
               "invalid.enum.value",
               "CWWKC2273E", "NON", "ejbJar.jar : META-INF/validation.xml" );

        // provide executable-type that is empty
        parse( validationConfig11(
                   "<executable-validation>" +
                       "<default-validated-executable-types>" +
                           "<executable-type></executable-type>" +
                       "</default-validated-executable-types>" +
                   "</executable-validation>"),
                "invalid.enum.value",
                "CWWKC2273E", "ejbJar.jar : META-INF/validation.xml" );                
    }

    @Test
    public void testGetConstraintMappings() throws Exception {
        List<String> constraintMappings = parseNoVersion().getConstraintMappings();
        assertEquals("constraint mapping should have length 0", 0, constraintMappings.size());

        constraintMappings = parseNoVersion("<constraint-mapping></constraint-mapping>")
                .getConstraintMappings();
        assertEquals("constraint mapping should have length 1", 1, constraintMappings.size());
        assertEquals("Incorrect constraint mapping", "", constraintMappings.get(0));

        constraintMappings = parseNoVersion("<constraint-mapping>META-INF/my-mapping.xml</constraint-mapping>")
                .getConstraintMappings();
        assertEquals("constraint-mapping should have length 1", 1, constraintMappings.size());
        assertEquals("Incorrect constraint mapping", "META-INF/my-mapping.xml", constraintMappings.get(0));

        constraintMappings =
                parseNoVersion(
                        "<constraint-mapping>META-INF/my-mapping.xml</constraint-mapping>" +
                        "<constraint-mapping>META-INF/my-other-mapping.xml</constraint-mapping>")
                    .getConstraintMappings();
        assertEquals("constraint mapping should have length 2", 2, constraintMappings.size());
        assertEquals("Incorrect constraint mapping at element 0",
                     "META-INF/my-mapping.xml", constraintMappings.get(0));
        assertEquals("Incorrect constraint mapping at element 1",
                     "META-INF/my-other-mapping.xml", constraintMappings.get(1));
    }

    @Test
    public void testGetProperties() throws Exception {
        // no properties
        List<Property> properties = parseNoVersion().getProperties();
        assertEquals("Properties should have length 0", 0, properties.size());

        // one property
        properties = parseNoVersion("<property name=\"x\">y</property>").getProperties();
        assertEquals("property should have length 1", 1, properties.size());
        Property property = properties.get(0);
        assertEquals("Incorrect property name", "x", property.getName());
        assertEquals("Incorrect property value", "y", property.getValue());

        // two properties
        properties = parseNoVersion(
                "<property name=\"x\">y</property>" +
                "<property name=\"x1\">y1</property>").getProperties();
        assertEquals("property should have length 2", 2, properties.size());
        property = properties.get(0);
        assertEquals("Incorrect property name 0", "x", property.getName());
        assertEquals("Incorrect property value 0", "y", property.getValue());
        
        property = properties.get(1);
        assertEquals("Incorrect property name 1", "x1", property.getName());
        assertEquals("Incorrect property vale 1", "y1", property.getValue());        

        // one property with no value
        properties = parseNoVersion("<property name=\"x\"></property>").getProperties();
        assertEquals("property should have length 1", 1, properties.size());
        property = properties.get(0);
        assertEquals("Incorrect property name",  "x", property.getName());
        assertEquals("Incorrect property value", "", property.getValue());

        // one property with empty name
        properties = parseNoVersion("<property name=\"\">y</property>").getProperties();
        assertEquals("property should have length 1", 1, properties.size());
        property = properties.get(0);
        assertEquals("Incorrect property name", "", property.getName());
        assertEquals("Incorrect property value", "y", property.getValue());

        // one property with no name attribute
        parse( validationConfigNoVersion("<property>y</property>"),
               "required.attribute.missing", "CWWKC2251E");
    }
}
