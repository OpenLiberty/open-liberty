/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.validation.test;

import org.eclipse.microprofile.openapi.models.security.SecurityScheme.In;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme.Type;
import org.junit.Assert;
import org.junit.Test;

import io.openliberty.microprofile.openapi20.test.utils.TestValidationContextHelper;
import io.openliberty.microprofile.openapi20.test.utils.TestValidationHelper;
import io.openliberty.microprofile.openapi20.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.validation.SecuritySchemeValidator;
import io.smallrye.openapi.api.models.OpenAPIImpl;
import io.smallrye.openapi.api.models.security.OAuthFlowImpl;
import io.smallrye.openapi.api.models.security.OAuthFlowsImpl;
import io.smallrye.openapi.api.models.security.SecuritySchemeImpl;

public class SecuritySchemeValidatorTest {

    /*
     * Test Cases
     *
     * Negative:
     *
     * 1. SecurityScheme has no type set
     * 2. Type is set to apiKey, but name field is not set
     * 3. Type is set to apiKey, but in field is not set
     * 4. Type is set to apiKey with a Scheme field set
     * 5. apiKey type with an invalid In field - TODO
     * 6. Type is set to http, but scheme field is not set
     * 7. http type with openIdConnectUrl field set
     * 8. http type with flows field set
     * 9. Type is set to oauth2, but flows field is not set
     * 10. Type is set to oauth2 with name field set
     * 11. Type is set to openIdConnect, but openIdConnectUrl field is not set
     * 12. openIdConnect type with scheme field set
     * 12. Type is set to openIdConnect, openIdConnectUrl field is set, but not a valid url
     *
     * Positive:
     * 1. correctly set apiKey type SecurityScheme
     * 2. correctly set http type SecurityScheme
     * 3. correctly set oauth2 type SecurityScheme
     * 4. correctly set openIdConnect type SecurityScheme
     */

    String key;

    OpenAPIImpl model = new OpenAPIImpl();
    Context context = new TestValidationContextHelper(model);

    @Test
    public void testNoType() {

        SecuritySchemeImpl noType = new SecuritySchemeImpl();

        TestValidationHelper vh = new TestValidationHelper();
        SecuritySchemeValidator validator = SecuritySchemeValidator.getInstance();
        validator.validate(vh, context, key, noType);

        //Check for number of events only to keep assert statement independent of error message
        Assert.assertEquals(1, vh.getEventsSize());
    }

    @Test
    public void testNoName() {

        Type type = Type.APIKEY;
        In in = In.HEADER;

        SecuritySchemeImpl noName = new SecuritySchemeImpl();
        noName.setType(type);
        noName.setIn(in);

        TestValidationHelper vh = new TestValidationHelper();
        SecuritySchemeValidator validator = SecuritySchemeValidator.getInstance();
        validator.validate(vh, context, key, noName);

        //Check for number of events only to keep assert statement independent of error message
        Assert.assertEquals(1, vh.getEventsSize());

    }

    @Test
    public void testNoIn() {

        Type type = Type.APIKEY;

        SecuritySchemeImpl noIn = new SecuritySchemeImpl();
        noIn.setType(type);
        noIn.setName("noIn");

        TestValidationHelper vh = new TestValidationHelper();
        SecuritySchemeValidator validator = SecuritySchemeValidator.getInstance();
        validator.validate(vh, context, key, noIn);

        //Check for number of events only to keep assert statement independent of error message
        Assert.assertEquals(1, vh.getEventsSize());

    }

    @Test
    public void testApiKeyWithScheme() {

        Type type = Type.APIKEY;
        In in = In.HEADER;

        SecuritySchemeImpl apiKeyWithScheme = new SecuritySchemeImpl();
        apiKeyWithScheme.setType(type);
        apiKeyWithScheme.setName("apiKeyWithScheme");
        apiKeyWithScheme.setIn(in);
        apiKeyWithScheme.setScheme("apiKeyWithScheme");

        TestValidationHelper vh = new TestValidationHelper();
        SecuritySchemeValidator validator = SecuritySchemeValidator.getInstance();
        validator.validate(vh, context, key, apiKeyWithScheme);

        //Check for number of events only to keep assert statement independent of error message
        Assert.assertEquals(1, vh.getEventsSize());

    }

    @Test
    public void testInvalidIn() {
        //TODO SecurityScheme model has enum types for in field and no setter, but invalid values maybe set in the document
        //May need to validate the in field
    }

    @Test
    public void testHttpWithNoScheme() {

        Type type = Type.HTTP;

        SecuritySchemeImpl noScheme = new SecuritySchemeImpl();
        noScheme.setType(type);

        TestValidationHelper vh = new TestValidationHelper();
        SecuritySchemeValidator validator = SecuritySchemeValidator.getInstance();
        validator.validate(vh, context, key, noScheme);

        //Check for number of events only to keep assert statement independent of error message
        Assert.assertEquals(1, vh.getEventsSize());

    }

    @Test
    public void testHttpWithOpenIdConnectUrl() {

        Type type = Type.HTTP;

        SecuritySchemeImpl httpWithOpenIdConnectUrl = new SecuritySchemeImpl();
        httpWithOpenIdConnectUrl.setType(type);
        httpWithOpenIdConnectUrl.setScheme("httpWithOpenIdConnectUrl");
        httpWithOpenIdConnectUrl.setOpenIdConnectUrl("https://www.example.com");

        TestValidationHelper vh = new TestValidationHelper();
        SecuritySchemeValidator validator = SecuritySchemeValidator.getInstance();
        validator.validate(vh, context, key, httpWithOpenIdConnectUrl);

        //Check for number of events only to keep assert statement independent of error message
        Assert.assertEquals(1, vh.getEventsSize());

    }

    @Test
    public void testHttpWithFlows() {

        Type type = Type.HTTP;
        OAuthFlowsImpl flows = new OAuthFlowsImpl();
        OAuthFlowImpl flow = new OAuthFlowImpl();
        String url = "http://www.example.com";

        SecuritySchemeImpl httpWithFlows = new SecuritySchemeImpl();
        httpWithFlows.setType(type);
        httpWithFlows.setScheme("httpWithFlows");
        flow.setAuthorizationUrl(url);
        flows.setImplicit(flow);
        httpWithFlows.setFlows(flows);

        TestValidationHelper vh = new TestValidationHelper();
        SecuritySchemeValidator validator = SecuritySchemeValidator.getInstance();
        validator.validate(vh, context, key, httpWithFlows);

        //Check for number of events only to keep assert statement independent of error message
        Assert.assertEquals(1, vh.getEventsSize());

    }

    @Test
    public void testOauthWithNoFlows() {

        Type type = Type.OAUTH2;

        SecuritySchemeImpl noFlows = new SecuritySchemeImpl();
        noFlows.setType(type);

        TestValidationHelper vh = new TestValidationHelper();
        SecuritySchemeValidator validator = SecuritySchemeValidator.getInstance();
        validator.validate(vh, context, key, noFlows);

        //Check for number of events only to keep assert statement independent of error message
        Assert.assertEquals(1, vh.getEventsSize());

    }

    @Test
    public void testOauthWithName() {

        Type type = Type.OAUTH2;
        OAuthFlowsImpl flows = new OAuthFlowsImpl();

        SecuritySchemeImpl oauthWithName = new SecuritySchemeImpl();
        oauthWithName.setType(type);
        oauthWithName.setFlows(flows);
        oauthWithName.setName("oauthWithName");

        TestValidationHelper vh = new TestValidationHelper();
        SecuritySchemeValidator validator = SecuritySchemeValidator.getInstance();
        validator.validate(vh, context, key, oauthWithName);

        //Check for number of events only to keep assert statement independent of error message
        Assert.assertEquals(1, vh.getEventsSize());
    }

    @Test
    public void testOpenIdConnectWithNoOpenIdConnectUrl() {

        Type type = Type.OPENIDCONNECT;

        SecuritySchemeImpl noOpenIdConnectUrl = new SecuritySchemeImpl();
        noOpenIdConnectUrl.setType(type);

        TestValidationHelper vh = new TestValidationHelper();
        SecuritySchemeValidator validator = SecuritySchemeValidator.getInstance();
        validator.validate(vh, context, key, noOpenIdConnectUrl);

        //Check for number of events only to keep assert statement independent of error message
        Assert.assertEquals(1, vh.getEventsSize());

    }

    @Test
    public void testOpenIdConnectWithScheme() {

        Type type = Type.OPENIDCONNECT;
        String url = "https://www.ibm.com/ca-en/";

        SecuritySchemeImpl openIdConnectWithScheme = new SecuritySchemeImpl();
        openIdConnectWithScheme.setType(type);
        openIdConnectWithScheme.setOpenIdConnectUrl(url);
        openIdConnectWithScheme.setScheme("openIdConnectWithScheme");

        TestValidationHelper vh = new TestValidationHelper();
        SecuritySchemeValidator validator = SecuritySchemeValidator.getInstance();
        validator.validate(vh, context, key, openIdConnectWithScheme);

        //Check for number of events only to keep assert statement independent of error message
        Assert.assertEquals(1, vh.getEventsSize());
    }

    @Test
    public void testOpenIdConnectWithInvalidURL() {

        Type type = Type.OPENIDCONNECT;
        String url = "this string is an invalid url";

        SecuritySchemeImpl invalidUrl = new SecuritySchemeImpl();
        invalidUrl.setType(type);
        invalidUrl.setOpenIdConnectUrl(url);

        TestValidationHelper vh = new TestValidationHelper();
        SecuritySchemeValidator validator = SecuritySchemeValidator.getInstance();
        validator.validate(vh, context, key, invalidUrl);

        //Check for number of events only to keep assert statement independent of error message
        Assert.assertEquals(1, vh.getEventsSize());

    }

    @Test
    public void testOpenIdConnectWithRelativeURL() {

        Type type = Type.OPENIDCONNECT;
        String url = "../../myserver/security/relative-url";

        SecuritySchemeImpl invalidUrl = new SecuritySchemeImpl();
        invalidUrl.setType(type);
        invalidUrl.setOpenIdConnectUrl(url);

        TestValidationHelper vh = new TestValidationHelper();
        SecuritySchemeValidator validator = SecuritySchemeValidator.getInstance();
        validator.validate(vh, context, key, invalidUrl);

        //Check for number of events only to keep assert statement independent of error message
        Assert.assertEquals(0, vh.getEventsSize());

    }

    @Test
    public void testPositiveApiKey() {

        Type type = Type.APIKEY;
        In in = In.HEADER;

        SecuritySchemeImpl positiveApiKey = new SecuritySchemeImpl();
        positiveApiKey.setName("apiKey");
        positiveApiKey.setType(type);
        positiveApiKey.setIn(in);

        TestValidationHelper vh = new TestValidationHelper();
        SecuritySchemeValidator validator = SecuritySchemeValidator.getInstance();
        validator.validate(vh, context, key, positiveApiKey);

        //Check for number of events only to keep assert statement independent of error message
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testPositiveHttp() {

        Type type = Type.HTTP;

        SecuritySchemeImpl positiveHttp = new SecuritySchemeImpl();
        positiveHttp.setType(type);
        positiveHttp.setScheme("TestScheme");

        TestValidationHelper vh = new TestValidationHelper();
        SecuritySchemeValidator validator = SecuritySchemeValidator.getInstance();
        validator.validate(vh, context, key, positiveHttp);

        //Check for number of events only to keep assert statement independent of error message
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testPositiveOauth() {

        Type type = Type.OAUTH2;
        OAuthFlowsImpl flows = new OAuthFlowsImpl();

        SecuritySchemeImpl positiveOauth = new SecuritySchemeImpl();
        positiveOauth.setType(type);
        positiveOauth.setFlows(flows);

        TestValidationHelper vh = new TestValidationHelper();
        SecuritySchemeValidator validator = SecuritySchemeValidator.getInstance();
        validator.validate(vh, context, key, positiveOauth);

        //Check for number of events only to keep assert statement independent of error message
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testPositiveOpenIdConnect() {

        Type type = Type.OPENIDCONNECT;
        String url = "https://www.ibm.com/ca-en/";

        SecuritySchemeImpl positiveOpenIdConnect = new SecuritySchemeImpl();
        positiveOpenIdConnect.setType(type);
        positiveOpenIdConnect.setOpenIdConnectUrl(url);

        TestValidationHelper vh = new TestValidationHelper();
        SecuritySchemeValidator validator = SecuritySchemeValidator.getInstance();
        validator.validate(vh, context, key, positiveOpenIdConnect);

        //Check for number of events only to keep assert statement independent of error message
        Assert.assertEquals(0, vh.getEventsSize());
    }

}
