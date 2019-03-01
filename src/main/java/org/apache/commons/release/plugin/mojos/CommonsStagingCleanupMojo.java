package org.apache.commons.release.plugin.mojos;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.SettingsDecrypter;

import java.io.File;

/**
 * This class checks out the dev distribution location, checkes whether anything exists in the
 * distribution location, and if it is non-empty it deletes all of the resources there.
 *
 * @author chtompki
 * @since 1.6
 */
@Mojo(name = "clean-staging",
        defaultPhase = LifecyclePhase.COMPILE,
        threadSafe = true,
        aggregator = true)
public class CommonsStagingCleanupMojo extends AbstractMojo {

    /**
     * The main working directory for the plugin, namely <code>target/commons-release-plugin</code>, but
     * that assumes that we're using the default maven <code>${project.build.directory}</code>.
     */
    @Parameter(defaultValue = "${project.build.directory}/commons-release-plugin", property = "commons.outputDirectory")
    private File workingDirectory;

    /**
     * The location to which to checkout the dist subversion repository under our working directory, which
     * was given above. We then do an SVN delete on all of the directories in this repository.
     */
    @Parameter(defaultValue = "${project.build.directory}/commons-release-plugin/scm-cleanup",
            property = "commons.distCleanupDirectory")
    private File distCleanupDirectory;

    /**
     * A boolean that determines whether or not we actually commit the files up to the subversion repository.
     * If this is set to <code>true</code>, we do all but make the commits. We do checkout the repository in question
     * though.
     */
    @Parameter(property = "commons.release.dryRun", defaultValue = "false")
    private Boolean dryRun;

    /**
     * The url of the subversion repository to which we wish the artifacts to be staged. Typically this would need to
     * be of the form: <code>scm:svn:https://dist.apache.org/repos/dist/dev/commons/foo/version-RC#</code>. Note. that
     * the prefix to the substring <code>https</code> is a requirement.
     */
    @Parameter(defaultValue = "", property = "commons.distSvnStagingUrl")
    private String distSvnStagingUrl;

    /**
     * A parameter to generally avoid running unless it is specifically turned on by the consuming module.
     */
    @Parameter(defaultValue = "false", property = "commons.release.isDistModule")
    private Boolean isDistModule;/**
     * The ID of the server (specified in settings.xml) which should be used for dist authentication.
     * This will be used in preference to {@link #username}/{@link #password}.
     */
    @Parameter(property = "commons.distServer")
    private String distServer;

    /**
     * The username for the distribution subversion repository. This is typically your Apache id.
     */
    @Parameter(property = "user.name")
    private String username;

    /**
     * The password associated with {@link CommonsDistributionStagingMojo#username}.
     */
    @Parameter(property = "user.password")
    private String password;

    /**
     * Maven {@link Settings}.
     */
    @Parameter(defaultValue = "${settings}", readonly = true, required = true)
    private Settings settings;

    /**
     * Maven {@link SettingsDecrypter} component.
     */
    @Component
    private SettingsDecrypter settingsDecrypter;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!isDistModule) {
            getLog().info("This module is marked as a non distribution "
                    + "or assembly module, and the plugin will not run.");
            return;
        }
        if (StringUtils.isEmpty(distSvnStagingUrl)) {
            getLog().warn("commons.distSvnStagingUrl is not set, the commons-release-plugin will not run.");
            return;
        }
        if (!workingDirectory.exists()) {
            getLog().info("Current project contains no distributions. Not executing.");
            return;
        }

    }
}
