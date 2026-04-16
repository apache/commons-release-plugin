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

    private static final String ARTIFACTS_DIR = "src/test/resources/mojos/detach-distributions/target/";

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

    private static MavenProject createMavenProject(MavenProjectHelper projectHelper, MavenRepositorySystem repoSystem) {
        MavenProject project = new MavenProject(new Model());
        Artifact artifact = repoSystem.createArtifact("org.apache.commons", "commons-text", "1.4", null, "jar");
        artifact.setFile(new File(ARTIFACTS_DIR + "commons-text-1.4.jar"));
        project.setArtifact(artifact);
        project.setGroupId("org.apache.commons");
        project.setArtifactId("commons-text");
        project.setVersion("1.4");
        projectHelper.attachArtifact(project, "pom", null, new File(ARTIFACTS_DIR + "commons-text-1.4.pom"));
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

    private static void assertSubject(final String statementJson, final String name, final String sha256) {
        assertThatJson(statementJson)
                .node("subject").isArray()
                .anySatisfy(s -> {
                    assertThatJson(s).node("name").isEqualTo(name);
                    assertThatJson(s).node("digest.sha256").isEqualTo(sha256);
                });
    }

    private static void assertStatementContent(final String statementJson) {
        assertSubject(statementJson, "commons-text-1.4.jar",
                "ad2d2eacf15ab740c115294afc1192603d8342004a6d7d0ad35446f7dda8a134");
        assertSubject(statementJson, "commons-text-1.4.pom",
                "4d6277b1e0720bb054c640620679a9da120f753029342150e714095f48934d76");
        assertSubject(statementJson, "commons-text-1.4-sources.jar",
                "58a95591fe7fc94db94a0a9e64b4a5bcc1c49edf17f2b24d7c0747357d855761");
        assertSubject(statementJson, "commons-text-1.4-javadoc.jar",
                "42f5b341d0fbeaa30b06aed90612840bc513fb39792c3d39446510670216e8b1");
        assertSubject(statementJson, "commons-text-1.4-tests.jar",
                "e4e365d08d601a4bda44be2a31f748b96762504d301742d4a0f7f5953d4c793a");
        assertSubject(statementJson, "commons-text-1.4-test-sources.jar",
                "9200a2a41b35f2d6d30c1c698308591cf577547ec39514657dff0e2f7dff18ca");
        assertSubject(statementJson, "commons-text-1.4-bin.tar.gz",
                "8b9393f7ddc2efb69d8c2b6f4d85d8711dddfe77009799cf21619fc9b8411897");
        assertSubject(statementJson, "commons-text-1.4-bin.zip",
                "ad3732dcb38e510b1dbb1544115d0eb797fab61afe0008fdb187cd4ef1706cd7");
        assertSubject(statementJson, "commons-text-1.4-src.tar.gz",
                "1cb8536c375c3cff66757fd40c2bf878998254ba0a247866a6536bd48ba2e88a");
        assertSubject(statementJson, "commons-text-1.4-src.zip",
                "e4a6c992153faae4f7faff689b899073000364e376736b9746a5d0acb9d8b980");

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
                .anySatisfy(dep -> assertThatJson(dep).node("uri").isString()
                        .startsWith("git+https://github.com/apache/commons-text.git"));
    }

    @Test
    void attestationTest() throws Exception {
        MavenProjectHelper projectHelper = container.lookup(MavenProjectHelper.class);
        MavenRepositorySystem repoSystem = container.lookup(MavenRepositorySystem.class);
        MavenProject project = createMavenProject(projectHelper, repoSystem);

        BuildAttestationMojo mojo = createBuildAttestationMojo(project, projectHelper);
        mojo.setOutputDirectory(new File("target/attestations"));
        mojo.setScmDirectory(new File("."));
        mojo.setScmConnectionUrl("scm:git:https://github.com/apache/commons-text.git");
        mojo.setMavenHome(new File(System.getProperty("maven.home", ".")));
        mojo.execute();

        String json = new String(Files.readAllBytes(getAttestation(project).getFile().toPath()), StandardCharsets.UTF_8);
        assertStatementContent(json);
    }

    @Test
    void signingTest() throws Exception {
        MavenProjectHelper projectHelper = container.lookup(MavenProjectHelper.class);
        MavenRepositorySystem repoSystem = container.lookup(MavenRepositorySystem.class);
        MavenProject project = createMavenProject(projectHelper, repoSystem);

        BuildAttestationMojo mojo = createBuildAttestationMojo(project, projectHelper);
        mojo.setOutputDirectory(new File("target/attestations"));
        mojo.setScmDirectory(new File("."));
        mojo.setScmConnectionUrl("scm:git:https://github.com/apache/commons-text.git");
        mojo.setMavenHome(new File(System.getProperty("maven.home", ".")));
        mojo.setSignAttestation(true);
        mojo.setSigner(createMockSigner());
        mojo.execute();

        String envelopeJson = new String(Files.readAllBytes(getAttestation(project).getFile().toPath()), StandardCharsets.UTF_8);

        assertThatJson(envelopeJson).node("payloadType").isEqualTo(DsseEnvelope.PAYLOAD_TYPE);
        assertThatJson(envelopeJson).node("signatures").isArray().hasSize(1);
        assertThatJson(envelopeJson).node("signatures[0].sig").isString().isNotEmpty();

        DsseEnvelope envelope = OBJECT_MAPPER.readValue(envelopeJson.trim(), DsseEnvelope.class);
        String statementJson = new String(envelope.getPayload(), StandardCharsets.UTF_8);
        assertStatementContent(statementJson);
    }
}
