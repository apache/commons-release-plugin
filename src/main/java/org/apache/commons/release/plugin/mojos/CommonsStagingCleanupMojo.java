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
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.release.plugin.SharedFunctions;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.command.checkin.CheckInScmResult;
import org.apache.maven.scm.command.checkout.CheckOutScmResult;
import org.apache.maven.scm.command.remove.RemoveScmResult;
import org.apache.maven.scm.manager.BasicScmManager;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.provider.svn.repository.SvnScmProviderRepository;
import org.apache.maven.scm.provider.svn.svnexe.SvnExeScmProvider;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.SettingsDecrypter;

/**
 * Checks out the development distribution repository and deletes its contents if it is not empty.
 *
 * @since 1.6
 */
@Mojo(name = "clean-staging",
        defaultPhase = LifecyclePhase.POST_CLEAN,
        threadSafe = true,
        aggregator = true)
public final class CommonsStagingCleanupMojo extends AbstractMojo {

    /**
     * The Maven project context for the current build.
     */
    @Parameter(defaultValue = "${project}", required = true)
    private MavenProject project;

    /**
     * The main working directory for the plugin, which is <code>target/commons-release-plugin</code> when Maven uses
     * the default <code>${project.build.directory}</code>.
     */
    @Parameter(defaultValue = "${project.build.directory}/commons-release-plugin", property = "commons.outputDirectory")
    private File workingDirectory;

    /**
     * The directory under the working directory in which to check out the distribution Subversion repository for cleanup.
     */
    @Parameter(defaultValue = "${project.build.directory}/commons-release-plugin/scm-cleanup",
            property = "commons.distCleanupDirectory")
    private File distCleanupDirectory;

    /**
     * Whether to perform a dry run without committing changes to the Subversion repository.
     *
     * <p>
     * If {@code true}, the mojo checks out the repository and identifies files to delete but does not remove them or commit changes.
     * </p>
     */
    @Parameter(property = "commons.release.dryRun", defaultValue = "false")
    private Boolean dryRun;

    /**
     * The URL of the Subversion repository used to stage artifacts.
     *
     * <p>
     * Typically, this has the form <code>scm:svn:https://dist.apache.org/repos/dist/dev/commons/foo/version-RC#</code>.
     * The <code>scm:svn:</code> prefix is required.
     * </p>
     */
    @Parameter(defaultValue = "", property = "commons.distSvnStagingUrl")
    private String distSvnStagingUrl;

    /**
     * Whether the consuming module enables this mojo for distribution artifacts.
     */
    @Parameter(defaultValue = "false", property = "commons.release.isDistModule")
    private Boolean isDistModule;

    /**
     * The ID of the server in <code>settings.xml</code> used for authentication to the distribution repository.
     *
     * <p>
     * These credentials take precedence over {@link #username} and {@link #password}.
     * </p>
     */
    @Parameter(property = "commons.distServer")
    private String distServer;

    /**
     * The username for the distribution Subversion repository. This is typically your Apache ID.
     */
    @Parameter(property = "user.name")
    private String username;

    /**
     * The password associated with {@link #username}.
     */
    @Parameter(property = "user.password")
    private String password;

    /**
     * Maven {@link Settings}.
     */
    @Parameter(defaultValue = "${settings}", readonly = true, required = true)
    private Settings settings;

    /**
     * Maven {@link SettingsDecrypter} component.
     */
    @Component
    private SettingsDecrypter settingsDecrypter;

    /**
     * Constructs a new instance.
     */
    public CommonsStagingCleanupMojo() {
        // No initialization required.
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!isDistModule) {
            getLog().info("This module is marked as a non distribution "
                    + "or assembly module, and the plugin will not run.");
            return;
        }
        if (StringUtils.isEmpty(distSvnStagingUrl)) {
            getLog().warn("commons.distSvnStagingUrl is not set, the commons-release-plugin will not run.");
            return;
        }
        if (!workingDirectory.exists()) {
            SharedFunctions.initDirectory(getLog(), workingDirectory);
        }
        try {
            final ScmManager scmManager = new BasicScmManager();
            scmManager.setScmProvider("svn", new SvnExeScmProvider());
            final ScmRepository repository = scmManager.makeScmRepository(distSvnStagingUrl);
            final ScmProvider provider = scmManager.getProviderByRepository(repository);
            final SvnScmProviderRepository providerRepository = (SvnScmProviderRepository) repository
                    .getProviderRepository();
            SharedFunctions.setAuthentication(
                    providerRepository,
                    distServer,
                    settings,
                    settingsDecrypter,
                    username,
                    password
            );
            getLog().info("Checking out dist from: " + distSvnStagingUrl);
            final ScmFileSet scmFileSet = new ScmFileSet(distCleanupDirectory);
            final CheckOutScmResult checkOutResult = provider.checkOut(repository, scmFileSet);
            if (!checkOutResult.isSuccess()) {
                throw new MojoExecutionException("Failed to checkout files from SCM: "
                        + checkOutResult.getProviderMessage() + " [" + checkOutResult.getCommandOutput() + "]");
            }
            final List<File> filesToRemove = Arrays.asList(distCleanupDirectory.listFiles());
            if (filesToRemove.size() == 1) {
                getLog().info("No files to delete");
                return;
            }
            if (!dryRun) {
                final ScmFileSet fileSet = new ScmFileSet(distCleanupDirectory, filesToRemove);
                final RemoveScmResult removeScmResult = provider.remove(repository, fileSet,
                        "Cleaning up staging area");
                if (!removeScmResult.isSuccess()) {
                    throw new MojoFailureException("Failed to remove files from SCM: "
                            + removeScmResult.getProviderMessage()
                            + " [" + removeScmResult.getCommandOutput() + "]");
                }
                getLog().info("Cleaning distribution area for: " + project.getArtifactId());
                final CheckInScmResult checkInResult = provider.checkIn(
                        repository,
                        fileSet,
                        "Cleaning distribution area for: " + project.getArtifactId()
                );
                if (!checkInResult.isSuccess()) {
                    throw new MojoFailureException("Failed to commit files: " + removeScmResult.getProviderMessage()
                            + " [" + removeScmResult.getCommandOutput() + "]");
                }
            } else {
                getLog().info("Would have attempted to delete files from: " + distSvnStagingUrl);
            }
        } catch (final ScmException e) {
            throw new MojoFailureException(e.getMessage());
        }

    }
}
