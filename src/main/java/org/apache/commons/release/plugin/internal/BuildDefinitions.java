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
package org.apache.commons.release.plugin.internal;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.commons.release.plugin.slsa.v1_2.ResourceDescriptor;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;

/**
 * Factory methods for the SLSA {@code BuildDefinition} fields: JVM, Maven descriptors and external build parameters.
 */
public final class BuildDefinitions {

    /**
     * Reconstructs the Maven command line string from the given execution request.
     *
     * @param request the Maven execution request
     * @return a string representation of the Maven command line
     */
    static String commandLine(final MavenExecutionRequest request) {
        final List<String> args = new ArrayList<>(request.getGoals());
        final String profiles = String.join(",", request.getActiveProfiles());
        if (!profiles.isEmpty()) {
            args.add("-P" + profiles);
        }
        request.getUserProperties().forEach((key, value) -> args.add("-D" + key + "=" + value));
        return String.join(" ", args);
    }

    /**
     * Returns a map of external build parameters captured from the current JVM and Maven session.
     *
     * @param session the current Maven session
     * @return a map of parameter names to values
     */
    public static Map<String, Object> externalParameters(final MavenSession session) {
        final Map<String, Object> params = new HashMap<>();
        params.put("jvm.args", ManagementFactory.getRuntimeMXBean().getInputArguments());
        final MavenExecutionRequest request = session.getRequest();
        params.put("maven.goals", request.getGoals());
        params.put("maven.profiles", request.getActiveProfiles());
        params.put("maven.user.properties", request.getUserProperties());
        params.put("maven.cmdline", commandLine(request));
        final Map<String, Object> env = new HashMap<>();
        params.put("env", env);
        for (final Map.Entry<String, String> entry : System.getenv().entrySet()) {
            final String key = entry.getKey();
            if ("TZ".equals(key) || "LANG".equals(key) || key.startsWith("LC_")) {
                env.put(key, entry.getValue());
            }
        }
        return params;
    }

    /**
     * Creates a {@link ResourceDescriptor} for the JDK used during the build.
     *
     * @param javaHome path to the JDK home directory (value of the {@code java.home} system property)
     * @return a descriptor with digest and annotations populated from system properties
     * @throws IOException if hashing the JDK directory fails
     */
    public static ResourceDescriptor jvm(final Path javaHome) throws IOException {
        final String[] propertyNames = {
                "java.home", "java.specification.maintenance.version", "java.specification.name", "java.specification.vendor", "java.specification.version",
                "java.vendor", "java.vendor.url", "java.vendor.version", "java.version", "java.version.date", "java.vm.name", "java.vm.specification.name",
                "java.vm.specification.vendor", "java.vm.specification.version", "java.vm.vendor", "java.vm.version"
        };
        final Map<String, Object> annotations = new TreeMap<>();
        for (final String prop : propertyNames) {
            annotations.put(prop.substring("java.".length()), System.getProperty(prop));
        }
        return new ResourceDescriptor()
                .setName("JDK")
                .setDigest(Collections.singletonMap("gitTree", GitUtils.gitTree(javaHome)))
                .setAnnotations(annotations);
    }

    /**
     * Creates a {@link ResourceDescriptor} for the Maven installation used during the build.
     *
     * <p>{@code build.properties} resides in a JAR inside {@code ${maven.home}/lib/}, which is loaded by Maven's Core Classloader.
     * Plugin code runs in an isolated Plugin Classloader, which does see that resources. Therefore, we need to pass the classloader from a class from
     * Maven Core, such as {@link org.apache.maven.rtinfo.RuntimeInformation}.</p>
     *
     * @param version         Maven version string
     * @param mavenHome       path to the Maven home directory
     * @param coreClassLoader a classloader from Maven's Core Classloader realm, used to load core resources
     * @return a descriptor for the Maven installation
     * @throws IOException if hashing the Maven home directory fails
     */
    public static ResourceDescriptor maven(final String version, final Path mavenHome, final ClassLoader coreClassLoader) throws IOException {
        final ResourceDescriptor descriptor = new ResourceDescriptor()
                .setName("Maven")
                .setUri("pkg:maven/org.apache.maven/apache-maven@" + version)
                .setDigest(Collections.singletonMap("gitTree", GitUtils.gitTree(mavenHome)));
        final Properties buildProps = new Properties();
        try (InputStream in = coreClassLoader.getResourceAsStream("org/apache/maven/messages/build.properties")) {
            if (in != null) {
                buildProps.load(in);
            }
        }
        if (!buildProps.isEmpty()) {
            final Map<String, Object> annotations = new HashMap<>();
            buildProps.forEach((key, value) -> annotations.put((String) key, value));
            descriptor.setAnnotations(annotations);
        }
        return descriptor;
    }

    /**
     * No instances.
     */
    private BuildDefinitions() {
    }
}
