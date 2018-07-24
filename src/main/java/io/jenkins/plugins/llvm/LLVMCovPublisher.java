package io.jenkins.plugins.llvm;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import java.io.IOException;

public class LLVMCovPublisher extends Recorder implements SimpleBuildStep {

    private LLVMCovReportAdapter coverageApiAdapter;

    @DataBoundConstructor
    public LLVMCovPublisher() {
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        if (coverageApiAdapter != null) {
            coverageApiAdapter.performCoveragePlugin(run, workspace, launcher, listener);
        }
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    public LLVMCovReportAdapter getCoverageApiAdapter() {
        return coverageApiAdapter;
    }

    @DataBoundSetter
    public void setCoverageApiAdapter(LLVMCovReportAdapter coverageApiAdapter) {
        this.coverageApiAdapter = coverageApiAdapter;
    }

    @Symbol("llvmCoverage")
    @Extension
    public static final class LLVMCovPublisherDescriptor extends BuildStepDescriptor<Publisher> {

        public LLVMCovPublisherDescriptor() {
            super(LLVMCovPublisher.class);
            load();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            super.configure(req, json);
            save();
            return true;
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return Messages.LLVMCovPublisher_displayName();
        }
    }
}
