/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package io.openliberty.org.apache.faces40.fat.literals;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.openliberty.org.apache.myfaces40.fat.tests.AnnotationLiteralsTest;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.faces.annotation.ApplicationMap;
import jakarta.faces.annotation.FacesConfig;
import jakarta.faces.annotation.HeaderMap;
import jakarta.faces.annotation.HeaderValuesMap;
import jakarta.faces.annotation.InitParameterMap;
import jakarta.faces.annotation.ManagedProperty;
import jakarta.faces.annotation.RequestCookieMap;
import jakarta.faces.annotation.RequestMap;
import jakarta.faces.annotation.RequestParameterMap;
import jakarta.faces.annotation.RequestParameterValuesMap;
import jakarta.faces.annotation.SessionMap;
import jakarta.faces.annotation.View;
import jakarta.faces.component.behavior.BehaviorBase;
import jakarta.faces.component.behavior.ClientBehaviorBase;
import jakarta.faces.component.behavior.FacesBehavior;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;
import jakarta.faces.event.WebsocketEvent;
import jakarta.faces.model.DataModel;
import jakarta.faces.model.FacesDataModel;
import jakarta.faces.validator.FacesValidator;
import jakarta.faces.validator.Validator;
import jakarta.faces.validator.ValidatorException;
import jakarta.faces.view.facelets.Facelet;
import jakarta.inject.Named;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpSession;

/**
 * This is a main testing bean which will attempt to lookup annotation literals and validate they were created properly
 */
@Named(value = "testBean")
@RequestScoped
@SuppressWarnings("serial")
public class TestBean {

    /**
     * barAttribute - set in AttributeFilter class
     *
     * @see AttributeFilter
     *
     * @return the applicationMap
     */
    public String getApplicationMap() {
        Map<String, Object> applicationMap = CDI.current().select(new TypeLiteral<Map<String, Object>>() {}, ApplicationMap.Literal.INSTANCE).get();
        return Boolean.toString(applicationMap.containsKey("barAttribute"));
    }

    /**
     * Test data is the ClientBehavior class itself
     *
     * @see ClientBehavior
     *
     * @return the facesBehavior
     */
    public String getFacesBehavior() {
        BehaviorBase clientBehavior = CDI.current().select(new TypeLiteral<BehaviorBase>() {}, FacesBehavior.Literal.of("clientBehavior", true)).get();
        return Boolean.toString(clientBehavior != null && clientBehavior instanceof ClientBehaviorBase && clientBehavior.getClass().equals(ClientBehavior.class));
    }

    /**
     * TODO
     *
     * @return the facesConfig
     */
    public String getFacesConfig() {
        Object config = CDI.current().select(new TypeLiteral<>() {}, FacesConfig.Literal.INSTANCE).get();
        return Boolean.toString(config != null && config.getClass().equals(ConfigBean.class));
    }

    /**
     * Test data is the IntegerConverter class itself
     *
     * @see IntegerConverter
     *
     * @return the facesConverter
     */
    public String getFacesConverter() {
        Converter<Integer> converter = CDI.current().select(new TypeLiteral<Converter<Integer>>() {}, FacesConverter.Literal.of("integerConverter", Integer.class, true)).get();
        return Boolean.toString(converter != null && converter.getAsObject(FacesContext.getCurrentInstance(), null, "31").intValue() == 31);
    }

    /**
     * Test data is the IntegerDataModel class itself
     *
     * @see IntegerDataModel
     *
     * @return the facesDataModel
     */
    public String getFacesDataModel() {
        DataModel<Integer> dataModel = CDI.current().select(new TypeLiteral<DataModel<Integer>>() {}, FacesDataModel.Literal.of(Integer.class)).get();
        return Boolean.toString(dataModel != null && dataModel.isRowAvailable() && dataModel.getRowData().intValue() == 32);
    }

    /**
     * Test data is the IntegerValidator class itself
     *
     * @see IntegerValidator
     *
     * @return the facesValidator
     */
    public String getFacesValidator() {
        Validator<Integer> validator = CDI.current().select(new TypeLiteral<Validator<Integer>>() {}, FacesValidator.Literal.of("integerValidator", false, true)).get();
        try {
            validator.validate(FacesContext.getCurrentInstance(), null, Integer.valueOf(33));
            return "false"; //should have thrown exception
        } catch (ValidatorException ve) {
            return Boolean.toString(ve.getFacesMessage().getSummary().equalsIgnoreCase("TO_HIGH"));
        }
    }

    /**
     * @return the websocketEventOpened
     */
    public String getWebsocketEventOpened() {
        Object opened = CDI.current().select(new TypeLiteral<>() {}, WebsocketEvent.Opened.Literal.INSTANCE).get();
        return opened.toString();
    }

    /**
     * @return the websocketEventClosed
     */
    public String getWebsocketEventClosed() {
        Object closed = CDI.current().select(new TypeLiteral<>() {}, WebsocketEvent.Closed.Literal.INSTANCE).get();
        return closed.toString();
    }

    /**
     * foo - added to request header of test
     *
     * @see AnnotationLiteralsTest#testHeaderMap()
     *
     * @return the headerMap
     */
    public String getHeaderMap() {
        Map<String, String> headerMap = CDI.current().select(new TypeLiteral<Map<String, String>>() {}, HeaderMap.Literal.INSTANCE).get();
        return headerMap.get("foo");
    }

    /**
     * foo - added to request header of test
     *
     * @see AnnotationLiteralsTest#testHeaderValuesMap()
     *
     * @return the headerValuesMap
     */
    public String getHeaderValuesMap() {
        Map<String, String[]> headerValuesMap = CDI.current().select(new TypeLiteral<Map<String, String[]>>() {}, HeaderValuesMap.Literal.INSTANCE).get();

        return Stream.of(headerValuesMap.get("foo")).collect(Collectors.joining(":"));
    }

    /**
     * MY_TEST_PARAMETER - set in web.xml of application
     *
     * @see AnnotationLiteralsTest.war/WEB-INF/web.xml
     *
     * @return the initParameterMap
     */
    public String getInitParameterMap() {
        Map<String, String> initParameterMap = CDI.current().select(new TypeLiteral<Map<String, String>>() {}, InitParameterMap.Literal.INSTANCE).get();
        return initParameterMap.get("MY_TEST_PARAMETER");
    }

    /**
     * Get values from a ManagedPropertyBean and assert the correct values.
     *
     * @see ManagedPropertyBean
     *
     * @return the managedProperty
     */
    public String getManagedProperty() {
        Integer myInteger = CDI.current().select(new TypeLiteral<Integer>() {}, ManagedProperty.Literal.of("#{managedPropertyBean.myInteger}")).get();
        Map<String, String> myStringMap = CDI.current().select(new TypeLiteral<Map<String, String>>() {}, ManagedProperty.Literal.of("#{managedPropertyBean.myStringMap}")).get();

        return Boolean.toString(myInteger.intValue() == 42 && myStringMap.containsKey("foo") && myStringMap.get("foo").equals("bar"));
    }

    /**
     * TESTCOOKIE - set in request
     *
     * @see AnnotationLiteralsTest#testRequestCookieMap()
     *
     * @return the requestCookieMap
     */
    public String getRequestCookieMap() {
        Map<String, Object> requestCookieMap = CDI.current().select(new TypeLiteral<Map<String, Object>>() {}, RequestCookieMap.Literal.INSTANCE).get();
        return ((Cookie) requestCookieMap.get("TESTCOOKIE")).getValue();
    }

    /**
     * fooAttribute - set in AttributeFilter class
     *
     * @see AttributeFilter
     *
     * @return the requestMap
     */
    public String getRequestMap() {
        Map<String, Object> requestMap = CDI.current().select(new TypeLiteral<Map<String, Object>>() {}, RequestMap.Literal.INSTANCE).get();
        return (String) requestMap.get("fooAttribute");
    }

    /**
     * foo - set as URL param
     *
     * @see AnnotationLiteralsTest#testRequestParameterMap()
     *
     * @return the requestParameterMap
     */
    public String getRequestParameterMap() {
        Map<String, String> requestParameterMap = CDI.current().select(new TypeLiteral<Map<String, String>>() {}, RequestParameterMap.Literal.INSTANCE).get();
        return requestParameterMap.get("foo");
    }

    /**
     * foo - set as URL params
     *
     * @see AnnotationLiteralsTest#testRequestParameterValuesMap()
     *
     * @return the requestParameterValuesMap
     */
    public String getRequestParameterValuesMap() {
        Map<String, String[]> requestParameterValuesMap = CDI.current().select(new TypeLiteral<Map<String, String[]>>() {}, RequestParameterValuesMap.Literal.INSTANCE).get();
        return Stream.of(requestParameterValuesMap.get("foo")).collect(Collectors.joining(":"));
    }

    /**
     * /fake.xhtml - path to view defined in FaceletViewBean class
     *
     * @see FaceletViewBean
     *
     * @return the view
     */
    public String getView() {
        Facelet view = CDI.current().select(new TypeLiteral<Facelet>() {}, View.Literal.of("/fake.xhtml")).get();
        return Boolean.toString(view != null && view.getClass().getCanonicalName().contains(FaceletViewBean.class.getCanonicalName()));
    }

    /**
     * Test data is the session itself. Every request to a servlet is within a session.
     *
     * @see HttpSession
     *
     * @return the sessionMap
     */
    public String getSessionMap() {
        Map<String, Object> sessions = CDI.current().select(new TypeLiteral<Map<String, Object>>() {}, SessionMap.Literal.INSTANCE).get();
        return Boolean.toString(sessions != null && !sessions.isEmpty());
    }
}