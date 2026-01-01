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
package org.apache.commons.release.plugin.mojos;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;

import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CommonsSiteCompressionMojo}.
 */
@SuppressWarnings("deprecation") // testing a deprecated class
@MojoTest
public class CommonsSiteCompressionMojoTest {

    private static final String COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH = "target/testing-commons-release-plugin";

    @BeforeEach
    public void setUp() throws Exception {
        final File testingDirectory = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH);
        if (testingDirectory.exists()) {
            FileUtils.deleteDirectory(testingDirectory);
        }
    }

    @Test
    @InjectMojo(goal = "compress-site", pom = "src/test/resources/mojos/compress-site/compress-site-failure.xml")
    public void testCompressSiteDirNonExistentFailure(final CommonsSiteCompressionMojo mojo) throws Exception {
        try {
            mojo.execute();
        } catch (final MojoFailureException e) {
            assertEquals(
                    "\"mvn site\" was not run before this goal, or a siteDirectory did not exist.", e.getMessage()
            );
        }
    }

    @Test
    @InjectMojo(goal = "compress-site", pom = "src/test/resources/mojos/compress-site/compress-site.xml")
    public void testCompressSiteSuccess(final CommonsSiteCompressionMojo mojo) throws Exception {
        final File testingDirectory = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH);
        testingDirectory.mkdir();
        mojo.execute();
        final File siteZip = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/site.zip");
        assertTrue(siteZip.exists());
    }

    @Test
    @InjectMojo(goal = "compress-site", pom = "src/test/resources/mojos/compress-site/compress-site-disabled.xml")
    public void testDisabled(final CommonsSiteCompressionMojo mojo) throws Exception {
        mojo.execute();
        final File testingDirectory = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH);
        assertFalse(testingDirectory.exists());
    }
}
