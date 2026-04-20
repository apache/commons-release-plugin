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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class BuildDefinitionsTest {

    static Stream<Arguments> commandLineArguments() {
        return Stream.of(
                Arguments.of("empty", emptyList(), emptyList(), new Properties(), ""),
                Arguments.of("single goal", singletonList("verify"), emptyList(), new Properties(), "verify"),
                Arguments.of("multiple goals", asList("clean", "verify"), emptyList(), new Properties(), "clean verify"),
                Arguments.of("single profile", singletonList("verify"), singletonList("release"), new Properties(), "verify -Prelease"),
                Arguments.of("multiple profiles", singletonList("verify"), asList("release", "sign"), new Properties(), "verify -Prelease,sign"),
                Arguments.of("user property", singletonList("verify"), emptyList(), singletonProperties("foo", "bar"), "verify -Dfoo=bar"),
                Arguments.of("goals, profile and property", singletonList("verify"), singletonList("release"), singletonProperties("foo", "bar"),
                        "verify -Prelease -Dfoo=bar")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("commandLineArguments")
    void commandLineTest(final String description, final List<String> goals, final List<String> profiles,
            final Properties userProperties, final String expected) {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setGoals(goals);
        request.setActiveProfiles(profiles);
        request.setUserProperties(userProperties);
        assertEquals(expected, BuildDefinitions.commandLine(request));
    }

    private static Properties singletonProperties(final String key, final String value) {
        Properties p = new Properties();
        p.setProperty(key, value);
        return p;
    }
}
