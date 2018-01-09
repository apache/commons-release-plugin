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
import org.junit.Rule;
import org.junit.Test;

import java.io.File;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotNull;

/**
 * Unit tests for {@link CommonsDistributionStagingMojo}.
 *
 * @author chtompki
 * @since 1.0.
 */
public class CommonsDistributionStagingMojoTest {

    @Rule
    public MojoRule rule = new MojoRule() {
        @Override
        protected void before() throws Throwable {
        }

        @Override
        protected void after() {
        }
    };

    private CommonsDistributionDetachmentMojo detatchmentMojo;

    private CommonsDistributionStagingMojo mojoForTest;

    @Test
    public void testSuccess() throws Exception {
        File testPom = new File("src/test/resources/mojos/stage-distributions/stage-distributions.xml");
        assertNotNull(testPom);
        assertTrue(testPom.exists());
        File detachmentPom = new File("src/test/resources/mojos/detach-distributions/detach-distributions.xml");
        assertNotNull(detachmentPom);
        assertTrue(detachmentPom.exists());
        mojoForTest = (CommonsDistributionStagingMojo) rule.lookupMojo("stage-distributions", testPom);
        detatchmentMojo = (CommonsDistributionDetachmentMojo) rule.lookupMojo("detach-distributions", detachmentPom);
        detatchmentMojo.execute();
        File releaseNotesBasedir = new File("src/test/resources/mojos/stage-distributions/");
        mojoForTest.setBasedir(releaseNotesBasedir);
        mojoForTest.execute();
        File targetScmDirectory = new File("target/commons-release-plugin/scm");
        assertTrue(targetScmDirectory.exists());
    }
}
