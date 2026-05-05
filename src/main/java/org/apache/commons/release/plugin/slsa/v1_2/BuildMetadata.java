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

import java.time.OffsetDateTime;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Metadata about a build invocation: its identifier and start and finish timestamps.
 *
 * @see <a href="https://slsa.dev/spec/v1.2">SLSA v1.2 Specification</a>
 * @since 1.10.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BuildMetadata {

    /** Timestamp when the build completed. */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @JsonProperty("finishedOn")
    private OffsetDateTime finishedOn;
    /** Identifier for this build invocation. */
    @JsonProperty("invocationId")
    private String invocationId;
    /** Timestamp when the build started. */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @JsonProperty("startedOn")
    private OffsetDateTime startedOn;

    /** Creates a new BuildMetadata instance. */
    public BuildMetadata() {
    }

    /**
     * Creates a new BuildMetadata instance with all fields set.
     *
     * @param invocationId identifier for this build invocation
     * @param startedOn    timestamp when the build started
     * @param finishedOn   timestamp when the build completed
     */
    public BuildMetadata(String invocationId, OffsetDateTime startedOn, OffsetDateTime finishedOn) {
        this.invocationId = invocationId;
        this.startedOn = startedOn;
        this.finishedOn = finishedOn;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BuildMetadata)) {
            return false;
        }
        BuildMetadata that = (BuildMetadata) o;
        return Objects.equals(invocationId, that.invocationId) && Objects.equals(startedOn, that.startedOn) && Objects.equals(finishedOn, that.finishedOn);
    }

    /**
     * Gets the timestamp of when the build completed, serialized as RFC 3339 in UTC ({@code "Z"} suffix).
     *
     * @return the completion timestamp, or {@code null} if not set
     */
    public OffsetDateTime getFinishedOn() {
        return finishedOn;
    }

    /**
     * Gets the identifier for this build invocation.
     *
     * @return the invocation identifier, or {@code null} if not set
     */
    public String getInvocationId() {
        return invocationId;
    }

    /**
     * Gets the timestamp of when the build started, serialized as RFC 3339 in UTC ({@code "Z"} suffix).
     *
     * @return the start timestamp, or {@code null} if not set
     */
    public OffsetDateTime getStartedOn() {
        return startedOn;
    }

    @Override
    public int hashCode() {
        return Objects.hash(invocationId, startedOn, finishedOn);
    }

    /**
     * Sets the timestamp of when the build completed.
     *
     * @param finishedOn the completion timestamp
     * @return this for chaining
     */
    public BuildMetadata setFinishedOn(OffsetDateTime finishedOn) {
        this.finishedOn = finishedOn;
        return this;
    }

    /**
     * Sets the identifier for this build invocation.
     *
     * @param invocationId the invocation identifier
     * @return this for chaining
     */
    public BuildMetadata setInvocationId(String invocationId) {
        this.invocationId = invocationId;
        return this;
    }

    /**
     * Sets the timestamp of when the build started.
     *
     * @param startedOn the start timestamp
     * @return this for chaining
     */
    public BuildMetadata setStartedOn(OffsetDateTime startedOn) {
        this.startedOn = startedOn;
        return this;
    }

    @Override
    public String toString() {
        return "BuildMetadata{invocationId='" + invocationId + "', startedOn=" + startedOn + ", finishedOn=" + finishedOn + '}';
    }
}
