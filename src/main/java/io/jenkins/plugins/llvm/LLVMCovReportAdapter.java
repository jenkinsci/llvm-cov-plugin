package io.jenkins.plugins.llvm;

import com.google.common.collect.Lists;
import hudson.Extension;
import io.jenkins.plugins.coverage.adapter.CoverageReportAdapter;
import io.jenkins.plugins.coverage.adapter.CoverageReportAdapterDescriptor;
import io.jenkins.plugins.coverage.adapter.JSONCoverageReportAdapter;
import io.jenkins.plugins.coverage.adapter.converter.JSONDocumentConverter;
import io.jenkins.plugins.coverage.exception.CoverageException;
import io.jenkins.plugins.coverage.targets.CoverageElement;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.w3c.dom.Document;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.List;

public class LLVMCovReportAdapter extends JSONCoverageReportAdapter {

    /**
     * @param path Ant-style path of report files.
     */
    @DataBoundConstructor
    public LLVMCovReportAdapter(String path) {
        super(path);
    }

    @Override
    protected JSONDocumentConverter getConverter() {
        return new LLVMCovReportDocumentConverter();
    }

    @CheckForNull
    @Override
    protected CoverageResult parseToResult(Document document, String reportName) throws CoverageException {
        return new LLVMCoverageParser(reportName).parse(document);
    }


    @Symbol(value = {"llvmAdapter", "llvm"})
    @Extension
    public static class LLVMCovReportAdapterDescriptor extends CoverageReportAdapterDescriptor<CoverageReportAdapter> {

        public LLVMCovReportAdapterDescriptor() {
            super(LLVMCovReportAdapter.class);
        }

        @Override
        public List<CoverageElement> getCoverageElements() {
            return Lists.newArrayList(new CoverageElement("LLVM Data", 0),
                    new CoverageElement("LLVM Directory", 1),
                    new CoverageElement("LLVM File", 2),
                    new CoverageElement("LLVM Function", 3));
        }

        @Override
        public String getCoverageElementType() {
            return "llvm-cov";
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return Messages.LLVMCovReportAdapter_displayName();
        }
    }
}
