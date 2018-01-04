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
import org.apache.maven.scm.provider.ScmProviderRepository;
import org.apache.maven.scm.provider.svn.repository.SvnScmProviderRepository;
import org.apache.maven.scm.provider.svn.svnexe.SvnExeScmProvider;
import org.apache.maven.scm.repository.ScmRepository;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class checks out the dev distribution location, copies the distributions into that directory
 * structure under the <code>target</code> directory. Then commits the distributions back up to SVN.
 * Also, we include the built and zipped site as well as the RELEASE-NOTES.txt.
 *
 * @author chtompki
 * @since 1.0
 */
@Mojo(name = "stage-distributions", defaultPhase = LifecyclePhase.DEPLOY, threadSafe = true)
public class CommonsDistributionStagingMojo extends AbstractMojo {

    /**
     */
    @Parameter(defaultValue = "${project}", required = true )
    private MavenProject project;

    /**
     */
    @Parameter(defaultValue = "${basedir}")
    private File basedir;

    /**
     */
    @Parameter(defaultValue = "${project.build.directory}/commons-release-plugin", alias = "outputDirectory")
    private File workingDirectory;

    /**
     */
    @Parameter(defaultValue = "${project.build.directory}/commons-release-plugin/scm", alias = "outputDirectory")
    private File distCheckoutDirectory;

    /**
     */
    @Parameter(defaultValue = "false")
    private Boolean dryRun;

    /**
     */
    @Parameter(required = true)
    private String distSvnStagingUrl;

    /**
     */
    @Parameter(property = "user.name")
    private String username;

    /**
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
            ScmFileSet scmFileSetToCommit = new ScmFileSet(distCheckoutDirectory, filesToCommit);
            if (!dryRun) {
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
                        throw new MojoExecutionException("Committing dist files failed: " + checkInResult.getCommandOutput());
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

    private void copyReleaseNotesToWorkingDirectory() throws MojoExecutionException {
        StringBuffer copiedReleaseNotesAbsolutePath;
        getLog().info("Copying RELEASE-NOTES.txt to working directory.");
        File releaseNotes = new File(basedir + "/RELEASE-NOTES.txt");
        copiedReleaseNotesAbsolutePath = new StringBuffer(workingDirectory.getAbsolutePath());
        copiedReleaseNotesAbsolutePath.append("/scm/");
        copiedReleaseNotesAbsolutePath.append(releaseNotes.getName());
        File copiedReleaseNotes = new File(copiedReleaseNotesAbsolutePath.toString());
        getLog().info("Copying: " + releaseNotes.getName());
        SharedFunctions.copyFile(getLog(), releaseNotes, copiedReleaseNotes);
    }

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
            } else if (file.getName().contains("scm")){
                //do nothing because we are copying into scm
            } else {
                copy = new File(distCheckoutDirectory.getAbsolutePath() + "/" + file.getName());
                SharedFunctions.copyFile(getLog(), file, copy);
                filesForMavenScmFileSet.add(copy);
            }
        }
        filesForMavenScmFileSet.add(new File(distCheckoutDirectory + "/RELEASE-NOTES.txt"));
        return filesForMavenScmFileSet;
    }

    private String buildDistBinariesRoot() {
        StringBuffer buffer = new StringBuffer(distCheckoutDirectory.getAbsolutePath());
        buffer.append("/binaries");
        return buffer.toString();
    }

    private String buildDistSourceRoot() {
        StringBuffer buffer = new StringBuffer(distCheckoutDirectory.getAbsolutePath());
        buffer.append("/source");
        return buffer.toString();
    }
}
