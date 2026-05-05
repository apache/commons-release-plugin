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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Details about the build invocation: the builder identity, execution metadata, and any byproduct artifacts.
 *
 * @see <a href="https://slsa.dev/spec/v1.2">SLSA v1.2 Specification</a>
 * @since 1.10.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RunDetails {

    /**
     * Entity that executed the build.
     */
    @JsonProperty("builder")
    private Builder builder;
    /**
     * Artifacts produced as a side effect of the build.
     */
    @JsonProperty("byproducts")
    private List<ResourceDescriptor> byproducts;
    /**
     * Metadata about the build invocation.
     */
    @JsonProperty("metadata")
    private BuildMetadata metadata;

    /**
     * Creates a new RunDetails instance.
     */
    public RunDetails() {
    }

    /**
     * Creates a new RunDetails with the given builder and metadata.
     *
     * @param builder  entity that executed the build.
     * @param metadata metadata about the build invocation.
     */
    public RunDetails(Builder builder, BuildMetadata metadata) {
        this.builder = builder;
        this.metadata = metadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RunDetails that = (RunDetails) o;
        return Objects.equals(builder, that.builder) && Objects.equals(metadata, that.metadata) && Objects.equals(byproducts, that.byproducts);
    }

    /**
     * Gets the builder that executed the invocation.
     *
     * <p>Trusted to have correctly performed the operation and populated this provenance.</p>
     *
     * @return the builder, or {@code null} if not set.
     */
    public Builder getBuilder() {
        return builder;
    }

    /**
     * Gets artifacts produced as a side effect of the build that are not the primary output.
     *
     * @return the list of byproduct artifacts, or {@code null} if not set.
     */
    public List<ResourceDescriptor> getByproducts() {
        return byproducts;
    }

    /**
     * Gets the metadata about the build invocation, including its identifier and timing.
     *
     * @return the build metadata, or {@code null} if not set.
     */
    public BuildMetadata getMetadata() {
        return metadata;
    }

    @Override
    public int hashCode() {
        return Objects.hash(builder, metadata, byproducts);
    }

    /**
     * Sets the builder that executed the invocation.
     *
     * @param builder the builder.
     * @return this for chaining.
     */
    public RunDetails setBuilder(Builder builder) {
        this.builder = builder;
        return this;
    }

    /**
     * Sets the artifacts produced as a side effect of the build that are not the primary output.
     *
     * @param byproducts the list of byproduct artifacts.
     * @return this for chaining.
     */
    public RunDetails setByproducts(List<ResourceDescriptor> byproducts) {
        this.byproducts = byproducts;
        return this;
    }

    /**
     * Sets the metadata about the build invocation.
     *
     * @param metadata the build metadata.
     * @return this for chaining.
     */
    public RunDetails setMetadata(BuildMetadata metadata) {
        this.metadata = metadata;
        return this;
    }

    @Override
    public String toString() {
        return "RunDetails{builder=" + builder + ", metadata=" + metadata + ", byproducts=" + byproducts + '}';
    }
}
