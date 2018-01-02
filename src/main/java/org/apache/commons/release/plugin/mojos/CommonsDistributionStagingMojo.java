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

import org.apache.commons.release.plugin.SharedFunctions;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.scm.manager.BasicScmManager;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.provider.svn.svnexe.SvnExeScmProvider;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.apache.maven.settings.Settings;

import java.io.File;

@Mojo( name = "stage-distributions", defaultPhase = LifecyclePhase.DEPLOY, threadSafe = true)
public class CommonsDistributionStagingMojo extends AbstractMojo {

    /**
     */
    @Parameter( defaultValue = "${settings}", readonly = true, required = true )
    private Settings settings;

    /**
     */
    @Parameter( defaultValue = "${project.build.directory}/commons-release-plugin", alias = "outputDirectory" )
    private File workingDirectory;

    /**
     */
    @Parameter( defaultValue = "${project.build.directory}/commons-release-plugin/scm", alias = "outputDirectory" )
    private File distCheckoutDirectory;

    /**
     */
    @Parameter ( required = true )
    private String distSvnStagingUrl;

    /**
     * The SCM username to use.
     */
    @Parameter( property = "username" )
    private String username;

    /**
     * The SCM password to use.
     */
    @Parameter( property = "password" )
    private String password;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            ScmManager scmManager = new BasicScmManager();
            scmManager.setScmProvider("svn", new SvnExeScmProvider());
            ScmRepository repository = scmManager.makeScmRepository(distSvnStagingUrl);
            if (!workingDirectory.exists()) {
                SharedFunctions.initDirectory(getLog(), workingDirectory);
            }
            if (!distCheckoutDirectory.exists()) {
                SharedFunctions.initDirectory(getLog(), distCheckoutDirectory);
            }
        } catch (ScmRepositoryException e) {
            getLog().error("Failed getting scm repository: " + distSvnStagingUrl, e);
            throw new MojoExecutionException("Failed getting scm repository: " + distSvnStagingUrl, e);
        } catch (NoSuchScmProviderException e) {
            getLog().error("No Scm Provider For: " + distSvnStagingUrl, e);
            throw new MojoExecutionException("No Scm Provider For: " + distSvnStagingUrl, e);
        }
    }
}
