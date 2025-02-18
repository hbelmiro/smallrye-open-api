package io.smallrye.openapi.runtime.scanner;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.DiscriminatorMapping;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.jboss.jandex.Index;
import org.json.JSONException;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import test.io.smallrye.openapi.runtime.scanner.dataobject.SingleAnnotatedConstructorArgument;

class StandaloneSchemaScanTest extends IndexScannerTestBase {

    @Test
    void testUnreferencedSchemasInComponents() throws Exception {
        Index index = indexOf(Cat.class, Dog.class, Class.forName(getClass().getPackage().getName() + ".package-info"));
        OpenApiAnnotationScanner scanner = new OpenApiAnnotationScanner(emptyConfig(), index);

        OpenAPI result = scanner.scan();

        printToConsole(result);
        assertJsonEquals("components.schemas.unreferenced.json", result);
    }

    @Test
    void testInheritanceAnyOf() throws Exception {
        Index index = indexOf(Reptile.class, Lizard.class, Snake.class, Turtle.class);
        OpenApiAnnotationScanner scanner = new OpenApiAnnotationScanner(emptyConfig(), index);

        OpenAPI result = scanner.scan();

        printToConsole(result);
        assertJsonEquals("components.schemas.inheritance.json", result);

    }

    /****************************************************************/

    static class Cat {
        public String name;
        @Schema(minimum = "1", maximum = "20")
        public int age;
        @Schema(nullable = true)
        public String color;
    }

    @Schema(name = "DogType")
    static class Dog {
        public String name;
        public int age;
        @Schema(required = true)
        public int volume;
    }

    @Schema(discriminatorProperty = "type", discriminatorMapping = {
            @DiscriminatorMapping(value = "lizard", schema = Lizard.class),
            @DiscriminatorMapping(value = "snake", schema = Snake.class),
            @DiscriminatorMapping(value = "turtle", schema = Turtle.class)
    })
    static abstract class Reptile {
        @Schema(required = true)
        private String type;
    }

    @Schema(allOf = { Reptile.class, Lizard.class })
    static class Lizard extends Reptile {
        String color;
    }

    @Schema(allOf = { Reptile.class, Snake.class })
    static class Snake extends Reptile {
        int length;
        String lengthUnits;
    }

    @Schema(allOf = { Reptile.class, Turtle.class })
    static class Turtle extends Reptile {
        String shellPattern;
    }

    /****************************************************************/

    /*
     * Test case derived from original example in Smallrye OpenAPI issue #649.
     *
     * https://github.com/smallrye/smallrye-open-api/issues/649
     * https://github.com/quarkusio/quarkus/issues/14670
     */
    @Test
    void testRegisteredSchemaTypePreserved() throws IOException, JSONException {
        Index index = indexOf(RegisteredSchemaTypePreservedModel.Animal.class,
                RegisteredSchemaTypePreservedModel.AnimalListEnvelope.class,
                RegisteredSchemaTypePreservedModel.MessageBase.class,
                RegisteredSchemaTypePreservedModel.MessageData.class,
                RegisteredSchemaTypePreservedModel.MessageDataItems.class);
        OpenApiAnnotationScanner scanner = new OpenApiAnnotationScanner(emptyConfig(), index);

        OpenAPI result = scanner.scan();

        printToConsole(result);
        assertJsonEquals("components.schemas.registered-schema-type-preserved.json", result);
    }

    static class RegisteredSchemaTypePreservedModel {
        @Schema
        static class AnimalListEnvelope extends MessageData<MessageDataItems<Animal>> {
            public AnimalListEnvelope() {
            }

            public AnimalListEnvelope(List<Animal> animals) {
                super(new MessageDataItems<Animal>(animals));
            }
        }

        static class Animal {
            private String name;
            private int age;

            public Animal() {
            }

            public Animal(String name, int age) {
                this.name = name;
                this.age = age;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public int getAge() {
                return age;
            }

            public void setAge(int age) {
                this.age = age;
            }
        }

        @Schema
        static class MessageData<T> extends MessageBase {
            @Schema(description = "The business data object")
            private T data;

            public MessageData() {
            }

            public MessageData(T data) {
                this.data = data;
            }

            public T getData() {
                return data;
            }

            public void setData(T data) {
                this.data = data;
            }

            @Schema(description = "The class-name of the business data object")
            public String getKind() {
                if (data == null) {
                    return null;
                } else {
                    return data.getClass()
                            .getSimpleName();
                }
            }
        }

        @Schema
        static class MessageBase {

            @Schema(description = "The API version", example = "v3")
            protected String apiVersion = "v3";

            @Schema(description = "Unique request-id (used for logging)", example = "F176f717c7a71")
            protected String requestId;

            @Schema(description = "Optional context-value for request/response correlation")
            protected String context;

            protected MessageBase() {
            }

            public String getRequestId() {
                return requestId;
            }

            public void setRequestId(String id) {
                this.requestId = id;
            }

            public String getContext() {
                return context;
            }

            public void setContext(String context) {
                this.context = context;
            }

            public String getApiVersion() {
                return apiVersion;
            }

            public void setApiVersion(String apiVersion) {
                this.apiVersion = apiVersion;
            }
        }

        @Schema
        static class MessageDataItems<T> {
            private List<T> items;

            public MessageDataItems() {
            }

            public MessageDataItems(List<T> items) {
                this.items = new ArrayList<T>(items);
            }

            public List<T> getItems() {
                return Collections.unmodifiableList(items);
            }

            public void setItems(List<T> items) {
                this.items = new ArrayList<T>(items);
            }

            @Schema(example = "1")
            public int getCurrentItemCount() {
                return (items == null) ? 0 : items.size();
            }
        }
    }

    /****************************************************************/

    /*
     * Test case derived from original example in Smallrye OpenAPI issue #650.
     *
     * https://github.com/smallrye/smallrye-open-api/issues/650
     */
    @Test
    void testJavaxJaxbElementUnwrapped() throws IOException, JSONException {
        Index index = indexOf(test.io.smallrye.openapi.runtime.scanner.javax.JAXBElementDto.class);
        OpenApiAnnotationScanner scanner = new OpenApiAnnotationScanner(emptyConfig(), index);
        OpenAPI result = scanner.scan();
        printToConsole(result);
        assertJsonEquals("components.schemas.jaxbelement-generic-type-unwrapped.json", result);
    }

    @Test
    void testJakartaJaxbElementUnwrapped() throws IOException, JSONException {
        Index index = indexOf(test.io.smallrye.openapi.runtime.scanner.jakarta.JAXBElementDto.class);
        OpenApiAnnotationScanner scanner = new OpenApiAnnotationScanner(emptyConfig(), index);
        OpenAPI result = scanner.scan();
        printToConsole(result);
        assertJsonEquals("components.schemas.jaxbelement-generic-type-unwrapped.json", result);
    }

    /****************************************************************/

    /*
     * https://github.com/smallrye/smallrye-open-api/issues/226
     */
    @Test
    void testJacksonJsonUnwrapped() throws IOException, JSONException {
        Index index = indexOf(JacksonJsonPerson.class, JacksonJsonPersonWithPrefixedAddress.class,
                JacksonJsonPersonWithSuffixedAddress.class, JacksonJsonAddress.class);
        OpenApiAnnotationScanner scanner = new OpenApiAnnotationScanner(emptyConfig(), index);
        OpenAPI result = scanner.scan();
        printToConsole(result);
        assertJsonEquals("components.schemas-jackson-jsonunwrapped.json", result);
    }

    @Schema
    static class JacksonJsonPerson {
        protected String name;
        @JsonUnwrapped
        protected JacksonJsonAddress address;

        @Schema(description = "Ignored since address is unwrapped")
        public JacksonJsonAddress getAddress() {
            return address;
        }
    }

    @Schema
    static class JacksonJsonPersonWithPrefixedAddress {
        protected String name;
        @JsonUnwrapped(prefix = "addr-")
        protected JacksonJsonAddress address;
    }

    @Schema
    static class JacksonJsonPersonWithSuffixedAddress {
        protected String name;
        @JsonUnwrapped(suffix = "-addr")
        protected JacksonJsonAddress address;
    }

    @Schema
    static class JacksonJsonAddress {
        protected int streetNumber;
        protected String streetName;
        protected String city;
        protected String state;
        protected String postalCode;
    }

    /****************************************************************/

    /*
     * https://github.com/smallrye/smallrye-open-api/issues/688
     */
    @Test
    void testNestedCollectionSchemas() throws IOException, JSONException {
        // Place the JDK classes in the index to simulate Quarkus
        Index index = indexOf(CollectionBean.class,
                EntryBean.class,
                MultivaluedCollection.class,
                MultivaluedMap.class,
                // CustomMap.class excluded intentionally
                Collection.class,
                ArrayList.class,
                HashMap.class,
                List.class,
                Map.class,
                Set.class,
                UUID.class);
        OpenApiAnnotationScanner scanner = new OpenApiAnnotationScanner(emptyConfig(), index);
        OpenAPI result = scanner.scan();
        printToConsole(result);
        assertJsonEquals("components.schemas.nested-parameterized-collection-types.json", result);
    }

    @Schema
    static class CollectionBean {
        @Schema(description = "In-line schema, `additionalProperties` array `items` reference `EntryBean`")
        CustomMap<String, List<EntryBean>> a_customMapOfLists;

        @Schema(description = "Reference to `MultivaluedMapStringEntryBean")
        MultivaluedMap<String, EntryBean> b_multivaluedEntryMap;

        @Schema(description = "In-line schema, `additionalProperties` array `items` reference `EntryBean`")
        Map<String, List<EntryBean>> c_mapStringListEntryBean;

        @Schema(description = "In-line schema (All JDK types, no references)")
        Collection<Map<String, List<String>>> d_collectionOfMapsOfListsOfStrings;

        @Schema(description = "In-line schema")
        Map<UUID, Map<String, Set<UUID>>> e_mapOfMapsOfSetsOfUUIDs;

        @Schema(description = "Reference to `MultivaluedCollectionString`")
        MultivaluedCollection<String> f_listOfStringLists;
    }

    static class EntryBean {
        String name;
        String value;
    }

    /*
     * Not present in index - will cause call to Class.forName(...) and is not
     * eligible for a entry in #/components/schemas
     */
    static class CustomMap<K, V> extends HashMap<K, V> {
        private static final long serialVersionUID = 1L;

        static {
            // We shouldn't run any code while scanning
            if (true) {
                throw new RuntimeException("CustomMap was initialized!?");
            }
        }
    }

    static class MultivaluedCollection<T> extends ArrayList<List<T>> {
        private static final long serialVersionUID = 1L;
    }

    static class MultivaluedMap<K, V> extends HashMap<K, List<V>> {
        private static final long serialVersionUID = 1L;
    }

    /****************************************************************/

    /*
     * https://github.com/smallrye/smallrye-open-api/issues/715
     */
    @Test
    void testNestedCustomGenericSchemas() throws IOException, JSONException {
        Index index = indexOf(Foo.class, Generic0.class, Generic1.class, Generic2.class, CustomMap.class);
        OpenApiAnnotationScanner scanner = new OpenApiAnnotationScanner(emptyConfig(), index);
        OpenAPI result = scanner.scan();
        printToConsole(result);
        assertJsonEquals("components.schemas.nested-custom-generics.json", result);
    }

    /*
     * Do not annotate with @Schema - test relies on Generic0 registration
     * only as an array component of Generic2#arrayOfGeneric0.
     */
    static class Generic0<T> {
        T value;
    }

    static class Generic1<T> {
        T value;
    }

    static class Generic2<T> {
        Generic1<T> nested;
        CustomMap<T, T> nestedMap;
        // Do not reference Generic0 other than from this field!
        Generic0<T>[] arrayOfGeneric0;
    }

    @Schema
    static class Foo {
        Generic2<String> generic;
    }

    /****************************************************************/

    /*
     * https://github.com/smallrye/smallrye-open-api/issues/809
     */
    @Test
    @SuppressWarnings("unused")
    void testOptionalArrayTypes() throws IOException, JSONException {
        @Schema(name = "B")
        class B {
            public UUID id;
        }
        @Schema(name = "A")
        class A {
            public UUID id;
            public Optional<B> optionalOfB;
            public List<B> listOfB;
            public Optional<List<B>> optionalListOfB;
            public Optional<B[]> optionalArrayOfB;
            public Optional<B>[] arrayOfOptionalB;
            public List<Optional<B>> listOfOptionalB;
        }
        Index index = indexOf(B.class, A.class, UUID.class, List.class, Optional.class);
        OpenApiAnnotationScanner scanner = new OpenApiAnnotationScanner(emptyConfig(), index);
        OpenAPI result = scanner.scan();
        printToConsole(result);
        assertJsonEquals("components.schemas.optional-arraytype.json", result);
    }

    @Target(ElementType.TYPE_USE)
    @interface TestAnno {

    }

    /*
     * https://github.com/smallrye/smallrye-open-api/issues/831
     */
    @Test
    void testArraySchemaTypeOverridden() throws IOException, JSONException {
        @Schema(name = "Sample")
        class Sample {
            @Schema(type = SchemaType.STRING, format = "base64")
            public byte[] data;
            @Schema(type = SchemaType.STRING)
            public char[] chars;
            @Schema(type = SchemaType.ARRAY)
            public char[] arrayFromSchema;
            @Schema
            public char[] arrayFromType;
        }

        Index index = indexOf(Sample.class);
        OpenApiAnnotationScanner scanner = new OpenApiAnnotationScanner(emptyConfig(), index);
        OpenAPI result = scanner.scan();
        printToConsole(result);
        assertJsonEquals("components.schemas.array-type-override.json", result);
    }

    /*
     * https://github.com/smallrye/smallrye-open-api/issues/937
     */
    @Test
    void testSingleAnnotatedConstructorArgumentIgnored() throws IOException, JSONException {
        Index index = indexOf(SingleAnnotatedConstructorArgument.class);
        OpenApiAnnotationScanner scanner = new OpenApiAnnotationScanner(emptyConfig(), index);
        OpenAPI result = scanner.scan();
        printToConsole(result);
        assertJsonEquals("components.schemas.annotated-constructor-arg-ignored.json", result);
    }

    /*
     * https://github.com/smallrye/smallrye-open-api/issues/944
     */
    @Test
    void testParameterizedTypeWithNonparameterizedAncestryChainLink() throws IOException, JSONException {
        class Tuple implements Iterable<Object> {
            @Override
            public Iterator<Object> iterator() {
                return null;
            }
        }

        class Pair<T1, T2> extends Tuple {
        }

        @Schema(name = "TestBean")
        class Bean {
            @SuppressWarnings("unused")
            Pair<String, String> pair;
        }

        Index index = indexOf(Bean.class, Pair.class, Tuple.class);
        OpenApiAnnotationScanner scanner = new OpenApiAnnotationScanner(emptyConfig(), index);
        OpenAPI result = scanner.scan();
        printToConsole(result);
        assertJsonEquals("components.schemas.nonparameterized-ancestry-chain-link.json", result);
    }

    /*
     * https://github.com/smallrye/smallrye-open-api/issues/1049
     */
    @Test
    @SuppressWarnings("unused")
    void testSchemaDeprecation() throws IOException, JSONException {
        @Schema(name = "Bean1")
        @Deprecated
        class Bean1 {
            String prop1;
            String prop2;
        }

        @Schema(name = "Bean2")
        class Bean2 {
            @Deprecated
            String prop1;
            String prop2;
            Bean1 prop3;
        }

        @Schema(name = "Bean3")
        class Bean3 {
            String prop1;
            String prop2;
            Bean2 prop3;
        }

        Index index = indexOf(Bean1.class, Bean2.class, Bean3.class);
        OpenApiAnnotationScanner scanner = new OpenApiAnnotationScanner(emptyConfig(), index);
        OpenAPI result = scanner.scan();

        assertTrue(result.getComponents().getSchemas().get("Bean1").getDeprecated());
        assertNull(result.getComponents().getSchemas().get("Bean1").getProperties().get("prop1").getDeprecated());
        assertNull(result.getComponents().getSchemas().get("Bean1").getProperties().get("prop2").getDeprecated());

        assertNull(result.getComponents().getSchemas().get("Bean2").getDeprecated());
        assertTrue(result.getComponents().getSchemas().get("Bean2").getProperties().get("prop1").getDeprecated());
        assertNull(result.getComponents().getSchemas().get("Bean2").getProperties().get("prop2").getDeprecated());
        assertNull(result.getComponents().getSchemas().get("Bean2").getProperties().get("prop3").getDeprecated());

        assertNull(result.getComponents().getSchemas().get("Bean3").getDeprecated());
        assertNull(result.getComponents().getSchemas().get("Bean3").getProperties().get("prop1").getDeprecated());
        assertNull(result.getComponents().getSchemas().get("Bean3").getProperties().get("prop2").getDeprecated());
        assertNull(result.getComponents().getSchemas().get("Bean3").getProperties().get("prop3").getDeprecated());
    }
}
