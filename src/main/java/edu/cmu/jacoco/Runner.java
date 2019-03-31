package edu.cmu.jacoco;

import edu.cmu.jacoco.CoverageCalculator.CoverageInfo;
import org.apache.commons.cli.ParseException;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.data.ExecutionData;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static java.lang.Runtime.getRuntime;
import static java.util.concurrent.Executors.newFixedThreadPool;

public class Runner {

    public static void main(final String[] args) throws ParseException, IOException, ExecutionException, InterruptedException {
        ArgumentsExtractor argumentsExtractor = new ArgumentsExtractor();
        ArgumentsExtractor.Arguments arguments = argumentsExtractor.extractArguments(args);

        final File firstFile = new File(arguments.exec.get(0));
        final File secondFile = new File(arguments.exec.get(1));

        List<IBundleCoverage> coverages = analyze(
                firstFile,
                secondFile,
                getClasses(arguments).stream().map(File::new).filter(File::exists).collect(Collectors.toList())
        );

//        ClassesWithCoverageCollector collector = new ClassesWithCoverageCollector();
//        List<CoverageInfo> coverageInfo = calculateInfo(coverages, collector);
//
//        ReportsGenerator reportsGenerator = new ReportsGenerator(
//                new File(arguments.report),
//                getSources(arguments),
//                arguments.titles
//        );
//        reportsGenerator.generateDiffReport(coverageInfo, collector);
//        reportsGenerator.generateClassesCoverageReports(coverages);
    }

    private static List<String> getClasses(ArgumentsExtractor.Arguments arguments) {
        if (arguments.classes.isEmpty()) {
            List<String> javac = new LinkedList<>();
            visit(new File(arguments.root), javac, file -> file.getName().equals("javac"));
            List<String> classes = new LinkedList<>();
            for (String path: javac) {
                visit(new File(path), classes, file -> file.getName().equals("classes"));
            }
            return classes;
        } else {
            return arguments.classes;
        }
    }

    private static List<String> getSources(ArgumentsExtractor.Arguments arguments) {
        if (arguments.sources.isEmpty()) {
            List<String> src = new LinkedList<>();
            visit(new File(arguments.root), src, file -> file.getName().equals("src"));
            List<String> main = new LinkedList<>();
            for (String path: src) {
                visit(new File(path), main, file -> file.getName().equals("main"));
            }
            List<String> java = new LinkedList<>();
            for (String path: main) {
                visit(new File(path), java, file -> file.getName().equals("java"));
            }
            return java;
        } else {
            return arguments.sources;
        }
    }

    private static void visit(File directory, List<String> directories, PathStoreStrategy pathStoreStrategy) {
        File[] files = directory.listFiles();

        if (files == null) {
            return;
        }

        for (File file: files) {
            if (file.isDirectory()) {
                if (pathStoreStrategy.shouldBeStored(file)) {
                    directories.add(file.getAbsolutePath());
                }

                visit(file, directories, pathStoreStrategy);
            }
        }
    }

    private static List<IBundleCoverage> analyze(
            File firstFile,
            File secondFile,
            List<File> classesDirectory
    ) throws IOException, ExecutionException, InterruptedException {
        ExecutorService executorService = newFixedThreadPool(getRuntime().availableProcessors() * 2 + 1);
        CoverageAnalyzer analyzer = new CoverageAnalyzer(classesDirectory, executorService);
        ClassNamesCollector classNamesCollector = new ClassNamesCollector();
        ExecutionDataVisitor.StoreStrategy storeStrategy = data -> classNamesCollector.contains(data.getName());

        System.out.println("[dolf] Start analyze: " + new Date().toString());
        IBundleCoverage manualCoverage = analyzer.analyze(classNamesCollector, firstFile);
        System.out.println("[dolf] Stop analyze: " + new Date().toString());

//        Future<IBundleCoverage> coverage = executorService.submit(
//                () -> analyzer.analyze(storeStrategy, secondFile));
//        Future<IBundleCoverage> mergedCoverage = executorService.submit(
//                () -> analyzer.analyze(storeStrategy, firstFile, secondFile));

        return Arrays.asList(manualCoverage);//, coverage.get(), mergedCoverage.get());
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

        private final Set<String> classes = new ConcurrentSkipListSet<>();

        @Override
        public boolean shouldBeStored(ExecutionData data) {
            classes.add(data.getName());
            return true;
        }

        boolean contains(String className) {
            return classes.contains(className);
        }
    }

    private interface PathStoreStrategy {

        boolean shouldBeStored(File file);
    }
}
