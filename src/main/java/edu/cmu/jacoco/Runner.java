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
        List<String> fixedArgs = new ArrayList<String>() {
            @Override
            public boolean add(String o) {
                return super.add(o.replace("\"", ""));
            }

            @Override
            public String set(int index, String element) {
                return super.set(index, element.replace("\"", ""));
            }
        };

        boolean listStarted = false;
        for (String arg: args) {
            System.out.println("[Jacoco comparison tool] Args: " + arg);

            if (arg.startsWith("\"") && arg.endsWith("\"")) {
                fixedArgs.add(arg);
                continue;
            }

            if (arg.startsWith("\"")) {
                listStarted = true;
                fixedArgs.add(arg);
                continue;
            }

            if (!listStarted) {
                fixedArgs.add(arg);
                continue;
            }

            if (arg.endsWith("\"")) {
                listStarted = false;
            }

            int elements = fixedArgs.size() - 1;
            String lastArg = fixedArgs.get(elements);
            fixedArgs.set(elements, lastArg + arg);
        }

        System.out.println("[Jacoco comparison tool] Start: " + new Date().toString());

        ArgumentsExtractor argumentsExtractor = new ArgumentsExtractor();
        ArgumentsExtractor.Arguments arguments = argumentsExtractor.extractArguments((String[]) fixedArgs.toArray());

        List<IBundleCoverage> coverages = analyze(
                arguments.first.stream().map(File::new).collect(Collectors.toList()),
                arguments.second.stream().map(File::new).collect(Collectors.toList()),
                getClasses(arguments).stream().map(File::new).filter(File::exists).collect(Collectors.toList())
        );

        System.out.println("[Jacoco comparison tool] Stop analyze coverage: " + new Date().toString());

        ClassesWithCoverageCollector collector = new ClassesWithCoverageCollector();
        List<CoverageInfo> coverageInfo = calculateInfo(coverages, collector);

        System.out.println("[Jacoco comparison tool] Stop calculating info for report: " + new Date().toString());

        ReportsGenerator reportsGenerator = new ReportsGenerator(
                new File(arguments.report),
                getSources(arguments),
                arguments.titles
        );
        reportsGenerator.generateDiffReport(coverageInfo, collector);
        reportsGenerator.generateClassesCoverageReports(coverages);

        System.out.println("[Jacoco comparison tool] Stop: " + new Date().toString());
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
            List<File> firstFiles,
            List<File> secondFiles,
            List<File> classesDirectory
    ) throws IOException, ExecutionException, InterruptedException {
        ExecutorService executorService = newFixedThreadPool(getRuntime().availableProcessors() * 2 + 1);
        CoverageAnalyzer analyzer = new CoverageAnalyzer(classesDirectory, executorService);
        ClassNamesCollector classNamesCollector = new ClassNamesCollector();
        ExecutionDataVisitor.StoreStrategy storeStrategy = data -> classNamesCollector.contains(data.getName());

        IBundleCoverage manualCoverage = analyzer.analyze(classNamesCollector, firstFiles);

        List<File> allFiles = new ArrayList<>();
        allFiles.addAll(firstFiles);
        allFiles.addAll(secondFiles);

        Future<IBundleCoverage> coverage = executorService.submit(
                () -> analyzer.analyze(storeStrategy, secondFiles));
        Future<IBundleCoverage> mergedCoverage = executorService.submit(
                () -> analyzer.analyze(storeStrategy, allFiles));

        List<IBundleCoverage> result = Arrays.asList(manualCoverage, coverage.get(), mergedCoverage.get());

        executorService.shutdown();

        return result;
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
