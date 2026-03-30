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
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.release.plugin.slsa.v1_2.ResourceDescriptor;

/**
 * Factory methods for {@link ResourceDescriptor} instances representing build-tool dependencies.
 */
public final class BuildToolDescriptors {

    /** No instances. */
    private BuildToolDescriptors() {
        // no instantiation
    }

    /**
     * Creates a {@link ResourceDescriptor} for the JDK used during the build.
     *
     * @param javaHome path to the JDK home directory (value of the {@code java.home} system property)
     * @return a descriptor with digest and annotations populated from system properties
     * @throws IOException if hashing the JDK directory fails
     */
    public static ResourceDescriptor jvm(Path javaHome) throws IOException {
        ResourceDescriptor descriptor = new ResourceDescriptor();
        descriptor.setName("JDK");
        Map<String, String> digest = new HashMap<>();
        digest.put("gitTree", GitUtils.gitTree(javaHome));
        descriptor.setDigest(digest);
        String[] propertyNames = {"java.version", "java.vendor", "java.vendor.version", "java.vm.name", "java.vm.version", "java.vm.vendor",
                "java.runtime.name", "java.runtime.version", "java.specification.version"};
        Map<String, Object> annotations = new HashMap<>();
        for (String prop : propertyNames) {
            annotations.put(prop.substring("java.".length()), System.getProperty(prop));
        }
        descriptor.setAnnotations(annotations);
        return descriptor;
    }

    /**
     * Creates a {@link ResourceDescriptor} for the Maven installation used during the build.
     *
     * @param version   Maven version string
     * @param mavenHome path to the Maven home directory
     * @return a descriptor for the Maven installation
     * @throws IOException if hashing the Maven home directory fails
     */
    public static ResourceDescriptor maven(String version, Path mavenHome) throws IOException {
        ResourceDescriptor descriptor = new ResourceDescriptor();
        descriptor.setName("Maven");
        descriptor.setUri("pkg:maven/org.apache.maven/apache-maven@" + version);
        Map<String, String> digest = new HashMap<>();
        digest.put("gitTree", GitUtils.gitTree(mavenHome));
        descriptor.setDigest(digest);
        Properties buildProps = new Properties();
        try (InputStream in = BuildToolDescriptors.class.getResourceAsStream("/org/apache/maven/messages/build.properties")) {
            if (in != null) {
                buildProps.load(in);
            }
        }
        if (!buildProps.isEmpty()) {
            Map<String, Object> annotations = new HashMap<>();
            buildProps.forEach((key, value) -> annotations.put((String) key, value));
            descriptor.setAnnotations(annotations);
        }
        return descriptor;
    }
}
