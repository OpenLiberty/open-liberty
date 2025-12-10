/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.data.internal.persistence.orm;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import io.openliberty.data.internal.persistence.DataProvider;

/**
 * TODO test an annotated entity
 */
public class EntityParserTests {
    private final DataProvider provider;

    public EntityParserTests() {
        provider = new DataProvider(//
                        Map.of(), // properties
                        null, // CDIService
                        null, // ClassLoaderIdentifierService
                        new MockVersionCompatibility(), //
                        null, // ConfigurationAdmin
                        null, // ExecutorService
                        null, // LocalTransactionCurrent
                        null, // MetaDataIdentifierService
                        null, // ResourceConfigFactory
                        null // EmbeddableWebSphereTransactionManager
        );
    }

    @Test
    public void simpleEntityTest() {
        EntityParser p = new EntityParser("", null);
        p.parseUnannotatedEntity(Simple.class);
        List<String> xmls = p.generateView();

        assertEquals(1, xmls.size());

        final String expected = """
                          <entity class="io.openliberty.data.internal.persistence.orm.Simple">
                            <table name="Simple"/>
                            <attributes>
                              <id name="id" access="FIELD">
                                <column nullable="false"/>
                              </id>
                              <basic name="firstName" access="FIELD">
                              </basic>
                              <basic name="lastName" access="FIELD">
                              </basic>
                            </attributes>
                          </entity>
                        """;

        assertEquals(expected, xmls.get(0));
    }

    @Test
    public void simpleEntityWithPrefixTest() {
        EntityParser p = new EntityParser("prefix", null);
        p.parseUnannotatedEntity(Simple.class);
        List<String> xmls = p.generateView();

        assertEquals(1, xmls.size());

        final String expected = """
                          <entity class="io.openliberty.data.internal.persistence.orm.Simple">
                            <table name="prefixSimple"/>
                            <attributes>
                              <id name="id" access="FIELD">
                                <column nullable="false"/>
                              </id>
                              <basic name="firstName" access="FIELD">
                              </basic>
                              <basic name="lastName" access="FIELD">
                              </basic>
                            </attributes>
                          </entity>
                        """;

        assertEquals(expected, xmls.get(0));
    }

    @Test
    public void versionedEntityTest() {
        EntityParser p = new EntityParser("", null);
        p.parseUnannotatedEntity(Versioned.class);
        List<String> xmls = p.generateView();

        assertEquals(1, xmls.size());

        final String expected = """
                          <entity class="io.openliberty.data.internal.persistence.orm.Versioned">
                            <table name="Versioned"/>
                            <attributes>
                              <id name="identifier" access="FIELD">
                              </id>
                              <basic name="firstName" access="FIELD">
                              </basic>
                              <basic name="lastName" access="FIELD">
                              </basic>
                              <version name="version" access="FIELD">
                              </version>
                            </attributes>
                          </entity>
                        """;

        assertEquals(expected, xmls.get(0));
    }

    @Test
    public void recordEntityTest() {
        EntityParser p = new EntityParser("", null);
        p.parseRecord(RecordEntity.class, RecordEntityEntity.class);
        List<String> xmls = p.generateView();

        assertEquals(1, xmls.size());

        final String expected = """
                          <entity class="io.openliberty.data.internal.persistence.orm.RecordEntityEntity">
                            <table name="RecordEntity"/>
                            <attributes>
                              <id name="id" access="FIELD">
                                <column nullable="false"/>
                              </id>
                              <basic name="firstName" access="FIELD">
                              </basic>
                              <basic name="lastName" access="FIELD">
                              </basic>
                              <version name="version" access="FIELD">
                              </version>
                            </attributes>
                          </entity>
                        """;

        assertEquals(expected, xmls.get(0));
    }

    @Test
    public void recordComplexEntityTest() {
        EntityParser p = new EntityParser("", null);
        p.parseRecord(RecordComplex.class, RecordComplexEntity.class);
        List<String> xmls = p.generateView();

        assertEquals(2, xmls.size());

        String expected = """
                          <entity class="io.openliberty.data.internal.persistence.orm.RecordComplexEntity">
                            <table name="RecordComplex"/>
                            <attributes>
                              <id name="id" access="FIELD">
                                <column nullable="false"/>
                              </id>
                              <version name="version" access="FIELD">
                              </version>
                              <element-collection name="aliases" access="FIELD" fetch="EAGER">
                              </element-collection>
                              <embedded name="name" access="FIELD">
                                <attribute-override name="firstName">
                                  <column name="NAME_FIRSTNAME"/>
                                </attribute-override>
                                <attribute-override name="lastName">
                                  <column name="NAME_LASTNAME"/>
                                </attribute-override>
                              </embedded>
                            </attributes>
                          </entity>
                        """;
        assertEquals(expected, xmls.get(0));

        expected = """
                          <embeddable class="io.openliberty.data.internal.persistence.orm.RecordComplex$Name">
                            <attributes>
                              <basic name="firstName" access="FIELD">
                              </basic>
                              <basic name="lastName" access="FIELD">
                              </basic>
                            </attributes>
                          </embeddable>
                        """;
        assertEquals(expected, xmls.get(1));
    }

    @Test
    public void collectionEntityTest() {
        EntityParser p = new EntityParser("", null);
        p.parseUnannotatedEntity(Collection.class);
        List<String> xmls = p.generateView();

        assertEquals(1, xmls.size());

        final String expected = """
                          <entity class="io.openliberty.data.internal.persistence.orm.Collection">
                            <table name="Collection"/>
                            <attributes>
                              <id name="collectionId" access="FIELD">
                                <column nullable="false"/>
                              </id>
                              <basic name="firstName" access="FIELD">
                              </basic>
                              <basic name="lastName" access="FIELD">
                              </basic>
                              <element-collection name="friends" access="FIELD" fetch="EAGER">
                              </element-collection>
                            </attributes>
                          </entity>
                        """;

        assertEquals(expected, xmls.get(0));
    }

    @Test
    public void collectionEmbeddedEntityTest() {
        EntityParser p = new EntityParser("", null);
        p.parseUnannotatedEntity(CollectionEmbedded.class);
        List<String> xmls = p.generateView();

        assertEquals(2, xmls.size());

        String expected = """
                          <entity class="io.openliberty.data.internal.persistence.orm.CollectionEmbedded">
                            <table name="CollectionEmbedded"/>
                            <attributes>
                              <id name="collectionId" access="FIELD">
                                <column nullable="false"/>
                              </id>
                              <element-collection name="friends" access="FIELD" fetch="EAGER">
                                <collection-table name="COLLECTIONEMBEDDED_FRIENDS">
                                  <join-column name="collectionId"/>
                                </collection-table>
                                <attribute-override name="firstName">
                                  <column name="FRIENDS_FIRSTNAME"/>
                                </attribute-override>
                                <attribute-override name="lastName">
                                  <column name="FRIENDS_LASTNAME"/>
                                </attribute-override>
                              </element-collection>
                            </attributes>
                          </entity>
                        """;

        assertEquals(expected, xmls.get(0));

        expected = """
                          <embeddable class="io.openliberty.data.internal.persistence.orm.CollectionEmbedded$Name">
                            <attributes>
                              <basic name="firstName" access="FIELD">
                              </basic>
                              <basic name="lastName" access="FIELD">
                              </basic>
                            </attributes>
                          </embeddable>
                        """;

        assertEquals(expected, xmls.get(1));
    }

    @Test
    public void propertyEntityTest() {
        EntityParser p = new EntityParser("", null);
        p.parseUnannotatedEntity(Property.class);
        List<String> xmls = p.generateView();

        assertEquals(1, xmls.size());

        final String expected = """
                          <entity class="io.openliberty.data.internal.persistence.orm.Property">
                            <table name="Property"/>
                            <attributes>
                              <id name="prop_id" access="FIELD">
                                <column nullable="false"/>
                              </id>
                              <basic name="URL" access="PROPERTY">
                              </basic>
                              <basic name="email" access="PROPERTY">
                              </basic>
                              <basic name="firstName" access="FIELD">
                              </basic>
                              <basic name="lastName" access="FIELD">
                              </basic>
                            </attributes>
                          </entity>
                        """;

        assertEquals(expected, xmls.get(0));
    }

    @Test
    public void embeddedEntityTest() {
        EntityParser p = new EntityParser("", null);
        p.parseUnannotatedEntity(WithEmbedded.class);
        List<String> xmls = p.generateView();

        assertEquals(2, xmls.size());

        String expected = """
                          <entity class="io.openliberty.data.internal.persistence.orm.WithEmbedded">
                            <table name="WithEmbedded"/>
                            <attributes>
                              <id name="id" access="FIELD">
                                <column nullable="false"/>
                              </id>
                              <embedded name="name" access="FIELD">
                                <attribute-override name="firstName">
                                  <column name="NAME_FIRSTNAME"/>
                                </attribute-override>
                                <attribute-override name="lastName">
                                  <column name="NAME_LASTNAME"/>
                                </attribute-override>
                              </embedded>
                            </attributes>
                          </entity>
                        """;
        assertEquals(expected, xmls.get(0));

        expected = """
                          <embeddable class="io.openliberty.data.internal.persistence.orm.WithEmbedded$Name">
                            <attributes>
                              <basic name="firstName" access="FIELD">
                              </basic>
                              <basic name="lastName" access="FIELD">
                              </basic>
                            </attributes>
                          </embeddable>
                        """;
        assertEquals(expected, xmls.get(1));
    }

    @Test
    public void embeddedIdEntityTest() {
        EntityParser p = new EntityParser("", null);
        p.parseUnannotatedEntity(WithEmbeddedId.class);
        List<String> xmls = p.generateView();

        assertEquals(2, xmls.size());

        String expected = """
                          <entity class="io.openliberty.data.internal.persistence.orm.WithEmbeddedId">
                            <table name="WithEmbeddedId"/>
                            <attributes>
                              <embedded-id name="name_id" access="FIELD">
                                <attribute-override name="firstName">
                                  <column name="NAME_ID_FIRSTNAME"/>
                                </attribute-override>
                                <attribute-override name="lastName">
                                  <column name="NAME_ID_LASTNAME"/>
                                </attribute-override>
                              </embedded-id>
                              <version name="version" access="FIELD">
                              </version>
                            </attributes>
                          </entity>
                        """;
        assertEquals(expected, xmls.get(0));

        expected = """
                          <embeddable class="io.openliberty.data.internal.persistence.orm.WithEmbeddedId$Name">
                            <attributes>
                              <basic name="firstName" access="PROPERTY">
                              </basic>
                              <basic name="lastName" access="PROPERTY">
                              </basic>
                            </attributes>
                          </embeddable>
                        """;
        assertEquals(expected, xmls.get(1));
    }

    @Test
    public void embeddedRecordEntityTest() {
        EntityParser p = new EntityParser("", null);
        p.parseUnannotatedEntity(WithEmbeddedRecord.class);
        List<String> xmls = p.generateView();

        assertEquals(2, xmls.size());

        String expected = """
                          <entity class="io.openliberty.data.internal.persistence.orm.WithEmbeddedRecord">
                            <table name="WithEmbeddedRecord"/>
                            <attributes>
                              <id name="id" access="FIELD">
                                <column nullable="false"/>
                              </id>
                              <version name="version" access="FIELD">
                              </version>
                              <embedded name="name" access="FIELD">
                                <attribute-override name="firstName">
                                  <column name="NAME_FIRSTNAME"/>
                                </attribute-override>
                                <attribute-override name="lastName">
                                  <column name="NAME_LASTNAME"/>
                                </attribute-override>
                              </embedded>
                            </attributes>
                          </entity>
                        """;
        assertEquals(expected, xmls.get(0));

        expected = """
                          <embeddable class="io.openliberty.data.internal.persistence.orm.WithEmbeddedRecord$Name">
                            <attributes>
                              <basic name="firstName" access="FIELD">
                              </basic>
                              <basic name="lastName" access="FIELD">
                              </basic>
                            </attributes>
                          </embeddable>
                        """;
        assertEquals(expected, xmls.get(1));
    }

    @Test
    public void mappedSuperClassEntityTest() {
        EntityParser p = new EntityParser("", null);
        p.parseUnannotatedEntity(WithMappedSuperclass.class);
        List<String> xmls = p.generateView();

        assertEquals(5, xmls.size());

        String expected = """
                          <mapped-superclass class="io.openliberty.data.internal.persistence.orm.SuperAlpha">
                            <attributes>
                              <version name="version" access="FIELD">
                              </version>
                            </attributes>
                          </mapped-superclass>
                        """;

        assertEquals(expected, xmls.get(0));

        expected = """
                          <mapped-superclass class="io.openliberty.data.internal.persistence.orm.SuperBeta">
                            <attributes>
                              <embedded name="script" access="FIELD">
                                <attribute-override name="lowercase">
                                  <column name="SCRIPT_LOWERCASE"/>
                                </attribute-override>
                                <attribute-override name="uppercase">
                                  <column name="SCRIPT_UPPERCASE"/>
                                </attribute-override>
                              </embedded>
                            </attributes>
                          </mapped-superclass>
                        """;

        assertEquals(expected, xmls.get(1));

        expected = """
                          <mapped-superclass class="io.openliberty.data.internal.persistence.orm.SuperGamma">
                            <attributes>
                              <id name="id" access="FIELD">
                                <column nullable="false"/>
                              </id>
                            </attributes>
                          </mapped-superclass>
                        """;

        assertEquals(expected, xmls.get(2));

        expected = """
                          <entity class="io.openliberty.data.internal.persistence.orm.WithMappedSuperclass">
                            <table name="WithMappedSuperclass"/>
                            <attributes>
                              <basic name="URL" access="FIELD">
                              </basic>
                            </attributes>
                          </entity>
                        """;

        assertEquals(expected, xmls.get(3));

        expected = """
                          <embeddable class="io.openliberty.data.internal.persistence.orm.SuperBeta$Script">
                            <attributes>
                              <basic name="lowercase" access="FIELD">
                                <column nullable="false"/>
                              </basic>
                              <basic name="uppercase" access="FIELD">
                                <column nullable="false"/>
                              </basic>
                            </attributes>
                          </embeddable>
                        """;

        assertEquals(expected, xmls.get(4));
    }

    @Test
    public void mappedSuperClassEmbeddedIdEntityTest() {
        EntityParser p = new EntityParser("", null);
        p.parseUnannotatedEntity(WithMappedSuperclassPrime.class);
        List<String> xmls = p.generateView();

        assertEquals(6, xmls.size());

        String expected = """
                          <mapped-superclass class="io.openliberty.data.internal.persistence.orm.SuperAlpha">
                            <attributes>
                              <version name="version" access="FIELD">
                              </version>
                            </attributes>
                          </mapped-superclass>
                        """;

        assertEquals(expected, xmls.get(0));

        expected = """
                          <mapped-superclass class="io.openliberty.data.internal.persistence.orm.SuperBeta">
                            <attributes>
                              <embedded name="script" access="FIELD">
                                <attribute-override name="lowercase">
                                  <column name="SCRIPT_LOWERCASE"/>
                                </attribute-override>
                                <attribute-override name="uppercase">
                                  <column name="SCRIPT_UPPERCASE"/>
                                </attribute-override>
                              </embedded>
                            </attributes>
                          </mapped-superclass>
                        """;

        assertEquals(expected, xmls.get(1));

        expected = """
                          <mapped-superclass class="io.openliberty.data.internal.persistence.orm.SuperGammaPrime">
                            <attributes>
                              <embedded-id name="name_id" access="FIELD">
                                <attribute-override name="firstName">
                                  <column name="NAME_ID_FIRSTNAME"/>
                                </attribute-override>
                                <attribute-override name="lastName">
                                  <column name="NAME_ID_LASTNAME"/>
                                </attribute-override>
                              </embedded-id>
                            </attributes>
                          </mapped-superclass>
                        """;

        assertEquals(expected, xmls.get(2));

        expected = """
                          <entity class="io.openliberty.data.internal.persistence.orm.WithMappedSuperclassPrime">
                            <table name="WithMappedSuperclassPrime"/>
                            <attributes>
                              <basic name="URL" access="FIELD">
                              </basic>
                            </attributes>
                          </entity>
                        """;

        assertEquals(expected, xmls.get(3));

        expected = """
                          <embeddable class="io.openliberty.data.internal.persistence.orm.SuperBeta$Script">
                            <attributes>
                              <basic name="lowercase" access="FIELD">
                                <column nullable="false"/>
                              </basic>
                              <basic name="uppercase" access="FIELD">
                                <column nullable="false"/>
                              </basic>
                            </attributes>
                          </embeddable>
                        """;

        assertEquals(expected, xmls.get(4));

        expected = """
                          <embeddable class="io.openliberty.data.internal.persistence.orm.SuperGammaPrime$Name">
                            <attributes>
                              <basic name="firstName" access="PROPERTY">
                              </basic>
                              <basic name="lastName" access="PROPERTY">
                              </basic>
                            </attributes>
                          </embeddable>
                        """;

        assertEquals(expected, xmls.get(5));
    }

    @Test
    public void converterEntityTest() {
        EntityParser p = new EntityParser("", null);
        p.parseUnannotatedEntity(WithConverter.class);
        List<String> xmls = p.generateView();

        assertEquals(4, xmls.size());

        String expected = """
                          <entity class="io.openliberty.data.internal.persistence.orm.WithConverter">
                            <table name="WithConverter"/>
                            <attributes>
                              <id name="id" access="FIELD">
                                <column nullable="false"/>
                              </id>
                              <basic name="firstName" access="FIELD">
                              </basic>
                              <basic name="lastName" access="PROPERTY">
                              </basic>
                              <version name="version" access="FIELD">
                              </version>
                              <element-collection name="aliases" access="FIELD" fetch="EAGER">
                              </element-collection>
                            </attributes>
                          </entity>
                        """;
        assertEquals(expected, xmls.get(0));

        expected = """
                          <converter class="io.openliberty.data.internal.persistence.orm.TestConverters$ClassConverter">
                          </converter>
                        """;
        assertEquals(expected, xmls.get(1));

        expected = """
                          <converter class="io.openliberty.data.internal.persistence.orm.TestConverters$FieldConverter">
                          </converter>
                        """;
        assertEquals(expected, xmls.get(2));

        expected = """
                          <converter class="io.openliberty.data.internal.persistence.orm.TestConverters$MethodConverter">
                          </converter>
                        """;
        assertEquals(expected, xmls.get(3));
    }

    @Test
    public void converterComplexEntityTest() {
        EntityParser p = new EntityParser("", null);
        p.parseUnannotatedEntity(WithConverterComplex.class);
        List<String> xmls = p.generateView();

        assertEquals(6, xmls.size());

        String expected = """
                          <mapped-superclass class="io.openliberty.data.internal.persistence.orm.ConverterSuper">
                            <attributes>
                              <basic name="firstName" access="FIELD">
                              </basic>
                              <embedded name="emb" access="FIELD">
                                <attribute-override name="lastName">
                                  <column name="EMB_LASTNAME"/>
                                </attribute-override>
                              </embedded>
                            </attributes>
                          </mapped-superclass>
                        """;

        assertEquals(expected, xmls.get(0));

        expected = """
                          <entity class="io.openliberty.data.internal.persistence.orm.WithConverterComplex">
                            <table name="WithConverterComplex"/>
                            <attributes>
                              <id name="id" access="FIELD">
                                <column nullable="false"/>
                              </id>
                            </attributes>
                          </entity>
                        """;
        assertEquals(expected, xmls.get(1));

        expected = """
                          <embeddable class="io.openliberty.data.internal.persistence.orm.ConverterEmbedded">
                            <attributes>
                              <basic name="lastName" access="PROPERTY">
                              </basic>
                            </attributes>
                          </embeddable>
                        """;

        assertEquals(expected, xmls.get(2));

        expected = """
                          <converter class="io.openliberty.data.internal.persistence.orm.TestConverters$ClassConverter">
                          </converter>
                        """;
        assertEquals(expected, xmls.get(3));

        expected = """
                          <converter class="io.openliberty.data.internal.persistence.orm.TestConverters$FieldConverter">
                          </converter>
                        """;
        assertEquals(expected, xmls.get(4));

        expected = """
                          <converter class="io.openliberty.data.internal.persistence.orm.TestConverters$MethodConverter">
                          </converter>
                        """;
        assertEquals(expected, xmls.get(5));
    }

    @Test
    public void annotatedWithConverterEntityTest() {
        EntityParser p = new EntityParser("", null);
        p.parseAnnotatedEntity(WithEntityAnnotation.class);
        List<String> xmls = p.generateView();

        assertEquals(3, xmls.size());

        String expected = """
                          <converter class="io.openliberty.data.internal.persistence.orm.TestConverters$ClassConverter">
                          </converter>
                        """;
        assertEquals(expected, xmls.get(0));

        expected = """
                          <converter class="io.openliberty.data.internal.persistence.orm.TestConverters$FieldConverter">
                          </converter>
                        """;
        assertEquals(expected, xmls.get(1));

        expected = """
                          <converter class="io.openliberty.data.internal.persistence.orm.TestConverters$MethodConverter">
                          </converter>
                        """;
        assertEquals(expected, xmls.get(2));
    }

    @Test
    public void multilayerEmbeddedEntityTest() {
        EntityParser p = new EntityParser("", null);
        p.parseUnannotatedEntity(WithMultilayerEmbedded.class);
        List<String> xmls = p.generateView();

        assertEquals(3, xmls.size());

        String expected = """
                          <entity class="io.openliberty.data.internal.persistence.orm.WithMultilayerEmbedded">
                            <table name="WithMultilayerEmbedded"/>
                            <attributes>
                              <id name="id" access="FIELD">
                                <column nullable="false"/>
                              </id>
                              <embedded name="center" access="FIELD">
                                <attribute-override name="x">
                                  <column name="CENTER_X"/>
                                </attribute-override>
                                <attribute-override name="y">
                                  <column name="CENTER_Y"/>
                                </attribute-override>
                              </embedded>
                              <embedded name="side" access="FIELD">
                                <attribute-override name="a.x">
                                  <column name="SIDE_A_X"/>
                                </attribute-override>
                                <attribute-override name="a.y">
                                  <column name="SIDE_A_Y"/>
                                </attribute-override>
                                <attribute-override name="b.x">
                                  <column name="SIDE_B_X"/>
                                </attribute-override>
                                <attribute-override name="b.y">
                                  <column name="SIDE_B_Y"/>
                                </attribute-override>
                              </embedded>
                            </attributes>
                          </entity>
                        """;
        assertEquals(expected, xmls.get(0));

        expected = """
                          <embeddable class="io.openliberty.data.internal.persistence.orm.WithMultilayerEmbedded$Coordinate">
                            <attributes>
                              <basic name="x" access="FIELD">
                                <column nullable="false"/>
                              </basic>
                              <basic name="y" access="FIELD">
                                <column nullable="false"/>
                              </basic>
                            </attributes>
                          </embeddable>
                        """;
        assertEquals(expected, xmls.get(1));

        expected = """
                          <embeddable class="io.openliberty.data.internal.persistence.orm.WithMultilayerEmbedded$Side">
                            <attributes>
                              <embedded name="a" access="FIELD">
                              </embedded>
                              <embedded name="b" access="FIELD">
                              </embedded>
                            </attributes>
                          </embeddable>
                        """;
        assertEquals(expected, xmls.get(2));

    }

    @Test
    public void multilayerEmbeddedCollectionEntityTest() {
        EntityParser p = new EntityParser("", null);
        p.parseUnannotatedEntity(WithMultilayerEmbeddedCollection.class);
        List<String> xmls = p.generateView();

        assertEquals(3, xmls.size());

        String expected = """
                          <entity class="io.openliberty.data.internal.persistence.orm.WithMultilayerEmbeddedCollection">
                            <table name="WithMultilayerEmbeddedCollection"/>
                            <attributes>
                              <id name="id" access="FIELD">
                                <column nullable="false"/>
                              </id>
                              <element-collection name="sides" access="PROPERTY" fetch="EAGER">
                                <collection-table name="WITHMULTILAYEREMBEDDEDCOLLECTION_SIDES">
                                  <join-column name="id"/>
                                </collection-table>
                                <attribute-override name="a.x">
                                  <column name="SIDES_A_X"/>
                                </attribute-override>
                                <attribute-override name="a.y">
                                  <column name="SIDES_A_Y"/>
                                </attribute-override>
                                <attribute-override name="b.x">
                                  <column name="SIDES_B_X"/>
                                </attribute-override>
                                <attribute-override name="b.y">
                                  <column name="SIDES_B_Y"/>
                                </attribute-override>
                              </element-collection>
                              <embedded name="center" access="FIELD">
                                <attribute-override name="x">
                                  <column name="CENTER_X"/>
                                </attribute-override>
                                <attribute-override name="y">
                                  <column name="CENTER_Y"/>
                                </attribute-override>
                              </embedded>
                            </attributes>
                          </entity>
                        """;
        assertEquals(expected, xmls.get(0));

        expected = """
                          <embeddable class="io.openliberty.data.internal.persistence.orm.WithMultilayerEmbeddedCollection$Coordinate">
                            <attributes>
                              <basic name="x" access="FIELD">
                                <column nullable="false"/>
                              </basic>
                              <basic name="y" access="FIELD">
                                <column nullable="false"/>
                              </basic>
                            </attributes>
                          </embeddable>
                        """;
        assertEquals(expected, xmls.get(1));

        expected = """
                          <embeddable class="io.openliberty.data.internal.persistence.orm.WithMultilayerEmbeddedCollection$Side">
                            <attributes>
                              <embedded name="a" access="FIELD">
                              </embedded>
                              <embedded name="b" access="FIELD">
                              </embedded>
                            </attributes>
                          </embeddable>
                        """;
        assertEquals(expected, xmls.get(2));
    }

    @Test
    public void embeddedMulilayerCollectionEntityTest() {
        EntityParser p = new EntityParser("", null);
        p.parseUnannotatedEntity(WithEmbeddedMultilayerCollection.class);
        List<String> xmls = p.generateView();

        assertEquals(3, xmls.size());

        String expected = """
                          <entity class="io.openliberty.data.internal.persistence.orm.WithEmbeddedMultilayerCollection">
                            <table name="WithEmbeddedMultilayerCollection"/>
                            <attributes>
                              <id name="id" access="FIELD">
                                <column nullable="false"/>
                              </id>
                              <embedded name="center" access="FIELD">
                                <attribute-override name="x">
                                  <column name="CENTER_X"/>
                                </attribute-override>
                                <attribute-override name="y">
                                  <column name="CENTER_Y"/>
                                </attribute-override>
                              </embedded>
                              <embedded name="side" access="PROPERTY">
                                <attribute-override name="cords.x">
                                  <column name="SIDE_CORDS_X"/>
                                </attribute-override>
                                <attribute-override name="cords.y">
                                  <column name="SIDE_CORDS_Y"/>
                                </attribute-override>
                              </embedded>
                            </attributes>
                          </entity>
                        """;
        assertEquals(expected, xmls.get(0));

        expected = """
                          <embeddable class="io.openliberty.data.internal.persistence.orm.WithEmbeddedMultilayerCollection$Coordinate">
                            <attributes>
                              <basic name="x" access="FIELD">
                                <column nullable="false"/>
                              </basic>
                              <basic name="y" access="FIELD">
                                <column nullable="false"/>
                              </basic>
                            </attributes>
                          </embeddable>
                        """;
        assertEquals(expected, xmls.get(1));

        expected = """
                          <embeddable class="io.openliberty.data.internal.persistence.orm.WithEmbeddedMultilayerCollection$Side">
                            <attributes>
                              <element-collection name="cords" access="FIELD" fetch="EAGER">
                              </element-collection>
                            </attributes>
                          </embeddable>
                        """;
        assertEquals(expected, xmls.get(2));
    }
}
