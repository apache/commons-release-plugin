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
package org.apache.commons.release.plugin.mojos;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static net.javacrumbs.jsonunit.JsonAssert.assertJsonNodeAbsent;
import static net.javacrumbs.jsonunit.JsonAssert.assertJsonNodePresent;
import static net.javacrumbs.jsonunit.JsonAssert.assertJsonPartEquals;
import static net.javacrumbs.jsonunit.JsonAssert.whenIgnoringPaths;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.javacrumbs.jsonunit.JsonAssert;
import net.javacrumbs.jsonunit.core.Option;
import org.apache.commons.release.plugin.internal.MojoUtils;
import org.apache.commons.release.plugin.slsa.v1_2.DsseEnvelope;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.gpg.AbstractGpgSigner;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.rtinfo.RuntimeInformation;
import org.apache.maven.scm.manager.ScmManager;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.aether.RepositorySystemSession;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class BuildAttestationMojoTest {

    private static final String ARTIFACTS_DIR = "src/test/resources/mojos/detach-distributions/target/";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @TempDir
    private static Path localRepositoryPath;

    private static PlexusContainer container;
    private static RepositorySystemSession repoSession;
    private static JsonNode expectedStatement;

    @BeforeAll
    static void setup() throws Exception {
        container = MojoUtils.setupContainer();
        repoSession = MojoUtils.createRepositorySystemSession(container, localRepositoryPath);
        try (InputStream in = BuildAttestationMojoTest.class.getResourceAsStream("/attestations/commons-text-1.4.intoto.json")) {
            expectedStatement = OBJECT_MAPPER.readTree(in);
        }
    }

    private static MavenExecutionRequest createMavenExecutionRequest() {
        final DefaultMavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setStartTime(Date.from(Instant.parse("2026-04-20T09:28:44Z")));
        request.setActiveProfiles(Collections.singletonList("release"));
        request.setGoals(Collections.singletonList("deploy"));
        final Properties userProperties = new Properties();
        userProperties.setProperty("gpg.keyname", "3C8D57E0A2B5C6D7E8F9A0B1C2D3E4F5A6B7C8D9");
        request.setUserProperties(userProperties);
        return request;
    }

    @SuppressWarnings("deprecation")
    private static MavenSession createMavenSession(final MavenExecutionRequest request, final MavenExecutionResult result) {
        return new MavenSession(container, repoSession, request, result);
    }

    private static BuildAttestationMojo createBuildAttestationMojo(final MavenProject project, final MavenProjectHelper projectHelper)
            throws ComponentLookupException {
        final ScmManager scmManager = container.lookup(ScmManager.class);
        final RuntimeInformation runtimeInfo = container.lookup(RuntimeInformation.class);
        return new BuildAttestationMojo(project, scmManager, runtimeInfo,
                createMavenSession(createMavenExecutionRequest(), new DefaultMavenExecutionResult()), projectHelper);
    }

    private static void configureBuildAttestationMojo(final BuildAttestationMojo mojo, final boolean signAttestation) {
        mojo.setOutputDirectory(new File("target/attestations"));
        mojo.setScmDirectory(new File("."));
        mojo.setScmConnectionUrl("scm:git:https://github.com/apache/commons-text.git");
        mojo.setMavenHome(new File(System.getProperty("maven.home", ".")));
        mojo.setAlgorithmNames("SHA-512,SHA-256,SHA-1,MD5");
        mojo.setSignAttestation(signAttestation);
        mojo.setSigner(createMockSigner());
    }

    private static MavenProject createMavenProject(final MavenProjectHelper projectHelper, final MavenRepositorySystem repoSystem) throws Exception {
        final File pomFile = new File(ARTIFACTS_DIR + "commons-text-1.4.pom");
        final Model model;
        try (InputStream in = Files.newInputStream(pomFile.toPath())) {
            model = new MavenXpp3Reader().read(in);
        }
        // Group id is inherited from the missing parent, so we override it
        model.setGroupId("org.apache.commons");
        final MavenProject project = new MavenProject(model);
        final Artifact artifact = repoSystem.createArtifact(model.getArtifactId(), model.getArtifactId(), model.getVersion(), null, "jar");
        artifact.setFile(new File(ARTIFACTS_DIR + "commons-text-1.4.jar"));
        project.setArtifact(artifact);
        projectHelper.attachArtifact(project, "pom", null, pomFile);
        projectHelper.attachArtifact(project, "jar", "sources", new File(ARTIFACTS_DIR + "commons-text-1.4-sources.jar"));
        projectHelper.attachArtifact(project, "jar", "javadoc", new File(ARTIFACTS_DIR + "commons-text-1.4-javadoc.jar"));
        projectHelper.attachArtifact(project, "jar", "tests", new File(ARTIFACTS_DIR + "commons-text-1.4-tests.jar"));
        projectHelper.attachArtifact(project, "jar", "test-sources", new File(ARTIFACTS_DIR + "commons-text-1.4-test-sources.jar"));
        projectHelper.attachArtifact(project, "tar.gz", "bin", new File(ARTIFACTS_DIR + "commons-text-1.4-bin.tar.gz"));
        projectHelper.attachArtifact(project, "zip", "bin", new File(ARTIFACTS_DIR + "commons-text-1.4-bin.zip"));
        projectHelper.attachArtifact(project, "tar.gz", "src", new File(ARTIFACTS_DIR + "commons-text-1.4-src.tar.gz"));
        projectHelper.attachArtifact(project, "zip", "src", new File(ARTIFACTS_DIR + "commons-text-1.4-src.zip"));
        return project;
    }

    private static AbstractGpgSigner createMockSigner() {
        return new AbstractGpgSigner() {
            @Override
            public String signerName() {
                return "mock";
            }

            @Override
            public String getKeyInfo() {
                return "mock-key";
            }

            @Override
            protected void generateSignatureForFile(final File file, final File signature) throws MojoExecutionException {
                try {
                    Files.copy(Paths.get(ARTIFACTS_DIR + "commons-text-1.4.jar.asc"), signature.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (final IOException e) {
                    throw new MojoExecutionException("Failed to copy mock signature", e);
                }
            }
        };
    }

    private static Artifact getAttestation(final MavenProject project) {
        return project.getAttachedArtifacts().stream()
                .filter(a -> "intoto.jsonl".equals(a.getType()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No intoto.jsonl artifact attached to project"));
    }

    private static void assertStatementContent(final JsonNode statement) {
        assertJsonEquals(expectedStatement.get("subject"), statement.get("subject"),
                JsonAssert.when(Option.IGNORING_ARRAY_ORDER));
        assertJsonEquals(expectedStatement.get("predicateType"), statement.get("predicateType"));
        assertJsonEquals(expectedStatement.at("/predicate/buildDefinition/buildType"),
                statement.at("/predicate/buildDefinition/buildType"));
        assertJsonEquals(expectedStatement.at("/predicate/buildDefinition/externalParameters"),
                statement.at("/predicate/buildDefinition/externalParameters"),
                JsonAssert.when(Option.IGNORING_VALUES).whenIgnoringPaths("jvm.args", "env"));
        assertJsonEquals(expectedStatement.at("/predicate/buildDefinition/internalParameters"),
                statement.at("/predicate/buildDefinition/internalParameters"));
        // `[0].annotations` holds JVM system properties;
        // Not all properties are available on all JDKs, so they are either null or strings, which json-unit treats as a structural mismatch.
        // We will check them below
        assertJsonEquals(expectedStatement.at("/predicate/buildDefinition/resolvedDependencies"),
                statement.at("/predicate/buildDefinition/resolvedDependencies"),
                JsonAssert.when(Option.IGNORING_VALUES).whenIgnoringPaths("[0].annotations"));
        final Set<String> expectedJdkFields = fieldNames(
                expectedStatement.at("/predicate/buildDefinition/resolvedDependencies/0/annotations"));
        final Set<String> actualJdkFields = fieldNames(
                statement.at("/predicate/buildDefinition/resolvedDependencies/0/annotations"));
        assertEquals(expectedJdkFields, actualJdkFields);
        assertJsonEquals(expectedStatement.at("/predicate/runDetails"),
                statement.at("/predicate/runDetails"),
                whenIgnoringPaths("metadata.finishedOn"));
    }

    private static Set<String> fieldNames(final JsonNode node) {
        final Set<String> names = new TreeSet<>();
        final Iterator<String> it = node.fieldNames();
        while (it.hasNext()) {
            names.add(it.next());
        }
        return names;
    }

    @Test
    void attestationTest() throws Exception {
        final MavenProjectHelper projectHelper = container.lookup(MavenProjectHelper.class);
        final MavenRepositorySystem repoSystem = container.lookup(MavenRepositorySystem.class);
        final MavenProject project = createMavenProject(projectHelper, repoSystem);

        final BuildAttestationMojo mojo = createBuildAttestationMojo(project, projectHelper);
        configureBuildAttestationMojo(mojo, false);
        mojo.execute();

        final JsonNode statement = OBJECT_MAPPER.readTree(getAttestation(project).getFile());
        assertStatementContent(statement);
    }

    @Test
    void signingTest() throws Exception {
        final MavenProjectHelper projectHelper = container.lookup(MavenProjectHelper.class);
        final MavenRepositorySystem repoSystem = container.lookup(MavenRepositorySystem.class);
        final MavenProject project = createMavenProject(projectHelper, repoSystem);

        final BuildAttestationMojo mojo = createBuildAttestationMojo(project, projectHelper);
        configureBuildAttestationMojo(mojo, true);
        mojo.execute();

        final String envelopeJson = new String(Files.readAllBytes(getAttestation(project).getFile().toPath()), StandardCharsets.UTF_8);

        assertJsonPartEquals(DsseEnvelope.PAYLOAD_TYPE, envelopeJson, "payloadType");
        assertJsonNodePresent(envelopeJson, "signatures[0]");
        assertJsonNodeAbsent(envelopeJson, "signatures[1]");
        assertJsonPartEquals("${json-unit.regex}.+", envelopeJson, "signatures[0].sig");

        final DsseEnvelope envelope = OBJECT_MAPPER.readValue(envelopeJson.trim(), DsseEnvelope.class);
        final JsonNode statement = OBJECT_MAPPER.readTree(envelope.getPayload());
        assertStatementContent(statement);
    }
}
