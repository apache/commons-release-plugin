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

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Description of an artifact or resource referenced in the build, identified by URI and cryptographic digest.
 *
 * <p>Used to represent inputs to, outputs from, or byproducts of the build process.</p>
 *
 * @see <a href="https://slsa.dev/spec/v1.2">SLSA v1.2 Specification</a>
 * @since 1.10.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResourceDescriptor {

    /** Additional key-value metadata about the resource. */
    @JsonProperty("annotations")
    private Map<String, Object> annotations;
    /** Raw contents of the resource, base64-encoded in JSON. */
    @JsonProperty("content")
    private byte[] content;
    /** Map of digest algorithm names to hex-encoded values. */
    @JsonProperty("digest")
    private Map<String, String> digest;
    /** Download URI for the resource, if different from {@link #uri}. */
    @JsonProperty("downloadLocation")
    private String downloadLocation;
    /** Media type of the resource. */
    @JsonProperty("mediaType")
    private String mediaType;
    /** Human-readable name of the resource. */
    @JsonProperty("name")
    private String name;
    /** URI identifying the resource. */
    @JsonProperty("uri")
    private String uri;

    /** Creates a new ResourceDescriptor instance. */
    public ResourceDescriptor() {
    }

    /**
     * Creates a new ResourceDescriptor with the given URI and digest.
     *
     * @param uri    URI identifying the resource.
     * @param digest map of digest algorithm names to their hex-encoded values.
     */
    public ResourceDescriptor(String uri, Map<String, String> digest) {
        this.uri = uri;
        this.digest = digest;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ResourceDescriptor that = (ResourceDescriptor) o;
        return Objects.equals(uri, that.uri) && Objects.equals(digest, that.digest);
    }

    /**
     * Gets additional key-value metadata about the resource, such as filename, size, or builder-specific attributes.
     *
     * @return the annotations map, or {@code null} if not set.
     */
    public Map<String, Object> getAnnotations() {
        return annotations;
    }

    /**
     * Gets the raw contents of the resource, base64-encoded when serialized to JSON.
     *
     * @return the resource content, or {@code null} if not set.
     */
    public byte[] getContent() {
        return content;
    }

    /**
     * Gets the map of cryptographic digest algorithms to their corresponding hex-encoded values for this resource.
     *
     * <p>Common keys include {@code "sha256"} and {@code "sha512"}.</p>
     *
     * @return the digest map, or {@code null} if not set.
     */
    public Map<String, String> getDigest() {
        return digest;
    }

    /**
     * Gets the download URI for the resource, if different from {@link #getUri()}.
     *
     * @return the download location URI, or {@code null} if not set.
     */
    public String getDownloadLocation() {
        return downloadLocation;
    }

    /**
     * Gets the media type of the resource (e.g., {@code "application/octet-stream"}).
     *
     * @return the media type, or {@code null} if not set.
     */
    public String getMediaType() {
        return mediaType;
    }

    /**
     * Gets the name of the resource.
     *
     * @return the resource name, or {@code null} if not set.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the URI identifying the resource.
     *
     * @return the resource URI, or {@code null} if not set.
     */
    public String getUri() {
        return uri;
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri, digest);
    }

    /**
     * Sets additional key-value metadata about the resource.
     *
     * @param annotations the annotations map.
     * @return this for chaining.
     */
    public ResourceDescriptor setAnnotations(Map<String, Object> annotations) {
        this.annotations = annotations;
        return this;
    }

    /**
     * Sets the raw contents of the resource.
     *
     * @param content the resource content.
     * @return this for chaining.
     */
    public ResourceDescriptor setContent(byte[] content) {
        this.content = content;
        return this;
    }

    /**
     * Sets the map of cryptographic digest algorithms to their hex-encoded values.
     *
     * @param digest the digest map.
     * @return this for chaining.
     */
    public ResourceDescriptor setDigest(Map<String, String> digest) {
        this.digest = digest;
        return this;
    }

    /**
     * Sets the download URI for the resource.
     *
     * @param downloadLocation the download location URI.
     * @return this for chaining.
     */
    public ResourceDescriptor setDownloadLocation(String downloadLocation) {
        this.downloadLocation = downloadLocation;
        return this;
    }

    /**
     * Sets the media type of the resource.
     *
     * @param mediaType the media type.
     * @return this for chaining.
     */
    public ResourceDescriptor setMediaType(String mediaType) {
        this.mediaType = mediaType;
        return this;
    }

    /**
     * Sets the name of the resource.
     *
     * @param name the resource name.
     * @return this for chaining.
     */
    public ResourceDescriptor setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets the URI identifying the resource.
     *
     * @param uri the resource URI.
     * @return this for chaining.
     */
    public ResourceDescriptor setUri(String uri) {
        this.uri = uri;
        return this;
    }

    @Override
    public String toString() {
        return "ResourceDescriptor{uri='" + uri + '\'' + ", digest=" + digest + '}';
    }
}
