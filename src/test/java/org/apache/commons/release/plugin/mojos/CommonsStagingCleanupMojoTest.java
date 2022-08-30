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

import org.apache.maven.plugin.testing.MojoRule;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotNull;

/**
 * Unit tests for {@link CommonsSiteCompressionMojo}.
 *
 * @since 1.0
 */
public class CommonsStagingCleanupMojoTest {

    private static final String COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH = "target/testing-commons-release-plugin";

    @Rule
    public final MojoRule rule = new MojoRule() {
        @Override
        protected void before() throws Throwable {
            // noop
        }

        @Override
        protected void after() {
            // noop
        }
    };

    protected CommonsStagingCleanupMojo mojo;

    @Before
    public void setUp() throws Exception {
        final File testingDirectory = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH);
        if (testingDirectory.exists()) {
            FileUtils.deleteDirectory(testingDirectory);
        }
    }

    @Test
    public void testCompressSiteSuccess() throws Exception {
        final File testingDirectory = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH);
        testingDirectory.mkdir();
        final File testPom = new File("src/test/resources/mojos/staging-cleanup/staging-cleanup.xml");
        assertNotNull(testPom);
        assertTrue(testPom.exists());
        mojo = (CommonsStagingCleanupMojo) rule.lookupMojo("clean-staging", testPom);
        mojo.execute();
        final File cleanupDir = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm-cleanup");
        assertTrue(cleanupDir.exists());
    }
}
