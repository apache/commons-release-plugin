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
package org.apache.commons.release.plugin.velocity;

import static junit.framework.TestCase.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import org.junit.Test;

/**
 * Unit tests for {@link ReadmeHtmlVelocityDelegate}.
 */
public final class ReadmeHtmlVelocityDelegateTest {

    @Test
    public void testSuccessfulRun() throws IOException {
        final ReadmeHtmlVelocityDelegate delegate = ReadmeHtmlVelocityDelegate.builder()
                .withArtifactId("commons-text")
                .withVersion("1.4")
                .withSiteUrl("https://commons.apache.org/text")
                .build();
        try (Writer writer = delegate.render(new StringWriter())) {
            final String filledOutTemplate = writer.toString();
            assertTrue(filledOutTemplate.contains("<h1>Commons-TEXT v1.4.</h1>"));
        }
    }

    @Test
    public void testSuccessfulRunBcel() throws IOException {
        final ReadmeHtmlVelocityDelegate delegate = ReadmeHtmlVelocityDelegate.builder()
                .withArtifactId("bcel")
                .withVersion("1.5")
                .withSiteUrl("https://commons.apache.org/text")
                .build();
        try (Writer writer = delegate.render(new StringWriter())) {
            final String filledOutTemplate = writer.toString();
            assertTrue(filledOutTemplate.contains("<h1>Commons-BCEL v1.5.</h1>"));
        }
    }

    @Test
    public void testSuccessfulRunLang3() throws IOException {
        final ReadmeHtmlVelocityDelegate delegate = ReadmeHtmlVelocityDelegate.builder()
                .withArtifactId("commons-lang3")
                .withVersion("3.8.1")
                .withSiteUrl("https://commons.apache.org/text")
                .build();
        try (Writer writer = delegate.render(new StringWriter())) {
            final String filledOutTemplate = writer.toString();
            assertTrue(filledOutTemplate.contains("<h1>Commons-LANG v3.8.1.</h1>"));
        }
    }
}
