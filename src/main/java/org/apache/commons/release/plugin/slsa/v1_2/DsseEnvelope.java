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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DSSE (Dead Simple Signing Envelope) that wraps a signed in-toto statement payload.
 *
 * <p>The {@code payload} field holds the serialized {@link Statement} bytes; Jackson serializes them as Base64. The
 * {@code payloadType} identifies the content type of the payload. The {@code signatures} list contains one or more
 * cryptographic signatures over the PAE-encoded payload.</p>
 *
 * <p>All three fields are REQUIRED and MUST be set, even if empty.</p>
 *
 * @see <a href="https://github.com/secure-systems-lab/dsse/blob/v1.0.2/envelope.md">DSSE Envelope specification</a>
 */
public class DsseEnvelope {

    /** The payload type URI for in-toto attestation statements. */
    public static final String PAYLOAD_TYPE = "application/vnd.in-toto+json";

    /** Content type identifying the format of {@link #payload}. */
    @JsonProperty("payloadType")
    private String payloadType = PAYLOAD_TYPE;

    /** Serialized statement bytes, Base64-encoded in JSON. */
    @JsonProperty("payload")
    private byte[] payload;

    /** One or more signatures over the PAE-encoded payload. */
    @JsonProperty("signatures")
    private List<Signature> signatures;

    /** Creates a new DsseEnvelope instance with {@code payloadType} pre-set to {@link #PAYLOAD_TYPE}. */
    public DsseEnvelope() {
    }

    /**
     * Returns the payload type URI.
     *
     * @return the payload type, never {@code null} in a valid envelope
     */
    public String getPayloadType() {
        return payloadType;
    }

    /**
     * Sets the payload type URI.
     *
     * @param payloadType the payload type URI
     */
    public void setPayloadType(String payloadType) {
        this.payloadType = payloadType;
    }

    /**
     * Returns the serialized payload bytes.
     *
     * <p>When serialized to JSON the bytes are Base64-encoded.</p>
     *
     * @return the payload bytes, or {@code null} if not set
     */
    public byte[] getPayload() {
        return payload;
    }

    /**
     * Sets the serialized payload bytes.
     *
     * @param payload the payload bytes
     */
    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    /**
     * Returns the list of signatures over the PAE-encoded payload.
     *
     * @return the signatures, or {@code null} if not set
     */
    public List<Signature> getSignatures() {
        return signatures;
    }

    /**
     * Sets the list of signatures over the PAE-encoded payload.
     *
     * @param signatures the signatures
     */
    public void setSignatures(List<Signature> signatures) {
        this.signatures = signatures;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DsseEnvelope)) {
            return false;
        }
        DsseEnvelope envelope = (DsseEnvelope) o;
        return Objects.equals(payloadType, envelope.payloadType) && Arrays.equals(payload, envelope.payload)
                && Objects.equals(signatures, envelope.signatures);
    }

    @Override
    public int hashCode() {
        return Objects.hash(payloadType, Arrays.hashCode(payload), signatures);
    }

    @Override
    public String toString() {
        return "DsseEnvelope{payloadType='" + payloadType + "', payload=<" + (payload != null ? payload.length : 0)
                + " bytes>, signatures=" + signatures + '}';
    }
}
