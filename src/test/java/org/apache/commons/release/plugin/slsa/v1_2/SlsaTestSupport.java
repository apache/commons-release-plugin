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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Shared helpers for SLSA v1.2 fixture-based tests.
 */
final class SlsaTestSupport {

    static final Map<String, String> SAMPLE_DIGEST;
    static final Map<String, Object> SAMPLE_ANNOTATIONS;
    static final Map<String, Object> SAMPLE_EXTERNAL_PARAMETERS;
    static final Map<String, Object> SAMPLE_INTERNAL_PARAMETERS;
    static final Map<String, String> SAMPLE_BUILDER_VERSION;
    static final byte[] SAMPLE_CONTENT = {1, 2, 3, 4};
    static final String SAMPLE_BUILDER_ID = "pkg:maven/org.apache.commons/commons-release-plugin@1.10.0";
    static final OffsetDateTime SAMPLE_STARTED_ON = OffsetDateTime.parse("2026-04-20T09:28:44Z");
    static final OffsetDateTime SAMPLE_FINISHED_ON = OffsetDateTime.parse("2026-04-20T09:38:12Z");

    static {
        final Map<String, String> digest = new LinkedHashMap<>();
        digest.put("sha256", "ad2d2eacf15ab740c115294afc1192603d8342004a6d7d0ad35446f7dda8a134");
        digest.put("sha512", "126302c5f6865733774eb41fecc10ba8d0bb5ba11d14b9562047429abeb13bf8cdcdbfdf5e7d7708e2a40f67f4265cbbce609164f57"
                + "abcd676067a840aa48e6a");
        SAMPLE_DIGEST = Collections.unmodifiableMap(digest);

        final Map<String, Object> annotations = new LinkedHashMap<>();
        annotations.put("vendor", "Eclipse Adoptium");
        annotations.put("version", "25.0.2");
        SAMPLE_ANNOTATIONS = Collections.unmodifiableMap(annotations);

        final Map<String, Object> external = new LinkedHashMap<>();
        external.put("maven.profiles", Collections.singletonList("release"));
        external.put("maven.cmdline", "deploy -Prelease");
        SAMPLE_EXTERNAL_PARAMETERS = Collections.unmodifiableMap(external);

        final Map<String, Object> internal = new LinkedHashMap<>();
        internal.put("ci", Boolean.TRUE);
        SAMPLE_INTERNAL_PARAMETERS = Collections.unmodifiableMap(internal);

        final Map<String, String> builderVersion = new LinkedHashMap<>();
        builderVersion.put("commons-release-plugin", "1.10.0");
        SAMPLE_BUILDER_VERSION = Collections.unmodifiableMap(builderVersion);
    }

    private SlsaTestSupport() {
    }

    /** Creates an {@link ObjectMapper} configured for SLSA payloads (Java time module, ISO timestamps). */
    static ObjectMapper newObjectMapper() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }

    /** Reads a classpath resource and parses it into a {@link JsonNode}. */
    static JsonNode readJsonResource(final ObjectMapper objectMapper, final String path) throws Exception {
        try (InputStream in = SlsaTestSupport.class.getResourceAsStream(path)) {
            assertNotNull(in, "Fixture resource not found: " + path);
            return objectMapper.readTree(in);
        }
    }

    /**
     * Asserts that {@code statement} matches the {@code commons-text-1.4.intoto.json} fixture.
     */
    static void verifyDeserializedStatement(final Statement statement) {
        assertEquals(Statement.TYPE, statement.getType());
        assertEquals(Provenance.PREDICATE_TYPE, statement.getPredicateType());

        // Subject — only the first subject is checked exhaustively; the remaining nine follow the same shape.
        final List<ResourceDescriptor> subjects = statement.getSubject();
        assertNotNull(subjects);
        assertEquals(10, subjects.size());

        final ResourceDescriptor firstSubject = subjects.get(0);
        assertEquals("commons-text-1.4.jar", firstSubject.getName());
        assertEquals("pkg:maven/commons-text/commons-text@1.4?type=jar", firstSubject.getUri());
        final Map<String, String> firstDigest = firstSubject.getDigest();
        assertNotNull(firstDigest);
        assertEquals(4, firstDigest.size());
        assertEquals("9cbe22bb0ce86c70779213dfb7f3eb5a", firstDigest.get("md5"));
        assertEquals("c81f089b3542485d4d09b02aae822906e5d2f209", firstDigest.get("sha1"));
        assertEquals("ad2d2eacf15ab740c115294afc1192603d8342004a6d7d0ad35446f7dda8a134", firstDigest.get("sha256"));
        assertEquals("126302c5f6865733774eb41fecc10ba8d0bb5ba11d14b9562047429abeb13bf8"
                + "cdcdbfdf5e7d7708e2a40f67f4265cbbce609164f57abcd676067a840aa48e6a", firstDigest.get("sha512"));

        // Predicate
        final Provenance provenance = statement.getPredicate();
        assertNotNull(provenance);

        final BuildDefinition buildDefinition = provenance.getBuildDefinition();
        assertEquals("https://commons.apache.org/proper/commons-release-plugin/slsa/v0.1.0", buildDefinition.getBuildType());

        final Map<String, Object> externalParameters = buildDefinition.getExternalParameters();
        assertEquals(6, externalParameters.size());
        assertEquals(Collections.singletonList("release"), externalParameters.get("maven.profiles"));
        assertEquals("deploy -Prelease -Dgpg.keyname=3C8D57E0A2B5C6D7E8F9A0B1C2D3E4F5A6B7C8D9", externalParameters.get("maven.cmdline"));
        assertEquals(Arrays.asList("-Dfile.encoding=UTF-8", "-Dsun.stdout.encoding=UTF-8", "-Dsun.stderr.encoding=UTF-8"),
                externalParameters.get("jvm.args"));
        assertEquals(Collections.singletonMap("gpg.keyname", "3C8D57E0A2B5C6D7E8F9A0B1C2D3E4F5A6B7C8D9"),
                externalParameters.get("maven.user.properties"));
        assertEquals(Collections.singletonList("deploy"), externalParameters.get("maven.goals"));
        assertEquals(Collections.singletonMap("LANG", "pl_PL.UTF-8"), externalParameters.get("env"));

        assertEquals(Collections.emptyMap(), buildDefinition.getInternalParameters());

        final List<ResourceDescriptor> deps = buildDefinition.getResolvedDependencies();
        assertNotNull(deps);
        assertEquals(3, deps.size());

        // JDK dependency — annotations include a null value
        final ResourceDescriptor jdk = deps.get(0);
        assertEquals("JDK", jdk.getName());
        assertNull(jdk.getUri());
        assertEquals(Collections.singletonMap("gitTree", "bdb67e47c1b7df9c35ae045f29a348bb5bd32dc3"), jdk.getDigest());
        final Map<String, Object> jdkAnnotations = jdk.getAnnotations();
        assertNotNull(jdkAnnotations);
        assertEquals("/usr/lib/jvm/temurin-25-jdk-amd64", jdkAnnotations.get("home"));
        assertTrue(jdkAnnotations.containsKey("specification.maintenance.version"));
        assertNull(jdkAnnotations.get("specification.maintenance.version"));
        assertEquals("Java Platform API Specification", jdkAnnotations.get("specification.name"));
        assertEquals("Oracle Corporation", jdkAnnotations.get("specification.vendor"));
        assertEquals("25", jdkAnnotations.get("specification.version"));
        assertEquals("Eclipse Adoptium", jdkAnnotations.get("vendor"));
        assertEquals("https://adoptium.net/", jdkAnnotations.get("vendor.url"));
        assertEquals("Temurin-25.0.2+10", jdkAnnotations.get("vendor.version"));
        assertEquals("25.0.2", jdkAnnotations.get("version"));
        assertEquals("2026-01-20", jdkAnnotations.get("version.date"));
        assertEquals("OpenJDK 64-Bit Server VM", jdkAnnotations.get("vm.name"));
        assertEquals("Java Virtual Machine Specification", jdkAnnotations.get("vm.specification.name"));
        assertEquals("Oracle Corporation", jdkAnnotations.get("vm.specification.vendor"));
        assertEquals("25", jdkAnnotations.get("vm.specification.version"));
        assertEquals("Eclipse Adoptium", jdkAnnotations.get("vm.vendor"));
        assertEquals("25.0.2+10-LTS", jdkAnnotations.get("vm.version"));

        // Maven dependency
        final ResourceDescriptor maven = deps.get(1);
        assertEquals("Maven", maven.getName());
        assertEquals("pkg:maven/org.apache.maven/apache-maven@3.9.12", maven.getUri());
        assertEquals(Collections.singletonMap("gitTree", "3cdb4a67690dc18373f70ead98dc86567cc5ad67"), maven.getDigest());
        final Map<String, Object> mavenAnnotations = maven.getAnnotations();
        assertNotNull(mavenAnnotations);
        assertEquals("apache-maven", mavenAnnotations.get("distributionId"));
        assertEquals("Apache Maven", mavenAnnotations.get("distributionName"));
        assertEquals("Maven", mavenAnnotations.get("distributionShortName"));
        assertEquals("848fbb4bf2d427b72bdb2471c22fced7ebd9a7a1", mavenAnnotations.get("buildNumber"));
        assertEquals("3.9.12", mavenAnnotations.get("version"));

        // Git source dependency
        final ResourceDescriptor source = deps.get(2);
        assertNull(source.getName());
        assertEquals("git+https://github.com/apache/commons-text.git@feat/slsa", source.getUri());
        assertEquals(Collections.singletonMap("gitCommit", "f519b3670795da3fb4f43b6af1f727eadf8e6800"), source.getDigest());
        assertNull(source.getAnnotations());

        // Run details
        final RunDetails runDetails = provenance.getRunDetails();
        final Builder builder = runDetails.getBuilder();
        assertNotNull(builder.getId());
        // Resource filtering substitutes ${project.groupId} / ${project.artifactId} / ${project.version}, so match the prefix only.
        assertTrue(builder.getId().startsWith("pkg:maven/org.apache.commons/commons-release-plugin@"));
        assertEquals(Collections.emptyList(), builder.getBuilderDependencies());
        assertEquals(Collections.emptyMap(), builder.getVersion());

        assertNull(runDetails.getByproducts());

        final BuildMetadata metadata = runDetails.getMetadata();
        assertEquals(OffsetDateTime.parse("2026-04-20T09:28:44Z"), metadata.getStartedOn());
        assertEquals(OffsetDateTime.parse("2026-04-20T09:38:12Z"), metadata.getFinishedOn());
        assertNull(metadata.getInvocationId());
    }

    /** Builds a synthetic {@link Provenance} used by the {@code serializeThenDeserialize} round-trips. */
    static Provenance sampleProvenance() {
        final ResourceDescriptor dependency = new ResourceDescriptor()
                .setName("JDK")
                .setUri("pkg:generic/jdk@25.0.2")
                .setDigest(SAMPLE_DIGEST)
                .setAnnotations(SAMPLE_ANNOTATIONS)
                .setMediaType("application/java-archive")
                .setDownloadLocation("https://example.com/jdk.tar.gz")
                .setContent(SAMPLE_CONTENT.clone());

        final BuildDefinition buildDefinition = new BuildDefinition()
                .setBuildType("https://commons.apache.org/proper/commons-release-plugin/slsa/v0.1.0")
                .setExternalParameters(SAMPLE_EXTERNAL_PARAMETERS)
                .setInternalParameters(SAMPLE_INTERNAL_PARAMETERS)
                .setResolvedDependencies(Collections.singletonList(dependency));

        final Builder builder = new Builder()
                .setId(SAMPLE_BUILDER_ID)
                .setBuilderDependencies(Collections.emptyList())
                .setVersion(SAMPLE_BUILDER_VERSION);

        final BuildMetadata metadata = new BuildMetadata()
                .setInvocationId("invocation-1")
                .setStartedOn(SAMPLE_STARTED_ON)
                .setFinishedOn(SAMPLE_FINISHED_ON);

        final ResourceDescriptor byproduct = new ResourceDescriptor("pkg:generic/build-log", SAMPLE_DIGEST);

        final RunDetails runDetails = new RunDetails()
                .setBuilder(builder)
                .setMetadata(metadata)
                .setByproducts(Collections.singletonList(byproduct));

        return new Provenance()
                .setBuildDefinition(buildDefinition)
                .setRunDetails(runDetails);
    }

    /**
     * Asserts that {@code deserialized} matches the object returned by {@link #sampleProvenance()}.
     */
    static void verifyDeserializedSampleProvenance(final Provenance deserialized) {
        final BuildDefinition deserBuildDef = deserialized.getBuildDefinition();
        assertNotNull(deserBuildDef);
        assertEquals("https://commons.apache.org/proper/commons-release-plugin/slsa/v0.1.0", deserBuildDef.getBuildType());
        assertEquals(SAMPLE_EXTERNAL_PARAMETERS, deserBuildDef.getExternalParameters());
        assertEquals(SAMPLE_INTERNAL_PARAMETERS, deserBuildDef.getInternalParameters());

        assertEquals(1, deserBuildDef.getResolvedDependencies().size());
        final ResourceDescriptor deserDep = deserBuildDef.getResolvedDependencies().get(0);
        assertEquals("JDK", deserDep.getName());
        assertEquals("pkg:generic/jdk@25.0.2", deserDep.getUri());
        assertEquals(SAMPLE_DIGEST, deserDep.getDigest());
        assertEquals(SAMPLE_ANNOTATIONS, deserDep.getAnnotations());
        assertEquals("application/java-archive", deserDep.getMediaType());
        assertEquals("https://example.com/jdk.tar.gz", deserDep.getDownloadLocation());
        assertArrayEquals(SAMPLE_CONTENT, deserDep.getContent());

        final RunDetails deserRunDetails = deserialized.getRunDetails();
        assertNotNull(deserRunDetails);
        final Builder deserBuilder = deserRunDetails.getBuilder();
        assertEquals(SAMPLE_BUILDER_ID, deserBuilder.getId());
        assertEquals(Collections.emptyList(), deserBuilder.getBuilderDependencies());
        assertEquals(SAMPLE_BUILDER_VERSION, deserBuilder.getVersion());

        final BuildMetadata deserMetadata = deserRunDetails.getMetadata();
        assertEquals("invocation-1", deserMetadata.getInvocationId());
        assertEquals(SAMPLE_STARTED_ON, deserMetadata.getStartedOn());
        assertEquals(SAMPLE_FINISHED_ON, deserMetadata.getFinishedOn());

        assertNotNull(deserRunDetails.getByproducts());
        assertEquals(1, deserRunDetails.getByproducts().size());
        assertEquals("pkg:generic/build-log", deserRunDetails.getByproducts().get(0).getUri());
    }
}
