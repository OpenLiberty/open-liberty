/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.server.config.nested;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpService;

import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;

import test.server.BaseTestRunner;
import test.server.config.dynamic.ConfigWriter;
import test.server.config.dynamic.FactoryTest;

/**
 *
 */
@Component(service = NestedMergeRulesTestRunner.class,
           name = "test.server.config.nestedMergeRules",
           immediate = true,
           property = { "service.vendor=IBM" })
public class NestedMergeRulesTestRunner extends BaseTestRunner {

    private FactoryTest parentFactoryONE;
    private FactoryTest parentFactoryMULTIPLE;

    private static final String parentName = "test.nestedmerge.parent";

    private static final String XML_NEW_LINE = "\r\n";

    private ConfigWriter writer;

    private ConfigurationAdmin configAdmin;

    @Override
    @Reference(name = "locationService", service = WsLocationAdmin.class)
    protected void setLocationService(WsLocationAdmin ref) {
        this.locationService = ref;
    }

    @Override
    protected void unsetLocationService(WsLocationAdmin ref) {
        if (ref == this.locationService) {
            this.locationService = null;
        }
    }

    @Override
    @Reference(name = "http", service = HttpService.class)
    protected void setHttp(HttpService ref) {
        this.http = ref;
    }

    @Override
    protected void unsetHttp(HttpService ref) {
        if (ref == this.http) {
            this.http = null;
        }
    }

    @Override
    @Activate
    protected void activate(ComponentContext context) throws Exception {
        super.activate(context);
        registerServlet("/nested-merge-rules", new TestDynamicConfigServlet(), null, null);

        this.parentFactoryONE = new FactoryTest(parentName + ".ONE");
        this.parentFactoryMULTIPLE = new FactoryTest(parentName + ".MULTIPLE");
        addTest(parentFactoryONE);
        addTest(parentFactoryMULTIPLE);

        BundleContext bundleContext = context.getBundleContext();
        ServiceReference<?> ref = bundleContext.getServiceReference(ConfigurationAdmin.class.getName());
        this.configAdmin = (ConfigurationAdmin) bundleContext.getService(ref);

    }

    private static class Child {

        Map<String, String> attributes = new HashMap<String, String>();

        void addAttribute(String name, String value) {
            attributes.put(name, value);
        }

        void validateAttributes(Dictionary<String, Object> values) {
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                if (!"id".equals(entry.getKey())) {
                    assertEquals(entry.getValue(), values.get(entry.getKey()));
                }
            }
        }

        protected String getAlias() {
            return "child";
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append('<');
            builder.append(getAlias());
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                builder.append(' ');
                builder.append(entry.getKey());
                builder.append('=');
                builder.append('"');
                builder.append(entry.getValue());
                builder.append('"');
            }
            builder.append("/>");
            return builder.toString();
        }

        /**
         * @return
         */
        protected String getId() {
            return attributes.get("id");
        }

    }

    private static class DefaultChild extends Child {
        @Override
        protected String getAlias() {
            return "defaultChild";
        }

        @Override
        protected String getId() {
            return "childId";
        }
    }

    private class Parent {
        List<Child> children = new ArrayList<Child>();
        private final String id;
        private final Cardinality cardinality;
        private final String parentAliasName;

        public Parent(String id, Cardinality cardinality) {
            this.id = id;
            this.cardinality = cardinality;
            parentAliasName = "nmrParent." + cardinality;
        }

        void addChild(Child child) {
            children.add(child);
        }

        public String getId() {
            return this.id;
        }

        void validateAttributes(Dictionary<String, Object> values, Result expected) throws IOException {
            assertEquals("id should be " + getId(),
                         getId(), values.get("id"));

            validateChildren(values, expected);

        }

        void validateChildren(Dictionary<String, Object> values, Result expected) throws IOException {
            String childAliasName = "child";
            Object childValue = values.get(childAliasName);
            assertNotNull(childValue);

            if (expected.getNumberOfChildren() == 1) {
                if (cardinality == Cardinality.ONE) {
                    assertTrue("There should be one child pid", childValue instanceof String);
                    validateSingleChild((String) childValue);
                } else {
                    assertEquals("There should be one child pid", 1, ((String[]) childValue).length);
                    validateChildren((String[]) childValue);
                }
            } else {
                String[] childValues = (String[]) childValue;
                assertEquals("The number of children should equal the number of evaluated pids",
                             expected.getNumberOfChildren(), childValues.length);
                validateChildren(childValues);
            }

        }

        /**
         * @param childValue
         * @throws IOException
         */
        private void validateChildren(String[] childValues) throws IOException {
            for (String pid : childValues) {
                Configuration cfg = configAdmin.getConfiguration(pid);
                Dictionary<String, Object> dictionary = cfg.getProperties();
                Child child = getChild(dictionary);
                child.validateAttributes(dictionary);
            }

        }

        /**
         * @param object
         * @return
         */
        private Child getChild(Dictionary<String, Object> dictionary) {
            String id = (String) dictionary.get("id");
            if (id == null) {
                return null;
            } else {
                for (Child child : children) {
                    if (id.equals(child.getId())) {
                        return child;
                    } else if (id.contains("default-")) {
                        Enumeration<String> keys = dictionary.keys();
                        String key = keys.nextElement();
                        if ("id".equals(key))
                            key = keys.nextElement();
                        if (child.attributes.containsKey(key))
                            return child;
                    }
                }
                return null;
            }
        }

        /**
         * @param childValue
         * @throws IOException
         */
        private void validateSingleChild(String childValue) throws IOException {
            Configuration cfg = configAdmin.getConfiguration(childValue);
            for (Child child : children) {
                child.validateAttributes(cfg.getProperties());
            }

        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append('<');
            builder.append(parentAliasName);
            builder.append(" id=");
            builder.append('"');
            builder.append(id);
            builder.append('"');
            builder.append('>');

            for (Child child : children) {
                builder.append(XML_NEW_LINE);
                builder.append(child);
            }
            builder.append(XML_NEW_LINE);
            builder.append('<');
            builder.append('/');
            builder.append(parentAliasName);
            builder.append('>');
            return builder.toString();
        }

        public void addChildren(Child... children) {
            for (Child child : children) {
                addChild(child);
            }
        }

        /**
         *
         */
        public void write() throws Exception {
            writer = readConfiguration();
            writer.addConfig(toString());
            writeConfiguration(writer);
        }

        public void delete(Result expected) throws Exception {
            // Clean up
            writer = readConfiguration();
            writer.deleteConfig(parentAliasName, getId(), false);
            writeConfiguration(writer);
            if (expected.shouldSucceed())
                assertNull(getFactory().waitForUpdate(getId()));
            else
                assertFalse(getFactory().hasDictionary(getId()));

        }

        /**
         * @throws IOException
         *
         */
        public void validate(Result expected) throws IOException {
            if (expected.shouldSucceed()) {
                Dictionary<String, Object> dictionary = getFactory().waitForUpdate(getId());
                validateAttributes(dictionary, expected);
            } else {
                assertFalse(getFactory().hasDictionary(getId()));
            }
        }

        /**
         * @return
         */
        private FactoryTest getFactory() {
            if (cardinality == Cardinality.ONE)
                return parentFactoryONE;
            else
                return parentFactoryMULTIPLE;
        }

        /**
         *
         */
        public void test(Result result) throws Exception {

            try {
                clean();
                write();
                validate(result);
            } finally {
                delete(result);
            }

        }

        /**
         *
         */
        private void clean() {
            parentFactoryONE.reset();
            parentFactoryMULTIPLE.reset();
        }
    }

    /**
     * Multiple children with null IDs should be merged when cardinality is zero
     */
    public void testNullIDsCardinalityZero() throws Exception {

        Parent parent = new Parent("parent", Cardinality.ONE);
        Child c1 = new Child();
        c1.addAttribute("attr1", "a1");
        Child c2 = new Child();
        c2.addAttribute("attr2", "a2");
        parent.addChildren(c1, c2);

        Result r = new Result(true, 1);
        parent.test(r);

    }

    /**
     * Multiple children with distinct IDs should be merged. This makes no sense to me whatsoever, but it was the
     * resolution of the design issue.
     *
     * @throws Exception
     */
    public void testMultipleIDsCardinalityZero() throws Exception {
        Parent parent = new Parent("parent", Cardinality.ONE);
        Child c1 = new Child();
        c1.addAttribute("id", "id1");
        c1.addAttribute("attr1", "a1");
        Child c2 = new Child();
        c2.addAttribute("id", "id2");
        c2.addAttribute("attr2", "a2");
        parent.addChildren(c1, c2);

        Result r = new Result(true, 1);
        parent.test(r);
    }

    /**
     * Multiple children with the same ID should be merged when cardinality is zero.
     *
     * @throws Exception
     */
    public void testSingleIDCardinalityZero() throws Exception {
        Parent parent = new Parent("parent", Cardinality.ONE);
        Child c1 = new Child();
        c1.addAttribute("id", "id1");
        c1.addAttribute("attr1", "a1");
        Child c2 = new Child();
        c2.addAttribute("id", "id1");
        c2.addAttribute("attr2", "a2");
        parent.addChildren(c1, c2);

        Result r = new Result(true, 1);
        parent.test(r);
    }

    /**
     * Multiple distinct IDs with multiple cardinality should result in multiple children.
     *
     * @throws Exception
     */
    public void testMultipleIDsCardinalityMultiple() throws Exception {
        Parent parent = new Parent("parent", Cardinality.MULTIPLE);
        Child c1 = new Child();
        c1.addAttribute("id", "id1");
        c1.addAttribute("attr1", "a1");
        Child c2 = new Child();
        c2.addAttribute("id", "id2");
        c2.addAttribute("attr2", "a2");
        parent.addChildren(c1, c2);

        Result r = new Result(true, 2);
        parent.test(r);
    }

    /**
     * Multiple null IDs with multiple cardinality should result in multiple children.
     *
     * @throws Exception
     */
    public void testNullIDsCardinalityMultiple() throws Exception {
        Parent parent = new Parent("parent", Cardinality.MULTIPLE);
        Child c1 = new Child();
        c1.addAttribute("attr1", "a1");
        Child c2 = new Child();
        c2.addAttribute("attr2", "a2");
        parent.addChildren(c1, c2);

        Result r = new Result(true, 2);
        parent.test(r);
    }

    /**
     * Multiple children with the same ID should result in one merged element
     *
     * @throws Exception
     */
    public void testSingleIDCardinalityMultiple() throws Exception {
        Parent parent = new Parent("parent", Cardinality.MULTIPLE);
        Child c1 = new Child();
        c1.addAttribute("id", "id1");
        c1.addAttribute("attr1", "a1");
        Child c2 = new Child();
        c2.addAttribute("id", "id1");
        c2.addAttribute("attr2", "a2");
        parent.addChildren(c1, c2);

        Result r = new Result(true, 1);
        parent.test(r);
    }

    public void testMixedIDsCardinalityMultiple() throws Exception {
        Parent parent = new Parent("parent", Cardinality.MULTIPLE);
        Child c1 = new Child();
        c1.addAttribute("id", "id1");
        c1.addAttribute("attr1", "a1");

        Child c2 = new Child();
        c2.addAttribute("id", "id1");
        c2.addAttribute("attr2", "a2");

        Child c3 = new Child();
        c3.addAttribute("id", "id2");
        c3.addAttribute("attr1", "b1");

        parent.addChildren(c1, c2, c3);

        Result r = new Result(true, 2);
        parent.test(r);
    }

    public void testMultipleParentsNullIDsCardinalityZero() throws Exception {
        Parent parent = new Parent("parent", Cardinality.ONE);
        Child c1 = new Child();
        c1.addAttribute("attr1", "a1");
        parent.addChild(c1);

        Parent parent2 = new Parent("parent2", Cardinality.ONE);
        Child c2 = new Child();
        c2.addAttribute("attr2", "a2");
        parent2.addChild(c2);

        Result r = new Result(true, 1);
        parent.test(r);
        parent2.test(r);
    }

    public void testMultipleParentsSingleIDCardinalityZero() throws Exception {
        Parent parent = new Parent("parent", Cardinality.ONE);
        Child c1 = new Child();
        c1.addAttribute("id", "id1");
        c1.addAttribute("attr1", "a1");
        parent.addChild(c1);

        Parent parent2 = new Parent("parent2", Cardinality.ONE);
        Child c2 = new Child();
        c2.addAttribute("id", "id1");
        c2.addAttribute("attr2", "a2");
        parent2.addChild(c2);

        Result r = new Result(true, 1);
        parent.test(r);
        parent2.test(r);
    }

    public void testMultipleParentsMixedIDsCardinalityMultiple() throws Exception {
        Parent parent = new Parent("parent", Cardinality.MULTIPLE);
        Parent parent2 = new Parent("parent2", Cardinality.MULTIPLE);

        Child c1 = new Child();
        c1.addAttribute("id", "id1");
        c1.addAttribute("attr1", "a1");

        Child c2 = new Child();
        c2.addAttribute("id", "id1");
        c2.addAttribute("attr2", "a2");

        Child c3 = new Child();
        c3.addAttribute("id", "id2");
        c3.addAttribute("attr1", "b1");

        parent.addChildren(c1, c2, c3);

        Child c4 = new Child();
        c4.addAttribute("id", "id1");
        c4.addAttribute("attr4", "a4");

        Child c5 = new Child();
        c5.addAttribute("id", "id2");
        c5.addAttribute("attr5", "attr5");

        parent2.addChildren(c4, c5);

        Result r = new Result(true, 2);

        parent.test(r);
    }

    public void testDefaultIDMerging() throws Exception {
        Parent parent = new Parent("parent", Cardinality.MULTIPLE);
        DefaultChild c1 = new DefaultChild();
        c1.addAttribute("attr1", "a1");
        DefaultChild c2 = new DefaultChild();
        c2.addAttribute("attr2", "a2");
        parent.addChildren(c1, c2);

        Result r = new Result(true, 1);
        parent.test(r);
    }

    private static class Result {

        private final int numberOfElements;
        private final boolean shouldWork;

        public Result(boolean shouldWork, int elements) {
            this.shouldWork = shouldWork;
            this.numberOfElements = elements;
        }

        public boolean shouldSucceed() {
            return shouldWork;
        }

        public int getNumberOfChildren() {
            return numberOfElements;
        }
    }

    private static enum Cardinality {
        ONE, MULTIPLE
    };
}
