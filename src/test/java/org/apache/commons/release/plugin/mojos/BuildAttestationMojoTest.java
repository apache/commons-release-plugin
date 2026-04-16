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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @TempDir
    private static Path localRepositoryPath;

    private static PlexusContainer container;
    private static RepositorySystemSession repoSession;

    @BeforeAll
    static void setup() throws Exception {
        container = MojoUtils.setupContainer();
        repoSession = MojoUtils.createRepositorySystemSession(container, localRepositoryPath);
    }

    private static MavenExecutionRequest createMavenExecutionRequest() {
        DefaultMavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setStartTime(new Date());
        return request;
    }

    @SuppressWarnings("deprecation")
    private static MavenSession createMavenSession(MavenExecutionRequest request, MavenExecutionResult result) {
        return new MavenSession(container, repoSession, request, result);
    }

    private static BuildAttestationMojo createBuildAttestationMojo(MavenProject project, MavenProjectHelper projectHelper) throws ComponentLookupException {
        ScmManager scmManager = container.lookup(ScmManager.class);
        RuntimeInformation runtimeInfo = container.lookup(RuntimeInformation.class);
        return new BuildAttestationMojo(project, scmManager, runtimeInfo,
                createMavenSession(createMavenExecutionRequest(), new DefaultMavenExecutionResult()), projectHelper);
    }

    private static MavenProject createMavenProject(MavenProjectHelper projectHelper, MavenRepositorySystem repoSystem) throws ComponentLookupException {
        MavenProject project = new MavenProject(new Model());
        Artifact artifact = repoSystem.createArtifact("groupId", "artifactId", "1.2.3", null, "jar");
        project.setArtifact(artifact);
        project.setGroupId("groupId");
        project.setArtifactId("artifactId");
        project.setVersion("1.2.3");
        // Attach a couple of artifacts
        projectHelper.attachArtifact(project, "pom", null, new File("src/test/resources/artifacts/artifact-pom.txt"));
        artifact.setFile(new File("src/test/resources/artifacts/artifact-jar.txt"));
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
                    Files.copy(Paths.get("src/test/resources/mojos/detach-distributions/target/commons-text-1.4.jar.asc"),
                            signature.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (final IOException e) {
                    throw new MojoExecutionException("Failed to copy mock signature", e);
                }
            }
        };
    }

    private static void assertResolvedDependencies(final String statementJson) {
        String resolvedDeps = "predicate.buildDefinition.resolvedDependencies";
        String javaVersion = System.getProperty("java.version");

        assertThatJson(statementJson)
                .node(resolvedDeps).isArray()
                .anySatisfy(dep -> {
                    assertThatJson(dep).node("name").isEqualTo("JDK");
                    assertThatJson(dep).node("annotations.version").isEqualTo(javaVersion);
                });

        assertThatJson(statementJson)
                .node(resolvedDeps).isArray()
                .anySatisfy(dep -> assertThatJson(dep).node("name").isEqualTo("Maven"));

        assertThatJson(statementJson)
                .node(resolvedDeps).isArray()
                .anySatisfy(dep -> assertThatJson(dep).node("uri").isString().startsWith("git+https://github.com/apache/commons-lang.git"));
    }

    @Test
    void attestationTest() throws Exception {
        MavenProjectHelper projectHelper = container.lookup(MavenProjectHelper.class);
        MavenRepositorySystem repoSystem = container.lookup(MavenRepositorySystem.class);
        MavenProject project = createMavenProject(projectHelper, repoSystem);

        BuildAttestationMojo mojo = createBuildAttestationMojo(project, projectHelper);
        mojo.setOutputDirectory(new File("target/attestations"));
        mojo.setScmDirectory(new File("."));
        mojo.setScmConnectionUrl("scm:git:https://github.com/apache/commons-lang.git");
        mojo.setMavenHome(new File(System.getProperty("maven.home", ".")));
        mojo.execute();

        Artifact attestation = getAttestation(project);
        String json = new String(Files.readAllBytes(attestation.getFile().toPath()), StandardCharsets.UTF_8);

        assertResolvedDependencies(json);
    }

    @Test
    void signingTest() throws Exception {
        MavenProjectHelper projectHelper = container.lookup(MavenProjectHelper.class);
        MavenRepositorySystem repoSystem = container.lookup(MavenRepositorySystem.class);
        MavenProject project = createMavenProject(projectHelper, repoSystem);

        BuildAttestationMojo mojo = createBuildAttestationMojo(project, projectHelper);
        mojo.setOutputDirectory(new File("target/attestations"));
        mojo.setScmDirectory(new File("."));
        mojo.setScmConnectionUrl("scm:git:https://github.com/apache/commons-lang.git");
        mojo.setMavenHome(new File(System.getProperty("maven.home", ".")));
        mojo.setSignAttestation(true);
        mojo.setSigner(createMockSigner());
        mojo.execute();

        Artifact attestation = getAttestation(project);
        String envelopeJson = new String(Files.readAllBytes(attestation.getFile().toPath()), StandardCharsets.UTF_8);

        assertThatJson(envelopeJson).node("payloadType").isEqualTo(DsseEnvelope.PAYLOAD_TYPE);
        assertThatJson(envelopeJson).node("signatures").isArray().hasSize(1);
        assertThatJson(envelopeJson).node("signatures[0].sig").isString().isNotEmpty();

        DsseEnvelope envelope = OBJECT_MAPPER.readValue(envelopeJson.trim(), DsseEnvelope.class);
        String statementJson = new String(envelope.getPayload(), StandardCharsets.UTF_8);
        assertResolvedDependencies(statementJson);
    }

    private static Artifact getAttestation(MavenProject project) {
        return project.getAttachedArtifacts()
                .stream()
                .filter(a -> "intoto.jsonl".equals(a.getType()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No intoto.jsonl artifact attached to project"));
    }
}
