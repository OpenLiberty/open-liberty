/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.test.schema;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import io.openliberty.mcp.internal.schemas.SchemaDirection;
import io.openliberty.mcp.internal.schemas.SchemaRegistry;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.annotation.JsonbProperty;

/**
 * Test our assumptions about JSON-B's default mapping for classes and test we generate an appropriate schema
 */
public class JsonbMappingTest {

    private static Jsonb jsonb = JsonbBuilder.create();
    private static SchemaRegistry registry;

    @BeforeClass
    public static void setup() {
        registry = new SchemaRegistry();
    }

    // -------------------------------

    public static class FieldBean {
        public static String staticField = "staticValue";
        public String publicField;
        String packageField;
        protected String protectedField;
        private String privateField;
        @JsonbProperty("privateAnnotated")
        private String privateAnnotated;
    }

    @Test
    public void onlyPublicFieldsSerialized() {
        FieldBean fieldBean = new FieldBean();
        fieldBean.publicField = "a";
        fieldBean.packageField = "b";
        fieldBean.protectedField = "c";
        fieldBean.privateField = "d";
        fieldBean.privateAnnotated = "e";

        String expectedJson = """
                        {
                            "publicField": "a"
                        }
                        """;

        JSONAssert.assertEquals(expectedJson, jsonb.toJson(fieldBean), true);

        FieldBean beanFromJson = jsonb.fromJson("""
                        {
                        "publicField": "a",
                        "packageField": "b",
                        "protectedField": "c",
                        "privateField": "d",
                        "privateAnnotated": "e"
                        }
                        """, FieldBean.class);
        assertEquals("a", beanFromJson.publicField);
        assertNull(beanFromJson.packageField);
        assertNull(beanFromJson.protectedField);
        assertNull(beanFromJson.privateField);
        assertNull(beanFromJson.privateAnnotated);

        String expectedSchema = """
                        {
                            "type": "object",
                            "properties": {
                                "publicField": {
                                    "type": "string"
                                }
                            },
                            "required": [
                                "publicField"
                            ]
                        }
                        """;
        JSONAssert.assertEquals(expectedSchema,
                                registry.getSchema(FieldBean.class, SchemaDirection.INPUT).toString(),
                                true);

        JSONAssert.assertEquals(expectedSchema,
                                registry.getSchema(FieldBean.class, SchemaDirection.OUTPUT).toString(),
                                true);
    }

    // -------------------------------

    public static class SubTypeFieldBean extends SuperTypeFieldBean {
        public String subTypeField;
    }

    public static class SuperTypeFieldBean {
        public String superTypeField;
    }

    @Test
    public void superTypeFieldsSerialized() {
        SubTypeFieldBean bean = new SubTypeFieldBean();
        bean.subTypeField = "sub";
        bean.superTypeField = "super";

        String expectedJson = """
                        {
                            "subTypeField": "sub",
                            "superTypeField": "super"
                        }
                        """;
        JSONAssert.assertEquals(expectedJson, jsonb.toJson(bean), true);

        SubTypeFieldBean beanFromJson = jsonb.fromJson("""
                        {
                        "subTypeField": "sub",
                        "superTypeField": "super"
                        }
                        """, SubTypeFieldBean.class);

        assertEquals("sub", beanFromJson.subTypeField);
        assertEquals("super", beanFromJson.superTypeField);

        String expectedSchema = """
                        {
                            "type": "object",
                            "properties": {
                                "subTypeField": {
                                    "type": "string"
                                },
                                "superTypeField": {
                                    "type": "string"
                                },
                            },
                            "required": [
                                "subTypeField",
                                "superTypeField",
                            ]
                        }""";
        JSONAssert.assertEquals(expectedSchema,
                                registry.getSchema(SubTypeFieldBean.class, SchemaDirection.INPUT).toString(),
                                true);
        JSONAssert.assertEquals(expectedSchema,
                                registry.getSchema(SubTypeFieldBean.class, SchemaDirection.OUTPUT).toString(),
                                true);
    }

    // -------------------------------

    @SuppressWarnings("unused")
    public static class MethodHidesBean {
        public String field;

        private String getField() {
            return field;
        }
    }

    @Test
    public void privateMethodHidesField() {
        MethodHidesBean bean = new MethodHidesBean();
        bean.field = "a";

        // Field not read because getter is private
        String expectedJson = """
                        {
                        }
                        """;
        JSONAssert.assertEquals(expectedJson, jsonb.toJson(bean), true);

        // Field is set because field is public and there's no setter
        MethodHidesBean beanFromJson = jsonb.fromJson("""
                        {
                            "field": "a"
                        }
                        """, MethodHidesBean.class);
        assertEquals("a", beanFromJson.field);

        String expectedOutputSchema = """
                        {
                            "type": "object",
                            "properties": {
                            },
                            "required": [
                            ]
                        }
                        """;
        JSONAssert.assertEquals(expectedOutputSchema,
                                registry.getSchema(MethodHidesBean.class, SchemaDirection.OUTPUT).toString(),
                                true);

        String expectedInputSchema = """
                        {
                            "type": "object",
                            "properties": {
                                "field": {
                                    "type": "string"
                                }
                            },
                            "required": [
                                "field"
                            ]
                        }
                        """;
        JSONAssert.assertEquals(expectedInputSchema,
                                registry.getSchema(MethodHidesBean.class, SchemaDirection.INPUT).toString(),
                                true);
    }

    // -------------------------------

    public static class FieldRenamedBean {
        @JsonbProperty("altField")
        public String field1;
        public String field2;

        public String getField1() {
            return field2;
        }

        public void setField1(String field) {
            this.field2 = field;
        }
    }

    @Test
    public void fieldRenamed() {
        FieldRenamedBean bean = new FieldRenamedBean();
        bean.field1 = "a";
        bean.field2 = "b";

        // The JsonbProperty on field1, sets the name used for the getter and setter,
        // even though they don't set that field
        String expectedJson = """
                        {
                            "altField": "b",
                            "field2": "b",
                        }
                        """;
        JSONAssert.assertEquals(expectedJson, jsonb.toJson(bean), true);

        // field2 is set both directly and from setField1
        // field1 is never set
        FieldRenamedBean beanFromJson = jsonb.fromJson("""
                        {
                        "field1": "a",
                        "field2": "b",
                        "altField": "c"
                        }
                        """, FieldRenamedBean.class);
        assertEquals("c", beanFromJson.field2);
        assertNull(beanFromJson.field1);

        String expectedSchema = """
                        {
                            "type": "object",
                            "properties": {
                                "altField": {
                                    "type": "string"
                                },
                                "field2": {
                                    "type": "string"
                                },
                            },
                            "required": [
                                "altField",
                                "field2"
                            ]
                        }
                        """;
        JSONAssert.assertEquals(expectedSchema,
                                registry.getSchema(FieldRenamedBean.class, SchemaDirection.OUTPUT).toString(),
                                true);
        JSONAssert.assertEquals(expectedSchema,
                                registry.getSchema(FieldRenamedBean.class, SchemaDirection.INPUT).toString(),
                                true);
    }

    // -------------------------------

    public static class IsGetterBean {
        private boolean bool;
        private String string;
        private boolean boolBoth;
        private String stringBoth;

        public boolean isBool() {
            return bool;
        }

        public void setBool(boolean bool) {
            this.bool = bool;
        }

        public String isString() {
            return string;
        }

        public void setString(String string) {
            this.string = string;
        }

        public boolean isBoolBoth() {
            return boolBoth;
        }

        public boolean getBoolBoth() {
            return false;
        }

        public void setBoolBoth(boolean boolBoth) {
            this.boolBoth = boolBoth;
        }

        @JsonbProperty("altStringBoth") // Ignored because getStringBoth is used
        public String isStringBoth() {
            return "stringBothFromIs";
        }

        public String getStringBoth() {
            return "stringBothFromGet";
        }

        public void setStringBoth(String stringBoth) {
            this.stringBoth = stringBoth;
        }
    }

    @Test
    public void testIsGetter() {
        IsGetterBean bean = new IsGetterBean();
        bean.bool = true;
        bean.string = "value";
        bean.stringBoth = "foo";
        bean.boolBoth = true;

        // isGetter works for booleans and non-booleans
        // If isGetter and setGetter are both present, setGetter is preferred
        String expectedJson = """
                        {
                            "bool": true,
                            "boolBoth": false,
                            "string": "value",
                            "stringBoth": "stringBothFromGet",
                        }
                        """;
        JSONAssert.assertEquals(expectedJson, jsonb.toJson(bean), true);

        IsGetterBean beanFromJson = jsonb.fromJson("""
                        {
                            "bool": true,
                            "boolBoth": true,
                            "string": "value",
                            "stringBoth": "value2"
                        }
                        """, IsGetterBean.class);
        assertEquals(true, beanFromJson.bool);
        assertEquals(true, beanFromJson.boolBoth);
        assertEquals("value", beanFromJson.string);
        assertEquals("value2", beanFromJson.stringBoth);

        String expectedSchema = """
                        {
                            "type": "object",
                            "properties": {
                                "bool": {
                                    "type": "boolean"
                                },
                                "boolBoth": {
                                    "type": "boolean"
                                },
                                "string": {
                                    "type": "string"
                                },
                                "stringBoth": {
                                    "type": "string"
                                },
                            },
                            "required": [
                                "bool",
                                "boolBoth",
                                "string",
                                "stringBoth",
                            ]
                        }
                        """;
        JSONAssert.assertEquals(expectedSchema,
                                registry.getSchema(IsGetterBean.class, SchemaDirection.OUTPUT).toString(),
                                JSONCompareMode.NON_EXTENSIBLE);
        JSONAssert.assertEquals(expectedSchema,
                                registry.getSchema(IsGetterBean.class, SchemaDirection.INPUT).toString(),
                                JSONCompareMode.NON_EXTENSIBLE);
    }

    // -------------------------------

    public interface AnnotatedInterface {
        @JsonbProperty("altField")
        public String getField();
    }

    public static class AnnotatedImplementation {
        public String field;

        public String getField() {
            return field;
        }
    }

    @Test
    public void testAnnotatedInterface() {
        AnnotatedImplementation bean = new AnnotatedImplementation();
        bean.field = "value";

        // Annotation on interface method is ignored because it's overridden
        String expectedJson = """
                        {
                            "field": "value",
                        }
                        """;
        JSONAssert.assertEquals(expectedJson, jsonb.toJson(bean), true);

        AnnotatedImplementation beanFromJson = jsonb.fromJson("""
                        {
                            "field": "value"
                        }
                        """, AnnotatedImplementation.class);
        assertEquals("value", beanFromJson.field);
    }

    // -------------------------------

    public interface DefaultInterface {
        public default String getField() {
            return "constantString";
        }

        @JsonbProperty("altField2")
        public default String getField2() {
            return "constantString2";
        }
    }

    public interface BoringInterface extends DefaultInterface {};

    public static class DefaultImplementation implements BoringInterface {}

    @Test
    public void testDefaultMethod() {
        DefaultImplementation bean = new DefaultImplementation();

        // Annotation on interface method is ignored because it's overridden
        String expectedJson = """
                        {
                            "field": "constantString",
                            "altField2": "constantString2",
                        }
                        """;
        JSONAssert.assertEquals(expectedJson, jsonb.toJson(bean), true);

        String expectedOutputSchema = """
                        {
                            "type": "object",
                            "properties": {
                                "field": {
                                    "type": "string"
                                },
                                "altField2": {
                                    "type": "string"
                                },
                            },
                            "required": [
                                "field",
                                "altField2",
                            ]
                        }
                        """;
        JSONAssert.assertEquals(expectedOutputSchema,
                                registry.getSchema(DefaultImplementation.class, SchemaDirection.OUTPUT).toString(),
                                JSONCompareMode.NON_EXTENSIBLE);

        String expectedInputSchema = """
                        {
                            "type": "object",
                            "properties": {
                            },
                            "required": [
                            ]
                        }
                        """;
        JSONAssert.assertEquals(expectedInputSchema,
                                registry.getSchema(DefaultImplementation.class, SchemaDirection.INPUT).toString(),
                                JSONCompareMode.NON_EXTENSIBLE);

    }

    public static class BoxMap<K, V, T> {
        K var1;
        V var2;
        T var3;

        /**
         * @param var1
         * @param var2
         * @param var3
         */
        public BoxMap() {

        }

        /**
         * @param var1
         * @param var2
         * @param var3
         */
        public BoxMap(K var1, V var2, T var3) {
            super();
            this.var1 = var1;
            this.var2 = var2;
            this.var3 = var3;
        }

        /**
         * @return the var1
         */
        public K getVar1() {
            return var1;
        }

        /**
         * @param var1 the var1 to set
         */
        public void setVar1(K var1) {
            this.var1 = var1;
        }

        /**
         * @return the var2
         */
        public V getVar2() {
            return var2;
        }

        /**
         * @param var2 the var2 to set
         */
        public void setVar2(V var2) {
            this.var2 = var2;
        }

        /**
         * @return the var3
         */
        public T getVar3() {
            return var3;
        }

        /**
         * @param var3 the var3 to set
         */
        public void setVar3(T var3) {
            this.var3 = var3;
        }

    }

    public static class ContainerMap<X> {
        BoxMap<X, String, Integer> bm;

        public ContainerMap() {}

        /**
         * @param bm
         */
        public ContainerMap(BoxMap<X, String, Integer> bm) {
            this.bm = bm;
        }

        /**
         * @return the bm
         */
        public BoxMap<X, String, Integer> getBm() {
            return bm;
        }

        /**
         * @param bm the bm to set
         */
        public void setBm(BoxMap<X, String, Integer> bm) {
            this.bm = bm;
        }

    }

    public static class ContainerConcrete {
        public ContainerMap<String> cm;

        /**
         */
        public ContainerConcrete() {}

        /**
         * @param cm
         */
        public ContainerConcrete(ContainerMap<String> cm) {
            this.cm = cm;
        }

        /**
         * @return the cm
         */
        public ContainerMap<String> getCm() {
            return cm;
        }

        /**
         * @param cm the cm to set
         */
        public void setCm(ContainerMap<String> cm) {
            this.cm = cm;
        }

    }

    @Test
    public void testGeneric() {
        BoxMap<String, String, Integer> bm = new BoxMap<>("1str", "2str", 3);
        JSONAssert.assertEquals("{\"var1\":\"1str\",\"var2\":\"2str\",\"var3\":3}", jsonb.toJson(bm), true);
        BoxMap<String, String, Integer> bmIn = jsonb.fromJson("{\"var1\":\"1str\",\"var2\":\"2str\",\"var3\":3}", new TypeLiteral<BoxMap<String, String, Integer>>() {
            private static final long serialVersionUID = 1L;
        }.getType());
        JSONAssert.assertEquals("{\"var1\":\"1str\",\"var2\":\"2str\",\"var3\":3}", jsonb.toJson(bmIn), true);
        JSONAssert.assertEquals("{\"type\":\"object\",\"properties\":{\"var3\":{\"type\":\"object\"},\"var2\":{\"type\":\"object\"},\"var1\":{\"type\":\"object\"}},\"required\":[\"var3\",\"var2\",\"var1\"]}",
                                registry.getSchema(BoxMap.class, SchemaDirection.INPUT).toString(), true);

        ContainerMap<String> cm = new ContainerMap<>(bm);
        JSONAssert.assertEquals("{\"bm\":{\"var1\":\"1str\",\"var2\":\"2str\",\"var3\":3}}", jsonb.toJson(cm), true);
        ContainerMap<String> cmIn = jsonb.fromJson("{\"bm\":{\"var1\":\"1str\",\"var2\":\"2str\",\"var3\":3}}", new TypeLiteral<ContainerMap<String>>() {
            private static final long serialVersionUID = 1L;
        }.getType());
        JSONAssert.assertEquals("{\"bm\":{\"var1\":\"1str\",\"var2\":\"2str\",\"var3\":3}}", jsonb.toJson(cmIn), true);
        JSONAssert.assertEquals("{\"type\":\"object\",\"properties\":{\"bm\":{\"type\":\"object\",\"properties\":{\"var3\":{\"type\":\"integer\"},\"var2\":{\"type\":\"string\"},\"var1\":{\"type\":\"object\"}},\"required\":[\"var3\",\"var2\",\"var1\"]}},\"required\":[\"bm\"]}",
                                registry.getSchema(ContainerMap.class, SchemaDirection.INPUT).toString(), true);

        ContainerConcrete cc = new ContainerConcrete(cm);
        JSONAssert.assertEquals("{\"cm\":{\"bm\":{\"var1\":\"1str\",\"var2\":\"2str\",\"var3\":3}}}", jsonb.toJson(cc), true);
        ContainerConcrete ccIn = jsonb.fromJson("{\"cm\":{\"bm\":{\"var1\":\"1str\",\"var2\":\"2str\",\"var3\":3}}}", ContainerConcrete.class);
        JSONAssert.assertEquals("{\"cm\":{\"bm\":{\"var1\":\"1str\",\"var2\":\"2str\",\"var3\":3}}}", jsonb.toJson(ccIn), true);
        JSONAssert.assertEquals("{\"type\":\"object\",\"properties\":{\"cm\":{\"type\":\"object\",\"properties\":{\"bm\":{\"type\":\"object\",\"properties\":{\"var3\":{\"type\":\"integer\"},\"var2\":{\"type\":\"string\"},\"var1\":{\"type\":\"string\"}},\"required\":[\"var3\",\"var2\",\"var1\"]}},\"required\":[\"bm\"]}},\"required\":[\"cm\"]}",
                                registry.getSchema(ContainerConcrete.class, SchemaDirection.INPUT).toString(), true);

    }

    public static class MyClass<U> {
        public List<U> foo;
    }

    public static class MyClass2<T> extends MyClass<T> {
        public List<T> bar;
    }

    public static class ChildClass extends MyClass2<String> {};

    public static class Concrete {
        public ChildClass cc;
    };

    @Test
    public void testInheritedGenerics() {
        ChildClass myClass = new ChildClass();
        myClass.foo = List.of("a", "b", "c");
        myClass.bar = List.of("d", "e", "f");

        String expectedJson = """
                        {
                            "foo": ["a", "b", "c"],
                            "bar": ["d", "e", "f"]
                        }
                        """;

        JSONAssert.assertEquals(expectedJson,
                                jsonb.toJson(myClass), true);

        String expectedSchema = "{\"type\":\"object\",\"properties\":{\"bar\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}},\"foo\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}}},\"required\":[\"bar\",\"foo\"]}";
        JSONAssert.assertEquals(expectedSchema,
                                registry.getSchema(ChildClass.class, SchemaDirection.INPUT).toString(), true);

        ChildClass cmIn = jsonb.fromJson("{\"foo\":[\"a\",\"b\",\"c\"]}", ChildClass.class);
        JSONAssert.assertEquals("{\"foo\":[\"a\",\"b\",\"c\"]}", jsonb.toJson(cmIn), true);

    }

}
