package edu.cmu.jacoco;

import edu.cmu.jacoco.Runner.LinesInfo;
import org.jacoco.core.analysis.ICounter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;

import static j2html.TagCreator.*;

public class NewReportGenerator {

    public static final String TITLE = "Coverage comparison results";
    public static final String NOT_COVERED = "red";
//    public static final String NOT_COVERED_BUT_PARTITIALY_COVERED_BY_HANDS = "orange";
    public static final String PARTITIALY_COVERED = "yellow";
//    public static final String PARTITIALY_COVERED_AND_PARTITIALY_COVERED_BY_HANDS = "grey";

    private final List<File> sources;
    private final BufferedWriter writer;

    public NewReportGenerator(File reportDirectory, List<File> sources) throws IOException {
        File report = new File(reportDirectory, "/index.html");
        if (!report.exists()) {
            Files.createDirectories(Paths.get(reportDirectory.getPath()));
            report.createNewFile();
        }

        this.writer = new BufferedWriter(new FileWriter(report));
        this.sources = sources;
    }

    public void generateReport(LinesInfo baseCoverage, LinesInfo coverage) {
        HashMap<String, HashMap<String, String>> packages = getDifference(baseCoverage, coverage);
        render(document(
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
                                each(packages.get(packageName).keySet(), fullClassName -> {
                                    String[] parts = fullClassName.split("/");
                                    String className = parts[parts.length - 1];
                                    String href = packageName + "/" + className + ".java.html";
                                    return li(
                                            a().withText(className).withHref(href)
                                    ).withStyle("background-color:" + packages.get(packageName).get(fullClassName));
                                })
                            )
                        )
                    )
                )
            )
        ));
    }

    private HashMap<String, HashMap<String, String>> getDifference(LinesInfo baseCoverage, LinesInfo coverage) {
        HashMap<String, HashMap<String, String>> packages = new HashMap<>();
        for (String packageName: baseCoverage.keySet()) {
            for (String className: baseCoverage.get(packageName).keySet()) {
                HashMap<Integer, Integer> baseClassCoverage = baseCoverage.get(packageName).get(className);
                HashMap<Integer, Integer> classCoverage =
                        coverage.get(packageName) == null ? null : coverage.get(packageName).get(className);

                for (Integer lineNumber: baseClassCoverage.keySet()) {
                    Integer lineStatus = classCoverage == null ? null : classCoverage.get(lineNumber);
                    if (lineStatus != null && lineStatus.equals(ICounter.FULLY_COVERED)) {
                        continue;
                    }

                    HashMap<String, String> classes = packages.computeIfAbsent(packageName, k -> new HashMap<>());

                    if (lineStatus != null && lineStatus.equals(ICounter.PARTLY_COVERED)) {
                        classes.put(className, getColor(classes, className, PARTITIALY_COVERED));
                    } else {
                        classes.put(className, getColor(classes, className, NOT_COVERED));
                    }
                }
            }
        }
        return packages;
    }

    private String getColor(HashMap<String, String> classes, String className, String newColor) {
        String color = classes.get(className);
        if (color == null) {
            return newColor;
        }

        if (color.equals(PARTITIALY_COVERED) || newColor.equals(PARTITIALY_COVERED)) {
            return PARTITIALY_COVERED;
        } else {
            return NOT_COVERED;
        }
    }

    private void render(String domContent) {
        try {
            writer.write(domContent);
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
