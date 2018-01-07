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

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotNull;

/**
 * Unit tests for {@link CommonsSiteCompressionMojo}.
 *
 * @author chtompki
 * @since 1.0
 */
public class CommonsSiteCompressionMojoTest {

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

    @Test
    public void testCompressSite() throws Exception {
        File testFile = new File("src/test/resources/mojos/compress-site/compress-site.xml");
        assertNotNull(testFile);
        assertTrue(testFile.exists());
        mojo = (CommonsSiteCompressionMojo) rule.lookupMojo("compress-site", testFile);
        mojo.execute();
        File siteZip = new File("target/commons-release-plugin/site.zip");
        assertTrue(siteZip.exists());
    }
}
