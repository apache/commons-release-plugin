
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DsseEnvelopeTest {

    private static final String FIXTURE = "/attestations/commons-text-1.4.intoto.dsse.json";

    /** Same fake fingerprint used as {@code gpg.keyname} in the existing statement fixture. */
    private static final String SAMPLE_KEY_ID = "3C8D57E0A2B5C6D7E8F9A0B1C2D3E4F5A6B7C8D9";

    private static final byte[] SAMPLE_SIG_BYTES = {0x0A, 0x0B, 0x0C, 0x0D};

    private static ObjectMapper objectMapper;

    @BeforeAll
    static void setUp() {
        objectMapper = SlsaTestSupport.newObjectMapper();
    }

    @Test
    void deserializeThenSerialize() throws Exception {
        final JsonNode envelopeNode = SlsaTestSupport.readJsonResource(objectMapper, FIXTURE);
        final DsseEnvelope envelope = objectMapper.treeToValue(envelopeNode, DsseEnvelope.class);
        final JsonNode serialized = objectMapper.valueToTree(envelope);
        assertJsonEquals(envelopeNode, serialized);
    }

    @Test
    void checkDeserialized() throws Exception {
        final DsseEnvelope envelope = objectMapper.treeToValue(
                SlsaTestSupport.readJsonResource(objectMapper, FIXTURE), DsseEnvelope.class);

        assertEquals(DsseEnvelope.PAYLOAD_TYPE, envelope.getPayloadType());

        final List<Signature> signatures = envelope.getSignatures();
        assertNotNull(signatures);
        assertEquals(1, signatures.size());
        final Signature signature = signatures.get(0);
        assertEquals(SAMPLE_KEY_ID, signature.getKeyid());
        assertNotNull(signature.getSig());
        assertTrue(signature.getSig().length > 0);

        // The wrapped statement bytes should round-trip into the same object verified by StatementTest.
        final byte[] payload = envelope.getPayload();
        assertNotNull(payload);
        final Statement wrapped = objectMapper.readValue(payload, Statement.class);
        SlsaTestSupport.verifyDeserializedStatement(wrapped);
    }

    @Test
    void serializeThenDeserialize() throws Exception {
        final Provenance provenance = SlsaTestSupport.sampleProvenance();
        final byte[] payload = objectMapper.writeValueAsBytes(provenance);

        final Signature signature = new Signature()
                .setKeyid(SAMPLE_KEY_ID)
                .setSig(SAMPLE_SIG_BYTES.clone());

        final DsseEnvelope original = new DsseEnvelope()
                .setPayload(payload)
                .setPayloadType(DsseEnvelope.PAYLOAD_TYPE)
                .setSignatures(Collections.singletonList(signature));

        final String json = objectMapper.writeValueAsString(original);
        final DsseEnvelope deserialized = objectMapper.readValue(json, DsseEnvelope.class);

        assertEquals(DsseEnvelope.PAYLOAD_TYPE, deserialized.getPayloadType());
        assertEquals(1, deserialized.getSignatures().size());
        final Signature deserSignature = deserialized.getSignatures().get(0);
        assertEquals(SAMPLE_KEY_ID, deserSignature.getKeyid());
        assertNotNull(deserSignature.getSig());

        // The payload round-trips back to the sample Provenance.
        final Provenance deserProvenance = objectMapper.readValue(deserialized.getPayload(), Provenance.class);
        SlsaTestSupport.verifyDeserializedSampleProvenance(deserProvenance);

        assertEquals(original, deserialized);
        assertEquals(original.hashCode(), deserialized.hashCode());
    }
}
