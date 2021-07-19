package io.openliberty.microprofile.openapi20.test.utils;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.Paths;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;
import org.eclipse.microprofile.openapi.models.parameters.Parameter.In;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.junit.Test;

import io.openliberty.microprofile.openapi20.merge.ModelCopy;
import io.openliberty.microprofile.openapi20.merge.ModelType;
import io.openliberty.microprofile.openapi20.merge.ModelType.ModelParameter;

public class TestModelCopy {
    
    @Test
    public void testBasicCopy() {
        OpenAPI original = OASFactory.createOpenAPI();
        Paths paths = OASFactory.createPaths();
        original.setPaths(paths);
        
        PathItem fooPath = OASFactory.createPathItem();
        paths.addPathItem("/foo", fooPath);
        fooPath.setGET(OASFactory.createOperation().description("Get all foos"));
        fooPath.setPOST(OASFactory.createOperation().description("Create a foo"));
        
        PathItem fooIdPath = OASFactory.createPathItem();
        paths.addPathItem("/foo/{fooId}", fooIdPath);
        fooIdPath.addParameter(OASFactory.createParameter().in(In.PATH).name("fooId"));
        APIResponse getFooByIdResponse = OASFactory.createAPIResponse().content(OASFactory.createContent().addMediaType("application/json", OASFactory.createMediaType().schema(OASFactory.createSchema().ref("#/components/foo"))));
        fooIdPath.setGET(OASFactory.createOperation().description("Get foo by ID").responses(OASFactory.createAPIResponses().addAPIResponse("200", getFooByIdResponse)));
        fooIdPath.setPUT(OASFactory.createOperation().description("Update a foo"));
        
        original.components(OASFactory.createComponents());
        
        Schema barSchema = OASFactory.createSchema().type(SchemaType.OBJECT);
        original.getComponents().addSchema("bar", barSchema);
        HashMap<String, Schema> barProperties = new HashMap<>();
        barProperties.put("name", OASFactory.createSchema().type(SchemaType.STRING));
        barSchema.properties(barProperties).addRequired("name");
        
        Schema fooSchema = OASFactory.createSchema().type(SchemaType.OBJECT);
        original.getComponents().addSchema("foo", fooSchema);
        HashMap<String, Schema> fooProperties = new HashMap<>();
        fooProperties.put("id", OASFactory.createSchema().type(SchemaType.STRING).pattern("[0-9]{10}"));
        fooProperties.put("size", OASFactory.createSchema().type(SchemaType.NUMBER));
        fooProperties.put("bars", OASFactory.createSchema().type(SchemaType.ARRAY).items(OASFactory.createSchema().ref("#/components/bar")));
        fooSchema.properties(fooProperties);
        
        OpenAPI copy = (OpenAPI) ModelCopy.copy(original);
        
        assertNotSame(original, copy);
        assertModelNotSame(original, copy, m -> m.getComponents());
        assertModelNotSame(original, copy, m -> m.getComponents()
                           .getSchemas()
                           .get("foo")
                           .getProperties()
                           .get("id"));
        assertModelNotSame(original, copy, m -> m.getPaths().getPathItem("/foo/{fooId}").getGET().getResponses().getAPIResponse("200"));
        assertModelEqual(original, copy, m -> m.getPaths().getPathItem("/foo/{fooId}").getGET().getDescription());
        
        assertCopy(original, copy);
    }
    
    private <T, V> void assertModelNotSame(T a, T b, Function<T, V> transform) {
        assertNotSame(transform.apply(a),
                      transform.apply(b));
    }
    
    private <T, V> void assertModelEqual(T a, T b, Function<T, V> transform) {
        assertEquals(transform.apply(a),
                     transform.apply(b));
    }
    
    /**
     * Reflectively navigate the model asserting equality and non-sameness
     */
    private void assertCopy(Object expected, Object actual) {
        if (expected == null) {
            assertNull(actual);
            return;
        } else {
            assertNotNull(actual);
        }
        
        Optional<ModelType> mtExpected = ModelType.getModelObject(expected.getClass());
        Optional<ModelType> mtActual = ModelType.getModelObject(actual.getClass());
        assertEquals(mtExpected, mtActual);
        
        if (mtExpected.isPresent()) {
            assertCopyModelObject(expected, actual, mtExpected.get());
        } else if (expected instanceof Map) {
            assertCopyMap(expected, actual);
        } else if (expected instanceof List) {
            assertCopyList(expected, actual);
        } else {
            // assert that non-model, non-collection objects are not copied
            assertSame(expected, actual);
        }
    }

    private void assertCopyModelObject(Object expected, Object actual, ModelType mt) {
        assertNotSame(expected, actual);
        for (ModelParameter desc : mt.getParameters()) {
            assertCopy(desc.get(expected), desc.get(actual));
        }
    }
    
    private void assertCopyMap(Object expected, Object actual) {
        assertNotSame(expected, actual);
        Map<?,?> expectedMap = (Map<?,?>) expected;
        assertThat(actual, instanceOf(Map.class));
        Map<?,?> actualMap = (Map<?,?>) actual;
        
        assertEquals(expectedMap.keySet(), actualMap.keySet());
        for (Object key : expectedMap.keySet()) {
            assertCopy(expectedMap.get(key), actualMap.get(key));
        }
    }
    
    private void assertCopyList(Object expected, Object actual) {
        assertNotSame(expected, actual);
        List<?> expectedList = (List<?>) expected;
        assertThat(actual, instanceOf(List.class));
        List<?> actualList = (List<?>) actual;
        
        assertThat(actualList, hasSize(expectedList.size()));
        
        Iterator<?> expectedIterator = expectedList.iterator();
        Iterator<?> actualIterator = actualList.iterator();
        while (expectedIterator.hasNext()) {
            assertCopy(expectedIterator.next(), actualIterator.next());
        }
    }
    
}
