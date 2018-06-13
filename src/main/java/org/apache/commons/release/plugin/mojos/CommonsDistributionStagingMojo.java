/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.release.plugin.mojos;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.release.plugin.SharedFunctions;
import org.apache.commons.release.plugin.velocity.HeaderHtmlVelocityDelegate;
import org.apache.commons.release.plugin.velocity.ReadmeHtmlVelocityDelegate;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.command.add.AddScmResult;
import org.apache.maven.scm.command.checkin.CheckInScmResult;
import org.apache.maven.scm.manager.BasicScmManager;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.provider.svn.repository.SvnScmProviderRepository;
import org.apache.maven.scm.provider.svn.svnexe.SvnExeScmProvider;
import org.apache.maven.scm.repository.ScmRepository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class checks out the dev distribution location, copies the distributions into that directory
 * structure under the <code>target/commons-release-plugin/scm</code> directory. Then commits the
 * distributions back up to SVN. Also, we include the built and zipped site as well as the RELEASE-NOTES.txt.
 *
 * @author chtompki
 * @since 1.0
 */
@Mojo(name = "stage-distributions",
        defaultPhase = LifecyclePhase.DEPLOY,
        threadSafe = true,
        aggregator = true)
public class CommonsDistributionStagingMojo extends AbstractMojo {

    /** The name of file generated from the README.vm velocity template to be checked into the dist svn repo. */
    private static final String README_FILE_NAME = "README.html";
    /** The name of file generated from the HEADER.vm velocity template to be checked into the dist svn repo. */
    private static final String HEADER_FILE_NAME = "HEADER.html";

    /**
     * The {@link MavenProject} object is essentially the context of the maven build at
     * a given time.
     */
    @Parameter(defaultValue = "${project}", required = true)
    private MavenProject project;

    /**
     * The {@link File} that contains a file to the root directory of the working project. Typically
     * this directory is where the <code>pom.xml</code> resides.
     */
    @Parameter(defaultValue = "${basedir}")
    private File baseDir;

    /** The location to which the site gets built during running <code>mvn site</code>. */
    @Parameter(defaultValue = "${project.build.directory}/site", property = "commons.siteOutputDirectory")
    private File siteDirectory;

    /**
     * The main working directory for the plugin, namely <code>target/commons-release-plugin</code>, but
     * that assumes that we're using the default maven <code>${project.build.directory}</code>.
     */
    @Parameter(defaultValue = "${project.build.directory}/commons-release-plugin", property = "commons.outputDirectory")
    private File workingDirectory;

    /**
     * The location to which to checkout the dist subversion repository under our working directory, which
     * was given above.
     */
    @Parameter(defaultValue = "${project.build.directory}/commons-release-plugin/scm",
            property = "commons.distCheckoutDirectory")
    private File distCheckoutDirectory;

    /**
     * The location of the RELEASE-NOTES.txt file such that multi-module builds can configure it.
     */
    @Parameter(defaultValue = "${basedir}/RELEASE-NOTES.txt", property = "commons.releaseNotesLocation")
    private File releaseNotesFile;

    /**
     * A boolean that determines whether or not we actually commit the files up to the subversion repository.
     * If this is set to <code>true</code>, we do all but make the commits. We do checkout the repository in question
     * though.
     */
    @Parameter(property = "commons.release.dryRun", defaultValue = "false")
    private Boolean dryRun;

    /**
     * The url of the subversion repository to which we wish the artifacts to be staged. Typically this would need to
     * be of the form: <code>scm:svn:https://dist.apache.org/repos/dist/dev/commons/foo/version-RC#</code>. Note. that
     * the prefix to the substring <code>https</code> is a requirement.
     */
    @Parameter(defaultValue = "", property = "commons.distSvnStagingUrl")
    private String distSvnStagingUrl;

    /**
     * A parameter to generally avoid running unless it is specifically turned on by the consuming module.
     */
    @Parameter(defaultValue = "false", property = "commons.release.isDistModule")
    private Boolean isDistModule;

    /**
     * The release version of the artifact to be built.
     */
    @Parameter(property = "commons.release.version")
    private String commonsReleaseVersion;

    /**
     * The RC version of the release. For example the first voted on candidate would be "RC1".
     */
    @Parameter(property = "commons.rc.version")
    private String commonsRcVersion;

    /**
     * The username for the distribution subversion repository. This is typically your Apache id.
     */
    @Parameter(property = "user.name")
    private String username;

    /**
     * The password associated with {@link CommonsDistributionStagingMojo#username}.
     */
    @Parameter(property = "user.password")
    private String password;

    /**
     * A subdirectory of the dist directory into which we are going to stage the release candidate. We
     * build this up in the {@link CommonsDistributionStagingMojo#execute()} method. And, for example,
     * the directory should look like <code>https://https://dist.apache.org/repos/dist/dev/commons/text/1.4-RC1</code>.
     */
    private File distVersionRcVersionDirectory;

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
            getLog().info("Current project contains no distributions. Not executing.");
            return;
        }
        getLog().info("Preparing to stage distributions");
        try {
            ScmManager scmManager = new BasicScmManager();
            scmManager.setScmProvider("svn", new SvnExeScmProvider());
            ScmRepository repository = scmManager.makeScmRepository(distSvnStagingUrl);
            ScmProvider provider = scmManager.getProviderByRepository(repository);
            SvnScmProviderRepository providerRepository = (SvnScmProviderRepository) repository.getProviderRepository();
            providerRepository.setUser(username);
            providerRepository.setPassword(password);
            distVersionRcVersionDirectory =
                    new File(distCheckoutDirectory, commonsReleaseVersion + "-" + commonsRcVersion);
            if (!distCheckoutDirectory.exists()) {
                SharedFunctions.initDirectory(getLog(), distCheckoutDirectory);
            }
            ScmFileSet scmFileSet = new ScmFileSet(distCheckoutDirectory);
            getLog().info("Checking out dist from: " + distSvnStagingUrl);
            provider.checkOut(repository, scmFileSet);
            File copiedReleaseNotes = copyReleaseNotesToWorkingDirectory();
            List<File> filesToCommit = copyDistributionsIntoScmDirectoryStructure(copiedReleaseNotes);
            filesToCommit.addAll(copySiteToScmDirectory());
            if (!dryRun) {
                commitDirectories(filesToCommit, provider, repository);
                commitFiles(filesToCommit, provider, repository);
            } else {
                getLog().info("[Dry run] Would have committed to: " + distSvnStagingUrl);
                getLog().info(
                        "[Dry run] Staging release: " + project.getArtifactId() + ", version: " + project.getVersion());
            }
        } catch (ScmException e) {
            getLog().error("Could not commit files to dist: " + distSvnStagingUrl, e);
            throw new MojoExecutionException("Could not commit files to dist: " + distSvnStagingUrl, e);
        }
    }

    /**
     * Commits all the directories to SVN from the fileset.
     *
     * @param filesToCommit the {@link List} of {@link File} that we find the directories to which we must commit.
     * @param provider the maven {@link ScmProvider}.
     * @param repository the maven {@link ScmRepository}.
     * @throws ScmException if the maven SCM api fails.
     * @throws MojoExecutionException if we get a failure that does not throw an exception.
     */
    private void commitDirectories(List<File> filesToCommit, ScmProvider provider, ScmRepository repository)
            throws ScmException, MojoExecutionException {
        Collections.sort(filesToCommit);
        Set<File> committedDirectories = new HashSet<>();
        for (File file : filesToCommit) {
            if (file.getAbsolutePath().contains(commonsReleaseVersion + "-" + commonsRcVersion)
                    && !committedDirectories.contains(file.getParentFile())
                    && !file.getParentFile().getAbsolutePath().equals(distCheckoutDirectory)) {
                File parentFile = file.getParentFile();
                commitParentsIfNotCommitted(parentFile, committedDirectories);
                ScmFileSet scmFileSetToCommit = new ScmFileSet(distCheckoutDirectory, parentFile);
                AddScmResult addResult = provider.add(
                        repository,
                        scmFileSetToCommit
                );
                if (addResult.isSuccess()) {
                    getLog().info("Adding release directories: "
                            + project.getArtifactId() + ", version: " + project.getVersion());
                    CheckInScmResult checkInResult = provider.checkIn(
                            repository,
                            scmFileSetToCommit,
                            "Adding release directories: "
                                    + project.getArtifactId() + ", version: " + project.getVersion()
                    );
                    if (!checkInResult.isSuccess()) {
                        getLog().error("Committing directories  failed: " + checkInResult.getCommandOutput());
                        throw new MojoExecutionException(
                                "Committing directories files failed: " + checkInResult.getCommandOutput()
                        );
                    }
                    committedDirectories.add(parentFile);
                } else {
                    getLog().error("Adding directory failed: " + addResult.getCommandOutput());
                    throw new MojoExecutionException("Adding directory failed: "
                            + addResult.getCommandOutput());
                }
            }
        }
    }

    private void commitParentsIfNotCommitted(File file, Set<File> committedDirectories) {
        if (committedDirectories.contains(file.getParentFile())) {
            return;
        }
    }

    /**
     * Commits files to SVN from the fileset.
     *
     * @param filesToCommit the {@link List} of {@link File} we must commit.
     * @param provider the maven {@link ScmProvider}.
     * @param repository the maven {@link ScmRepository}.
     * @throws ScmException if the maven SCM api fails.
     * @throws MojoExecutionException if we get a failure that does not throw an exception.
     */
    private void commitFiles(List<File> filesToCommit, ScmProvider provider, ScmRepository repository)
            throws ScmException, MojoExecutionException {
        ScmFileSet scmFileSetToCommit = new ScmFileSet(distCheckoutDirectory, filesToCommit);
        AddScmResult addResult = provider.add(
                repository,
                scmFileSetToCommit
        );
        if (addResult.isSuccess()) {
            getLog().info("Staging release: " + project.getArtifactId() + ", version: " + project.getVersion());
            CheckInScmResult checkInResult = provider.checkIn(
                    repository,
                    scmFileSetToCommit,
                    "Staging release: " + project.getArtifactId() + ", version: " + project.getVersion()
            );
            if (!checkInResult.isSuccess()) {
                getLog().error("Committing dist files failed: " + checkInResult.getCommandOutput());
                throw new MojoExecutionException(
                        "Committing dist files failed: " + checkInResult.getCommandOutput()
                );
            }
            getLog().info("Committed revision " + checkInResult.getScmRevision());
        } else {
            getLog().error("Adding dist files failed: " + addResult.getCommandOutput());
            throw new MojoExecutionException("Adding dist files failed: " + addResult.getCommandOutput());
        }
    }

    /**
     * A utility method that takes the <code>RELEASE-NOTES.txt</code> file from the base directory of the
     * project and copies it into {@link CommonsDistributionStagingMojo#workingDirectory}.
     *
     * @return the RELEASE-NOTES.txt file that exists in the <code>target/commons-release-notes/scm</code>
     *         directory for the purpose of adding it to the scm change set in the method
     *         {@link CommonsDistributionStagingMojo#copyDistributionsIntoScmDirectoryStructure(File)}.
     * @throws MojoExecutionException if an {@link IOException} occurs as a wrapper so that maven
     *                                can properly handle the exception.
     */
    private File copyReleaseNotesToWorkingDirectory() throws MojoExecutionException {
        StringBuffer copiedReleaseNotesAbsolutePath;
        SharedFunctions.initDirectory(getLog(), distVersionRcVersionDirectory);
        getLog().info("Copying RELEASE-NOTES.txt to working directory.");
        File copiedReleaseNotes = new File(distVersionRcVersionDirectory, releaseNotesFile.getName());
        SharedFunctions.copyFile(getLog(), releaseNotesFile, copiedReleaseNotes);
        return copiedReleaseNotes;
    }

    /**
     * Copies the list of files at the root of the {@link CommonsDistributionStagingMojo#workingDirectory} into
     * the directory structure of the distribution staging repository. Specifically:
     * <ul>
     *     <li>root:</li>
     *     <li><ul>
     *         <li>site.zip</li>
     *         <li>RELEASE-NOTES.txt</li>
     *         <li>source:</li>
     *         <li><ul>
     *             <li>-src artifacts....</li>
     *         </ul></li>
     *         <li>binaries:</li>
     *         <li><ul>
     *             <li>-bin artifacts....</li>
     *         </ul></li>
     *     </ul></li>
     * </ul>
     *
     * @param copiedReleaseNotes is the RELEASE-NOTES.txt file that exists in the
     *                           <code>target/commons-release-plugin/scm</code> directory.
     * @return a {@link List} of {@link File}'s in the directory for the purpose of adding them to the maven
     *         {@link ScmFileSet}.
     * @throws MojoExecutionException if an {@link IOException} occurs so that Maven can handle it properly.
     */
    private List<File> copyDistributionsIntoScmDirectoryStructure(File copiedReleaseNotes)
            throws MojoExecutionException {
        List<File> workingDirectoryFiles = Arrays.asList(workingDirectory.listFiles());
        File scmBinariesRoot = new File(distVersionRcVersionDirectory, "binaries");
        File scmSourceRoot = new File(distVersionRcVersionDirectory, "source");
        SharedFunctions.initDirectory(getLog(), scmBinariesRoot);
        SharedFunctions.initDirectory(getLog(), scmSourceRoot);
        List<File> filesForMavenScmFileSet = new ArrayList<>();
        File copy;
        for (File file : workingDirectoryFiles) {
            if (file.getName().contains("src")) {
                copy = new File(scmSourceRoot,  file.getName());
                SharedFunctions.copyFile(getLog(), file, copy);
                filesForMavenScmFileSet.add(copy);
            } else if (file.getName().contains("bin")) {
                copy = new File(scmBinariesRoot,  file.getName());
                SharedFunctions.copyFile(getLog(), file, copy);
                filesForMavenScmFileSet.add(copy);
            } else if (StringUtils.containsAny(file.getName(), "scm", "sha1.properties", "sha256.properties")) {
                getLog().debug("Not copying scm directory over to the scm directory because it is the scm directory.");
                //do nothing because we are copying into scm
            } else {
                copy = new File(distCheckoutDirectory.getAbsolutePath(),  file.getName());
                SharedFunctions.copyFile(getLog(), file, copy);
                filesForMavenScmFileSet.add(copy);
            }
        }
        filesForMavenScmFileSet.addAll(buildReadmeAndHeaderHtmlFiles());
        filesForMavenScmFileSet.add(copiedReleaseNotes);
        return filesForMavenScmFileSet;
    }

    /**
     * Copies <code>${basedir}/target/site</code> to <code>${basedir}/target/commons-release-plugin/scm/site</code>.
     *
     * @return the {@link List} of {@link File}'s contained in
     *         <code>${basedir}/target/commons-release-plugin/scm/site</code>, after the copy is complete.
     * @throws MojoExecutionException if the site copying fails for some reason.
     */
    private List<File> copySiteToScmDirectory() throws MojoExecutionException {
        if (!siteDirectory.exists()) {
            getLog().error("\"mvn site\" was not run before this goal, or a siteDirectory did not exist.");
            throw new MojoExecutionException(
                    "\"mvn site\" was not run before this goal, or a siteDirectory did not exist."
            );
        }
        try {
            FileUtils.copyDirectoryToDirectory(siteDirectory, distVersionRcVersionDirectory);
        } catch (IOException e) {
            throw new MojoExecutionException("Site copying failed", e);
        }
        File siteInScm = new File(distVersionRcVersionDirectory, "site");
        return new ArrayList<>(FileUtils.listFiles(siteInScm, null, true));
    }

    /**
     * Builds up <code>README.html</code> and <code>HEADER.html</code> that reside in following.
     * <ul>
     *     <li>distRoot
     *     <ul>
     *         <li>binaries/HEADER.html (symlink)</li>
     *         <li>binaries/README.html (symlink)</li>
     *         <li>source/HEADER.html (symlink)</li>
     *         <li>source/README.html (symlink)</li>
     *         <li>HEADER.html</li>
     *         <li>README.html</li>
     *     </ul>
     *     </li>
     * </ul>
     * @return the {@link List} of created files above
     * @throws MojoExecutionException if an {@link IOException} occurs in the creation of these
     *                                files fails.
     */
    private List<File> buildReadmeAndHeaderHtmlFiles() throws MojoExecutionException {
        List<File> headerAndReadmeFiles = new ArrayList<>();
        File headerFile = new File(distVersionRcVersionDirectory, HEADER_FILE_NAME);
        //
        // HEADER file
        //
        try (Writer headerWriter = new OutputStreamWriter(new FileOutputStream(headerFile), "UTF-8")) {
            HeaderHtmlVelocityDelegate.builder().build().render(headerWriter);
        } catch (IOException e) {
            final String message = "Could not build HEADER html file " + headerFile;
            getLog().error(message, e);
            throw new MojoExecutionException(message, e);
        }
        headerAndReadmeFiles.add(headerFile);
        //
        // README file
        //
        File readmeFile = new File(distVersionRcVersionDirectory, README_FILE_NAME);
        try (Writer readmeWriter = new OutputStreamWriter(new FileOutputStream(readmeFile), "UTF-8")) {
            // @formatter:off
            ReadmeHtmlVelocityDelegate readmeHtmlVelocityDelegate = ReadmeHtmlVelocityDelegate.builder()
                    .withArtifactId(project.getArtifactId())
                    .withVersion(project.getVersion())
                    .withSiteUrl(project.getUrl())
                    .build();
            // @formatter:on
            readmeHtmlVelocityDelegate.render(readmeWriter);
        } catch (IOException e) {
            final String message = "Could not build README html file " + readmeFile;
            getLog().error(message, e);
            throw new MojoExecutionException(message, e);
        }
        headerAndReadmeFiles.add(readmeFile);
        headerAndReadmeFiles.addAll(copyHeaderAndReadmeToSubdirectories(headerFile, readmeFile));
        return headerAndReadmeFiles;
    }

    /**
     * Copies <code>README.html</code> and <code>HEADER.html</code> to the source and binaries
     * directories.
     *
     * @param headerFile The originally created <code>HEADER.html</code> file.
     * @param readmeFile The originally created <code>README.html</code> file.
     * @return a {@link List} of created files.
     * @throws MojoExecutionException if the {@link SharedFunctions#copyFile(Log, File, File)}
     *                                fails.
     */
    private List<File> copyHeaderAndReadmeToSubdirectories(File headerFile, File readmeFile)
            throws MojoExecutionException {
        List<File> symbolicLinkFiles = new ArrayList<>();
        File sourceRoot = new File(distVersionRcVersionDirectory, "source");
        File binariesRoot = new File(distVersionRcVersionDirectory, "binaries");
        File sourceHeaderFile = new File(sourceRoot, HEADER_FILE_NAME);
        File sourceReadmeFile = new File(sourceRoot, README_FILE_NAME);
        File binariesHeaderFile = new File(binariesRoot, HEADER_FILE_NAME);
        File binariesReadmeFile = new File(binariesRoot, README_FILE_NAME);
        SharedFunctions.copyFile(getLog(), headerFile, sourceHeaderFile);
        symbolicLinkFiles.add(sourceHeaderFile);
        SharedFunctions.copyFile(getLog(), readmeFile, sourceReadmeFile);
        symbolicLinkFiles.add(sourceReadmeFile);
        SharedFunctions.copyFile(getLog(), headerFile, binariesHeaderFile);
        symbolicLinkFiles.add(binariesHeaderFile);
        SharedFunctions.copyFile(getLog(), readmeFile, binariesReadmeFile);
        symbolicLinkFiles.add(binariesReadmeFile);
        return symbolicLinkFiles;
    }

    /**
     * This method is the setter for the {@link CommonsDistributionStagingMojo#baseDir} field, specifically
     * for the usage in the unit tests.
     *
     * @param baseDir is the {@link File} to be used as the project's root directory when this mojo
     *                is invoked.
     */
    protected void setBaseDir(File baseDir) {
        this.baseDir = baseDir;
    }
}
