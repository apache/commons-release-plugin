package org.apache.commons.release.plugin.mojos;

import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

public class CommonsDistributionStagingMojo {

    @Parameter( defaultValue = "${project.build.directory}/commons-release-plugin", alias = "outputDirectory" )
    private File workingDirectory;

    @Parameter ( required = true )
    private String distScmStagingUrl;
}
