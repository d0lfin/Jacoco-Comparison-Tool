package edu.cmu.jacoco;

import edu.cmu.jacoco.CoverageCalculator.CoverageInfo;
import org.apache.commons.cli.ParseException;
import org.jacoco.core.analysis.IBundleCoverage;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Runner {

    public static void main(final String[] args) throws ParseException, IOException {
        ArgumentsExtractor argumentsExtractor = new ArgumentsExtractor();
        ArgumentsExtractor.Arguments arguments = argumentsExtractor.extractArguments(args);

        final File firstFile = new File(arguments.exec.get(0));
        final File secondFile = new File(arguments.exec.get(1));

        List<IBundleCoverage> coverages = analyze(firstFile, secondFile, new File(arguments.classes));

        ClassesWithCoverageCollector collector = new ClassesWithCoverageCollector();
        List<CoverageInfo> coverageInfo = calculateInfo(coverages, collector);

        ReportsGenerator reportsGenerator = new ReportsGenerator(arguments);
        reportsGenerator.generateDiffReport(coverageInfo, collector);
        reportsGenerator.generateClassesCoverageReports(coverages);
    }

    private static  List<IBundleCoverage> analyze(
            File firstFile,
            File secondFile,
            File classesDirectory
    ) throws IOException {
        CoverageAnalyzer analyzer = new CoverageAnalyzer(classesDirectory);
        return Arrays.asList(
                analyzer.analyze(firstFile),
                analyzer.analyze(secondFile),
                analyzer.analyze(firstFile, secondFile)
        );
    }

    private static List<CoverageInfo> calculateInfo(
            List<IBundleCoverage> coverages,
            CoverageCalculator.Visitor visitor
    ) {
        CoverageCalculator calculator = new CoverageCalculator();
        calculator.setCoverageVisitor(visitor);
        return coverages.stream().map(calculator::getInfo).collect(Collectors.toList());
    }
}
