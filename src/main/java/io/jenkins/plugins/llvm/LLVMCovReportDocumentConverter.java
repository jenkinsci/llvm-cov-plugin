package io.jenkins.plugins.llvm;

import com.fasterxml.jackson.databind.JsonNode;
import io.jenkins.plugins.coverage.adapter.converter.JSONDocumentConverter;
import io.jenkins.plugins.coverage.exception.CoverageException;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class LLVMCovReportDocumentConverter extends JSONDocumentConverter {
    @Override
    protected Document convert(JsonNode report, Document document) throws CoverageException {
        // only support 2.0.0 version now
        if (!report.get("version").asText().equals("2.0.0")) {
            throw new CoverageException("Unsupported Json file - version must be 2.0.0");
        }

        if (!report.get("type").asText().equals("llvm.coverage.json.export")) {
            throw new CoverageException("Unsupported Json file - type must be llvm.coverage.json.export");
        }

        Element reportEle = document.createElement("report");
        reportEle.setAttribute("name", "llvm-cov");
        document.appendChild(reportEle);

        JsonNode dataArr = report.get("data");

        if (dataArr == null || dataArr.size() == 0) {
            throw new CoverageException("No data section found in coverage report, will skip it");
        }

        if (dataArr.size() == 1) {
            Element dataEle = document.createElement("data");
            dataEle.setAttribute("name", "data");
            reportEle.appendChild(dataEle);

            JsonNode dataObj = dataArr.get(0);
            processDataObj(dataObj, dataEle, document);
        } else {
            for (int i = 0; i < dataArr.size(); i++) {
                Element dataEle = document.createElement("data");
                dataEle.setAttribute("name", "data" + i);
                reportEle.appendChild(dataEle);

                JsonNode dataObj = dataArr.get(i);

                processDataObj(dataObj, dataEle, document);
            }
        }

        return document;
    }

    /**
     * parse each data object in JSON, and convert it to data element and then to document.
     *
     * @param dataObj  data object in JSON
     * @param dataEle  data element added to document
     * @param document document
     */
    private void processDataObj(JsonNode dataObj, Element dataEle, Document document) throws CoverageException {
        JsonNode files = dataObj.get("files");
        JsonNode functions = dataObj.get("functions");

        List<Element> fileElements = processFiles(files, document);

        // group file elements by its parent path
        fileElements.stream().collect(Collectors.groupingBy(f -> {
            String filename = f.getAttribute("filename");

            File path = new File(filename);
            if (StringUtils.isEmpty(path.getParent())) {
                return ".";
            } else {
                return path.getParent();
            }
        })).forEach((parentPath, fileEles) -> {
            Element directoryEle = document.createElement("directory");
            directoryEle.setAttribute("name", parentPath);
            fileEles.forEach(directoryEle::appendChild);
            dataEle.appendChild(directoryEle);
        });

        processFunctions(functions, fileElements, document);
    }

    /**
     * parse file objects in JSON format report, and convert them to file element.
     *
     * @param files    files array
     * @param document document
     * @return list of file elements
     */
    private List<Element> processFiles(JsonNode files, Document document) throws CoverageException {
        List<Element> fileElements = new LinkedList<>();
        for (int i = 0; i < files.size(); i++) {
            JsonNode file = files.get(i);

            Element fileEle = document.createElement("file");
            fileEle.setAttribute("filename", file.get("filename").asText());

            JsonNode segments = file.get("segments");

            processLines(segments, fileEle, document);

            fileElements.add(fileEle);
            fileEle.setAttribute("line-covered", file.get("summary").get("lines").get("covered").asText());
            fileEle.setAttribute("line-total", file.get("summary").get("lines").get("count").asText());

            fileEle.setAttribute("func-covered", file.get("summary").get("functions").get("covered").asText());
            fileEle.setAttribute("func-total", file.get("summary").get("functions").get("count").asText());
        }
        return fileElements;
    }

    /**
     * parse function objects in JSON, and parse them and them to its correspond file element.
     *
     * @param functions    functions array
     * @param fileElements file elements
     * @param document     document
     */
    private void processFunctions(JsonNode functions, List<Element> fileElements, Document document) {
        for (int i = 0; i < functions.size(); i++) {
            Element functionEle = document.createElement("function");

            JsonNode function = functions.get(i);
            String name = function.get("name").asText();
            JsonNode regions = function.get("regions");
            JsonNode filenames = function.get("filenames");

            functionEle.setAttribute("name", name);

            for (int j = 0; j < filenames.size(); j++) {
                String filename = filenames.get(j).asText();

                Optional<Element> correspondFileOptional = fileElements.stream()
                        .filter(f -> f.getAttribute("filename").equals(filename))
                        .findAny();

                if (!correspondFileOptional.isPresent()) {
                    continue;
                }

                Element correspondFile = correspondFileOptional.get();

                correspondFile.appendChild(functionEle);
                StreamSupport.stream(Spliterators.spliteratorUnknownSize(regions.iterator(), Spliterator.ORDERED), false)
                        .forEach(r -> {
                            NodeList lines = correspondFile.getElementsByTagName("line");

                            for (int k = 0; k < lines.getLength(); k++) {
                                Element lineEleInFile = (Element) lines.item(k);
                                int line = Integer.parseInt(lineEleInFile.getAttribute("number"));

                                if (line >= r.get(0).asInt() && line <= r.get(2).asInt()) {
                                    Node n = lineEleInFile.cloneNode(true);
                                    functionEle.appendChild(n);
                                    break;
                                }
                            }
                        });
            }

        }
    }


    private void processLines(JsonNode segmentsNode, Element fileEle, Document document) throws CoverageException {
        List<JsonNode> segments = StreamSupport.stream(Spliterators.spliteratorUnknownSize(segmentsNode.iterator(), Spliterator.ORDERED), false)
                .collect(Collectors.toList());
        if (segments.size() == 0) {
            return;
        }

        // if only has one segment, will convert segment to line if this segment has count
        if (segments.size() == 1) {
            JsonNode seg = segments.get(0);
            if (!isSegmentHasCount(seg)) {
                return;
            }

            int lineNumber = seg.get(0).asInt();
            int count = seg.get(2).asInt();
            appendLine(fileEle, document, lineNumber, count);
            return;
        }

        // we group segments by line number
        LinkedHashMap<Integer, List<JsonNode>> segmentsWithLineNum = segments
                .stream()
                .collect(Collectors.groupingBy(s -> s.get(0).asInt(),
                        LinkedHashMap::new, Collectors.toList()));


        Iterator<Map.Entry<Integer, List<JsonNode>>> lineIterator = segmentsWithLineNum.entrySet().iterator();

        Map.Entry<Integer, List<JsonNode>> previousLine = lineIterator.next();

        // process the first line of segments
        processLine(fileEle, document, previousLine.getKey(), previousLine.getValue());

        while (lineIterator.hasNext()) {
            Map.Entry<Integer, List<JsonNode>> currentLine = lineIterator.next();
            int previousLineNum = previousLine.getKey();
            int currentLineNum = currentLine.getKey();
            if (currentLineNum < previousLineNum) {
                throw new CoverageException(String.format("Not a valid segment sequences in file %s", fileEle.getAttribute("filename")));
            }

            if (currentLine.getKey() - previousLine.getKey() == 1) {
                processLine(fileEle, document, currentLine.getKey(), currentLine.getValue());
                previousLine = currentLine;
                continue;
            }

            JsonNode lastSegment = previousLine.getValue().get(previousLine.getValue().size() - 1);
            if (!isSegmentHasCount(lastSegment)) {
                previousLine = currentLine;
                continue;
            }

            int count = lastSegment.get(2).asInt();
            while (++previousLineNum < currentLineNum) {
                appendLine(fileEle, document, previousLineNum, count);
            }

            previousLine = currentLine;
        }
    }

    private void processLine(Element parent, Document document, int lineNum, List<JsonNode> segs) {
        int maxHits = 0;

        for (JsonNode s : segs) {
            int hasCount = s.get(3).asInt();
            if (hasCount == 0) {
                return;
            }

            int hits = s.get(2).asInt();
            maxHits = Math.max(hits, maxHits);
        }

        appendLine(parent, document, lineNum, maxHits);
    }


    private void appendLine(Element parent, Document document, int number, int hits) {
        Element lineEle = document.createElement("line");

        lineEle.setAttribute("number", Integer.toString(number));
        lineEle.setAttribute("hits", Integer.toString(hits));

        parent.appendChild(lineEle);
    }

    private boolean isSegmentHasCount(JsonNode segment) {
        return segment.get(3).asInt() == 1;
    }
}
