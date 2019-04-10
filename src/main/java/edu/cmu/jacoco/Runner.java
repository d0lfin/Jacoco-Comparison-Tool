package edu.cmu.jacoco;

import org.apache.commons.cli.ParseException;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.analysis.IPackageCoverage;
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
        System.out.println("[Jacoco comparison tool] Start: " + new Date().toString());

        ArgumentsExtractor argumentsExtractor = new ArgumentsExtractor();
        ArgumentsExtractor.Arguments arguments = argumentsExtractor.extractArguments(args);

        List<IBundleCoverage> coverage = analyze(
                arguments.first.stream().map(File::new).collect(Collectors.toList()),
                arguments.second.stream().map(File::new).collect(Collectors.toList()),
                getClasses(arguments).stream().map(File::new).filter(File::exists).collect(Collectors.toList())
        );

        System.out.println("[Jacoco comparison tool] Stop analyze coverage: " + new Date().toString());

        IBundleCoverage firstCoverage = coverage.get(0);
        IBundleCoverage secondCoverage = coverage.get(1);

        LinesInfo firstFileInfo = getInfo(firstCoverage);
        LinesInfo secondFileInfo = getInfo(secondCoverage);

        System.out.println("[Jacoco comparison tool] Stop calculating info for report: " + new Date().toString());

        List<File> sources = getSources(arguments).stream()
                .map(File::new).filter(File::exists).collect(Collectors.toList());

        new NewReportGenerator(new File(arguments.report), sources).generateReport(firstFileInfo, secondFileInfo);


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
        ExecutionDataVisitor.StoreStrategy firstFileClasses = data -> classNamesCollector.contains(data.getName());

        IBundleCoverage manualCoverage = analyzer.analyze(classNamesCollector, firstFiles);

        List<File> allFiles = new ArrayList<>();
        allFiles.addAll(firstFiles);
        allFiles.addAll(secondFiles);

        Future<IBundleCoverage> coverage = executorService.submit(
                () -> analyzer.analyze(firstFileClasses, secondFiles));
        Future<IBundleCoverage> mergedCoverage = executorService.submit(
                () -> analyzer.analyze(firstFileClasses, allFiles));

        List<IBundleCoverage> result = Arrays.asList(manualCoverage, coverage.get(), mergedCoverage.get());

        executorService.shutdown();

        return result;
    }

    private static LinesInfo getInfo(IBundleCoverage coverage) {
        // package ( class, (line number, status))
        HashMap<String, HashMap<String, HashMap<Integer, Integer>>> info = new HashMap<>();
        for (IPackageCoverage packageCoverage: coverage.getPackages()) {
            String packageName = packageCoverage.getName().replace('/', '.');
            HashMap<String, HashMap<Integer, Integer>> packageClasses = info.get(packageName);

            for (IClassCoverage classCoverage: packageCoverage.getClasses()) {
                int firstLineWithCoverage = classCoverage.getFirstLine();
                if (firstLineWithCoverage == -1) {
                    continue;
                }

                String[] classNameParts = classCoverage.getName().split("/");
                String className = classNameParts[classNameParts.length - 1];
                HashMap<Integer, Integer> classLines = packageClasses == null ? null : packageClasses.get(className);

                for (int linePosition = firstLineWithCoverage; linePosition < classCoverage.getLastLine(); linePosition++) {
                    int status = classCoverage.getLine(linePosition).getStatus();
                    if (status == ICounter.NOT_COVERED || status == ICounter.EMPTY) {
                        continue;
                    }

                    if (packageClasses == null) {
                        packageClasses = new HashMap<>();
                        info.put(packageName, packageClasses);
                    }

                    if (classLines == null) {
                        classLines = new HashMap<>();
                        packageClasses.put(className, classLines);
                    }

                    classLines.put(linePosition, status);
                }
            }
        }
        return new LinesInfo(info);
    }

    private static class ClassNamesCollector implements ExecutionDataVisitor.StoreStrategy {

        private final Set<String> classes = new ConcurrentSkipListSet<>();

        @Override
        public boolean shouldBeStored(ExecutionData data) {
            classes.add(data.getName()); // add full class path to set
            return true;
        }

        boolean contains(String className) {
            return classes.contains(className);
        }
    }

    private interface PathStoreStrategy {

        boolean shouldBeStored(File file);
    }

    public static class LinesInfo extends HashMap<String, HashMap<String, HashMap<Integer, Integer>>> {
        LinesInfo(Map<? extends String, ? extends HashMap<String, HashMap<Integer, Integer>>> m) {
            super(m);
        }
    }
}
