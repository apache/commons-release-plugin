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

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * In-toto v1 attestation envelope that binds a set of subject artifacts to an SLSA provenance predicate.
 *
 * @see <a href="https://github.com/in-toto/attestation/blob/main/spec/v1/statement.md">in-toto Statement v1</a>
 * @since 1.10.0
 */
public class Statement {

    /** The in-toto statement schema URI. */
    public static final String TYPE = "https://in-toto.io/Statement/v1";
    /** The provenance predicate. */
    @JsonProperty("predicate")
    private Provenance predicate;
    /** URI identifying the type of the predicate. */
    @JsonProperty("predicateType")
    private String predicateType;
    /** Software artifacts that the attestation applies to. */
    @JsonProperty("subject")
    private List<ResourceDescriptor> subject;

    /** Creates a new Statement instance. */
    public Statement() {
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Statement)) {
            return false;
        }
        Statement statement = (Statement) o;
        return Objects.equals(subject, statement.subject) && Objects.equals(predicateType, statement.predicateType) && Objects.equals(predicate,
                statement.predicate);
    }

    /**
     * Type of JSON object.
     *
     * @return Always {@value TYPE}
     */
    @JsonProperty("_type")
    public String getType() {
        return TYPE;
    }

    /**
     * Gets the provenance predicate.
     *
     * <p>Unset is treated the same as set-but-empty. May be omitted if {@code predicateType} fully describes the
     * predicate.</p>
     *
     * @return the provenance predicate, or {@code null} if not set
     */
    public Provenance getPredicate() {
        return predicate;
    }

    /**
     * Gets the URI identifying the type of the predicate.
     *
     * @return the predicate type URI, or {@code null} if no predicate has been set
     */
    public String getPredicateType() {
        return predicateType;
    }

    /**
     * Gets the set of software artifacts that the attestation applies to.
     *
     * <p>Each element represents a single artifact. Artifacts are matched purely by digest, regardless of content type.</p>
     *
     * @return the list of subject artifacts, or {@code null} if not set
     */
    public List<ResourceDescriptor> getSubject() {
        return subject;
    }

    @Override
    public int hashCode() {
        return Objects.hash(subject, predicateType, predicate);
    }

    /**
     * Sets the provenance predicate and automatically assigns {@code predicateType} to the SLSA provenance v1 URI.
     *
     * @param predicate the provenance predicate
     * @return this for chaining
     */
    public Statement setPredicate(Provenance predicate) {
        this.predicate = predicate;
        this.predicateType = Provenance.PREDICATE_TYPE;
        return this;
    }

    /**
     * Sets the set of software artifacts that the attestation applies to.
     *
     * @param subject the list of subject artifacts
     * @return this for chaining
     */
    public Statement setSubject(List<ResourceDescriptor> subject) {
        this.subject = subject;
        return this;
    }

    @Override
    public String toString() {
        return "Statement{_type='" + TYPE + "', subject=" + subject + ", predicateType='" + predicateType + "', predicate=" + predicate + '}';
    }
}
