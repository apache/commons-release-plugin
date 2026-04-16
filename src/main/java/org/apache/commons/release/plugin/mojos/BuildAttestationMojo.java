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
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.release.plugin.internal.ArtifactUtils;
import org.apache.commons.release.plugin.internal.BuildToolDescriptors;
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
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
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
     * Shared Jackson object mapper for serializing attestation statements.
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.findAndRegisterModules();
        OBJECT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        OBJECT_MAPPER.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
    }

    /**
     * The SCM connection URL for the current project.
     */
    @Parameter(defaultValue = "${project.scm.connection}", readonly = true)
    private String scmConnectionUrl;

    /**
     * The Maven home directory.
     */
    @Parameter(defaultValue = "${maven.home}", readonly = true)
    private File mavenHome;

    /**
     * Issue SCM actions at this local directory.
     */
    @Parameter(property = "commons.release.scmDirectory", defaultValue = "${basedir}")
    private File scmDirectory;

    /**
     * The output directory for the attestation file.
     */
    @Parameter(property = "commons.release.outputDirectory", defaultValue = "${project.build.directory}")
    private File outputDirectory;

    /**
     * Whether to skip attaching the attestation artifact to the project.
     */
    @Parameter(property = "commons.release.skipAttach")
    private boolean skipAttach;

    /**
     * Whether to sign the attestation envelope with GPG.
     */
    @Parameter(property = "commons.release.signAttestation", defaultValue = "true")
    private boolean signAttestation;

    /**
     * Path to the GPG executable; if not set, {@code gpg} is resolved from {@code PATH}.
     */
    @Parameter(property = "gpg.executable")
    private String executable;

    /**
     * Whether to include the default GPG keyring.
     *
     * <p>When {@code false}, passes {@code --no-default-keyring} to the GPG command.</p>
     */
    @Parameter(property = "gpg.defaultKeyring", defaultValue = "true")
    private boolean defaultKeyring;

    /**
     * GPG database lock mode passed via {@code --lock-once}, {@code --lock-multiple}, or
     * {@code --lock-never}; no lock flag is added when not set.
     */
    @Parameter(property = "gpg.lockMode")
    private String lockMode;

    /**
     * Name or fingerprint of the GPG key to use for signing.
     *
     * <p>Passed as {@code --local-user} to the GPG command; uses the default key when not set.</p>
     */
    @Parameter(property = "gpg.keyname")
    private String keyname;

    /**
     * Whether to use gpg-agent for passphrase management.
     *
     * <p>For GPG versions before 2.1, passes {@code --use-agent} or {@code --no-use-agent}
     * accordingly; ignored for GPG 2.1 and later where the agent is always used.</p>
     */
    @Parameter(property = "gpg.useagent", defaultValue = "true")
    private boolean useAgent;

    /**
     * The current Maven project.
     */
    private final MavenProject project;

    /**
     * SCM manager to detect the Git revision.
     */
    private final ScmManager scmManager;

    /**
     * Runtime information.
     */
    private final RuntimeInformation runtimeInformation;

    /**
     * The current Maven session, used to resolve plugin dependencies.
     */
    private final MavenSession session;

    /**
     * Helper to attach artifacts to the project.
     */
    private final MavenProjectHelper mavenProjectHelper;

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
    public BuildAttestationMojo(MavenProject project, ScmManager scmManager, RuntimeInformation runtimeInformation, MavenSession session,
            MavenProjectHelper mavenProjectHelper) {
        this.project = project;
        this.scmManager = scmManager;
        this.runtimeInformation = runtimeInformation;
        this.session = session;
        this.mavenProjectHelper = mavenProjectHelper;
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
     * Returns the SCM directory.
     *
     * @return The SCM directory.
     */
    public File getScmDirectory() {
        return scmDirectory;
    }

    /**
     * Sets the SCM directory.
     *
     * @param scmDirectory The SCM directory.
     */
    public void setScmDirectory(File scmDirectory) {
        this.scmDirectory = scmDirectory;
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
     * Sets the Maven home directory.
     *
     * @param mavenHome The Maven home directory.
     */
    void setMavenHome(final File mavenHome) {
        this.mavenHome = mavenHome;
    }

    @Override
    public void execute() throws MojoFailureException, MojoExecutionException {
        // Build definition
        BuildDefinition buildDefinition = new BuildDefinition();
        buildDefinition.setExternalParameters(getExternalParameters());
        buildDefinition.setResolvedDependencies(getBuildDependencies());
        // Builder
        Builder builder = new Builder();
        // RunDetails
        RunDetails runDetails = new RunDetails();
        runDetails.setBuilder(builder);
        runDetails.setMetadata(getBuildMetadata());
        // Provenance
        Provenance provenance = new Provenance();
        provenance.setBuildDefinition(buildDefinition);
        provenance.setRunDetails(runDetails);
        // Statement
        Statement statement = new Statement();
        statement.setSubject(getSubjects());
        statement.setPredicate(provenance);

        final Path outputPath = ensureOutputDirectory();
        final Path artifactPath = outputPath.resolve(ArtifactUtils.getFileName(project.getArtifact(), ATTESTATION_EXTENSION));
        if (signAttestation) {
            signAndWriteStatement(statement, outputPath, artifactPath);
        } else {
            writeStatement(statement, artifactPath);
        }
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
        } catch (IOException e) {
            throw new MojoExecutionException("Could not create output directory.", e);
        }
        return outputPath;
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
        } catch (JsonProcessingException e) {
            throw new MojoExecutionException("Failed to serialize attestation statement", e);
        }
        final AbstractGpgSigner signer = DsseUtils.createGpgSigner(executable, defaultKeyring, lockMode, keyname, useAgent, getLog());
        final Path paeFile = DsseUtils.writePaeFile(statementBytes, outputPath);
        final byte[] sigBytes = DsseUtils.signFile(signer, paeFile);

        final Signature sig = new Signature();
        sig.setKeyid(DsseUtils.getKeyId(sigBytes));
        sig.setSig(sigBytes);

        final DsseEnvelope envelope = new DsseEnvelope();
        envelope.setPayload(statementBytes);
        envelope.setSignatures(Collections.singletonList(sig));

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
        } catch (IOException e) {
            throw new MojoExecutionException("Could not write attestation to: " + artifactPath, e);
        }
        if (!skipAttach) {
            final Artifact mainArtifact = project.getArtifact();
            getLog().info(String.format("Attaching attestation as %s-%s.%s", mainArtifact.getArtifactId(), mainArtifact.getVersion(), ATTESTATION_EXTENSION));
            mavenProjectHelper.attachArtifact(project, ATTESTATION_EXTENSION, null, artifactPath.toFile());
        }
    }

    /**
     * Get the artifacts generated by the build.
     *
     * @return A list of resource descriptors for the build artifacts.
     * @throws MojoExecutionException If artifact hashing fails.
     */
    private List<ResourceDescriptor> getSubjects() throws MojoExecutionException {
        List<ResourceDescriptor> subjects = new ArrayList<>();
        subjects.add(ArtifactUtils.toResourceDescriptor(project.getArtifact()));
        for (Artifact artifact : project.getAttachedArtifacts()) {
            subjects.add(ArtifactUtils.toResourceDescriptor(artifact));
        }
        return subjects;
    }

    /**
     * Gets map of external build parameters captured from the current JVM and Maven session.
     *
     * @return A map of parameter names to values.
     */
    private Map<String, Object> getExternalParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("jvm.args", ManagementFactory.getRuntimeMXBean().getInputArguments());
        MavenExecutionRequest request = session.getRequest();
        params.put("maven.goals", request.getGoals());
        params.put("maven.profiles", request.getActiveProfiles());
        params.put("maven.user.properties", request.getUserProperties());
        params.put("maven.cmdline", getCommandLine(request));
        Map<String, Object> env = new HashMap<>();
        params.put("env", env);
        for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
            String key = entry.getKey();
            if ("TZ".equals(key) || "LANG".equals(key) || key.startsWith("LC_")) {
                env.put(key, entry.getValue());
            }
        }
        return params;
    }

    /**
     * Reconstructs the Maven command line string from the given execution request.
     *
     * @param request The Maven execution request.
     * @return A string representation of the Maven command line.
     */
    private String getCommandLine(final MavenExecutionRequest request) {
        StringBuilder sb = new StringBuilder();
        for (String goal : request.getGoals()) {
            sb.append(goal);
            sb.append(" ");
        }
        List<String> activeProfiles = request.getActiveProfiles();
        if (activeProfiles != null && !activeProfiles.isEmpty()) {
            sb.append("-P");
            for (String profile : activeProfiles) {
                sb.append(profile);
                sb.append(",");
            }
            removeLast(sb);
            sb.append(" ");
        }
        Properties userProperties = request.getUserProperties();
        for (String propertyName : userProperties.stringPropertyNames()) {
            sb.append("-D");
            sb.append(propertyName);
            sb.append("=");
            sb.append(userProperties.get(propertyName));
            sb.append(" ");
        }
        removeLast(sb);
        return sb.toString();
    }

    /**
     * Removes the last character from the given {@link StringBuilder} if it is non-empty.
     *
     * @param sb The string builder to trim.
     */
    private static void removeLast(final StringBuilder sb) {
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
    }

    /**
     * Returns resource descriptors for the JVM, Maven installation, SCM source, and project dependencies.
     *
     * @return A list of resolved build dependencies.
     * @throws MojoExecutionException If any dependency cannot be resolved or hashed.
     */
    private List<ResourceDescriptor> getBuildDependencies() throws MojoExecutionException {
        List<ResourceDescriptor> dependencies = new ArrayList<>();
        try {
            dependencies.add(BuildToolDescriptors.jvm(Paths.get(System.getProperty("java.home"))));
            dependencies.add(BuildToolDescriptors.maven(runtimeInformation.getMavenVersion(), mavenHome.toPath(),
                    runtimeInformation.getClass().getClassLoader()));
            dependencies.add(getScmDescriptor());
        } catch (IOException e) {
            throw new MojoExecutionException(e);
        }
        dependencies.addAll(getProjectDependencies());
        return dependencies;
    }

    /**
     * Returns resource descriptors for all resolved project dependencies.
     *
     * @return A list of resource descriptors for the project's resolved artifacts.
     * @throws MojoExecutionException If a dependency artifact cannot be described.
     */
    private List<ResourceDescriptor> getProjectDependencies() throws MojoExecutionException {
        List<ResourceDescriptor> dependencies = new ArrayList<>();
        for (Artifact artifact : project.getArtifacts()) {
            dependencies.add(ArtifactUtils.toResourceDescriptor(artifact));
        }
        return dependencies;
    }

    /**
     * Returns a resource descriptor for the current SCM source, including the URI and Git commit digest.
     *
     * @return A resource descriptor for the SCM source.
     * @throws IOException            If the current branch cannot be determined.
     * @throws MojoExecutionException If the SCM revision cannot be retrieved.
     */
    private ResourceDescriptor getScmDescriptor() throws IOException, MojoExecutionException {
        ResourceDescriptor scmDescriptor = new ResourceDescriptor();
        String scmUri = GitUtils.scmToDownloadUri(scmConnectionUrl, scmDirectory.toPath());
        scmDescriptor.setUri(scmUri);
        // Compute the revision
        Map<String, String> digest = new HashMap<>();
        digest.put("gitCommit", getScmRevision());
        scmDescriptor.setDigest(digest);
        return scmDescriptor;
    }

    /**
     * Creates and returns an SCM repository from the configured connection URL.
     *
     * @return The SCM repository.
     * @throws MojoExecutionException If the SCM repository cannot be created.
     */
    private ScmRepository getScmRepository() throws MojoExecutionException {
        try {
            return scmManager.makeScmRepository(scmConnectionUrl);
        } catch (ScmException e) {
            throw new MojoExecutionException("Failed to create SCM repository", e);
        }
    }

    /**
     * Returns the current SCM revision (commit hash) for the configured SCM directory.
     *
     * @return The current SCM revision string.
     * @throws MojoExecutionException If the revision cannot be retrieved from SCM.
     */
    private String getScmRevision() throws MojoExecutionException {
        ScmRepository scmRepository = getScmRepository();
        CommandParameters commandParameters = new CommandParameters();
        try {
            InfoScmResult result = scmManager.getProviderByRepository(scmRepository).info(scmRepository.getProviderRepository(), new ScmFileSet(scmDirectory)
                    , commandParameters);

            return getScmRevision(result);
        } catch (ScmException e) {
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

        InfoItem item = result.getInfoItems().get(0);

        String revision = item.getRevision();
        if (revision == null) {
            throw new MojoExecutionException("Empty SCM revision returned for " + scmDirectory);
        }
        return revision;
    }

    /**
     * Returns build metadata derived from the current Maven session, including start and finish timestamps.
     *
     * @return The build metadata.
     */
    private BuildMetadata getBuildMetadata() {
        OffsetDateTime startedOn = session.getStartTime().toInstant().atOffset(ZoneOffset.UTC);
        OffsetDateTime finishedOn = OffsetDateTime.now(ZoneOffset.UTC);
        return new BuildMetadata(session.getRequest().getBuilderId(), startedOn, finishedOn);
    }
}
