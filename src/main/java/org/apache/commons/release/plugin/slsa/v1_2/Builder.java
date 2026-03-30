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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Entity that executed the build and is trusted to have correctly performed the operation and populated the provenance.
 *
 * @see <a href="https://slsa.dev/spec/v1.2">SLSA v1.2 Specification</a>
 */
public class Builder {

    /** Identifier URI of the builder. */
    @JsonProperty("id")
    private String id = "https://commons.apache.org/builds/0.1.0";

    /** Orchestrator dependencies that may affect provenance generation. */
    @JsonProperty("builderDependencies")
    private List<ResourceDescriptor> builderDependencies = new ArrayList<>();

    /** Map of build platform component names to their versions. */
    @JsonProperty("version")
    private Map<String, String> version = new HashMap<>();

    /** Creates a new Builder instance. */
    public Builder() {
    }

    /**
     * Returns the identifier of the builder.
     *
     * @return the builder identifier URI
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the identifier of the builder.
     *
     * @param id the builder identifier URI
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns orchestrator dependencies that do not run within the build workload and do not affect the build output,
     * but may affect provenance generation or security guarantees.
     *
     * @return the list of builder dependencies, or {@code null} if not set
     */
    public List<ResourceDescriptor> getBuilderDependencies() {
        return builderDependencies;
    }

    /**
     * Sets the orchestrator dependencies that may affect provenance generation or security guarantees.
     *
     * @param builderDependencies the list of builder dependencies
     */
    public void setBuilderDependencies(List<ResourceDescriptor> builderDependencies) {
        this.builderDependencies = builderDependencies;
    }

    /**
     * Returns a map of build platform component names to their versions.
     *
     * @return the version map, or {@code null} if not set
     */
    public Map<String, String> getVersion() {
        return version;
    }

    /**
     * Sets the map of build platform component names to their versions.
     *
     * @param version the version map
     */
    public void setVersion(Map<String, String> version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Builder)) {
            return false;
        }
        Builder that = (Builder) o;
        return Objects.equals(id, that.id)
                && Objects.equals(builderDependencies, that.builderDependencies)
                && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, builderDependencies, version);
    }

    @Override
    public String toString() {
        return "Builder{id='" + id + "', builderDependencies=" + builderDependencies + ", version=" + version + '}';
    }
}
