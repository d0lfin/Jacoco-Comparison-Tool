package edu.cmu.jacoco;

import edu.cmu.jacoco.ExecutionDataVisitor.StoreStrategy;
import edu.cmu.jacoco.async.Analyzer;
import edu.cmu.jacoco.async.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.data.ExecutionDataReader;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.IExecutionDataVisitor;
import org.jacoco.core.data.SessionInfoStore;

import java.io.*;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static java.util.concurrent.CompletableFuture.runAsync;

class CoverageAnalyzer {

    private final List<File> classesPath;
    private ExecutorService executorService;

    CoverageAnalyzer(List<File> classesPath, ExecutorService executorService) {
        this.classesPath = classesPath;
        this.executorService = executorService;
    }

    IBundleCoverage analyze(StoreStrategy storeStrategy, File... executionFiles) throws IOException {
        return analyze(getExecutionDataStore(storeStrategy, executionFiles));
    }

    private ExecutionDataStore getExecutionDataStore(
            StoreStrategy storeStrategy,
            File... executionFiles
    ) throws IOException {
        ExecutionDataVisitor visitor = new ExecutionDataVisitor();
        visitor.setStoreStrategy(storeStrategy);

        FileLoader fileLoader = new FileLoader(visitor);
        for (File executionFile : executionFiles) {
            fileLoader.load(executionFile); // merge files
        }

        return visitor.getExecutionDataStore();
    }

    private IBundleCoverage analyze(ExecutionDataStore executionDataStore) {
        CoverageBuilder coverageBuilder = new CoverageBuilder();

        List<CompletableFuture<Void>> tasks = classesPath.stream()
                .map(file -> runAsync(() -> analyzeFile(file, executionDataStore, coverageBuilder), executorService))
                .collect(Collectors.toList());

        try {
            CompletableFuture.allOf((CompletableFuture<?>) tasks).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return coverageBuilder.getBundle(String.valueOf(new Date().getTime()));
    }

    private void analyzeFile(File file, ExecutionDataStore executionDataStore, CoverageBuilder coverageBuilder) {
        Analyzer analyzer = new Analyzer(executionDataStore, coverageBuilder) {
            @Override
            public void analyzeClass(final InputStream input, final String location)  {
                try {
                    super.analyzeClass(input, location);
                } catch (IOException ignored) {}
            }
        };

        try {
            analyzer.analyzeAll(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private class FileLoader {

        private final SessionInfoStore sessionInfos;
        private final IExecutionDataVisitor executionData;

        FileLoader(IExecutionDataVisitor executionDataVisitor) {
            sessionInfos = new SessionInfoStore();
            executionData = executionDataVisitor;
        }

        void load(final File file) throws IOException {
            try (InputStream stream = new FileInputStream(file)) {
                final ExecutionDataReader reader = new ExecutionDataReader(new BufferedInputStream(stream));
                reader.setExecutionDataVisitor(executionData);
                reader.setSessionInfoVisitor(sessionInfos);
                reader.read();
            }
        }
    }
}