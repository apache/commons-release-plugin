package org.apache.commons.release.plugin.mojos;

import org.apache.commons.compress.archivers.zip.ParallelScatterZipCreator;
import org.apache.commons.compress.archivers.zip.ScatterZipOutputStream;
import org.apache.commons.release.plugin.SharedFunctions;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Takes the built <code>./target/site</code> directory and compresses it to
 * <code>./target/commons-release-plugin/site.zip</code>.
 *
 * @author chtompki
 * @since 1.0
 */
@Mojo(name = "compress-site", defaultPhase = LifecyclePhase.POST_SITE, threadSafe = true)
public class CommonsSiteCompressionMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project.build.directory}/commons-release-plugin", alias = "outputDirectory")
    private File workingDirectory;

    @Parameter(defaultValue = "${project.build.directory}/site", alias = "siteOutputDirectory")
    private File siteDirectory;

    private ScatterZipOutputStream dirs;

    private ParallelScatterZipCreator scatterZipCreator;

    private List<File> filesToCompress;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!workingDirectory.exists()) {
            SharedFunctions.initWorkingDirectory(getLog(), workingDirectory);
        }
        try {
            filesToCompress = new ArrayList<>();
            getAllSiteFiles(siteDirectory, filesToCompress);
            writeZipFile(workingDirectory, siteDirectory, filesToCompress);
        } catch (IOException e) {
            getLog().error("Failed to create ./target/commons-release-plugin/site.zip: " + e.getMessage(), e);
            throw new MojoExecutionException("Failed to create ./target/commons-release-plugin/site.zip: " + e.getMessage(), e);
        }
    }

    private void getAllSiteFiles(File siteDirectory, List<File> filesToCompress) throws IOException {
        File[] files = siteDirectory.listFiles();
        for (File file : files) {
            filesToCompress.add(file);
            if (file.isDirectory()) {
                getAllSiteFiles(file, filesToCompress);
            }
        }
    }

    private void writeZipFile(File workingDirectory, File directoryToZip, List<File> fileList) throws IOException {
        FileOutputStream fos = new FileOutputStream(workingDirectory.getAbsolutePath() + "/site.zip");
        ZipOutputStream zos = new ZipOutputStream(fos);
        for (File file : fileList) {
            if (!file.isDirectory()) { // we only zip files, not directories
                addToZip(directoryToZip, file, zos);
            }
        }
        zos.close();
        fos.close();
    }

    private void addToZip(File directoryToZip, File file, ZipOutputStream zos) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        // we want the zipEntry's path to be a relative path that is relative
        // to the directory being zipped, so chop off the rest of the path
        String zipFilePath = file.getCanonicalPath().substring(directoryToZip.getCanonicalPath().length() + 1,
                file.getCanonicalPath().length());
        ZipEntry zipEntry = new ZipEntry(zipFilePath);
        zos.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zos.write(bytes, 0, length);
        }
        zos.closeEntry();
        fis.close();
    }
}
