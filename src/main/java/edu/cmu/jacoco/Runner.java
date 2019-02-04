package edu.cmu.jacoco;

import edu.cmu.jacoco.CoverageCalculator.CoverageInfo;
import org.apache.commons.cli.ParseException;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.data.ExecutionData;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Runner {

    public static void main(final String[] args) throws ParseException, IOException {
        ArgumentsExtractor argumentsExtractor = new ArgumentsExtractor();
        ArgumentsExtractor.Arguments arguments = argumentsExtractor.extractArguments(args);

        final File firstFile = new File(arguments.exec.get(0));
        final File secondFile = new File(arguments.exec.get(1));

        List<IBundleCoverage> coverages = analyze(
                firstFile,
                secondFile,
                getClasses(arguments).stream().map(File::new).collect(Collectors.toList())
        );

        ClassesWithCoverageCollector collector = new ClassesWithCoverageCollector();
        List<CoverageInfo> coverageInfo = calculateInfo(coverages, collector);

        ReportsGenerator reportsGenerator = new ReportsGenerator(
                new File(arguments.report),
                getSources(arguments),
                arguments.titles
        );
        reportsGenerator.generateDiffReport(coverageInfo, collector);
        reportsGenerator.generateClassesCoverageReports(coverages);
    }

    private static List<String> getClasses(ArgumentsExtractor.Arguments arguments) {
        return arguments.classes;
    }

    private static List<String> getSources(ArgumentsExtractor.Arguments arguments) {
        return arguments.sources;
    }

    private static List<IBundleCoverage> analyze(
            File firstFile,
            File secondFile,
            List<File> classesDirectory
    ) throws IOException {
        CoverageAnalyzer analyzer = new CoverageAnalyzer(classesDirectory);
        ClassNamesCollector classNamesCollector = new ClassNamesCollector();
        ExecutionDataVisitor.StoreStrategy storeStrategy = data -> classNamesCollector.contains(data.getName());
        return Arrays.asList(
                analyzer.analyze(classNamesCollector, firstFile),
                analyzer.analyze(storeStrategy, secondFile),
                analyzer.analyze(storeStrategy, firstFile, secondFile)
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

    private static class ClassNamesCollector implements ExecutionDataVisitor.StoreStrategy {

        private final Set<String> classes = new LinkedHashSet<>();

        @Override
        public boolean shouldBeStored(ExecutionData data) {
            classes.add(data.getName());
            return true;
        }

        boolean contains(String className) {
            return classes.contains(className);
        }
    }
}
