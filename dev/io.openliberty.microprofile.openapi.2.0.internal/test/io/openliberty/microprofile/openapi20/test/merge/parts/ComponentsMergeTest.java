/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package io.openliberty.microprofile.openapi20.test.merge.parts;

import static io.openliberty.microprofile.openapi20.test.merge.AssertModel.assertModelMaps;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.Constructible;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.callbacks.Callback;
import org.eclipse.microprofile.openapi.models.examples.Example;
import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.links.Link;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;
import org.junit.Assert;
import org.junit.Test;

public class ComponentsMergeTest {

    @Test
    public void testComponentsMerge() {
        try {
            testComponentsItem(Example.class);
            testComponentsItem(Callback.class);
            testComponentsItem(Header.class);
            testComponentsItem(Link.class);
            testComponentsItem(RequestBody.class);
            testComponentsItem(Parameter.class);
            testComponentsItem(SecurityScheme.class);
            testComponentsItem(Schema.class);
            testComponentsItem(APIResponse.class);
            testComponentsItem(Object.class);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage() != null ? e.getMessage() : "Test failed: " + e);
        }

    }

    @SuppressWarnings("unchecked")
    public <T> void testComponentsItem(Class<T> clazz) throws Exception {
        OpenAPI primaryOpenAPI;
        Components components = OASFactory.createComponents();

        Method setter = findSetter(clazz);

        Map<String, T> map1 = getTestMap(clazz, "test", "test1", "test2");
        setter.invoke(components, map1);

        OpenAPI doc1 = OASFactory.createOpenAPI();
        doc1.setComponents(components);

        components = OASFactory.createComponents();

        Map<String, T> map2 = getTestMap(clazz, "test", "test3", "test4");
        setter.invoke(components, map2);

        OpenAPI doc2 = OASFactory.createOpenAPI();
        doc2.setComponents(components);

        components = OASFactory.createComponents();
        Map<String, T> map3 = getTestMap(clazz, "test", "test5", "test6");
        setter.invoke(components, map3);

        OpenAPI doc3 = OASFactory.createOpenAPI();
        doc3.setComponents(components);

        Method getter = findGetter(clazz);

        Map<String, T> doc1Map = (Map<String, T>) getter.invoke(doc1.getComponents());
        Map<String, T> doc2Map = (Map<String, T>) getter.invoke(doc2.getComponents());
        Map<String, T> doc3Map = (Map<String, T>) getter.invoke(doc3.getComponents());

        primaryOpenAPI = TestUtil.merge(doc1);

        Assert.assertNotNull(primaryOpenAPI.getComponents());
        Map<String, T> primaryOpenAPIMap = (Map<String, T>) getter.invoke(primaryOpenAPI.getComponents());
        Assert.assertNotNull(primaryOpenAPIMap);

        Assert.assertEquals(3, primaryOpenAPIMap.size());

        validateMap(primaryOpenAPIMap, doc1Map);

        primaryOpenAPI = TestUtil.merge(doc1, doc2);
        primaryOpenAPIMap = (Map<String, T>) getter.invoke(primaryOpenAPI.getComponents());

        Assert.assertNotNull(primaryOpenAPIMap);
        Assert.assertEquals(5, primaryOpenAPIMap.size());
        validateMap(primaryOpenAPIMap, doc1Map, doc2Map);

        primaryOpenAPI = TestUtil.merge(doc1, doc2, doc3);
        primaryOpenAPIMap = (Map<String, T>) getter.invoke(primaryOpenAPI.getComponents());

        Assert.assertNotNull(primaryOpenAPIMap);
        Assert.assertEquals(7, primaryOpenAPIMap.size());
        validateMap(primaryOpenAPIMap, doc1Map, doc2Map, doc3Map);

        primaryOpenAPI = TestUtil.merge(doc1, doc3);
        primaryOpenAPIMap = (Map<String, T>) getter.invoke(primaryOpenAPI.getComponents());

        Assert.assertNotNull(primaryOpenAPIMap);
        Assert.assertEquals(5, primaryOpenAPIMap.size());
        validateMap(primaryOpenAPIMap, doc1Map, doc3Map);

        primaryOpenAPI = TestUtil.merge(doc3);
        primaryOpenAPIMap = (Map<String, T>) getter.invoke(primaryOpenAPI.getComponents());

        Assert.assertNotNull(primaryOpenAPIMap);
        Assert.assertEquals(3, primaryOpenAPIMap.size());
        validateMap(primaryOpenAPIMap, doc3Map);

        primaryOpenAPI = TestUtil.merge();

        //no data in components anymore should have been set to null
        Assert.assertNull(primaryOpenAPI.getComponents());

    }

    private void validateMap(Map<String, ?> map, Map<?, ?>... maps) {
        Map<Object, Object> expectedMap = new HashMap<>();
        for (Map<?, ?> m : maps) {
            expectedMap.putAll(m);
        }
        assertModelMaps(expectedMap, map);
    }

    @SuppressWarnings("unchecked")
    private <T> Map<String, T> getTestMap(Class<T> clazz, String... keys) {
        Map<String, T> map = new HashMap<>();
        try {
            for (String key : keys) {
                if (clazz.equals(Object.class)) {
                    key = "x-" + key;
                    map.put(key, (T) clazz);
                } else {
                    map.put(key, (T) OASFactory.createObject((Class<? extends Constructible>) clazz));
                }
            }
        } catch (Exception e) {
            Assert.fail("Failed to create test map");
        }
        return map;
    }

    private Method findSetter(Class<?> clazz) {
        Method setter = Arrays.asList(Components.class.getMethods()).stream().filter(method -> method.getName().startsWith("set") &&
                                                                                               method.getGenericParameterTypes()[0].toString().contains(clazz.getName()))
                              .findFirst().get();
        return setter;
    }

    private Method findGetter(Class<?> clazz) {
        Method setter = Arrays.asList(Components.class.getMethods()).stream().filter(method -> method.getName().startsWith("get") &&
                                                                                               method.getGenericReturnType().toString().contains(clazz.getName()))
                              .findFirst().get();
        return setter;
    }
}
