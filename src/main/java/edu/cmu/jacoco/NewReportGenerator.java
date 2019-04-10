package edu.cmu.jacoco;

import edu.cmu.jacoco.Runner.LinesInfo;
import org.jacoco.core.analysis.ICounter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static j2html.TagCreator.*;

public class NewReportGenerator {

    public static final String TITLE = "Coverage comparison results";
    public static final String NOT_COVERED_COLOR = "red";
    public static final String PARTLY_COVERED_COLOR = "yellow";

    private final List<File> sources;
    private File reportDirectory;

    public NewReportGenerator(File reportDirectory, List<File> sources) {
        this.reportDirectory = reportDirectory;
        this.sources = sources;
    }

    private File createFile(String filePath) throws IOException {
        String[] parts = filePath.split("/");
        String fileName = parts.length == 1 ? parts[0] : parts[1];
        File report = new File(reportDirectory, "/" + filePath);
        if (!report.exists()) {
            Files.createDirectories(Paths.get(new File(reportDirectory, "/" + filePath.replace(fileName, "")).getPath()));
            report.createNewFile();
        }
        return report;
    }

    public void generateReport(LinesInfo baseCoverage, LinesInfo coverage) {
        try {
            generateIndex(baseCoverage, coverage);
            generateClassesReports(baseCoverage, coverage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void generateIndex(LinesInfo baseCoverage, LinesInfo coverage) throws IOException {
        HashMap<String, HashMap<String, String>> packages = getDifference(baseCoverage, coverage);
        render(createFile("index.html"), document(
                html(
                        head(
                                title(TITLE)
                        ),
                        body(
                                h2(TITLE),
                                each(packages.keySet(), packageName ->
                                        div(
                                                h3(packageName),
                                                ul(
                                                        each(packages.get(packageName).keySet(), className -> {
                                                            String href = getPathToClassReport(packageName, className);
                                                            String color = packages.get(packageName).get(className);
                                                            return li(
                                                                    a().withText(className).withHref(href)
                                                            ).withStyle("background-color:" + color);
                                                        })
                                                )
                                        )
                                )
                        )
                )
        ));
    }

    private String getPathToClassReport(String packageName, String className) {
        return packageName + "/" + className + ".java.html";
    }

    private void generateClassesReports(LinesInfo baseCoverage, LinesInfo coverage) throws IOException {
        for (String packageName : baseCoverage.keySet()) {
            for (String className : baseCoverage.get(packageName).keySet()) {
                generateClassReport(packageName, className, baseCoverage, coverage);
            }
        }
    }

    private void generateClassReport(
            String packageName, String className, LinesInfo baseCoverage, LinesInfo coverage
    ) throws IOException {
        Optional<BufferedReader> optionalBufferedReader = getSourceFileReader(packageName, className);
        if (!optionalBufferedReader.isPresent()) {
            return;
        }

        File classReport = createFile(getPathToClassReport(packageName, className));
        BufferedWriter writer = new BufferedWriter(new FileWriter(classReport));
        BufferedReader reader = optionalBufferedReader.get();

        String line;
        int linePosition = 0;

        writer.write("<!DOCTYPE html><html><head><title>" + className + "</title>" +
            "<link rel=\"stylesheet\" href=\"https://cdnjs.cloudflare.com/ajax/libs/highlight.js/9.13.1/styles/default.min.css\">" +
            "<script src=\"https://cdn.jsdelivr.net/gh/highlightjs/cdn-release@9.13.1/build/highlight.min.js\"></script>" +
            "<script>hljs.initHighlightingOnLoad();</script></head>");
        writer.write("<body><pre><code>");

        while ((line = reader.readLine()) != null) {
            linePosition++;

            LINE_STATUS baseInfoLineStatus = getLineStatus(baseCoverage, packageName, className, linePosition);
            if (baseInfoLineStatus == LINE_STATUS.NOT_COVERED) {
                writer.write(line);
                writer.write("\n");
                continue;
            }

            LINE_STATUS infoLineStatus = getLineStatus(coverage, packageName, className, linePosition);
            if (infoLineStatus == LINE_STATUS.FULLY_COVERED) {
                writer.write(line);
                writer.write("\n");
                continue;
            }

            String color = infoLineStatus == LINE_STATUS.NOT_COVERED ? NOT_COVERED_COLOR : PARTLY_COVERED_COLOR;
            writer.write("<span style=\"background-color:" + color + "\">");
            writer.write(line);
            writer.write("</span>\n");
        }

        writer.write("</code></pre></body></html>");

        writer.close();
        reader.close();
    }

    private Optional<BufferedReader> getSourceFileReader(String packageName, String className) {
        String filePath = packageName.replaceAll("\\.", "/");
        String fileName = className.concat(".java").replaceFirst("\\$[a-zA-Z_0-9]+", "");
        return sources.stream()
                .map(d -> new File(d, filePath + "/" + fileName))
                .filter(File::exists)
                .map(file -> {
                    try {
                        return new FileReader(file);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                })
                .map(BufferedReader::new)
                .findFirst();

    }

    private enum LINE_STATUS {
        FULLY_COVERED,
        PARTLY_COVERED,
        NOT_COVERED
    }

    private HashMap<String, HashMap<String, String>> getDifference(LinesInfo baseCoverage, LinesInfo coverage) {
        HashMap<String, HashMap<String, String>> packages = new HashMap<>();
        for (String packageName : baseCoverage.keySet()) {
            for (String className : baseCoverage.get(packageName).keySet()) {
                HashMap<Integer, Integer> baseClassCoverage = baseCoverage.get(packageName).get(className);

                for (Integer lineNumber : baseClassCoverage.keySet()) {
                    LINE_STATUS lineStatus = getLineStatus(coverage, packageName, className, lineNumber);

                    if (lineStatus == LINE_STATUS.FULLY_COVERED) {
                        continue;
                    }

                    HashMap<String, String> classes = packages.computeIfAbsent(packageName, k -> new HashMap<>());

                    if (lineStatus == LINE_STATUS.PARTLY_COVERED) {
                        classes.put(className, getColor(classes, className, PARTLY_COVERED_COLOR));
                    } else {
                        classes.put(className, getColor(classes, className, NOT_COVERED_COLOR));
                    }
                }
            }
        }
        return packages;
    }

    private LINE_STATUS getLineStatus(LinesInfo info, String packageName, String className, int line) {
        HashMap<String, HashMap<Integer, Integer>> classesInfo = info.get(packageName);

        if (classesInfo == null) {
            return LINE_STATUS.NOT_COVERED;
        }

        HashMap<Integer, Integer> linesInfo = classesInfo.get(className);

        if (linesInfo == null || linesInfo.get(line) == null) {
            return LINE_STATUS.NOT_COVERED;
        }

        if (linesInfo.get(line).equals(ICounter.FULLY_COVERED)) {
            return LINE_STATUS.FULLY_COVERED;
        }

        if (linesInfo.get(line).equals(ICounter.PARTLY_COVERED)) {
            return LINE_STATUS.PARTLY_COVERED;
        }

        return LINE_STATUS.NOT_COVERED;
    }

    private String getColor(HashMap<String, String> classes, String className, String newColor) {
        String color = classes.get(className);
        if (color == null) {
            return newColor;
        }

        if (color.equals(PARTLY_COVERED_COLOR) || newColor.equals(PARTLY_COVERED_COLOR)) {
            return PARTLY_COVERED_COLOR;
        } else {
            return NOT_COVERED_COLOR;
        }
    }

    private void render(File file, String content) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.write(content);
        writer.close();
    }
}
