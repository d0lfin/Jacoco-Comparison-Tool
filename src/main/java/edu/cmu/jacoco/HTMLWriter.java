package edu.cmu.jacoco;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import edu.cmu.jacoco.CoverageDiff.Coverage;

public class HTMLWriter {
	
	File file;
	BufferedWriter bw;
	
	public HTMLWriter(String output) {
		try {
			file = new File(output);

			if (!file.exists()) {
				file.createNewFile();
			}

			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			bw = new BufferedWriter(fw);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


	public void renderHeader(List<String> testSuiteTitles) {
		try {
			bw.write("<table width='100%' border='1' cellspacing='0'>");
			bw.write("<tr>");
			
			String s = String.format("<td>%-50s </td> <td>%20s</td><td> %20s</td><td> %20s</td><td>%-50s </td> <td>%20s</td><td> %20s</td>", 
					  "", testSuiteTitles.get(0), testSuiteTitles.get(0) + "%", testSuiteTitles.get(1), testSuiteTitles.get(1) + "%", "Union Coverage", "Union Coverage %");
			bw.write(s);
			
			bw.write("</tr>");
		} catch (IOException e) {
			
			e.printStackTrace();
		}		

	}
	

	public void renderFooter() {
		try {
			bw.write("</table>");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	public void renderTotalCoverage(List<CoverageCalculator.CoverageInfo> totalCoverage, List<String> testSuiteTitles) {
		
		renderHeader(testSuiteTitles);
		
		renderClassHeader("", "Total Lines Coverage", false);
		
		for (CoverageCalculator.CoverageInfo coverageInfo : totalCoverage) {
			renderTestSuitCoverage(coverageInfo, new HashMap<String, String>() {{put("bgcolor", "C3FAF9");}});
		}
		
		renderClassFooter();	
		
		renderFooter();
	}


	public void renderPackageHeader(String title, List<String> testSuiteTitles) {
		String s;
		
		try {
			s = String.format("<p> Package: %s </p>", title);
			bw.write(s);
			
			bw.write("<table width='100%' border='1' cellspacing='0'>");
			bw.write("<tr>");
	
			s = String.format("<td>%-50s </td> <td>%20s</td><td> %20s</td><td> %20s</td><td>%-50s </td> <td>%20s</td><td> %20s</td>", 
					 		  "Class", testSuiteTitles.get(0), testSuiteTitles.get(0) + "%", testSuiteTitles.get(1), testSuiteTitles.get(1) + "%", "Union Coverage", "Union Coverage %");
			bw.write(s);
			bw.write("</tr>");
		
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	

	public void renderPackageFooter() {

		try {
			bw.write("</table>");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

	public void renderClassHeader(String packageName, String title, boolean different) {
				
		try {
			title = title.replace(packageName, "").replace(".", "");
			String path = packageName.concat("/" + title + ".java.html");
			
			if (different) {
				bw.write("<tr bgcolor='#F5F507'>");
			}
			else {
				bw.write("<tr>");
			}
			
			bw.write(String.format("<td width='300px'><a href='%100s'> %-50s</a></td>", path, title));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

	public void renderClassFooter() {
		try {
			bw.write("</tr>");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void renderTestSuitCoverage(CoverageCalculator.CoverageInfo c, Map<String, String> options) {
		
		String s = String.format("<td> %-5d of %-5d </td> <td bgcolor='#%s'> %-7.0f </td> ",
								c.getCoveredLines(),
								c.getTotalLines(),
								options.get("bgcolor"),
								c.getTotalLines() > 0 ? c.getCoveredLines() * 100 / c.getTotalLines() : 0.0);
		try {
			bw.write(s);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	

	public void renderReportEnd() {
		try {
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


}
