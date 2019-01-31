package edu.cmu.jacoco;

import org.apache.commons.cli.ParseException;
import org.jacoco.core.analysis.IBundleCoverage;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Runner {

    public static void main(final String[] args) throws ParseException, IOException {
        ArgumentsExtractor argumentsExtractor = new ArgumentsExtractor();
        ArgumentsExtractor.Arguments arguments = argumentsExtractor.extractArguments(args);

        final File firstFile = new File(arguments.exec.get(0));
        final File secondFile = new File(arguments.exec.get(1));

        CoverageDiff coverageDiff = new CoverageDiff(arguments);

        List<IBundleCoverage> coverages = coverageDiff.analyze(firstFile, secondFile);

        ClassesWithCoverageCollector collector = new ClassesWithCoverageCollector();
        List<CoverageCalculator.CoverageInfo> coverageInfo = coverageDiff.calculateInfo(coverages, collector);

        ReportsGenerator reportsGenerator = new ReportsGenerator(arguments);
        reportsGenerator.generateDiffReport(coverageInfo, collector);
        reportsGenerator.generateClassesCoverageReports(coverages);
    }
}
