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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Inputs that define the build: the build type, external and internal parameters, and resolved dependencies.
 *
 * <p>Specifies everything that influenced the build output. Together with {@link RunDetails}, it forms the complete
 * {@link Provenance} record.</p>
 *
 * @see <a href="https://slsa.dev/spec/v1.2">SLSA v1.2 Specification</a>
 * @since 1.10.0
 */
public class BuildDefinition {

    /**
     * URI indicating what type of build was performed.
     */
    @JsonProperty("buildType")
    private String buildType = "https://commons.apache.org/proper/commons-release-plugin/slsa/v0.1.0";

    /**
     * Inputs passed to the build.
     */
    @JsonProperty("externalParameters")
    private Map<String, Object> externalParameters = new HashMap<>();

    /**
     * Parameters set by the build platform.
     */
    @JsonProperty("internalParameters")
    private Map<String, Object> internalParameters = new HashMap<>();

    /**
     * Artifacts the build depends on, specified by URI and digest.
     */
    @JsonProperty("resolvedDependencies")
    private List<ResourceDescriptor> resolvedDependencies;

    /**
     * Creates a new BuildDefinition instance with the default build type.
     */
    public BuildDefinition() {
    }

    /**
     * Creates a new BuildDefinition with the given build type and external parameters.
     *
     * @param buildType          URI indicating what type of build was performed
     * @param externalParameters inputs passed to the build
     */
    public BuildDefinition(String buildType, Map<String, Object> externalParameters) {
        this.buildType = buildType;
        this.externalParameters = externalParameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BuildDefinition that = (BuildDefinition) o;
        return Objects.equals(buildType, that.buildType) && Objects.equals(externalParameters, that.externalParameters) && Objects.equals(internalParameters,
                that.internalParameters) && Objects.equals(resolvedDependencies, that.resolvedDependencies);
    }

    /**
     * Gets the URI indicating what type of build was performed.
     *
     * <p>Determines the meaning of {@code externalParameters} and {@code internalParameters}.</p>
     *
     * @return the build type URI
     */
    public String getBuildType() {
        return buildType;
    }

    /**
     * Gets the inputs passed to the build, such as command-line arguments or environment variables.
     *
     * @return the external parameters map, or {@code null} if not set
     */
    public Map<String, Object> getExternalParameters() {
        return externalParameters;
    }

    /**
     * Gets the artifacts the build depends on, such as sources, dependencies, build tools, and base images,
     * specified by URI and digest.
     *
     * @return the internal parameters map, or {@code null} if not set
     */
    public Map<String, Object> getInternalParameters() {
        return internalParameters;
    }

    /**
     * Gets the materials that influenced the build.
     *
     * <p>Considered incomplete unless resolved materials are present.</p>
     *
     * @return the list of resolved dependencies, or {@code null} if not set
     */
    public List<ResourceDescriptor> getResolvedDependencies() {
        return resolvedDependencies;
    }

    @Override
    public int hashCode() {
        return Objects.hash(buildType, externalParameters, internalParameters, resolvedDependencies);
    }

    /**
     * Sets the URI indicating what type of build was performed.
     *
     * @param buildType the build type URI
     * @return this for chaining
     */
    public BuildDefinition setBuildType(String buildType) {
        this.buildType = buildType;
        return this;
    }

    /**
     * Sets the inputs passed to the build.
     *
     * @param externalParameters the external parameters map
     * @return this for chaining
     */
    public BuildDefinition setExternalParameters(Map<String, Object> externalParameters) {
        this.externalParameters = externalParameters;
        return this;
    }

    /**
     * Sets the artifacts the build depends on.
     *
     * @param internalParameters the internal parameters map
     * @return this for chaining
     */
    public BuildDefinition setInternalParameters(Map<String, Object> internalParameters) {
        this.internalParameters = internalParameters;
        return this;
    }

    /**
     * Sets the materials that influenced the build.
     *
     * @param resolvedDependencies the list of resolved dependencies
     * @return this for chaining
     */
    public BuildDefinition setResolvedDependencies(List<ResourceDescriptor> resolvedDependencies) {
        this.resolvedDependencies = resolvedDependencies;
        return this;
    }

    @Override
    public String toString() {
        return "BuildDefinition{buildType='" + buildType
                + "', externalParameters=" + externalParameters
                + ", internalParameters=" + internalParameters
                + ", resolvedDependencies=" + resolvedDependencies + '}';
    }
}
