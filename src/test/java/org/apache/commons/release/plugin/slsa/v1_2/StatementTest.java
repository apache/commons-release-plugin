/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.release.plugin.slsa.v1_2;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class StatementTest {

    private static final String FIXTURE = "/attestations/commons-text-1.4.intoto.json";

    private static ObjectMapper objectMapper;

    @BeforeAll
    static void setUp() {
        objectMapper = SlsaTestSupport.newObjectMapper();
    }

    @Test
    void deserializeThenSerialize() throws Exception {
        final JsonNode statementNode = SlsaTestSupport.readJsonResource(objectMapper, FIXTURE);
        final Statement statement = objectMapper.treeToValue(statementNode, Statement.class);
        final JsonNode serialized = objectMapper.valueToTree(statement);
        assertJsonEquals(statementNode, serialized);
    }

    @Test
    void checkDeserialized() throws Exception {
        final Statement statement = objectMapper.treeToValue(
                SlsaTestSupport.readJsonResource(objectMapper, FIXTURE), Statement.class);
        SlsaTestSupport.verifyDeserializedStatement(statement);
    }

    @Test
    void serializeThenDeserialize() throws Exception {
        final Provenance original = SlsaTestSupport.sampleProvenance();

        final String json = objectMapper.writeValueAsString(original);
        final Provenance deserialized = objectMapper.readValue(json, Provenance.class);

        SlsaTestSupport.verifyDeserializedSampleProvenance(deserialized);
        assertEquals(original, deserialized);
        assertEquals(original.hashCode(), deserialized.hashCode());
    }
}
