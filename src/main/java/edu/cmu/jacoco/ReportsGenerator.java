package edu.cmu.jacoco;

import edu.cmu.jacoco.CoverageCalculator.CoverageInfo;
import org.jacoco.core.analysis.IBundleCoverage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class ReportsGenerator {

    private final CodeDirector director;
    private final int numberOfTestSuites;
    private final HTMLWriter writer;
    private final List<String> titles;

    public ReportsGenerator(File reportDirectory, List<String> sources, List<String> titles) {
        createDirectoryIfNotExists(reportDirectory);
        this.titles = titles;
        numberOfTestSuites = titles.size();
        writer = new HTMLWriter(reportDirectory + "/index.html");
        director = new CodeDirector(
                sources.stream().map(File::new).filter(File::exists).collect(Collectors.toList()),
                reportDirectory,
                new HTMLHighlighter()
        );
    }

    public void generateDiffReport(List<CoverageInfo> coverageInfos, ClassesWithCoverageCollector collector) {
        List<String> testSuiteTitles = wrapTitles(titles);
        renderBranchCoverage(testSuiteTitles, coverageInfos, collector.classesWithCoverage);
    }

    public void generateClassesCoverageReports(List<IBundleCoverage> coverages) {
        director.generateClassCoverageReport(coverages);
    }

    private void createDirectoryIfNotExists(File path) {
        try {
            if (!path.exists()) {
                Files.createDirectories(Paths.get(path.getPath()));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private List<String> wrapTitles(List<String> optionValues) {

        int givenTitles = optionValues.size();

        if (givenTitles == numberOfTestSuites) return optionValues;

        List<String> wrapped = new ArrayList<>();

        System.arraycopy(optionValues, 0, wrapped, 0, givenTitles);

        for (int counter = givenTitles; counter < numberOfTestSuites; counter++) {
            wrapped.add("Test Suite " + (counter + 1));
        }

        return wrapped;

    }

    private void renderBranchCoverage(List<String> testSuiteTitles,
                                      List<CoverageInfo> coverageInfoList,
                                      Map<String, Set<String>> classesWithCoverage) {

        writer.renderTotalCoverage(coverageInfoList, testSuiteTitles);

        for (String packageName : classesWithCoverage.keySet()) {
            Set<String> packages = classesWithCoverage.get(packageName);
            renderPackageBranchCoverage(packageName, packages, coverageInfoList, testSuiteTitles);
        }

        writer.renderReportEnd();
    }

    private void renderPackageBranchCoverage(
            String packageName,
            Set<String> classesNames,
            List<CoverageInfo> coverageInfoList,
            List<String> testSuiteTitles
    ) {

        writer.renderPackageHeader(packageName, testSuiteTitles);

        List<CoverageInfo> packageCoverages = coverageInfoList.stream()
                .map(info -> getChildCoverageInfo(info, packageName))
                .collect(Collectors.toList());

        for (String className : classesNames) {
            List<CoverageInfo> classCoverages = packageCoverages.stream()
                    .map(info -> getChildCoverageInfo(info, className))
                    .collect(Collectors.toList());
            HashMap<String, String> options = new HashMap<>();
            options.put("bgcolor", "F7E4E4");
            renderClassBranchCoverage(packageName, className, classCoverages, options);
        }


        renderClassBranchCoverage("", "Total Lines Coverage", packageCoverages, new HashMap<String, String>() {{
            put("bgcolor", "C3FAF9");
        }});

        writer.renderPackageFooter();
    }

    private CoverageInfo getChildCoverageInfo(CoverageInfo coverageInfo, String childName) {
        Map<String, CoverageInfo> packagesCoverageInfo = coverageInfo.getChilds();
        if (packagesCoverageInfo.containsKey(childName)) {
            return packagesCoverageInfo.get(childName);
        }

        return new CoverageInfo();
    }


    private void renderClassBranchCoverage(String packageName,
                                           String className,
                                           List<CoverageInfo> classCoverages,
                                           HashMap<String, String> options) {

        boolean different = classCoverages.get(0).getCoveredLines() != classCoverages.get(1).getCoveredLines();
        writer.renderClassHeader(replace(packageName), replace(className), different);

        for (CoverageInfo coverage : classCoverages) {
            writer.renderTestSuitCoverage(coverage, options);
        }

        writer.renderClassFooter();
    }

    private String replace(String string) {
        return string.replaceAll("/", ".");
    }
}
