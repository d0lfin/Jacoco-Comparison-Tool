package edu.cmu.jacoco;

import edu.cmu.jacoco.CoverageCalculator.CoverageInfo;
import org.apache.commons.io.FileUtils;
import org.jacoco.core.analysis.IBundleCoverage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static edu.cmu.jacoco.ArgumentsExtractor.*;

public class CoverageDiff {

	private final File classesDirectory;
	private final File sourceDirectory;
	private final File reportDirectory;

	private HTMLWriter writer;
	private CodeDirector director;
	
	private static int numberOfTestSuites;

	public CoverageDiff(final File projectDirectory, File reportDirectory/*, int numberOfTestSuites*/) {
		this.classesDirectory = new File(projectDirectory, "classes");
		this.sourceDirectory = new File(projectDirectory, "src");
		this.reportDirectory = reportDirectory;
		prepareReportDirectory();
		
		numberOfTestSuites = getOptionValues("exec", ",").length;

		this.director = new CodeDirectorImpl(sourceDirectory, this.reportDirectory, new HTMLHighlighter());
	}
	
	private void prepareReportDirectory() {		

		try {
			if (!reportDirectory.exists()) {
				Files.createDirectories(Paths.get(reportDirectory.getPath()));
			}
			
			FileUtils.copyDirectory(new File(".resources"), new File(reportDirectory, ".resources"));
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	}

	public void setWriter(HTMLWriter writer) {
		this.writer = writer;
	}

	
	public static void main(final String[] args) throws IOException {

		if (!extractArguments(args)) return;

		CoverageDiff coverageDiff = new CoverageDiff(new File(getOptionValue("source")),
										  new File(getOptionValue("report"))/*, 
										   args.length - EXEC_DATA_INDEX*/);
	    
	    String[] files = getOptionValues("exec", ",");

		final CoverageAnalyzer analyzer = new CoverageAnalyzer(coverageDiff.classesDirectory);
		final File firstFile = new File(files[0]);
		final File secondFile = new File(files[1]);

		IBundleCoverage firstCoverage = analyzer.analyze(firstFile);
		IBundleCoverage secondCoverage = analyzer.analyze(secondFile);
		IBundleCoverage mergedCoverage = analyzer.analyze(firstFile, secondFile);

        Map<String, Set<String>> classesWithCoverage = new HashMap<>();
        CoverageCalculator.Visitor visitor = (packageName, className, coverageInfo) -> {
            if (coverageInfo.getCoveredBranches() == 0) {
                return;
            }
            if (!classesWithCoverage.containsKey(packageName)) {
                classesWithCoverage.put(packageName, new HashSet<>());
            }
            classesWithCoverage.get(packageName).add(className);
        };
        CoverageCalculator calculator = new CoverageCalculator();
        calculator.setCoverageVisitor(visitor);
		CoverageInfo firstCoverageInfo = calculator.getInfo(firstCoverage);
		CoverageInfo secondCoverageInfo = calculator.getInfo(secondCoverage);
		CoverageInfo mergedCoverageInfo = calculator.getInfo(mergedCoverage);

		coverageDiff.setWriter(new HTMLWriter(coverageDiff.reportDirectory + "/index.html"));
		
		String[] testSuiteTitles = wrapTitles(getOptionValues("title", ","));
		coverageDiff.renderBranchCoverage(testSuiteTitles,
				Arrays.asList(firstCoverageInfo, secondCoverageInfo, mergedCoverageInfo), classesWithCoverage);
		
		coverageDiff.director.generateClassCoverageReport(Arrays.asList(firstCoverage, secondCoverage, mergedCoverage));
		
	}
	

	private static String[] wrapTitles(String[] optionValues) {
		
		int givenTitles = optionValues.length;
		
		if (givenTitles == numberOfTestSuites) return optionValues;
		
		String[] wrapped = new String[numberOfTestSuites];
		
		System.arraycopy(optionValues, 0, wrapped, 0, givenTitles);

		for (int counter = givenTitles; counter < numberOfTestSuites; counter++) {
			wrapped[counter] = "Test Suite " + (counter + 1);
		}
		
		return wrapped;
		
	}

	private void renderBranchCoverage(String[] testSuiteTitles,
                                      List<CoverageInfo> coverageInfoList,
                                      Map<String, Set<String>> classesWithCoverage) {
		
		writer.renderTotalCoverage(coverageInfoList, testSuiteTitles);

		for (String packageName: classesWithCoverage.keySet()) {
		    renderPackageBranchCoverage(packageName, classesWithCoverage.get(packageName), coverageInfoList, testSuiteTitles);
		}
		
		writer.renderReportEnd();
	}

	private void renderPackageBranchCoverage(
			String packageName,
			Set<String> classesNames,
			List<CoverageInfo> coverageInfoList,
			String[] testSuiteTitles
	) {

		writer.renderPackageHeader(packageName, testSuiteTitles);

		List<CoverageInfo> packageCoverages = coverageInfoList.stream()
				.map(info -> getChildCoverageInfo(info, packageName))
				.collect(Collectors.toList());

		for (String className: classesNames) {
			List<CoverageInfo> classCoverages = packageCoverages.stream()
					.map(info -> getChildCoverageInfo(info, className))
					.collect(Collectors.toList());
			HashMap<String, String> options = new HashMap<>();
			options.put("bgcolor", "F7E4E4");
			renderClassBranchCoverage(packageName, className, classCoverages, options);
		}


		renderClassBranchCoverage("", "Total Branch Coverage", packageCoverages, new HashMap<String, String>() {{put("bgcolor", "C3FAF9");}});
		
		writer.renderPackageFooter();
	}

	private CoverageInfo getChildCoverageInfo(CoverageInfo coverageInfo, String childName) {
		Map<String, CoverageInfo> packagesCoverageInfo = coverageInfo.getChilds();
		if (packagesCoverageInfo.containsKey(childName)) {
			return packagesCoverageInfo.get(childName);
		}

		return new CoverageInfo();
	}


	private void renderClassBranchCoverage(String packageName, String className, List<CoverageInfo> classCoverages, HashMap<String, String> options) {
		
		boolean different = classCoverages.get(0).getCoveredBranches() != classCoverages.get(1).getCoveredBranches();
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
