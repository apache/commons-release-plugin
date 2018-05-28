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

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.testing.MojoRule;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * Unit tests for {@link CommonsSiteCompressionMojo}.
 *
 * @author chtompki
 * @since 1.0
 */
public class CommonsSiteCompressionMojoTest {

    private static final String COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH = "target/testing-commons-release-plugin";

    @Rule
    public MojoRule rule = new MojoRule() {
        @Override
        protected void before() throws Throwable {
        }

        @Override
        protected void after() {
        }
    };

    protected CommonsSiteCompressionMojo mojo;

    @Before
    public void setUp() throws Exception {
        File testingDirectory = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH);
        if (testingDirectory.exists()) {
            FileUtils.deleteDirectory(testingDirectory);
        }
    }

    @Test
    public void testCompressSiteSuccess() throws Exception {
        File testingDirectory = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH);
        testingDirectory.mkdir();
        File testPom = new File("src/test/resources/mojos/compress-site/compress-site.xml");
        assertNotNull(testPom);
        assertTrue(testPom.exists());
        mojo = (CommonsSiteCompressionMojo) rule.lookupMojo("compress-site", testPom);
        mojo.execute();
        File siteZip = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/site.zip");
        assertTrue(siteZip.exists());
    }

    @Test
    public void testCompressSiteDirNonExistentFailure() throws Exception {
        File testPom = new File("src/test/resources/mojos/compress-site/compress-site-failure.xml");
        assertNotNull(testPom);
        assertTrue(testPom.exists());
        mojo = (CommonsSiteCompressionMojo) rule.lookupMojo("compress-site", testPom);
        try {
            mojo.execute();
        } catch (MojoFailureException e) {
            assertEquals(
                    "\"mvn site\" was not run before this goal, or a siteDirectory did not exist.", e.getMessage()
            );
        }
    }

    @Test
    public void testDisabled() throws Exception {
        File testPom = new File("src/test/resources/mojos/compress-site/compress-site-disabled.xml");
        assertNotNull(testPom);
        assertTrue(testPom.exists());
        mojo = (CommonsSiteCompressionMojo) rule.lookupMojo("compress-site", testPom);
        mojo.execute();
        File testingDirectory = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH);
        assertFalse(testingDirectory.exists());
    }
}
