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
import static org.junit.Assert.fail;

import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import com.ibm.ws.javaee.dd.bval.DefaultValidatedExecutableTypes.ExecutableTypeEnum;
import com.ibm.ws.javaee.dd.bval.ExecutableValidation;
import com.ibm.ws.javaee.dd.bval.Property;
import com.ibm.ws.javaee.dd.bval.ValidationConfig;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

public class ValidationConfigTest extends ValidationConfigTestBase {

    @Test
    public void testGetVersionNoVersion() throws Exception {
        int versionId = parse(validationConfigNoVersion() + "</validation-config>")
                .getVersionID();
        Assert.assertEquals("Version should be 1.0",
                ValidationConfig.VERSION_1_0,
                versionId);
    }

    @Test
    public void testGetVersion11() throws Exception {
        int versionId = parse(validationConfig11() + "</validation-config>")
                .getVersionID();
        Assert.assertEquals("Version should be 1.1",
                ValidationConfig.VERSION_1_1,
                versionId);
    }

    @Test
    public void testGetVersion10() throws Exception {
        try {
            parse(validationConfig10() + "</validation-config>");
            fail("having validation.xml with version=1.0 isn't valid");
        } catch (UnableToAdaptException e) {
            verifyMessage(e, "unsupported.descriptor.version", "CWWKC2263E");
        }
    }

    //

    @Test
    public void testGetDefaultProvider() throws Exception {
        String defaultProvider = parse(validationConfigNoVersion() +
                                       "</validation-config>").getDefaultProvider();
        assertEquals("no default-provider specified should have returned null: " + defaultProvider,
                     null, defaultProvider);

        defaultProvider = parse(validationConfigNoVersion() +
                                "<default-provider></default-provider>" +
                                "</validation-config>").getDefaultProvider();
        assertEquals("Emtpy default provider didn't return empty string: " + defaultProvider,
                     "", defaultProvider);

        defaultProvider = parse(validationConfigNoVersion() +
                                "<default-provider>provider.class.Name</default-provider>" +
                                "</validation-config>").getDefaultProvider();
        assertEquals("default provider shouldn't have returned: " + defaultProvider,
                     "provider.class.Name", defaultProvider);

        try {
            defaultProvider = parse(validationConfig10() +
                                    "<default-provider>provider.class.Name</default-provider>" +
                                    "</validation-config>").getDefaultProvider();
        } catch (UnableToAdaptException e) {
            verifyMessage(e, "unsupported.descriptor.version", "CWWKC2263E");
        }

        defaultProvider = parse(validationConfig11() +
                                "<default-provider>provider.class.Name</default-provider>" +
                                "</validation-config>").getDefaultProvider();
        assertEquals("default provider shouldn't have returned: " + defaultProvider,
                     "provider.class.Name", defaultProvider);
    }

    @Test
    public void testGetMessageInterpolator() throws Exception {
        String messageInterpolator = parse(validationConfigNoVersion() +
                                           "</validation-config>").getMessageInterpolator();
        assertEquals("no message-interpolator specified should have returned null: " + messageInterpolator,
                     null, messageInterpolator);

        messageInterpolator = parse(validationConfigNoVersion() +
                                    "<message-interpolator></message-interpolator>" +
                                    "</validation-config>").getMessageInterpolator();
        assertEquals("Emtpy message-interpolator didn't return empty string: " + messageInterpolator,
                     "", messageInterpolator);

        messageInterpolator = parse(validationConfigNoVersion() +
                                    "<message-interpolator>provider.class.Name1</message-interpolator>" +
                                    "</validation-config>").getMessageInterpolator();
        assertEquals("message-interpolator shouldn't have returned: " + messageInterpolator,
                     "provider.class.Name1", messageInterpolator);
    }

    @Test
    public void testGetTraversableResolver() throws Exception {
        String traversableResolver = parse(validationConfigNoVersion() +
                                           "</validation-config>").getTraversableResolver();
        assertEquals("no traversable-resolver specified should have returned null: " + traversableResolver,
                     null, traversableResolver);

        traversableResolver = parse(validationConfigNoVersion() +
                                    "<traversable-resolver></traversable-resolver>" +
                                    "</validation-config>").getTraversableResolver();
        assertEquals("Emtpy traversable-resolver didn't return empty string: " + traversableResolver,
                     "", traversableResolver);

        traversableResolver = parse(validationConfigNoVersion() +
                                    "<traversable-resolver>provider.class.Name2</traversable-resolver>" +
                                    "</validation-config>").getTraversableResolver();
        assertEquals("traversable-resolver shouldn't have returned: " + traversableResolver,
                     "provider.class.Name2", traversableResolver);
    }

    @Test
    public void testGetConstraintValidatorFactory() throws Exception {
        String constraintValidatorFactory = parse(validationConfigNoVersion() +
                                                  "</validation-config>").getConstraintValidatorFactory();
        assertEquals("no constraint-validator-factory specified should have returned null: " + constraintValidatorFactory,
                     null, constraintValidatorFactory);

        constraintValidatorFactory = parse(validationConfigNoVersion() +
                                           "<constraint-validator-factory></constraint-validator-factory>" +
                                           "</validation-config>").getConstraintValidatorFactory();
        assertEquals("Emtpy constraint-validator-factory didn't return empty string: " + constraintValidatorFactory,
                     "", constraintValidatorFactory);

        constraintValidatorFactory = parse(validationConfigNoVersion() +
                                           "<constraint-validator-factory>provider.class.Name3</constraint-validator-factory>" +
                                           "</validation-config>").getConstraintValidatorFactory();
        assertEquals("constraint-validator-factory shouldn't have returned: " + constraintValidatorFactory,
                     "provider.class.Name3", constraintValidatorFactory);
    }

    @Test
    public void testGetParameterNameProvider() throws Exception {
        String parameterNameProvider = parse(validationConfigNoVersion() +
                                             "</validation-config>").getParameterNameProvider();
        assertEquals("no parameter-name-provider specified should have returned null: " + parameterNameProvider,
                     null, parameterNameProvider);

        parameterNameProvider = parse(validationConfigNoVersion() +
                                      "<parameter-name-provider></parameter-name-provider>" +
                                      "</validation-config>").getParameterNameProvider();
        assertEquals("Emtpy parameter-name-provider didn't return empty string: " + parameterNameProvider,
                     "", parameterNameProvider);

        parameterNameProvider = parse(validationConfigNoVersion() +
                                      "<parameter-name-provider>provider.class.Name4</parameter-name-provider>" +
                                      "</validation-config>").getParameterNameProvider();
        assertEquals("parameter-name-provider shouldn't have returned: " + parameterNameProvider,
                     "provider.class.Name4", parameterNameProvider);
    }

    @Test
    public void testGetDefaultValidatedExecutableTypes() throws Exception {
        // no executable-validation specified
        ExecutableValidation validation = parse(validationConfig11() +
                                                "</validation-config>").getExecutableValidation();
        assertNull("executable-validation didn't return null" + validation, validation);

        // enabled NOT specified and one executable-type specified
        validation = parse(validationConfig11() +
                           "<executable-validation>" +
                           "<default-validated-executable-types>" +
                           "<executable-type>NONE</executable-type>" +
                           "</default-validated-executable-types>" +
                           "</executable-validation>" +
                           "</validation-config>").getExecutableValidation();
        assertTrue("executable validation should be enabled by default, but isn't", validation.getEnabled());

        List<ExecutableTypeEnum> list = validation.getDefaultValidatedExecutableTypes().getExecutableTypes();
        assertEquals("executable types list should be of size 1, but was" + list.size(), 1, list.size());
        assertEquals("the only executable-type should be NONE: " + list.get(0),
                     ExecutableTypeEnum.NONE, list.get(0));

        // enabled specified false and two executable-type's specified
        validation = parse(validationConfig11() +
                           "<executable-validation enabled=\"false\">" +
                           "<default-validated-executable-types>" +
                           "<executable-type>NONE</executable-type>" +
                           "<executable-type>GETTER_METHODS</executable-type>" +
                           "</default-validated-executable-types>" +
                           "</executable-validation>" +
                           "</validation-config>").getExecutableValidation();
        assertFalse("executable validation should false", validation.getEnabled());

        list = validation.getDefaultValidatedExecutableTypes().getExecutableTypes();
        assertEquals("executable types list should be of size 2, but was" + list.size(), 2, list.size());
        assertEquals("the executable-type should be NONE: " + list.get(0),
                     ExecutableTypeEnum.NONE, list.get(0));
        assertEquals("the executable-type should be GETTER_METHODS: " + list.get(1),
                     ExecutableTypeEnum.GETTER_METHODS, list.get(1));

        // enabled specified true and NO executable-type's specified, but 
        // default-validated-executable-types specified
        validation = parse(validationConfig11() +
                           "<executable-validation enabled=\"true\">" +
                           "<default-validated-executable-types>" +
                           "</default-validated-executable-types>" +
                           "</executable-validation>" +
                           "</validation-config>").getExecutableValidation();
        assertTrue("executable validation should true", validation.getEnabled());

        list = validation.getDefaultValidatedExecutableTypes().getExecutableTypes();
        assertEquals("executable types list should be of size 0, but was" + list.size(), 0, list.size());

        // NO executable-type's specified and default-validated-executable-types not specified
        validation = parse(validationConfig11() +
                           "<executable-validation>" +
                           "</executable-validation>" +
                           "</validation-config>").getExecutableValidation();
        assertTrue("default executable validation should true", validation.getEnabled());

        assertNull("default-validated-executable-types wasn't specified so it should be null",
                   validation.getDefaultValidatedExecutableTypes());

        // provide executable-type that isn't recognized
        try {
            validation = parse(validationConfig11() +
                               "<executable-validation>" +
                               "<default-validated-executable-types>" +
                               "<executable-type>NON</executable-type>" +
                               "</default-validated-executable-types>" +
                               "</executable-validation>" +
                               "</validation-config>").getExecutableValidation();
            fail("parsing should fail when executable-type is specified which isn't a valid enum value");
        } catch (UnableToAdaptException e) {
            // Ignore: Expected
        }

        // provide executable-type that is empty
        try {
            validation = parse(validationConfig11() +
                               "<executable-validation>" +
                               "<default-validated-executable-types>" +
                               "<executable-type></executable-type>" +
                               "</default-validated-executable-types>" +
                               "</executable-validation>" +
                               "</validation-config>").getExecutableValidation();
            fail("parsing should fail when an empty executable-type is specified");
        } catch (UnableToAdaptException e) {
            // Ignore: Expected
        }
    }

    @Test
    public void testGetConstraintMappings() throws Exception {
        List<String> constraintMappings = parse(validationConfigNoVersion() +
                                                "</validation-config>").getConstraintMappings();
        assertEquals("constraint-mapping didn't return list of size 0: " + constraintMappings,
                     0, constraintMappings.size());

        constraintMappings = parse(validationConfigNoVersion() +
                                   "<constraint-mapping></constraint-mapping>" +
                                   "</validation-config>").getConstraintMappings();
        assertEquals("constraint-mapping didn't return list of size 1: " + constraintMappings,
                     1, constraintMappings.size());
        assertEquals("constraint-mapping shouldn't have returned: " + constraintMappings,
                     "", constraintMappings.get(0));

        constraintMappings = parse(validationConfigNoVersion() +
                                   "<constraint-mapping>META-INF/my-mapping.xml</constraint-mapping>" +
                                   "</validation-config>").getConstraintMappings();
        assertEquals("constraint-mapping didn't return list of size 1: " + constraintMappings,
                     1, constraintMappings.size());
        assertEquals("constraint-mapping shouldn't have returned: " + constraintMappings,
                     "META-INF/my-mapping.xml", constraintMappings.get(0));

        constraintMappings = parse(validationConfigNoVersion() +
                                   "<constraint-mapping>META-INF/my-mapping.xml</constraint-mapping>" +
                                   "<constraint-mapping>META-INF/my-other-mapping.xml</constraint-mapping>" +
                                   "</validation-config>").getConstraintMappings();
        assertEquals("constraint-mapping didn't return list of size 2: " + constraintMappings,
                     2, constraintMappings.size());
        assertEquals("constraint-mapping shouldn't have returned: " + constraintMappings,
                     "META-INF/my-mapping.xml", constraintMappings.get(0));
        assertEquals("constraint-mapping shouldn't have returned: " + constraintMappings,
                     "META-INF/my-other-mapping.xml", constraintMappings.get(1));
    }

    @Test
    public void testGetProperties() throws Exception {
        // no properties
        List<Property> properties = parse(validationConfigNoVersion() + "</validation-config>")
                .getProperties();
        assertEquals("property didn't return list of size 0: " + properties,
                     0, properties.size());

        // one property
        properties = parse(validationConfigNoVersion() +
                               "<property name=\"x\">y</property>" +
                           "</validation-config>").getProperties();
        assertEquals("property didn't return list of size 1: " + properties,
                     1, properties.size());
        Property property = properties.get(0);
        assertEquals("property name shouldn't have returned: " + property.getName(),
                     "x", property.getName());
        assertEquals("property value shouldn't have returned: " + property.getValue(),
                     "y", property.getValue());

        // two properties
        properties = parse(validationConfigNoVersion() +
                               "<property name=\"x\">y</property>" +
                               "<property name=\"x1\">y1</property>" +
                           "</validation-config>").getProperties();
        assertEquals("property didn't return list of size 2: " + properties,
                     2, properties.size());
        property = properties.get(0);
        assertEquals("property name shouldn't have returned: " + property.getName(),
                     "x", property.getName());
        assertEquals("property value shouldn't have returned: " + property.getValue(),
                     "y", property.getValue());
        property = properties.get(1);
        assertEquals("property name shouldn't have returned: " + property.getName(),
                     "x1", property.getName());
        assertEquals("property value shouldn't have returned: " + property.getValue(),
                     "y1", property.getValue());

        // one property with no value
        properties = parse(validationConfigNoVersion() +
                               "<property name=\"x\"></property>" +
                           "</validation-config>").getProperties();
        assertEquals("property didn't return list of size 1: " + properties,
                     1, properties.size());
        property = properties.get(0);
        assertEquals("property name shouldn't have returned: " + property.getName(),
                     "x", property.getName());
        assertEquals("property value shouldn't have returned: " + property.getValue(),
                     "", property.getValue());

        // one property with empty name
        properties = parse(validationConfigNoVersion() +
                               "<property name=\"\">y</property>" +
                           "</validation-config>").getProperties();
        assertEquals("property didn't return list of size 1: " + properties,
                     1, properties.size());
        property = properties.get(0);
        assertEquals("property name shouldn't have returned: " + property.getName(),
                     "", property.getName());
        assertEquals("property value shouldn't have returned: " + property.getValue(),
                     "y", property.getValue());

        // one property with no name attribute
        try {
            properties = parse(validationConfigNoVersion() +
                                   "<property>y</property>" +
                               "</validation-config>").getProperties();
            fail("an exception should be thrown if the property element doesn't have a name attribute");
        } catch (UnableToAdaptException e) {
            verifyMessage(e, "required.attribute.missing", "CWWKC2251E");
        }
    }

    @Test
    public void testNotBValValidationXML() throws Exception {
        ValidationConfig config = parse(notValidationConfig(), true);
        assertNull("non Bean Validation xml should return null: " + config, config);
    }

    @Test
    public void testUnknownElement() throws Exception {
        try {
            parse(validationConfigNoVersion() +
                    "<not-part-of-validation-config>x</not-part-of-validation-config>" +
                  "</validation-config>");
            fail("an exception should be thrown if unknown elements are used");
        } catch (UnableToAdaptException e) {
            verifyMessage(e, "unexpected.child.element", "CWWKC2259E");
        }
    }
}
