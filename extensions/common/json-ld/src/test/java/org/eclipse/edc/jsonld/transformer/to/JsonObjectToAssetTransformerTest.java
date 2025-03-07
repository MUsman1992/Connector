/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.jsonld.transformer.to;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.eclipse.edc.connector.core.transform.TypeTransformerRegistryImpl;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.transformer.Payload;
import org.eclipse.edc.jsonld.transformer.PayloadTransformer;
import org.eclipse.edc.junit.assertions.AbstractResultAssert;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.jsonld.transformer.to.TestInput.getExpanded;
import static org.eclipse.edc.jsonld.util.JacksonJsonLd.createObjectMapper;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.CoreConstants.EDC_PREFIX;
import static org.eclipse.edc.spi.types.domain.asset.Asset.EDC_ASSET_PRIVATE_PROPERTIES;
import static org.eclipse.edc.spi.types.domain.asset.Asset.EDC_ASSET_TYPE;
import static org.eclipse.edc.spi.types.domain.asset.Asset.PROPERTY_CONTENT_TYPE;
import static org.eclipse.edc.spi.types.domain.asset.Asset.PROPERTY_DESCRIPTION;
import static org.eclipse.edc.spi.types.domain.asset.Asset.PROPERTY_ID;
import static org.eclipse.edc.spi.types.domain.asset.Asset.PROPERTY_NAME;
import static org.eclipse.edc.spi.types.domain.asset.Asset.PROPERTY_VERSION;
import static org.mockito.Mockito.mock;

class JsonObjectToAssetTransformerTest {

    private static final String TEST_ASSET_ID = "some-asset-id";
    private static final String TEST_ASSET_NAME = "some-asset-name";
    private static final String TEST_ASSET_DESCRIPTION = "some description";
    private static final String TEST_ASSET_CONTENTTYPE = "application/json";
    private static final String TEST_ASSET_VERSION = "0.2.1";
    private static final int CUSTOM_PAYLOAD_AGE = 34;
    private static final String CUSTOM_PAYLOAD_NAME = "max";
    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TitaniumJsonLd jsonLd = new TitaniumJsonLd(mock(Monitor.class));
    private ObjectMapper jsonPmapper;
    private TypeTransformerRegistry typeTransformerRegistry;

    @BeforeEach
    void setUp() throws JsonProcessingException {

        jsonPmapper = createObjectMapper();
        var transformer = new JsonObjectToAssetTransformer();
        typeTransformerRegistry = new TypeTransformerRegistryImpl();
        typeTransformerRegistry.register(new JsonValueToGenericTypeTransformer(jsonPmapper));
        typeTransformerRegistry.register(transformer);
        typeTransformerRegistry.register(new PayloadTransformer());
        typeTransformerRegistry.registerTypeAlias(EDC_NAMESPACE + "customPayload", Payload.class);
    }

    @Test
    void transform_onlyKnownProperties() {
        var jsonObj = jsonFactory.createObjectBuilder()
                .add(CONTEXT, createContextBuilder().build())
                .add(TYPE, EDC_ASSET_TYPE)
                .add(ID, TEST_ASSET_ID)
                .add("properties", createPropertiesBuilder().build())
                .build();
        jsonObj = expand(jsonObj);
        var asset = typeTransformerRegistry.transform(getExpanded(jsonObj), Asset.class);

        AbstractResultAssert.assertThat(asset).withFailMessage(asset::getFailureDetail).isSucceeded();
        assertThat(asset.getContent().getProperties())
                .hasSize(5)
                .containsEntry(PROPERTY_ID, TEST_ASSET_ID)
                .containsEntry(PROPERTY_ID, asset.getContent().getId())
                .containsEntry(PROPERTY_NAME, TEST_ASSET_NAME)
                .containsEntry(PROPERTY_DESCRIPTION, TEST_ASSET_DESCRIPTION)
                .containsEntry(PROPERTY_CONTENT_TYPE, TEST_ASSET_CONTENTTYPE)
                .containsEntry(PROPERTY_VERSION, TEST_ASSET_VERSION);
    }

    @Test
    void transform_withPrivateProperties() {
        var jsonObj = jsonFactory.createObjectBuilder()
                .add(CONTEXT, createContextBuilder().build())
                .add(TYPE, EDC_ASSET_TYPE)
                .add(ID, TEST_ASSET_ID)
                .add("properties", createPropertiesBuilder().build())
                .add(EDC_ASSET_PRIVATE_PROPERTIES, jsonFactory.createObjectBuilder().add("test-prop", "test-val").build())
                .build();
        jsonObj = expand(jsonObj);
        var asset = typeTransformerRegistry.transform(jsonObj, Asset.class);

        AbstractResultAssert.assertThat(asset).withFailMessage(asset::getFailureDetail).isSucceeded();
        assertThat(asset.getContent().getProperties())
                .hasSize(5)
                .containsEntry(PROPERTY_ID, TEST_ASSET_ID)
                .containsEntry(PROPERTY_ID, asset.getContent().getId())
                .containsEntry(PROPERTY_NAME, TEST_ASSET_NAME)
                .containsEntry(PROPERTY_DESCRIPTION, TEST_ASSET_DESCRIPTION)
                .containsEntry(PROPERTY_CONTENT_TYPE, TEST_ASSET_CONTENTTYPE)
                .containsEntry(PROPERTY_VERSION, TEST_ASSET_VERSION);
        assertThat(asset.getContent().getPrivateProperties())
                .hasSize(1)
                .containsEntry(EDC_NAMESPACE + "test-prop", "test-val");
    }

    @Test
    void transform_withCustomProperty() {
        var jsonObj = jsonFactory.createObjectBuilder()
                .add(CONTEXT, createContextBuilder().build())
                .add(TYPE, EDC_ASSET_TYPE)
                .add(ID, TEST_ASSET_ID)
                .add("properties", createPropertiesBuilder()
                        .add("payload", createPayloadBuilder().build())
                        .build())
                .build();
        jsonObj = expand(jsonObj);
        var asset = typeTransformerRegistry.transform(getExpanded(jsonObj), Asset.class);
        AbstractResultAssert.assertThat(asset).withFailMessage(asset::getFailureDetail).isSucceeded();
        assertThat(asset.getContent().getProperties())
                .hasSize(6)
                .hasEntrySatisfying(EDC_NAMESPACE + "payload", o -> assertThat(o).isInstanceOf(Payload.class)
                        .hasFieldOrPropertyWithValue("age", CUSTOM_PAYLOAD_AGE)
                        .hasFieldOrPropertyWithValue("name", CUSTOM_PAYLOAD_NAME));
    }

    @Test
    void transform_noVocab_shouldFilterUnprefixedValues() {
        var jsonObj = jsonFactory.createObjectBuilder()
                .add(CONTEXT, createContextBuilder().addNull(VOCAB).build())
                .add(TYPE, EDC_ASSET_TYPE)
                .add(ID, TEST_ASSET_ID)
                // will only work if properties is prefixed with "edc:"
                .add("edc:properties", createPropertiesBuilder().build())
                .build();
        jsonObj = expand(jsonObj);

        var asset = typeTransformerRegistry.transform(getExpanded(jsonObj), Asset.class);

        assertThat(asset.getContent().getProperties()).hasSize(2)
                .containsEntry(PROPERTY_ID, TEST_ASSET_ID)
                .containsEntry(PROPERTY_VERSION, TEST_ASSET_VERSION);

        assertThat(asset.getContent().getProperties().get(PROPERTY_ID)).isEqualTo(asset.getContent().getId());
    }

    @Test
    void transform_noEdcContextDecl_shouldUseRawPrefix() {
        var jsonObj = jsonFactory.createObjectBuilder()
                .add(CONTEXT, createContextBuilder().addNull(EDC_PREFIX).build())
                .add(TYPE, EDC_ASSET_TYPE)
                .add(ID, TEST_ASSET_ID)
                .add("properties", createPropertiesBuilder().add("payload", createPayloadBuilder().build()).build())
                .build();
        jsonObj = expand(jsonObj);
        var asset = typeTransformerRegistry.transform(getExpanded(jsonObj), Asset.class);

        AbstractResultAssert.assertThat(asset).withFailMessage(asset::getFailureDetail).isSucceeded();
        assertThat(asset.getContent().getVersion()).isNull();
        assertThat(asset.getContent().getProperties())
                .containsEntry("edc:version", TEST_ASSET_VERSION);
    }

    private JsonObjectBuilder createPayloadBuilder() {
        return jsonFactory.createObjectBuilder()
                .add(TYPE, "customPayload")
                .add("name", CUSTOM_PAYLOAD_NAME)
                .add("age", CUSTOM_PAYLOAD_AGE);
    }

    private JsonObjectBuilder createPropertiesBuilder() {
        return jsonFactory.createObjectBuilder()
                .add("name", TEST_ASSET_NAME)
                .add("description", TEST_ASSET_DESCRIPTION)
                .add("edc:version", TEST_ASSET_VERSION)
                .add("contenttype", TEST_ASSET_CONTENTTYPE);
    }

    private JsonObjectBuilder createContextBuilder() {
        return jsonFactory.createObjectBuilder()
                .add(VOCAB, EDC_NAMESPACE)
                .add(EDC_PREFIX, EDC_NAMESPACE);
    }

    private JsonObject expand(JsonObject jsonObject) {
        return jsonLd.expand(jsonObject).orElseThrow(f -> new AssertionError(f.getFailureDetail()));
    }

}
