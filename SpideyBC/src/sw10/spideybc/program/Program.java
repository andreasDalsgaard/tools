package sw10.spideybc.program;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import sw10.spideybc.analysis.AnalysisResults;
import sw10.spideybc.analysis.Analyzer;
import sw10.spideybc.analysis.CostComputerMemory;
import sw10.spideybc.analysis.PlusAnalyzer;
import sw10.spideybc.analysis.ICostComputer;
import sw10.spideybc.analysis.ICostResult;
import sw10.spideybc.analysis.StackAnalyzer;
import sw10.spideybc.build.AnalysisEnvironmentBuilder;
import sw10.spideybc.build.JVMModel;
import sw10.spideybc.program.AnalysisSpecification.AnalysisType;
import sw10.spideybc.reports.ReportGenerator;
import sw10.spideybc.util.Config;
import sw10.spideybc.util.OutputPrinter;
import sw10.spideybc.util.RunConfiguration;

import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.io.CommandLine;

public class Program {

	public static void main(String[] args) throws IOException, IllegalArgumentException, CancelException, InstantiationException, IllegalAccessException, WalaException, SecurityException, InvocationTargetException, NoSuchMethodException {		
		AnalysisSpecification specification = parseCommandLineArguments(args); // Gets the arg
		AnalysisEnvironmentBuilder.makeFromSpecification(specification); // Make/gets the cfg and other wala stuff
		specification.setEntryPointCGNodes(); // set the entry point in the cg
		
		// This is here we have our two analyser (lpanalyzer, plusanalyzer)
		switch (specification.getrunAnalyzer()) {
		case 1:
			Analyzer analyzer = Analyzer.makeAnalyzer();
			analyzer.start((Class<? extends ICostComputer<ICostResult>>)CostComputerMemory.class);
			break;
		case 2:
			PlusAnalyzer.costpluss();
			break;
			
		default:
			break;
		}

		// Instead of calling report in our analyzer tool. Then call it here.
		// To make report of data.
		StackAnalyzer stackAnalyzer = new StackAnalyzer(specification.getJvmModel());
		if ( specification.getTypeOfAnalysisPerformed() != AnalysisType.ALLOCATIONS) {
			stackAnalyzer.analyze();
		}
		
		if ( specification.getShouldGenerateAnalysisReports() == true) {
			ReportGenerator gen = new ReportGenerator();
			gen.Generate(AnalysisResults.getAnalysisResults().getReportEntries());
		}
	}
	
	private static AnalysisSpecification parseCommandLineArguments(String[] args) {
		Properties properties = CommandLine.parse(args);
		
		AnalysisSpecification specification = AnalysisSpecification.getAnalysisSpecification();		
		
		/* Required arguments */
		String jvmModel = properties.getProperty(Config.COMMANDLINE_JVM_MODEL);
		String application = properties.getProperty(Config.COMMANDLINE_APPLICATION);
		String jarIncludesStdLibraries = properties.getProperty(Config.COMMANDLINE_JARINCLUDESSTDLIBRARIES);
		String sourceFilesRootDir = properties.getProperty(Config.COMMANDLINE_SOURCES);
		String outputDir = properties.getProperty(Config.COMMANDLINE_OUTPUT);
		String mainClass = properties.getProperty(Config.COMMANDLINE_MAINCLASS);
		
		/* Optional arguments */
		String analysis = properties.getProperty(Config.COMMANDLINE_ANALYSIS);
		String reports = properties.getProperty(Config.COMMANDLINE_REPORTS);
		String entryPoints = properties.getProperty(Config.COMMANDLINE_ENTRYPOINTS);
		String runConfiguration = properties.getProperty(Config.COMMANDLINE_RUNCONFIGURATION);
		String runAnalyzer = properties.getProperty(Config.COMMANDLINE_RUNANALYZER);
				
		if(jvmModel == null || application == null 
				|| jarIncludesStdLibraries == null || sourceFilesRootDir == null 
				|| outputDir == null || mainClass == null) {
			
			StringBuilder nulls = new StringBuilder();
			
			if(jvmModel == null)
				nulls.append(Config.COMMANDLINE_JVM_MODEL + "\n");
			if(application == null)
				nulls.append(Config.COMMANDLINE_APPLICATION + "\n");
			if(jarIncludesStdLibraries == null)
				nulls.append(Config.COMMANDLINE_JARINCLUDESSTDLIBRARIES + "\n");
			if(sourceFilesRootDir == null)
				nulls.append(Config.COMMANDLINE_SOURCES + "\n");
			if(outputDir == null)
				nulls.append(Config.COMMANDLINE_OUTPUT + "\n");
			if(mainClass == null)
				nulls.append(Config.COMMANDLINE_MAINCLASS + "\n");
			
			printCommandLineUsage(nulls);		
		} else {
			specification.setJvmModel(JVMModel.makeFromJson(jvmModel));
			specification.setJvmModelString(jvmModel);
			specification.setApplicationJar(application);
			specification.setJarIncludesStdLibraries(jarIncludesStdLibraries.equalsIgnoreCase("true") ? true : false);
			specification.setSourceFilesRootDir(sourceFilesRootDir);
			specification.setOutputDirectoryForReports(outputDir);
				
			if(mainClass != null) {
				specification.setMainClass(mainClass);
			}
			
			if(analysis != null) {
				AnalysisType type = null;
				if(analysis.equalsIgnoreCase("all")) {
					type = AnalysisType.ALL;
				} else if(analysis.equalsIgnoreCase("allocations")) {
					type = AnalysisType.ALLOCATIONS;
				} else if(analysis.equalsIgnoreCase("stack")) {
					type = AnalysisType.STACK; 
				} else {
					printCommandLineUsage(new StringBuilder(analysis + " is invalid for " + Config.COMMANDLINE_ANALYSIS));
				}
				
				specification.setTypeOfAnalysisPerformed(type);
			}

			if(reports != null) {
				boolean report = false;
				if(reports.equalsIgnoreCase("true")) {
					report = true;
				} else if(reports.equalsIgnoreCase("false")) {
					report = false;
				} else {
					printCommandLineUsage(new StringBuilder(report + " is invalid for " + Config.COMMANDLINE_REPORTS));
				}
				
				specification.setShouldGenerateAnalysisReports(report);
			}
			
			if(entryPoints != null)
				specification.setEntryPointSignatures(entryPoints);
			
			if(runConfiguration != null) {
				RunConfiguration config;
				if(runConfiguration.equalsIgnoreCase("debug")) {
					config = RunConfiguration.DEBUG;
					specification.setRunConfiguration(config);
				}	
				else if(runConfiguration.equalsIgnoreCase("deploy")) {
					config = RunConfiguration.DEPLOY;
					specification.setRunConfiguration(config);
				}
				else{
					OutputPrinter.printError(runConfiguration + " is invalid for " + Config.COMMANDLINE_RUNCONFIGURATION);
					System.exit(1);
				}
			} else {
				specification.setRunConfiguration(RunConfiguration.DEBUG);
			}
			
			if(runAnalyzer != null) {
				if(runAnalyzer.equalsIgnoreCase("lpcost")) {
					specification.setrunAnalyzer(1); // 1 is the lpcost in the swicth case
				}
				else if(runAnalyzer.equalsIgnoreCase("pluscost")) {
					specification.setrunAnalyzer(2); // 2 is the lpcost in the swicth case
				}
			}
		}	

		return specification;
	}
	
	public static void dumpCommandLineArguments(String[] args) {
		Properties properties = CommandLine.parse(args);
		
		Set<Entry<Object, Object>> entries = properties.entrySet();
		for(Entry<Object, Object> entry : entries) {
			System.out.println("KEY: " + entry.getKey() + " VALUE: " + entry.getValue());
		}
	}
	
	public static void printCommandLineUsage(StringBuilder nulls) {
		
		OutputPrinter.printError("The following arguments were null: \n" + nulls);
		OutputPrinter.printError("Usage: \n" +
				"Required\n" +
				"\t-jvm_model <file>.json : the corresponding JVM model for analysis, see documentation for format\n" +
				"\t-application <file>.jar : jar file containing the application to be analysed\n" +
				"\t-source_files_root_dir <directory> : root directory for source files for the application\n" +
				"\t-output_dir <directory> : directory for generated reports files\n" +
				"\t-entry_points <package.type> : the type containing main method\n" +
				"Optional\n" +
	      		"\t-analysis all|stack|allocations: type of analysis performed - defaults to all\n" +
	      		"\t-reports true|false : specifies if full reports should be generated for the output directory\n +" +
	      		"\t-run_configuration debug|deploy : report specific for determining whether or not reports should be portable\n");
		System.exit(1);
	}
}