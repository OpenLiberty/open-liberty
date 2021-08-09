/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.paramconverter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;

import com.ibm.ws.jaxrs.fat.paramconverter.annotations.NoPublicConstructorListObjectParam;
import com.ibm.ws.jaxrs.fat.paramconverter.annotations.NoPublicConstructorObjectParam;
import com.ibm.ws.jaxrs.fat.paramconverter.annotations.StringArrayParam;
import com.ibm.ws.jaxrs.fat.paramconverter.annotations.StringListParam;
import com.ibm.ws.jaxrs.fat.paramconverter.annotations.StringMapParam;
import com.ibm.ws.jaxrs.fat.paramconverter.annotations.StringSetParam;
import com.ibm.ws.jaxrs.fat.paramconverter.annotations.StringSortedSetParam;
import com.ibm.ws.jaxrs.fat.paramconverter.annotations.TestListObjectParam;
import com.ibm.ws.jaxrs.fat.paramconverter.annotations.TestObjectParam;
import com.ibm.ws.jaxrs.fat.paramconverter.objects.NoPublicConstructorListObject;
import com.ibm.ws.jaxrs.fat.paramconverter.objects.NoPublicConstructorObject;
import com.ibm.ws.jaxrs.fat.paramconverter.objects.TestListObject;
import com.ibm.ws.jaxrs.fat.paramconverter.objects.TestObject;

@Provider
public class TestParamConverterProvider implements ParamConverterProvider {

    private static class StringArrayParamConverter implements ParamConverter<String[]> {
        static final StringArrayParamConverter INSTANCE = new StringArrayParamConverter();

        @Override
        public String[] fromString(final String value) {
            System.out.println("StringArrayParamConverter.fromString() value=" + value);
            return value.split(",");
        }

        @Override
        public String toString(final String[] value) {
            String string = "";
            for (String s : value) {
                if (string == "") {
                    string = s;
                } else {
                    string += "," + s;
                }
            }
            return string;
        }
    }

    private static class StringListParamConverter implements ParamConverter<List<String>> {
        static final StringListParamConverter INSTANCE = new StringListParamConverter();

        @Override
        public List<String> fromString(final String value) {
            System.out.println("StringListParamConverter.fromString() value=" + value);
            return Arrays.asList(value.split(","));
        }

        @Override
        public String toString(final List<String> value) {
            String string = "";
            for (String s : value) {
                if (string == "") {
                    string = s;
                } else {
                    string += "," + s;
                }
            }
            return string;
        }
    }

    private static class StringSetParamConverter implements ParamConverter<Set<String>> {
        static final StringSetParamConverter INSTANCE = new StringSetParamConverter();

        @Override
        public Set<String> fromString(final String value) {
            System.out.println("StringSetParamConverter.fromString() value=" + value);
            return new HashSet<String>(Arrays.asList(value.split(",")));
        }

        @Override
        public String toString(final Set<String> value) {
            String string = "";
            for (String s : value) {
                if (string == "") {
                    string = s;
                } else {
                    string += "," + s;
                }
            }
            return string;
        }
    }

    private static class StringSortedSetParamConverter implements ParamConverter<SortedSet<String>> {
        static final StringSortedSetParamConverter INSTANCE = new StringSortedSetParamConverter();

        @Override
        public SortedSet<String> fromString(final String value) {
            System.out.println("StringSortedSetParamConverter.fromString() value=" + value);
            return new TreeSet<String>(Arrays.asList(value.split(",")));
        }

        @Override
        public String toString(final SortedSet<String> value) {
            String string = "";
            for (String s : value) {
                if (string == "") {
                    string = s;
                } else {
                    string += "," + s;
                }
            }
            return string;
        }
    }

    private static class StringMapParamConverter implements ParamConverter<Map<String, String>> {
        static final StringMapParamConverter INSTANCE = new StringMapParamConverter();

        @Override
        public Map<String, String> fromString(final String value) {
            System.out.println("StringMapParamConverter.fromString() value=" + value);
            Map<String, String> map = new HashMap<String, String>();
            map.put("a", "3");
            map.put("b", "1");
            map.put("c", "2");
            return map;
        }

        @Override
        public String toString(final Map<String, String> value) {
            String string = "";
            return string;
        }
    }

    private static class TestObjectParamConverter implements ParamConverter<TestObject> {
        static final TestObjectParamConverter INSTANCE = new TestObjectParamConverter();

        @Override
        public TestObject fromString(final String value) {
            System.out.println("TestObjectParamConverter.fromString() value=" + value);
            TestObject obj = new TestObject();
            obj.content = value;
            return obj;
        }

        @Override
        public String toString(final TestObject value) {
            return value.content;
        }
    }

    private static class TestListObjectParamConverter implements ParamConverter<TestListObject> {
        static final TestListObjectParamConverter INSTANCE = new TestListObjectParamConverter();

        @Override
        public TestListObject fromString(final String value) {
            System.out.println("TestListObjectParamConverter.fromString() value=" + value);
            TestListObject obj = new TestListObject();
            String[] values = value.split(",");
            for (String v : values) {
                obj.add(v);
            }
            return obj;
        }

        @Override
        public String toString(final TestListObject value) {
            String string = "";
            for (String s : value) {
                if (string == "") {
                    string = s;
                } else {
                    string += "," + s;
                }
            }
            return string;
        }
    }

    private static class NoPublicConstructorObjectParamConverter implements ParamConverter<NoPublicConstructorObject> {
        static final NoPublicConstructorObjectParamConverter INSTANCE = new NoPublicConstructorObjectParamConverter();

        @Override
        public NoPublicConstructorObject fromString(final String value) {
            System.out.println("NoPublicConstructorObjectParamConverter.fromString() value=" + value);
            NoPublicConstructorObject obj = NoPublicConstructorObject.getInstance();
            obj.content = value;
            return obj;
        }

        @Override
        public String toString(final NoPublicConstructorObject value) {
            return value.content;
        }
    }

    private static class NoPublicConstructorListObjectParamConverter implements ParamConverter<NoPublicConstructorListObject> {
        static final NoPublicConstructorListObjectParamConverter INSTANCE = new NoPublicConstructorListObjectParamConverter();

        @Override
        public NoPublicConstructorListObject fromString(final String value) {
            System.out.println("NoPublicConstructorListObjectParamConverter.fromString() value=" + value);
            NoPublicConstructorListObject obj = NoPublicConstructorListObject.getInstance();
            String[] values = value.split(",");
            for (String v : values) {
                obj.add(v);
            }
            return obj;
        }

        @Override
        public String toString(final NoPublicConstructorListObject value) {
            String string = "";
            for (String s : value) {
                if (string == "") {
                    string = s;
                } else {
                    string += "," + s;
                }
            }
            return string;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> ParamConverter<T> getConverter(final Class<T> rawType, final Type genericType, final Annotation[] annotations) {
        System.out.println("TestParamConverterProvider.getConverter rawType=" + rawType);
        System.out.println("TestParamConverterProvider.getConverter genericType=" + genericType);
        for (final Annotation annotation : annotations) {
            if (StringArrayParam.class.isInstance(annotation)) {
                return (ParamConverter<T>) StringArrayParamConverter.INSTANCE;
            } else if (StringListParam.class.isInstance(annotation)) {
                return (ParamConverter<T>) StringListParamConverter.INSTANCE;
            } else if (StringSetParam.class.isInstance(annotation)) {
                return (ParamConverter<T>) StringSetParamConverter.INSTANCE;
            } else if (StringSortedSetParam.class.isInstance(annotation)) {
                return (ParamConverter<T>) StringSortedSetParamConverter.INSTANCE;
            } else if (StringMapParam.class.isInstance(annotation)) {
                return (ParamConverter<T>) StringMapParamConverter.INSTANCE;
            } else if (TestObjectParam.class.isInstance(annotation)) {
                return (ParamConverter<T>) TestObjectParamConverter.INSTANCE;
            } else if (TestListObjectParam.class.isInstance(annotation)) {
                return (ParamConverter<T>) TestListObjectParamConverter.INSTANCE;
            } else if (NoPublicConstructorObjectParam.class.isInstance(annotation)) {
                return (ParamConverter<T>) NoPublicConstructorObjectParamConverter.INSTANCE;
            } else if (NoPublicConstructorListObjectParam.class.isInstance(annotation)) {
                return (ParamConverter<T>) NoPublicConstructorListObjectParamConverter.INSTANCE;
            }
        }
        return null;
    }
}
