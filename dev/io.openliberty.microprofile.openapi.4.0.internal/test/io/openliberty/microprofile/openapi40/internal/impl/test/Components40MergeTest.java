package io.openliberty.microprofile.openapi40.internal.impl.test;

import org.eclipse.microprofile.openapi.models.PathItem;
import org.junit.BeforeClass;
import org.junit.Test;

import io.openliberty.microprofile.openapi20.test.merge.parts.ComponentsMergeTest;
import io.openliberty.microprofile.openapi20.test.merge.parts.TestUtils;

public class Components40MergeTest {

    @BeforeClass
    public static void setup() {
        TestUtils.setCurrent(new TestUtil40Impl());
    }

    @Test
    public void test40Components() throws Exception {
        ComponentsMergeTest.testComponentsItem(PathItem.class);
    }

}
