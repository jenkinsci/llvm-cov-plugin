package io.jenkins.plugins.llvm;

import io.jenkins.plugins.coverage.adapter.parser.CoverageParser;
import io.jenkins.plugins.coverage.targets.CoverageElement;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import io.jenkins.plugins.coverage.targets.Ratio;
import org.w3c.dom.Element;

public class LLVMCoverageParser extends CoverageParser {
    /**
     * Report name will show in the UI, to differentiate different report.
     *
     * @param reportName name of the report
     */
    public LLVMCoverageParser(String reportName) {
        super(reportName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected CoverageResult processElement(Element current, CoverageResult parentResult) {
        CoverageResult result = null;

        String name = current.getLocalName();
        if (name == null) {
            name = current.getTagName();
        }

        switch (name) {
            case "report":
                result = new CoverageResult(CoverageElement.REPORT, null,
                        getAttribute(current, "name", "") + ": " + getReportName());
                break;
            case "data":
                result = new CoverageResult(CoverageElement.get("LLVM Data"), parentResult,
                        getAttribute(current, "name", ""));
                break;
            case "directory":
                result = new CoverageResult(CoverageElement.get("LLVM Directory"), parentResult,
                        getAttribute(current, "name", ""));
                break;
            case "file":
                result = new CoverageResult(CoverageElement.get("LLVM File"), parentResult,
                        getAttribute(current, "filename", ""));

                result.setRelativeSourcePath(getAttribute(current, "filename", null));


                String coveredLineStr = getAttribute(current, "line-covered", "0");
                String totalLineStr = getAttribute(current, "line-total", coveredLineStr);
                result.updateCoverage(CoverageElement.LINE, Ratio.create(Integer.parseInt(coveredLineStr), Integer.parseInt(totalLineStr)));

                String coveredFuncStr = getAttribute(current, "func-covered", "0");
                String totalFuncStr = getAttribute(current, "func-total", coveredFuncStr);
                result.updateCoverage(CoverageElement.get("LLVM Function"), Ratio.create(Integer.parseInt(coveredFuncStr), Integer.parseInt(totalFuncStr)));

                break;
            case "line":
                String hitsString = current.getAttribute("hits");
                String lineNumber = current.getAttribute("number");

                int hits = Integer.parseInt(hitsString);
                int number = Integer.parseInt(lineNumber);

                parentResult.paint(number, hits);
                break;
            default:
                break;
        }


        return result;
    }

}
