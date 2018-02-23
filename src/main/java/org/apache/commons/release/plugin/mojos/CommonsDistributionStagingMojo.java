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

import org.apache.commons.release.plugin.SharedFunctions;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    private File basedir;

    /**
     * The main working directory for the plugin, namely <code>target/commons-release-plugin</code>, but
     * that assumes that we're using the default maven <code>${project.build.directory}</code>.
     */
    @Parameter(defaultValue = "${project.build.directory}/commons-release-plugin", alias = "outputDirectory")
    private File workingDirectory;

    /**
     * The location to which to checkout the dist subversion repository under our working directory, which
     * was given above.
     */
    @Parameter(defaultValue = "${project.build.directory}/commons-release-plugin/scm", alias = "distCheckoutDirectory")
    private File distCheckoutDirectory;

    /**
     * The location of the RELEASE-NOTES.txt file such that multimodule builds can configure it.
     */
    @Parameter(defaultValue = "${basedir}/RELEASE-NOTES.txt", alias = "releaseNotesLocation")
    private File releaseNotesFile;

    /**
     * A boolean that determines whether or not we actually commit the files up to the subversion repository.
     * If this is set to <code>true</code>, we do all but make the commits. We do checkout the repository in question
     * though.
     */
    @Parameter(property = "commons.release.dryRun", defaultValue = "false")
    private Boolean dryRun;

    /**
     * The url of the subversion repository to which we wish the artifacts to be staged. Typicallly
     * this would need to be of the form:
     * <code>scm:svn:https://dist.apache.org/repos/dist/dev/commons/foo</code>. Note. that the prefix to the
     * substring <code>https</code> is a requirement.
     */
    @Parameter(required = true)
    private String distSvnStagingUrl;

    /**
     * The username for the distribution subversion repository. This is typically your apache id.
     */
    @Parameter(property = "user.name")
    private String username;

    /**
     * The password associated with {@link CommonsDistributionStagingMojo#username}.
     */
    @Parameter(property = "user.password")
    private String password;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Preparing to stage distributions");
        try {
            ScmManager scmManager = new BasicScmManager();
            scmManager.setScmProvider("svn", new SvnExeScmProvider());
            ScmRepository repository = scmManager.makeScmRepository(distSvnStagingUrl);
            ScmProvider provider = scmManager.getProviderByRepository(repository);
            SvnScmProviderRepository providerRepository = (SvnScmProviderRepository) repository.getProviderRepository();
            providerRepository.setUser(username);
            providerRepository.setPassword(password);
            if (!workingDirectory.exists()) {
                SharedFunctions.initDirectory(getLog(), workingDirectory);
            }
            if (!distCheckoutDirectory.exists()) {
                SharedFunctions.initDirectory(getLog(), distCheckoutDirectory);
            }
            ScmFileSet scmFileSet = new ScmFileSet(distCheckoutDirectory);
            getLog().info("Checking out dist from: " + distSvnStagingUrl);
            provider.checkOut(repository, scmFileSet);
            copyReleaseNotesToWorkingDirectory();
            List<File> filesToCommit = copyDistributionsIntoScmDirectoryStructure();
            if (!dryRun) {
                ScmFileSet scmFileSetToCommit = new ScmFileSet(distCheckoutDirectory, filesToCommit);
                AddScmResult addResult = provider.add(
                        repository,
                        scmFileSetToCommit,
                        "Staging release: " + project.getArtifactId() + ", version: " + project.getVersion()
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
                } else {
                    getLog().error("Adding dist files failed: " + addResult.getCommandOutput());
                    throw new MojoExecutionException("Adding dist files failed: " + addResult.getCommandOutput());
                }
            } else {
                getLog().info("Would have committed to: " + distSvnStagingUrl);
                getLog().info("Staging release: " + project.getArtifactId() + ", version: " + project.getVersion());
            }
        } catch (ScmException e) {
            getLog().error("Could not commit files to dist: " + distSvnStagingUrl, e);
            throw new MojoExecutionException("Could not commit files to dist: " + distSvnStagingUrl, e);
        }
    }

    /**
     * A utility method that takes the <code>RELEASE-NOTES.txt</code> file from the base directory of the
     * project and copies it into {@link CommonsDistributionStagingMojo#workingDirectory}.
     *
     * @throws MojoExecutionException if an {@link IOException} occurrs as a wrapper so that maven
     *                                can properly handle the exception.
     */
    private void copyReleaseNotesToWorkingDirectory() throws MojoExecutionException {
        StringBuffer copiedReleaseNotesAbsolutePath;
        getLog().info("Copying RELEASE-NOTES.txt to working directory.");
        copiedReleaseNotesAbsolutePath = new StringBuffer(workingDirectory.getAbsolutePath());
        copiedReleaseNotesAbsolutePath.append("/scm/");
        copiedReleaseNotesAbsolutePath.append(releaseNotesFile.getName());
        File copiedReleaseNotes = new File(copiedReleaseNotesAbsolutePath.toString());
        SharedFunctions.copyFile(getLog(), releaseNotesFile, copiedReleaseNotes);
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
     * @return a {@link List} of {@link File}'s in the directory for the purpose of adding them to the maven
     *         {@link ScmFileSet}.
     * @throws MojoExecutionException if an {@link IOException} occurrs so that Maven can handle it properly.
     */
    private List<File> copyDistributionsIntoScmDirectoryStructure() throws MojoExecutionException {
        List<File> workingDirectoryFiles = Arrays.asList(workingDirectory.listFiles());
        String scmBinariesRoot = buildDistBinariesRoot();
        String scmSourceRoot = buildDistSourceRoot();
        List<File> filesForMavenScmFileSet = new ArrayList<>();
        File copy;
        for (File file : workingDirectoryFiles) {
            if (file.getName().contains("src")) {
                copy = new File(scmSourceRoot + "/" + file.getName());
                SharedFunctions.copyFile(getLog(), file, copy);
                filesForMavenScmFileSet.add(copy);
            } else if (file.getName().contains("bin")) {
                copy = new File(scmBinariesRoot + "/" + file.getName());
                SharedFunctions.copyFile(getLog(), file, copy);
                filesForMavenScmFileSet.add(copy);
            } else if (file.getName().contains("scm")) {
                getLog().debug("Not copying scm directory over to the scm directory because it is the scm directory.");
                //do nothing because we are copying into scm
            } else {
                copy = new File(distCheckoutDirectory.getAbsolutePath() + "/" + file.getName());
                SharedFunctions.copyFile(getLog(), file, copy);
                filesForMavenScmFileSet.add(copy);
            }
        }
        filesForMavenScmFileSet.add(releaseNotesFile);
        return filesForMavenScmFileSet;
    }

    /**
     * Build the path for the distribution binaries directory.
     *
     * @return the local absolute path into the checkedout subversion repository that is where
     *         the binaries distributions are to be copied.
     */
    private String buildDistBinariesRoot() {
        StringBuffer buffer = new StringBuffer(distCheckoutDirectory.getAbsolutePath());
        buffer.append("/binaries");
        return buffer.toString();
    }

    /**
     * Build the path for the distribution source directory.
     *
     * @return the local absolute path into the checkedout subversion repository that is where
     *         the source distributions are to be copied.
     */
    private String buildDistSourceRoot() {
        StringBuffer buffer = new StringBuffer(distCheckoutDirectory.getAbsolutePath());
        buffer.append("/source");
        return buffer.toString();
    }

    /**
     * This method is the setter for the {@link CommonsDistributionStagingMojo#basedir} field, specifically
     * for the usage in the unit tests.
     *
     * @param basedir is the {@link File} to be used as the project's root directory when this mojo
     *                is invoked.
     */
    protected void setBasedir(File basedir) {
        this.basedir = basedir;
    }
}
