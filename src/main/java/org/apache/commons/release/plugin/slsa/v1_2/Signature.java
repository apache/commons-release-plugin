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
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single cryptographic signature within a DSSE envelope.
 *
 * <p>The {@code sig} field holds the raw signature bytes; Jackson serializes them as Base64. The optional
 * {@code keyid} field identifies which key produced the signature.</p>
 *
 * @see <a href="https://github.com/secure-systems-lab/dsse/blob/v1.0.2/envelope.md">DSSE Envelope specification</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Signature {

    /**
     * Hint for which key was used to sign; unset is treated as empty.
     *
     * <p>Consumers MUST NOT require this field to be set, and MUST NOT use it for security decisions.</p>
     */
    @JsonProperty("keyid")
    private String keyid;

    /** Raw signature bytes over the PAE-encoded payload, Base64-encoded in JSON. */
    @JsonProperty("sig")
    private byte[] sig;

    /** Creates a new Signature instance. */
    public Signature() {
    }

    /**
     * Returns the key identifier hint, or {@code null} if not set.
     *
     * @return the key identifier, or {@code null}
     */
    public String getKeyid() {
        return keyid;
    }

    /**
     * Sets the key identifier hint.
     *
     * @param keyid the key identifier, or {@code null} to leave unset
     */
    public void setKeyid(String keyid) {
        this.keyid = keyid;
    }

    /**
     * Returns the raw signature bytes.
     *
     * <p>When serialized to JSON the bytes are Base64-encoded.</p>
     *
     * @return the signature bytes, or {@code null} if not set
     */
    public byte[] getSig() {
        return sig;
    }

    /**
     * Sets the raw signature bytes.
     *
     * @param sig the signature bytes
     */
    public void setSig(byte[] sig) {
        this.sig = sig;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Signature)) {
            return false;
        }
        Signature signature = (Signature) o;
        return Objects.equals(keyid, signature.keyid) && Arrays.equals(sig, signature.sig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyid, Arrays.hashCode(sig));
    }

    @Override
    public String toString() {
        return "Signature{keyid='" + keyid + "', sig=<" + (sig != null ? sig.length : 0) + " bytes>}";
    }
}
