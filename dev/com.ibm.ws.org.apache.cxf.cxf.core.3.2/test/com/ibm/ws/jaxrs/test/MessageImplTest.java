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
package com.ibm.ws.jaxrs.test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.junit.Test;

import junit.framework.Assert;

public class MessageImplTest {
    private static String[] methodSuffixes = new String[] { "ContentType", "ProtocolHeaders", "QueryString", "HttpRequest", "HttpResponse",
                                                            "PathToMatchSlash", "HttpRequestMethod", "InterceptorProviders", "TemplateParameters",
                                                            "Accept", "ContinuationProvider", "Destination", "OperationResourceInfoStack", "WsdlDescription", 
                                                            "WsdlInterface", "WsdlOperation", "WsdlPort", "WsdlService", "RequestUrl", "RequestUri", 
                                                            "PathInfo", "BasePath", "FixedParamOrder", "InInterceptors", "OutInterceptors", "ResponseCode",
                                                            "Attachments", "Encoding", "HttpContext", "HttpConfig", "HttpContextMatchStrategy", "HttpBasePath",
                                                            "AsyncPostDispatch", "SecurityContext", "AuthorizationPolicy", "CertConstraints", 
                                                            "ServiceRedirection", "HttpServletResponse", "ResourceMethod", "OneWayRequest", "AsyncResponse",
                                                            "ThreadContextSwitched", "CacheInputProperty", "PreviousMessage", "ResponseHeadersCopied", 
                                                            "SseEventSink", "RequestorRole", "PartialResponse", "EmptyPartialResponse", "EndpointAddress", 
                                                            "InboundMessage" };
                  
    @Test
    public void testMessageImpl() throws Exception {
        
        MessageImpl message = new MessageImpl();
        assertEquals("Need to update MessageImplTest.methodSuffixes array to match MessageImpl.propertyNames", message.getPropertyNames().length, methodSuffixes.length);
        String[] propertyNames = message.getPropertyNames();
        Method[] allMethods = MessageImpl.class.getMethods();
        HashMap<String,Method> getterMap = new HashMap<String,Method>();
        HashMap<String,Method> setterMap = new HashMap<String,Method>();
        HashMap<String,Method> removeMap = new HashMap<String,Method>();
        HashMap<String,Method> containMap = new HashMap<String,Method>();
        for (Method m : allMethods) {
            String methodName = m.getName();
            if (!methodName.endsWith("ContextualPropertyKeys") && !methodName.endsWith("ContextualProperty") &&
                            !methodName.endsWith("ContentFormats") && !methodName.endsWith("Content") && 
                            !methodName.endsWith("Exchange") && !methodName.endsWith("Id") && 
                            !methodName.endsWith("InterceptorChain") && 
                            !methodName.endsWith("PropertyNames") && !methodName.endsWith("Class") && 
                            !methodName.endsWith("OrDefault") && !methodName.endsWith("AttachmentMimeType")) {
                if (methodName.startsWith("get") && methodName.length() > 3) {
                    getterMap.put(methodName, m);
                } else if (methodName.startsWith("set") && methodName.length() > 3) {
                    setterMap.put(methodName, m);
                } else if (methodName.startsWith("remove") && methodName.length() > 6) {
                    removeMap.put(methodName, m);
                } else if (methodName.startsWith("contains") && methodName.length() > 8) {
                    containMap.put(methodName, m);
                }
            }
        }

        for (String methodName : getterMap.keySet()) {
            String trimmedMethod = methodName.substring(3);
            String setMethodName = "set".concat(trimmedMethod);
            String removeMethodName = "remove".concat(trimmedMethod);
            String containsMethodName = "contains".concat(trimmedMethod);
              
            Method getter = getterMap.get(methodName);
            Method setter = setterMap.get(setMethodName);
            Method remove = removeMap.get(removeMethodName);
            Method contains = containMap.get(containsMethodName);
            Class<?> type = setter.getParameterTypes()[0];
            for (int i = 0; i < methodSuffixes.length; i++) {
                String s = methodSuffixes[i];
                if (s.equals(trimmedMethod)) {
                    exerciseMethods(message, propertyNames[i], getter, setter, remove, contains, type);
                    break;
                }
            }
        }
    }    
 
    @SuppressWarnings({ "unchecked", "serial" })
    private void exerciseMethods(MessageImpl message, String propertyKey, Method getter, Method setter, Method remove, Method contains, Class<?> type) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        assertNull(message.get(propertyKey));
        assertNull(getter.invoke(message, new Object[] {}));
        assertFalse(message.containsKey(propertyKey));
        if (contains != null) {
            assertFalse((boolean) contains.invoke(message, new Object[] {}));
        } else {
            assertFalse(message.containsKey(propertyKey));
        }
        Object value = getValue(type, "123");
        assertNull(message.put(propertyKey, value));
        assertEquals(value, message.get(propertyKey));

        Object value1 = getValue(type, "abc");
        assertEquals(value, message.put(propertyKey, value1));
        assertEquals(value1, getter.invoke(message, new Object[] {}));
        value = getValue(type, "xyz");
        setter.invoke(message, new Object[] { value });
        assertEquals(value, message.get(propertyKey));
        assertTrue(message.containsKey(propertyKey));
        if (contains != null) {
            assertTrue((boolean) contains.invoke(message, new Object[] {}));
        } else {
            assertTrue(message.containsKey(propertyKey));
        }
        if (remove != null) {
            remove.invoke(message, new Object[] {});
        } else {
            message.remove(propertyKey);
        }
        assertFalse(message.containsKey(propertyKey));
        if (contains != null) {
            assertFalse((boolean) contains.invoke(message, new Object[] {}));
        } else {
            assertFalse(message.containsKey(propertyKey));
        }
        value = getValue(type, "456");
        setter.invoke(message, new Object[] { value });
        assertFalse(message.isEmpty());
        assertEquals(1, message.size());
        assertEquals(value, message.remove(propertyKey));
        assertFalse(message.containsKey(propertyKey));
        if (contains != null) {
            assertFalse((boolean) contains.invoke(message, new Object[] {}));
        } else {
            assertFalse(message.containsKey(propertyKey));
        }
        assertTrue(message.isEmpty());
        assertEquals(0, message.size());
        value = getValue(type, "789");
        assertNull(message.put(propertyKey, value));
        message.clear();
        assertTrue(message.isEmpty());
        value = getValue(type, "def");
        assertFalse(message.containsValue(value));
        assertNull(message.put(propertyKey, value));
        assertTrue(message.containsValue(value));
        Map<String, Object> m = new HashMap<String, Object>();
        value = getValue(type, "ghi");
        m.put(propertyKey, value);
        m.put("dummy", "value");
        message.putAll(m);
        assertEquals(value, getter.invoke(message, new Object[] {}));
        assertEquals("value", message.get("dummy"));
        value1 = getValue(type, "jkl");

        assertEquals(value, message.putIfAbsent(propertyKey, value1));
        if (remove != null) {
            remove.invoke(message, new Object[] {});
        } else {
            message.remove(propertyKey);
        }
        assertNull(message.putIfAbsent(propertyKey, value1));
        assertEquals(value1, message.get(propertyKey));
        assertFalse(message.remove(propertyKey, getValue(type, "mno")));
        assertEquals(value1, message.get(propertyKey));
        assertTrue(message.remove(propertyKey, value1));
        if (contains != null) {
            assertFalse((boolean) contains.invoke(message, new Object[] {}));
        } else {
            assertFalse(message.containsKey(propertyKey));
        }
        
        setter.invoke(message, new Object[] { value1 });
        setter.invoke(message, new Object[] { value = getValue(type, "pqr") });
        Object value2 = getValue(type, "stu");
        assertFalse(message.replace(propertyKey, value1, value2));
        assertEquals(value, getter.invoke(message, new Object[] {}));
        assertTrue(message.replace(propertyKey, value, value2));
        assertEquals(value2, getter.invoke(message, new Object[] {}));
        assertEquals(value2, message.replace(propertyKey, value = getValue(type, "vwx")));
        assertEquals(value, getter.invoke(message, new Object[] {}));
        
        Set<String> keys = message.keySet();
        assertEquals(2, keys.size());
        assertFalse(keys.contains("abasdfwerqwe"));
        assertTrue(keys.contains("dummy"));
        
        Iterator<String> it = keys.iterator();
        
        assertTrue (it.hasNext());
        assertTrue (it.hasNext());
        assertTrue (it.hasNext());
        String k = it.next();

        if (k.equals("dummy")) {
            assertTrue(it.hasNext());
            k = it.next();
            assertEquals(propertyKey, k);
            assertFalse(it.hasNext());
            it.remove();
            try {
                it.remove();
                Assert.fail("Should have caught IllegalStateException");
            } catch (IllegalStateException e) {
                
            }
            assertFalse(message.containsKey(propertyKey));
            assertTrue(message.containsKey("dummy"));
            assertTrue(keys.remove("dummy"));
            assertNull(getter.invoke(message, new Object[] {}));
            assertFalse(keys.remove("dummy"));
            message.put("a", "b");
        } else if (k.equals(propertyKey)) {
            assertTrue(it.hasNext());
            k = it.next();
            assertEquals("dummy", k);
            assertFalse(it.hasNext());
            it.remove();
            try {
                it.remove();
                Assert.fail("Should have caught IllegalStateException");
            } catch (IllegalStateException e) {
                
            }
            assertFalse(message.containsKey("dummy"));
            assertTrue(message.containsKey(propertyKey));
            assertTrue(keys.remove(propertyKey));
            assertNull(message.get("dummy"));
            assertFalse(keys.remove(propertyKey));
            message.put("a", "b");
        } else {
            Assert.fail("Iterator does not contain proper contents");
        }
        
        keys.clear();
        assertTrue(message.isEmpty());
        
        setter.invoke(message, new Object[] { value = getValue(type, "CT") });
        message.put("dummy2", "value2");
        Set<Map.Entry<String, Object>> entries = message.entrySet();
        assertEquals(2, entries.size());
        assertFalse(entries.contains(new AbstractMap.SimpleEntry<String, Object>("abasdfwerqwe", "ERWWER")));
        assertTrue(entries.contains(new AbstractMap.SimpleEntry<String, Object>("dummy2", "value2")));
        assertFalse(entries.contains(new AbstractMap.SimpleEntry<String, Object>("dummy2", "value3")));
        
        Iterator<Map.Entry<String, Object>> it2 = entries.iterator();
        
        assertTrue (it2.hasNext());
        assertTrue (it2.hasNext());
        assertTrue (it2.hasNext());
        Map.Entry<String, Object> e = it2.next();

        if (e.getKey().equals("dummy2") && e.getValue().equals("value2")) {
            assertTrue(it2.hasNext());
            e = it2.next();
            assertEquals(propertyKey, e.getKey());
            assertEquals(value, e.getValue());
            assertFalse(it2.hasNext());
            it2.remove();
            try {
                it2.remove();
                Assert.fail("Should have caught IllegalStateException");
            } catch (IllegalStateException ex) {
                
            }
            assertFalse(message.containsKey(propertyKey));
            assertTrue(message.containsKey("dummy2"));
            assertTrue(entries.remove(new AbstractMap.SimpleEntry<String, Object>("dummy2", "value2")));
            assertNull(getter.invoke(message, new Object[] {}));
            assertFalse(keys.remove("dummy2"));
            message.put("a", "b");
        } else if (e.getKey().equals(propertyKey) && e.getValue().equals(value)) {
            assertTrue(it2.hasNext());
            e = it2.next();
            assertEquals("dummy2", e.getKey());
            assertEquals("value2", e.getValue());
            assertFalse(it2.hasNext());
            it2.remove();
            try {
                it2.remove();
                Assert.fail("Should have caught IllegalStateException");
            } catch (IllegalStateException ex) {
                
            }
            assertFalse(message.containsKey("dummy2"));
            assertTrue(message.containsKey(propertyKey));
            assertTrue(entries.remove(new AbstractMap.SimpleEntry<String, Object>(propertyKey, value)));
            assertNull(message.get("dummy"));
            assertFalse(keys.remove(propertyKey));
            message.put("a", "b");
        } else {
            Assert.fail("Iterator does not contain proper contents");
        }
        
        entries.clear();
        assertTrue(message.isEmpty());
        
        setter.invoke(message, new Object[] { value = getValue(type, "CT2") });
        message.put("dummy3", "value3");
        Collection<Object> values = message.values();
        assertEquals(values.size(), 2);
        assertFalse(values.contains("ERWWER"));
        assertTrue(values.contains(value));
        
        Iterator<Object> it3 = values.iterator();
        
        assertTrue (it3.hasNext());
        assertTrue (it3.hasNext());
        assertTrue (it3.hasNext());
        Object v = it3.next();

        if (v.equals("value3")) {
            assertTrue(it3.hasNext());
            v = it3.next();
            assertEquals(value, v);
            assertFalse(it3.hasNext());
            it3.remove();
            try {
                it3.remove();
                Assert.fail("Should have caught IllegalStateException");
            } catch (IllegalStateException ex) {
                
            }
            assertFalse(message.containsKey(propertyKey));
            assertTrue(message.containsKey("dummy3"));
            assertTrue(values.remove("value3"));
            assertNull(getter.invoke(message, new Object[] {}));
            assertFalse(values.remove("value3"));
            message.put("a", "b");
        } else if (v.equals(value)) {
            assertTrue(it3.hasNext());
            v = it3.next();
            assertEquals("value3", v);
            assertFalse(it3.hasNext());
            it3.remove();
            try {
                it3.remove();
                Assert.fail("Should have caught IllegalStateException");
            } catch (IllegalStateException ex) {
                
            }
            assertFalse(message.containsKey("dummy3"));
            assertTrue(message.containsKey(propertyKey));
            assertTrue(values.remove(value));
            assertNull(message.get("dummy3"));
            assertFalse(values.remove(value));
            message.put("a", "b");
        } else {
            Assert.fail("Iterator does not contain proper contents");
        }
        
        values.clear();
        assertTrue(message.isEmpty());
        
        setter.invoke(message, new Object[] { value = getValue(type, "Content")});
        if (type == String.class || type == Object.class) {
            assertEquals("Content-Type", message.compute(propertyKey, (key, val) -> ((String) val).concat("-Type")));
            assertTrue("Content-Type".equals(message.get(propertyKey)));
            assertNull(message.compute("dummy", (key, val) -> val ));
            assertFalse(message.containsKey("dummy"));
            assertEquals("value2", message.computeIfAbsent("dummy", key -> "value" + "2"));
            assertEquals("Content-Type", message.computeIfAbsent(propertyKey, key -> "value" + "2"));
            assertEquals("Content-Type", getter.invoke(message, new Object[] {}));
            assertEquals("Content-Type2", message.computeIfPresent(propertyKey, (key, val) -> val + "2"));
            assertEquals("Content-Type2", getter.invoke(message, new Object[] {}));
            assertNull(message.computeIfPresent("not-there", (key, val) -> val + "2"));
            assertFalse(message.containsKey("not-there"));
            assertEquals("Content-Type23", message.merge(propertyKey, "3", (v1, v2) -> (String) v1 + v2));
            assertEquals("abc", message.merge("123", "abc", (v1, v2) -> (String) v1 + v2));
            assertNull(message.computeIfPresent(propertyKey, (key, val) -> null));
            if (contains != null) {
                assertFalse((boolean) contains.invoke(message, new Object[] {}));
            } else {
                assertFalse(message.containsKey(propertyKey));
            }
            assertNull(message.compute("123", (key, val) -> null));
            assertFalse(message.containsKey("123"));
        } else if (type == Map.class) {
            Map<String,String> map = (Map<String,String>) getter.invoke(message, new Object[] {});
            Map<String,String> compute = (Map<String,String>)message.compute(propertyKey, (key, val) -> new HashMap<String,String>() {{put("Content", ((String) ((Map<?,?>) val).get("Content")).concat("-Type"));}});
            Object fromMap = ((Map<String,String>)getter.invoke(message, new Object[] {})).get("Content");
            assertEquals(fromMap, compute.get("Content"));
            assertTrue("235-Type".equals(((Map<?,?>) message.get(propertyKey)).get("Content")));
            assertNull(message.compute("dummy", (key, val) -> val ));
            assertFalse(message.containsKey("dummy"));
            assertEquals("value2", message.computeIfAbsent("dummy", key -> "value" + "2"));
            
            map = (Map<String,String>) getter.invoke(message, new Object[] {});
            assertEquals(map, message.computeIfAbsent(propertyKey, key -> new HashMap<String, String>().put("new", "newval")));
            map = (Map<String,String>) getter.invoke(message, new Object[] {});
            assertEquals(1, map.size());
            assertEquals("235-Type", ((Map<String,String>)getter.invoke(message, new Object[] {})).get("Content"));
            compute = (Map<String,String>)message.computeIfPresent(propertyKey, (key, val) -> new HashMap<String,String>() {{put("Content", ((String) ((Map<?,?>) val).get("Content")).concat("2"));}});
            assertEquals(compute, getter.invoke(message, new Object[] {}));
            assertEquals("235-Type2", ((Map<String,String>)getter.invoke(message, new Object[] {})).get("Content"));
            assertNull(message.computeIfPresent("not-there", (key, val) -> val + "2"));
            assertFalse(message.containsKey("not-there"));
            
            Map<String,String> merge = (Map<String,String>)message.merge(propertyKey, 
                                                                         getter.invoke(message, new Object[] {}), 
                                                                         (v1, v2) -> new HashMap<String,String>() {{put("Content", (((String) ((Map<?,?>) v2).get("Content")).concat("3")));}});
            assertEquals("235-Type23", merge.get("Content"));
            assertEquals("235-Type23", ((Map<String,String>)getter.invoke(message, new Object[] {})).get("Content"));
            assertEquals("abc", message.merge("123", "abc", (v1, v2) -> (String) v1 + v2));
            assertNull(message.computeIfPresent(propertyKey, (key, val) -> null));
            if (contains != null) {
                assertFalse((boolean) contains.invoke(message, new Object[] {}));
            } else {
                assertFalse(message.containsKey(propertyKey));
            }
            assertNull(message.compute("123", (key, val) -> null));
            assertFalse(message.containsKey("123"));
        } else if (type == Destination.class) {
            MyDestination myDest = new MyDestination();
            myDest.setString("Content-Type");
            assertEquals(myDest, message.compute(propertyKey, (key, val) -> ((MyDestination) val).setString(((MyDestination) val).getString().concat("-Type"))));
            assertTrue(myDest.equals(message.get(propertyKey)));
            assertNull(message.compute("dummy", (key, val) -> val ));
            assertFalse(message.containsKey("dummy"));
            assertEquals("value2", message.computeIfAbsent("dummy", key -> "value" + "2"));
            assertEquals(myDest, message.computeIfAbsent(propertyKey, key -> "value" + "2"));
            assertEquals(myDest, getter.invoke(message, new Object[] {}));
            myDest.setString("Content-Type2");
            assertEquals(myDest, message.computeIfPresent(propertyKey, (key, val) -> ((MyDestination) val).setString(((MyDestination) val).getString().concat("2"))));
            assertEquals(myDest, getter.invoke(message, new Object[] {}));
            assertNull(message.computeIfPresent("not-there", (key, val) -> val + "2"));
            assertFalse(message.containsKey("not-there"));
            myDest.setString("Content-Type23");
            assertEquals(myDest, message.merge(propertyKey, getter.invoke(message, new Object[] {}), (v1, v2) -> ((MyDestination) v2).setString(((MyDestination) v2).getString() + "3")));
            assertEquals("abc", message.merge("123", "abc", (v1, v2) -> (String) v1 + v2));
            assertNull(message.computeIfPresent(propertyKey, (key, val) -> null));
            if (contains != null) {
                assertFalse((boolean) contains.invoke(message, new Object[] {}));
            } else {
                assertFalse(message.containsKey(propertyKey));
            }
            assertNull(message.compute("123", (key, val) -> null));
            assertFalse(message.containsKey("123"));
        } else if (type == Collection.class) {
            HashSet<String> set = (HashSet<String>) getter.invoke(message, new Object[] {});
            assertEquals(1, set.size());
            String item = set.iterator().next();
            Collection<String> compute = (Collection<String>)message.compute(propertyKey, (key, val) -> new HashSet<String>() {{add(item.concat("-Type"));}});
            set = (HashSet<String>)getter.invoke(message, new Object[] {});
            assertEquals(1, set.size());
            assertEquals("Content-Type", set.iterator().next());
            assertEquals(set, compute);
            assertTrue("Content-Type".equals(((Collection<?>) message.get(propertyKey)).iterator().next()));
            assertNull(message.compute("dummy", (key, val) -> val ));
            assertFalse(message.containsKey("dummy"));
            assertEquals("value2", message.computeIfAbsent("dummy", key -> "value" + "2"));
            
            set = (HashSet<String>) getter.invoke(message, new Object[] {});
            assertEquals(set, message.computeIfAbsent(propertyKey, key -> new HashMap<String, String>().put("new", "newval")));
            set = (HashSet<String>) getter.invoke(message, new Object[] {});
            assertEquals(1, set.size());
            assertEquals("Content-Type", ((HashSet<String>)getter.invoke(message, new Object[] {})).iterator().next());
            String item2 = set.iterator().next();
            compute = (HashSet<String>)message.computeIfPresent(propertyKey, (key, val) -> new HashSet<String>() {{add(item2.concat("2"));}});
            assertEquals(compute, getter.invoke(message, new Object[] {}));
            set = (HashSet<String>)getter.invoke(message, new Object[] {});
            assertEquals(1, set.size());
            assertEquals("Content-Type2", ((HashSet<String>)getter.invoke(message, new Object[] {})).iterator().next());
            assertNull(message.computeIfPresent("not-there", (key, val) -> val + "2"));
            assertFalse(message.containsKey("not-there"));
            
            HashSet<String> merge = (HashSet<String>)message.merge(propertyKey, 
                                                                         getter.invoke(message, new Object[] {}), 
                                                                         (v1, v2) -> new HashSet<String>() {{add((((String) ((HashSet<?>) v2).iterator().next()).concat("3")));}});
            assertEquals(1, ((HashSet<String>) getter.invoke(message, new Object[] {})).size());
            assertEquals("Content-Type23", merge.iterator().next());
            assertEquals(1, ((HashSet<String>) getter.invoke(message, new Object[] {})).size());
            assertEquals("abc", message.merge("123", "abc", (v1, v2) -> (String) v1 + v2));
            assertNull(message.computeIfPresent(propertyKey, (key, val) -> null));
            if (contains != null) {
                assertFalse((boolean) contains.invoke(message, new Object[] {}));
            } else {
                assertFalse(message.containsKey(propertyKey));
            }
            assertNull(message.compute("123", (key, val) -> null));
            assertFalse(message.containsKey("123"));
        }
        message.clear();
    }
    
    @SuppressWarnings({ "unchecked" })
    private Object getValue(Class<?> type, String v) {
        Object value = null;
        if (type == String.class) {
            value = v;
        } else if (type == Map.class) {
            value = new HashMap<String, String>();
            ((HashMap<String,String>) value).put(v, "235");
        } else if (type == Object.class) {
            value = v;
        } else if (type == Destination.class) {
            value = new MyDestination();
            ((MyDestination) value).setString(v);
        } else if (type == Collection.class) {
            value = new HashSet<String>();
            ((HashSet<String>) value).add(v);
        }
        return value;
    }
    
    private static class MyDestination implements Destination {

        private String myString = null;
        public MyDestination setString(String s) {
            myString = s;
            return this;
        }
        public String getString() {
            return myString;
        }
        public EndpointReferenceType getAddress() {
            return null;
        }

        public Conduit getBackChannel(Message inMessage) throws IOException {
            return null;
        }

        public void shutdown() {

        }

        public void setMessageObserver(MessageObserver observer) {

        }

        public MessageObserver getMessageObserver() {
            return null;
        }
        @Override
        public boolean equals(Object obj) {
            if (obj != null && obj instanceof MyDestination) {
                MyDestination myDest = (MyDestination) obj;
                if (myDest.myString == null) {
                    return this.myString == null;
                }
                if (this.myString == null) {
                    return false;
                }
                return this.myString.equals(myDest.myString);
            }
            return false;
        }
    }

}
