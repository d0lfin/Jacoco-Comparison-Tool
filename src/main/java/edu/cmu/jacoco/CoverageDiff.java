package edu.cmu.jacoco;

import edu.cmu.jacoco.CoverageCalculator.CoverageInfo;
import org.jacoco.core.analysis.IBundleCoverage;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CoverageDiff {

    private final File classesDirectory;

    public CoverageDiff(ArgumentsExtractor.Arguments arguments) {
        classesDirectory = new File(arguments.classes);
    }

    public List<IBundleCoverage> analyze(File firstFile, File secondFile) throws IOException {
        CoverageAnalyzer analyzer = new CoverageAnalyzer(classesDirectory);
        return Arrays.asList(
                analyzer.analyze(firstFile),
                analyzer.analyze(secondFile),
                analyzer.analyze(firstFile, secondFile)
        );
    }

    public List<CoverageInfo> calculateInfo(List<IBundleCoverage> coverages, CoverageCalculator.Visitor visitor) {
        CoverageCalculator calculator = new CoverageCalculator();
        calculator.setCoverageVisitor(visitor);
        return coverages.stream().map(calculator::getInfo).collect(Collectors.toList());
    }
}
