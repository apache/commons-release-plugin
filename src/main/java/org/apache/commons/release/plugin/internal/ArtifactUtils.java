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
package org.apache.commons.release.plugin.internal;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.release.plugin.slsa.v1_2.ResourceDescriptor;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Utilities to convert {@link Artifact} from and to other types.
 */
public final class ArtifactUtils {

    /**
     * Maps standard JDK {@link java.security.MessageDigest} algorithm names to the in-toto digest names used in SLSA {@link ResourceDescriptor} digest sets.
     *
     * <p>JDK algorithms that have no in-toto equivalent (such as {@code MD2}) are omitted.</p>
     *
     * @see <a href="https://docs.oracle.com/en/java/javase/25/docs/specs/security/standard-names.html#messagedigest-algorithms">
     *      JDK standard {@code MessageDigest} algorithm names</a>
     * @see <a href="https://github.com/in-toto/attestation/blob/main/spec/v1/digest_set.md">
     *      in-toto digest set specification</a>
     */
    private static final Map<String, String> IN_TOTO_DIGEST_NAMES;

    static {
        final Map<String, String> m = new HashMap<>();
        m.put("MD5", "md5");
        m.put("SHA-1", "sha1");
        m.put("SHA-224", "sha224");
        m.put("SHA-256", "sha256");
        m.put("SHA-384", "sha384");
        m.put("SHA-512", "sha512");
        m.put("SHA-512/224", "sha512_224");
        m.put("SHA-512/256", "sha512_256");
        m.put("SHA3-224", "sha3_224");
        m.put("SHA3-256", "sha3_256");
        m.put("SHA3-384", "sha3_384");
        m.put("SHA3-512", "sha3_512");
        IN_TOTO_DIGEST_NAMES = Collections.unmodifiableMap(m);
    }

    /** No instances. */
    private ArtifactUtils() {
        // prevent instantiation
    }

    /**
     * Gets the filename of an artifact in the default Maven repository layout.
     *
     * @param artifact A Maven artifact.
     * @return A filename.
     */
    public static String getFileName(Artifact artifact) {
        return getFileName(artifact, artifact.getArtifactHandler().getExtension());
    }

    /**
     * Gets the filename of an artifact in the default Maven repository layout, using the specified extension.
     *
     * @param artifact A Maven artifact.
     * @param extension The file name extension.
     * @return A filename.
     */
    public static String getFileName(Artifact artifact, String extension) {
        StringBuilder fileName = new StringBuilder();
        fileName.append(artifact.getArtifactId()).append("-").append(artifact.getVersion());
        if (artifact.getClassifier() != null) {
            fileName.append("-").append(artifact.getClassifier());
        }
        fileName.append(".").append(extension);
        return fileName.toString();
    }

    /**
     * Gets the Package URL corresponding to this artifact.
     *
     * @param artifact A maven artifact.
     * @return A PURL for the given artifact.
     */
    public static String getPackageUrl(Artifact artifact) {
        StringBuilder sb = new StringBuilder();
        sb.append("pkg:maven/").append(artifact.getGroupId()).append("/").append(artifact.getArtifactId()).append("@").append(artifact.getVersion())
                .append("?");
        String classifier = artifact.getClassifier();
        if (classifier != null) {
            sb.append("classifier=").append(classifier).append("&");
        }
        sb.append("type=").append(artifact.getType());
        return sb.toString();
    }

    /**
     * Gets a map of checksum algorithm names to hex-encoded digest values for the given artifact file.
     *
     * @param artifact A Maven artifact.
     * @param algorithms JSSE names of algorithms to use
     * @return A map of checksum algorithm names to hex-encoded digest values.
     * @throws IOException If an I/O error occurs reading the artifact file.
     * @throws IllegalArgumentException If any of the algorithms is not supported.
     */
    private static Map<String, String> getChecksums(Artifact artifact, String... algorithms) throws IOException {
        Map<String, String> checksums = new HashMap<>();
        for (String algorithm : algorithms) {
            String key = IN_TOTO_DIGEST_NAMES.get(algorithm);
            if (key == null) {
                throw new IllegalArgumentException("Invalid algorithm name for in-toto attestation: " + algorithm);
            }
            DigestUtils digest = new DigestUtils(DigestUtils.getDigest(algorithm));
            String checksum = digest.digestAsHex(artifact.getFile());
            checksums.put(key, checksum);
        }
        return checksums;
    }

    /**
     * Converts a Maven artifact to a SLSA {@link ResourceDescriptor}.
     *
     * @param artifact A Maven artifact.
     * @param algorithms A comma-separated list of checksum algorithms to use.
     * @return A SLSA resource descriptor.
     * @throws MojoExecutionException If an I/O error occurs retrieving the artifact.
     */
    public static ResourceDescriptor toResourceDescriptor(Artifact artifact, String algorithms) throws MojoExecutionException {
        ResourceDescriptor descriptor = new ResourceDescriptor();
        descriptor.setName(getFileName(artifact));
        descriptor.setUri(getPackageUrl(artifact));
        if (artifact.getFile() != null) {
            try {
                descriptor.setDigest(getChecksums(artifact, StringUtils.split(algorithms, ",")));
            } catch (IOException e) {
                throw new MojoExecutionException("Unable to compute hash for artifact file: " + artifact.getFile(), e);
            }
        }
        return descriptor;
    }
}
