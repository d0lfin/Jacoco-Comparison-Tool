package edu.cmu.jacoco;

import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.tools.ExecFileLoader;

import java.io.File;
import java.io.IOException;
import java.util.Date;

class CoverageAnalyzer {

    private File classesPath;

    CoverageAnalyzer(File classesPath) {
        this.classesPath = classesPath;
    }

    IBundleCoverage analyze(File... executionFiles) throws IOException {
        return analyze(getExecutionDataStore(executionFiles));
    }

    private ExecutionDataStore getExecutionDataStore(File... executionFiles) throws IOException {
        ExecFileLoader fileLoader = new ExecFileLoader();
        for (File executionFile : executionFiles) {
            fileLoader.load(executionFile);
        }
        return fileLoader.getExecutionDataStore();
    }

    private IBundleCoverage analyze(ExecutionDataStore executionDataStore) {
        CoverageBuilder coverageBuilder = new CoverageBuilder();
        Analyzer analyzer = new Analyzer(executionDataStore, coverageBuilder);

        try {
            analyzer.analyzeAll(classesPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return coverageBuilder.getBundle(String.valueOf(new Date().getTime()));
    }
}