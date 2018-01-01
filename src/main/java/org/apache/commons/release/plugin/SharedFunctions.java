package org.apache.commons.release.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Shared static functions for all of our Mojos
 *
 * @author chtompki
 * @since 1.0
 */
public class SharedFunctions {

    private SharedFunctions() {
        //Uitility Class
    }

    /**
     * Cleans and then initializes an empty directory that is given by the <code>workingDirectory</code>
     * parameter.
     *
     * @param log is the maven log for output logging, particularly in regards to error management.
     * @param workingDirectory is a {@link File} that represents the directory to first attempt to delete then create.
     */
    public static void initWorkingDirectory(Log log, File workingDirectory) throws MojoExecutionException {
        if (workingDirectory.exists()) {
            try {
                FileUtils.deleteDirectory(workingDirectory);
            } catch (IOException e) {
                log.error(e.getMessage());
                throw new MojoExecutionException("Unable to remove working directory: " + e.getMessage(), e);
            }
        }
        if (!workingDirectory.exists()) {
            workingDirectory.mkdirs();
        }
    }
}
