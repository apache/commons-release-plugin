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
package org.apache.commons.release.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Shared static functions for all of our Mojos.
 *
 * @author chtompki
 * @since 1.0
 */
public final class SharedFunctions {

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
    public static void initDirectory(Log log, File workingDirectory) throws MojoExecutionException {
        if (workingDirectory.exists()) {
            try {
                FileUtils.deleteDirectory(workingDirectory);
            } catch (IOException e) {
                log.error(e.getMessage());
                throw new MojoExecutionException("Unable to remove directory: " + e.getMessage(), e);
            }
        }
        if (!workingDirectory.exists()) {
            workingDirectory.mkdirs();
        }
    }

    /**
     * Copies a {@link File} from the <code>fromfile</code> to the <code>tofile</code> and logs the failure
     * using the maven {@link Log}.
     *
     * @param log the {@link Log}, the maven logger.
     * @param fromFile the {@link File} from which to copy.
     * @param toFile the {@link File} to which to copy into.
     * @throws MojoExecutionException if an {@link IOException} occurs.
     */
    public static void copyFile(Log log, File fromFile,  File toFile) throws MojoExecutionException{
        FileInputStream in;
        FileOutputStream out;
        try {
            in = new FileInputStream(fromFile);
            out = new FileOutputStream(toFile);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new MojoExecutionException("Unable to copy file: " + e.getMessage(), e);
        }
    }
}
