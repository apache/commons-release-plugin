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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.release.plugin.internal.ArtifactUtils;
import org.apache.commons.release.plugin.internal.BuildDefinitions;
import org.apache.commons.release.plugin.internal.DsseUtils;
import org.apache.commons.release.plugin.internal.GitUtils;
import org.apache.commons.release.plugin.slsa.v1_2.BuildDefinition;
import org.apache.commons.release.plugin.slsa.v1_2.BuildMetadata;
import org.apache.commons.release.plugin.slsa.v1_2.Builder;
import org.apache.commons.release.plugin.slsa.v1_2.DsseEnvelope;
import org.apache.commons.release.plugin.slsa.v1_2.Provenance;
import org.apache.commons.release.plugin.slsa.v1_2.ResourceDescriptor;
import org.apache.commons.release.plugin.slsa.v1_2.RunDetails;
import org.apache.commons.release.plugin.slsa.v1_2.Signature;
import org.apache.commons.release.plugin.slsa.v1_2.Statement;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.plugins.gpg.AbstractGpgSigner;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.rtinfo.RuntimeInformation;
import org.apache.maven.scm.CommandParameters;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.command.info.InfoItem;
import org.apache.maven.scm.command.info.InfoScmResult;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.repository.ScmRepository;

/**
 * This plugin generates an in-toto attestation for all the artifacts.
 */
@Mojo(name = "build-attestation", defaultPhase = LifecyclePhase.VERIFY, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class BuildAttestationMojo extends AbstractMojo {

    /**
     * The file extension for in-toto attestation files.
     */
    private static final String ATTESTATION_EXTENSION = "intoto.jsonl";

    /**
     * Shared Jackson object mapper used to serialize SLSA statements and DSSE envelopes to JSON.
     *
     * <p>Each attestation is written as a single JSON value followed by a line separator, matching
     * the <a href="https://jsonlines.org/">JSON Lines</a> format used by {@code .intoto.jsonl}
     * files. The mapper is configured not to auto-close the output stream so the caller can append
     * the trailing newline, and to emit ISO-8601 timestamps rather than numeric ones.</p>
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.findAndRegisterModules();
        OBJECT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        OBJECT_MAPPER.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
    }

    /**
     * Checksum algorithms used in the generated attestation.
     */
    @Parameter(property = "commons.release.checksums.algorithms", defaultValue = "SHA-512,SHA-256,SHA-1,MD5")
    private String algorithmNames;
    /**
     * Whether to include the default GPG keyring.
     *
     * <p>When {@code false}, passes {@code --no-default-keyring} to the GPG command.</p>
     */
    @Parameter(property = "gpg.defaultKeyring", defaultValue = "true")
    private boolean defaultKeyring;
    /**
     * Path to the GPG executable; if not set, {@code gpg} is resolved from {@code PATH}.
     */
    @Parameter(property = "gpg.executable")
    private String executable;
    /**
     * Name or fingerprint of the GPG key to use for signing.
     *
     * <p>Passed as {@code --local-user} to the GPG command; uses the default key when not set.</p>
     */
    @Parameter(property = "gpg.keyname")
    private String keyname;
    /**
     * GPG database lock mode passed via {@code --lock-once}, {@code --lock-multiple}, or
     * {@code --lock-never}; no lock flag is added when not set.
     */
    @Parameter(property = "gpg.lockMode")
    private String lockMode;
    /**
     * The Maven home directory.
     */
    @Parameter(defaultValue = "${maven.home}", readonly = true)
    private File mavenHome;
    /**
     * Helper to attach artifacts to the project.
     */
    private final MavenProjectHelper mavenProjectHelper;
    /**
     * The output directory for the attestation file.
     */
    @Parameter(property = "commons.release.outputDirectory", defaultValue = "${project.build.directory}")
    private File outputDirectory;
    /**
     * The current Maven project.
     */
    private final MavenProject project;
    /**
     * Runtime information.
     */
    private final RuntimeInformation runtimeInformation;
    /**
     * The SCM connection URL for the current project.
     */
    @Parameter(defaultValue = "${project.scm.connection}", readonly = true)
    private String scmConnectionUrl;
    /**
     * Issue SCM actions at this local directory.
     */
    @Parameter(property = "commons.release.scmDirectory", defaultValue = "${basedir}")
    private File scmDirectory;
    /**
     * SCM manager to detect the Git revision.
     */
    private final ScmManager scmManager;
    /**
     * The current Maven session, used to resolve plugin dependencies.
     */
    private final MavenSession session;
    /**
     * Whether to sign the attestation envelope with GPG.
     */
    @Parameter(property = "commons.release.signAttestation", defaultValue = "true")
    private boolean signAttestation;
    /**
     * Descriptor of this plugin; used to fill in {@code builder.id} with the plugin's own
     * Package URL so that consumers can resolve the exact code that produced the provenance.
     */
    @Parameter(defaultValue = "${plugin}", readonly = true)
    private PluginDescriptor pluginDescriptor;
    /**
     * GPG signer used for signing; lazily initialized from plugin parameters when {@code null}.
     */
    private AbstractGpgSigner signer;
    /**
     * Whether to skip attaching the attestation artifact to the project.
     */
    @Parameter(property = "commons.release.skipAttach")
    private boolean skipAttach;
    /**
     * Whether to use gpg-agent for passphrase management.
     *
     * <p>For GPG versions before 2.1, passes {@code --use-agent} or {@code --no-use-agent}
     * accordingly; ignored for GPG 2.1 and later where the agent is always used.</p>
     */
    @Parameter(property = "gpg.useagent", defaultValue = "true")
    private boolean useAgent;

    /**
     * Creates a new instance with the given dependencies.
     *
     * @param project            A Maven project.
     * @param scmManager         A SCM manager.
     * @param runtimeInformation Maven runtime information.
     * @param session            A Maven session.
     * @param mavenProjectHelper A helper to attach artifacts to the project.
     */
    @Inject
    public BuildAttestationMojo(final MavenProject project, final ScmManager scmManager, final RuntimeInformation runtimeInformation,
            final MavenSession session, final MavenProjectHelper mavenProjectHelper) {
        this.project = project;
        this.scmManager = scmManager;
        this.runtimeInformation = runtimeInformation;
        this.session = session;
        this.mavenProjectHelper = mavenProjectHelper;
    }

    /**
     * Creates the output directory if it does not already exist and returns its path.
     *
     * @return the output directory path
     * @throws MojoExecutionException if the directory cannot be created
     */
    private Path ensureOutputDirectory() throws MojoExecutionException {
        final Path outputPath = outputDirectory.toPath();
        try {
            if (!Files.exists(outputPath)) {
                Files.createDirectories(outputPath);
            }
        } catch (final IOException e) {
            throw new MojoExecutionException("Could not create output directory.", e);
        }
        return outputPath;
    }

    @Override
    public void execute() throws MojoFailureException, MojoExecutionException {
        final BuildDefinition buildDefinition = new BuildDefinition()
                .setExternalParameters(BuildDefinitions.externalParameters(session))
                .setResolvedDependencies(getBuildDependencies());
        final String builderId = String.format("pkg:maven/%s/%s@%s",
                pluginDescriptor.getGroupId(), pluginDescriptor.getArtifactId(), pluginDescriptor.getVersion());
        final RunDetails runDetails = new RunDetails()
                .setBuilder(new Builder().setId(builderId))
                .setMetadata(getBuildMetadata());
        final Provenance provenance = new Provenance()
                .setBuildDefinition(buildDefinition)
                .setRunDetails(runDetails);
        final Statement statement = new Statement()
                .setSubject(getSubjects())
                .setPredicate(provenance);

        final Path outputPath = ensureOutputDirectory();
        final Path artifactPath = outputPath.resolve(ArtifactUtils.getFileName(project.getArtifact(), ATTESTATION_EXTENSION));
        if (signAttestation) {
            signAndWriteStatement(statement, outputPath, artifactPath);
        } else {
            writeStatement(statement, artifactPath);
        }
    }

    /**
     * Gets resource descriptors for the JVM, Maven installation, SCM source, and project dependencies.
     *
     * @return A list of resolved build dependencies.
     * @throws MojoExecutionException If any dependency cannot be resolved or hashed.
     */
    private List<ResourceDescriptor> getBuildDependencies() throws MojoExecutionException {
        final List<ResourceDescriptor> dependencies = new ArrayList<>();
        try {
            dependencies.add(BuildDefinitions.jvm(Paths.get(System.getProperty("java.home"))));
            dependencies.add(BuildDefinitions.maven(runtimeInformation.getMavenVersion(), mavenHome.toPath(),
                    runtimeInformation.getClass().getClassLoader()));
            dependencies.add(getScmDescriptor());
        } catch (final IOException e) {
            throw new MojoExecutionException(e);
        }
        dependencies.addAll(getProjectDependencies());
        return dependencies;
    }

    /**
     * Gets build metadata derived from the current Maven session, including start and finish timestamps.
     *
     * @return The build metadata.
     */
    private BuildMetadata getBuildMetadata() {
        final OffsetDateTime startedOn = session.getStartTime().toInstant().atOffset(ZoneOffset.UTC);
        final OffsetDateTime finishedOn = OffsetDateTime.now(ZoneOffset.UTC);
        return new BuildMetadata(null, startedOn, finishedOn);
    }

    /**
     * Gets resource descriptors for all resolved project dependencies.
     *
     * @return A list of resource descriptors for the project's resolved artifacts.
     * @throws MojoExecutionException If a dependency artifact cannot be described.
     */
    private List<ResourceDescriptor> getProjectDependencies() throws MojoExecutionException {
        final List<ResourceDescriptor> dependencies = new ArrayList<>();
        for (final Artifact artifact : project.getArtifacts()) {
            dependencies.add(ArtifactUtils.toResourceDescriptor(artifact, algorithmNames));
        }
        return dependencies;
    }

    /**
     * Gets a resource descriptor for the current SCM source, including the URI and Git commit digest.
     *
     * @return A resource descriptor for the SCM source.
     * @throws IOException            If the current branch cannot be determined.
     * @throws MojoExecutionException If the SCM revision cannot be retrieved.
     */
    private ResourceDescriptor getScmDescriptor() throws IOException, MojoExecutionException {
        return new ResourceDescriptor()
                .setUri(GitUtils.scmToDownloadUri(scmConnectionUrl, scmDirectory.toPath()))
                .setDigest(Collections.singletonMap("gitCommit", getScmRevision()));
    }

    /**
     * Gets the SCM directory.
     *
     * @return The SCM directory.
     */
    public File getScmDirectory() {
        return scmDirectory;
    }

    /**
     * Gets an SCM repository from the configured connection URL.
     *
     * @return The SCM repository.
     * @throws MojoExecutionException If the SCM repository cannot be created.
     */
    private ScmRepository getScmRepository() throws MojoExecutionException {
        try {
            return scmManager.makeScmRepository(scmConnectionUrl);
        } catch (final ScmException e) {
            throw new MojoExecutionException("Failed to create SCM repository", e);
        }
    }

    /**
     * Gets the current SCM revision (commit hash) for the configured SCM directory.
     *
     * @return The current SCM revision string.
     * @throws MojoExecutionException If the revision cannot be retrieved from SCM.
     */
    private String getScmRevision() throws MojoExecutionException {
        final ScmRepository scmRepository = getScmRepository();
        final CommandParameters commandParameters = new CommandParameters();
        try {
            final InfoScmResult result = scmManager.getProviderByRepository(scmRepository).info(scmRepository.getProviderRepository(),
                    new ScmFileSet(scmDirectory), commandParameters);

            return getScmRevision(result);
        } catch (final ScmException e) {
            throw new MojoExecutionException("Failed to retrieve SCM revision", e);
        }
    }

    /**
     * Extracts the revision string from an SCM info result.
     *
     * @param result The SCM info result.
     * @return The revision string.
     * @throws MojoExecutionException If the result is unsuccessful or contains no revision.
     */
    private String getScmRevision(final InfoScmResult result) throws MojoExecutionException {
        if (!result.isSuccess()) {
            throw new MojoExecutionException("Failed to retrieve SCM revision: " + result.getProviderMessage());
        }

        if (result.getInfoItems() == null || result.getInfoItems().isEmpty()) {
            throw new MojoExecutionException("No SCM revision information found for " + scmDirectory);
        }

        final InfoItem item = result.getInfoItems().get(0);

        final String revision = item.getRevision();
        if (revision == null) {
            throw new MojoExecutionException("Empty SCM revision returned for " + scmDirectory);
        }
        return revision;
    }

    /**
     * Gets the GPG signer, creating and preparing it from plugin parameters if not already set.
     *
     * @return the prepared signer
     * @throws MojoFailureException if signer preparation fails
     */
    private AbstractGpgSigner getSigner() throws MojoFailureException {
        if (signer == null) {
            signer = DsseUtils.createGpgSigner(executable, defaultKeyring, lockMode, keyname, useAgent, getLog());
        }
        return signer;
    }

    /**
     * Get the artifacts generated by the build.
     *
     * @return A list of resource descriptors for the build artifacts.
     * @throws MojoExecutionException If artifact hashing fails.
     */
    private List<ResourceDescriptor> getSubjects() throws MojoExecutionException {
        final List<ResourceDescriptor> subjects = new ArrayList<>();
        subjects.add(ArtifactUtils.toResourceDescriptor(project.getArtifact(), algorithmNames));
        for (final Artifact artifact : project.getAttachedArtifacts()) {
            subjects.add(ArtifactUtils.toResourceDescriptor(artifact, algorithmNames));
        }
        return subjects;
    }

    /**
     * Sets the list of checksum algorithms to use.
     *
     * @param algorithmNames A comma-separated list of {@link java.security.MessageDigest} algorithm names to use.
     */
    void setAlgorithmNames(final String algorithmNames) {
        this.algorithmNames = algorithmNames;
    }

    /**
     * Sets the Maven home directory.
     *
     * @param mavenHome The Maven home directory.
     */
    void setMavenHome(final File mavenHome) {
        this.mavenHome = mavenHome;
    }

    /**
     * Sets the output directory for the attestation file.
     *
     * @param outputDirectory The output directory.
     */
    void setOutputDirectory(final File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    /**
     * Sets the public SCM connection URL.
     *
     * @param scmConnectionUrl The SCM connection URL.
     */
    void setScmConnectionUrl(final String scmConnectionUrl) {
        this.scmConnectionUrl = scmConnectionUrl;
    }

    /**
     * Sets the SCM directory.
     *
     * @param scmDirectory The SCM directory.
     */
    public void setScmDirectory(final File scmDirectory) {
        this.scmDirectory = scmDirectory;
    }

    /**
     * Sets whether to sign the attestation envelope.
     *
     * @param signAttestation {@code true} to sign, {@code false} to skip signing
     */
    void setSignAttestation(final boolean signAttestation) {
        this.signAttestation = signAttestation;
    }

    /**
     * Sets the plugin descriptor. Intended for testing.
     *
     * @param pluginDescriptor the plugin descriptor
     */
    void setPluginDescriptor(final PluginDescriptor pluginDescriptor) {
        this.pluginDescriptor = pluginDescriptor;
    }

    /**
     * Sets the GPG signer used for signing. Intended for testing.
     *
     * @param signer the signer to use
     */
    void setSigner(final AbstractGpgSigner signer) {
        this.signer = signer;
    }

    /**
     * Signs the attestation statement with GPG and writes it to {@code artifactPath}.
     *
     * @param statement    the attestation statement to sign and write
     * @param outputPath   directory used for intermediate PAE and signature files
     * @param artifactPath the destination file path for the envelope
     * @throws MojoExecutionException if serialization, signing, or file I/O fails
     * @throws MojoFailureException   if the GPG signer cannot be prepared
     */
    private void signAndWriteStatement(final Statement statement, final Path outputPath,
            final Path artifactPath) throws MojoExecutionException, MojoFailureException {
        final byte[] statementBytes;
        try {
            statementBytes = OBJECT_MAPPER.writeValueAsBytes(statement);
        } catch (final JsonProcessingException e) {
            throw new MojoExecutionException("Failed to serialize attestation statement", e);
        }
        final AbstractGpgSigner signer = getSigner();
        final Path paeFile = DsseUtils.writePaeFile(statementBytes, outputPath);
        final byte[] sigBytes = DsseUtils.signFile(signer, paeFile);

        final Signature sig = new Signature()
                .setKeyid(DsseUtils.getKeyId(sigBytes))
                .setSig(sigBytes);
        final DsseEnvelope envelope = new DsseEnvelope()
                .setPayload(statementBytes)
                .setSignatures(Collections.singletonList(sig));

        getLog().info("Writing signed attestation envelope to: " + artifactPath);
        writeAndAttach(envelope, artifactPath);
    }

    /**
     * Writes {@code value} as a JSON line to {@code artifactPath} and optionally attaches it to the project.
     *
     * @param value        the object to serialize
     * @param artifactPath the destination file path
     * @throws MojoExecutionException if the file cannot be written
     */
    private void writeAndAttach(final Object value, final Path artifactPath) throws MojoExecutionException {
        try (OutputStream os = Files.newOutputStream(artifactPath)) {
            OBJECT_MAPPER.writeValue(os, value);
            os.write('\n');
        } catch (final IOException e) {
            throw new MojoExecutionException("Could not write attestation to: " + artifactPath, e);
        }
        if (!skipAttach) {
            final Artifact mainArtifact = project.getArtifact();
            getLog().info(String.format("Attaching attestation as %s-%s.%s", mainArtifact.getArtifactId(), mainArtifact.getVersion(), ATTESTATION_EXTENSION));
            mavenProjectHelper.attachArtifact(project, ATTESTATION_EXTENSION, null, artifactPath.toFile());
        }
    }

    /**
     * Serializes the attestation statement as a bare JSON line and writes it to {@code artifactPath}.
     *
     * @param statement    the attestation statement to write
     * @param artifactPath the destination file path
     * @throws MojoExecutionException if the file cannot be written
     */
    private void writeStatement(final Statement statement, final Path artifactPath) throws MojoExecutionException {
        getLog().info("Writing attestation statement to: " + artifactPath);
        writeAndAttach(statement, artifactPath);
    }
}
