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
package org.apache.commons.release.plugin;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.scm.provider.ScmProviderRepository;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.codehaus.plexus.util.FileUtils;

/**
 * Shared static functions for all of our Mojos.
 *
 * @since 1.0
 */
public final class SharedFunctions {

    /**
     * The buffer size in bytes.
     */
    public static final int BUFFER_BYTE_SIZE = 1024;

    /**
     * Copies <code>fromFile</code> to <code>toFile</code> and logs I/O failures using the Maven {@link Log}.
     *
     * @param log The {@link Log}, the Maven logger.
     * @param fromFile The {@link File} from which to copy.
     * @param toFile The destination {@link File}.
     * @throws MojoExecutionException Thrown if an {@link IOException} occurs.
     */
    public static void copyFile(final Log log, final File fromFile, final File toFile) throws MojoExecutionException {
        final String format = "Unable to copy file %s to %s: %s";
        requireNonNull(fromFile, () -> String.format(format, fromFile, toFile));
        requireNonNull(toFile, () -> String.format(format, fromFile, toFile));
        try {
            FileUtils.copyFile(fromFile, toFile);
        } catch (final IOException e) {
            final String message = String.format(format, fromFile, toFile, e.getMessage());
            log.error(message);
            throw new MojoExecutionException(message, e);
        }
    }

    /**
     * Cleans and then initializes the directory specified by <code>workingDirectory</code>.
     *
     * @param log The Maven logger.
     * @param workingDirectory The directory to delete and then create.
     * @throws MojoExecutionException Thrown if an {@link IOException} occurs.
     */
    public static void initDirectory(final Log log, final File workingDirectory) throws MojoExecutionException {
        final String format = "Unable to remove directory %s: %s";
        requireNonNull(workingDirectory, () -> String.format(format, workingDirectory));
        if (workingDirectory.exists()) {
            try {
                FileUtils.deleteDirectory(workingDirectory);
            } catch (final IOException e) {
                final String message = String.format(format, workingDirectory, e.getMessage());
                log.error(message);
                throw new MojoExecutionException(message, e);
            }
        }
        if (!workingDirectory.exists()) {
            workingDirectory.mkdirs();
        }
    }

    /**
     * Checks that the specified object reference is not {@code null}.
     *
     * <p>
     * This method is designed primarily for parameter validation in methods and constructors, as demonstrated below.
     * </p>
     * <blockquote>
     *
     * <pre>
     * public Foo(Bar bar) {
     *     this.bar = SharedFunctions.requireNonNull(bar);
     * }
     * </pre>
     *
     * </blockquote>
     *
     * @param obj The object reference to check for nullity.
     * @param <T> The type of the reference.
     * @return {@code obj} if not {@code null}.
     * @throws MojoExecutionException Thrown if {@code obj} is {@code null}.
     */
    public static <T> T requireNonNull(final T obj) throws MojoExecutionException {
        if (obj == null) {
            throw new MojoExecutionException(new NullPointerException());
        }
        return obj;
    }

    /**
     * Checks that the specified object reference is not {@code null} and throws a customized {@link MojoExecutionException} if it is.
     *
     * <p>
     * This method is designed primarily for parameter validation in methods and constructors with multiple parameters, as demonstrated below.
     * </p>
     * <blockquote>
     *
     * <pre>
     * public Foo(Bar bar, Baz baz) {
     *     this.bar = SharedFunctions.requireNonNull(bar, "bar must not be null");
     *     this.baz = SharedFunctions.requireNonNull(baz, "baz must not be null");
     * }
     * </pre>
     *
     * </blockquote>
     *
     * @param obj The object reference to check for nullity.
     * @param message The detail message for the {@code NullPointerException} wrapped in the thrown {@code MojoExecutionException}.
     * @param <T> The type of the reference.
     * @return {@code obj} if not {@code null}.
     * @throws MojoExecutionException Thrown if {@code obj} is {@code null}.
     */
    public static <T> T requireNonNull(final T obj, final String message) throws MojoExecutionException {
        if (obj == null) {
            throw new MojoExecutionException(new NullPointerException(message));
        }
        return obj;
    }

    /**
     * Checks that the specified object reference is not {@code null} and throws a customized {@link MojoExecutionException} if it is.
     *
     * <p>
     * Unlike {@link #requireNonNull(Object, String)}, this method defers message creation until after the null check. This may improve performance when the
     * object is not null, provided that creating the message supplier costs less than creating the message directly.
     * </p>
     *
     * @param obj The object reference to check for nullity.
     * @param messageSupplier The supplier of the detail message for the {@code NullPointerException} wrapped in the thrown {@code MojoExecutionException}.
     * @param <T> The type of the reference.
     * @return {@code obj} if not {@code null}.
     * @throws MojoExecutionException Thrown if {@code obj} is {@code null}.
     */
    public static <T> T requireNonNull(final T obj, final Supplier<String> messageSupplier) throws MojoExecutionException {
        if (obj == null) {
            throw new MojoExecutionException(new NullPointerException(messageSupplier.get()));
        }
        return obj;
    }

    /**
     * Sets authentication information on the specified {@link ScmProviderRepository}.
     *
     * @param providerRepository The repository to configure.
     * @param distServer The server ID used to look up credentials in Maven settings.
     * @param settings The Maven settings.
     * @param settingsDecrypter The decrypter for stored credentials.
     * @param username The fallback username when no server username is available.
     * @param password The fallback password when no server password is available.
     */
    public static void setAuthentication(final ScmProviderRepository providerRepository,
                                   final String distServer,
                                   final Settings settings,
                                   final SettingsDecrypter settingsDecrypter,
                                   final String username,
                                   final String password) {
        final Optional<Server> server =
                Optional.ofNullable(distServer).map(settings::getServer).map(DefaultSettingsDecryptionRequest::new)
                        .map(settingsDecrypter::decrypt).map(SettingsDecryptionResult::getServer);

        providerRepository.setUser(server.map(Server::getUsername).orElse(username));
        providerRepository.setPassword(server.map(Server::getPassword).orElse(password));
    }

    /**
     * Prevents instantiation of this utility class.
     */
    private SharedFunctions() {
        // Utility class.
    }
}
