package org.apache.commons.release.plugin.mojos;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

@Mojo( name = "stage-distributions", defaultPhase = LifecyclePhase.DEPLOY, threadSafe = true)
public class CommonsDistributionStagingMojo extends AbstractMojo {

    @Parameter( defaultValue = "${project.build.directory}/commons-release-plugin", alias = "outputDirectory" )
    private File workingDirectory;

    @Parameter ( required = true )
    private String distScmStagingUrl;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

    }
}
