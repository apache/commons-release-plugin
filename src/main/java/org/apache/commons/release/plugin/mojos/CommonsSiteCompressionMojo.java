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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.archivers.zip.ParallelScatterZipCreator;
import org.apache.commons.compress.archivers.zip.ScatterZipOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Takes the built <code>./target/site</code> directory and compresses it to
 * <code>./target/commons-release-plugin/site.zip</code>.
 *
 * @author chtompki
 * @since 1.0
 * @deprecated - as we no longer wish to compress the site, we are going to put this functionality in the
 *               {@link CommonsDistributionStagingMojo}.
 */
@Deprecated
@Mojo(name = "compress-site",
        defaultPhase = LifecyclePhase.POST_SITE,
        threadSafe = true,
        aggregator = true)
public class CommonsSiteCompressionMojo extends AbstractMojo {

    /**
     * The working directory for the plugin which, assuming the maven uses the default
     * <code>${project.build.directory}</code>, this becomes <code>target/commons-release-plugin</code>.
     */
    @Parameter(defaultValue = "${project.build.directory}/commons-release-plugin",
            property = "commons.outputDirectory")
    private File workingDirectory;

    /**
     */
    @Parameter(defaultValue = "${project.build.directory}/site", property = "commons.siteOutputDirectory")
    private File siteDirectory;

    /**
     * The url of the subversion repository to which we wish the artifacts to be staged. Typicallly
     * this would need to be of the form:
     * <code>scm:svn:https://dist.apache.org/repos/dist/dev/commons/foo</code>. Note. that the prefix to the
     * substring <code>https</code> is a requirement.
     */
    @Parameter(defaultValue = "", property = "commons.distSvnStagingUrl")
    private String distSvnStagingUrl;

    /**
     * A parameter to generally avoid running unless it is specifically turned on by the consuming module.
     */
    @Parameter(defaultValue = "false", property = "commons.release.isDistModule")
    private Boolean isDistModule;

    /**
     * A variable for the process of creating the site.zip file.
     */
    private ScatterZipOutputStream dirs;

    /**
     * A second variable for the process of creating the site.zip file.
     */
    private ParallelScatterZipCreator scatterZipCreator;

    /**
     * The list of files to compress into the site.zip file.
     */
    private List<File> filesToCompress;

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
        if (!siteDirectory.exists()) {
            getLog().error("\"mvn site\" was not run before this goal, or a siteDirectory did not exist.");
            throw new MojoFailureException(
                    "\"mvn site\" was not run before this goal, or a siteDirectory did not exist."
            );
        }
        if (!workingDirectory.exists()) {
            getLog().info("Current project contains no distributions. Not executing.");
            return;
        }
        try {
            filesToCompress = new ArrayList<>();
            getAllSiteFiles(siteDirectory, filesToCompress);
            writeZipFile(workingDirectory, siteDirectory, filesToCompress);
        } catch (IOException e) {
            getLog().error("Failed to create ./target/commons-release-plugin/site.zip: " + e.getMessage(), e);
            throw new MojoExecutionException(
                    "Failed to create ./target/commons-release-plugin/site.zip: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * By default this method iterates across the <code>target/site</code> directory and adds all of the files
     * to the {@link CommonsSiteCompressionMojo#filesToCompress} {@link List}.
     *
     * @param siteDirectory the {@link File} that represents the <code>target/site</code> directory.
     * @param filesToCompress the {@link List} to which to add all the files.
     */
    private void getAllSiteFiles(File siteDirectory, List<File> filesToCompress) {
        File[] files = siteDirectory.listFiles();
        for (File file : files) {
            filesToCompress.add(file);
            if (file.isDirectory()) {
                getAllSiteFiles(file, filesToCompress);
            }
        }
    }

    /**
     * A helper method for writing all of the files in our <code>fileList</code> to a <code>site.zip</code> file
     * in the <code>workingDirectory</code>.
     *
     * @param outputDirectory is a {@link File} representing the place to put the site.zip file.
     * @param directoryToZip is a {@link File} representing the directory of the site (normally
     *                       <code>target/site</code>).
     * @param fileList the list of files to be zipped up, generally generated by
     *                 {@link CommonsSiteCompressionMojo#getAllSiteFiles(File, List)}.
     * @throws IOException when the copying of the files goes incorrectly.
     */
    private void writeZipFile(File outputDirectory, File directoryToZip, List<File> fileList) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(outputDirectory.getAbsolutePath() + "/site.zip");
                ZipOutputStream zos = new ZipOutputStream(fos)) {
            for (File file : fileList) {
                if (!file.isDirectory()) { // we only zip files, not directories
                    addToZip(directoryToZip, file, zos);
                }
            }
        }
    }

    /**
     * Given the <code>directoryToZip</code> we add the <code>file</code> to the zip archive represented by
     * <code>zos</code>.
     *
     * @param directoryToZip a {@link File} representing the directory from which the file exists that we are
     *                       compressing. Generally this is <code>target/site</code>.
     * @param file a {@link File} to add to the {@link ZipOutputStream} <code>zos</code>.
     * @param zos the {@link ZipOutputStream} to which to add our <code>file</code>.
     * @throws IOException if adding the <code>file</code> doesn't work out properly.
     */
    private void addToZip(File directoryToZip, File file, ZipOutputStream zos) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            // we want the zipEntry's path to be a relative path that is relative
            // to the directory being zipped, so chop off the rest of the path
            String zipFilePath = file.getCanonicalPath().substring(directoryToZip.getCanonicalPath().length() + 1,
                    file.getCanonicalPath().length());
            ZipEntry zipEntry = new ZipEntry(zipFilePath);
            zos.putNextEntry(zipEntry);
            IOUtils.copy(fis, zos);
        }
    }
}
