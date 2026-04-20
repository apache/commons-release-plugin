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

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Root predicate of an SLSA v1.2 provenance attestation, describing what was built and how.
 *
 * <p>Combines a {@link BuildDefinition} (the inputs) with {@link RunDetails} (the execution context). Intended to be
 * used as the {@code predicate} field of an in-toto {@link Statement}.</p>
 *
 * @see <a href="https://slsa.dev/spec/v1.2">SLSA v1.2 Specification</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Provenance {

    /** Predicate type URI used in the in-toto {@link Statement} wrapping this provenance. */
    public static final String PREDICATE_TYPE = "https://slsa.dev/provenance/v1";

    /** Inputs that defined the build. */
    @JsonProperty("buildDefinition")
    private BuildDefinition buildDefinition;

    /** Details about the build invocation. */
    @JsonProperty("runDetails")
    private RunDetails runDetails;

    /** Creates a new Provenance instance. */
    public Provenance() {
    }

    /**
     * Creates a new Provenance with the given build definition and run details.
     *
     * @param buildDefinition inputs that defined the build
     * @param runDetails      details about the build invocation
     */
    public Provenance(BuildDefinition buildDefinition, RunDetails runDetails) {
        this.buildDefinition = buildDefinition;
        this.runDetails = runDetails;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Provenance that = (Provenance) o;
        return Objects.equals(buildDefinition, that.buildDefinition) && Objects.equals(runDetails, that.runDetails);
    }

    /**
     * Gets the build definition describing all inputs that produced the build output.
     *
     * <p>Includes source code, dependencies, build tools, base images, and other materials.</p>
     *
     * @return the build definition, or {@code null} if not set
     */
    public BuildDefinition getBuildDefinition() {
        return buildDefinition;
    }

    /**
     * Gets the details about the invocation of the build tool and the environment in which it was run.
     *
     * @return the run details, or {@code null} if not set
     */
    public RunDetails getRunDetails() {
        return runDetails;
    }

    @Override
    public int hashCode() {
        return Objects.hash(buildDefinition, runDetails);
    }

    /**
     * Sets the build definition describing all inputs that produced the build output.
     *
     * @param buildDefinition the build definition
     */
    public void setBuildDefinition(BuildDefinition buildDefinition) {
        this.buildDefinition = buildDefinition;
    }

    /**
     * Sets the details about the invocation of the build tool and the environment in which it was run.
     *
     * @param runDetails the run details
     */
    public void setRunDetails(RunDetails runDetails) {
        this.runDetails = runDetails;
    }

    @Override
    public String toString() {
        return "Provenance{buildDefinition=" + buildDefinition + ", runDetails=" + runDetails + '}';
    }
}
