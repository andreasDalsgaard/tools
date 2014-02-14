package sw10.spideybc.program;

import java.util.HashMap;
import java.util.LinkedList;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.strings.StringStuff;

import sw10.spideybc.build.AnalysisEnvironment;
import sw10.spideybc.build.JVMModel;
import sw10.spideybc.util.RunConfiguration;

public class AnalysisSpecification {
	private static AnalysisSpecification singletonObject;
	public enum AnalysisType { ALL, STACK, ALLOCATIONS };
	
	private String applicationJar;
	private boolean jarIncludesStdLibraries;
	private String sourceFilesRootDir;
	private String outputDir;
	private String mainClass;
	private String[] entryPointSignatures;
	private AnalysisType analysisType;
	private boolean generateAnalysisReports;
	private JVMModel jvmModel;
	private String jvmModelString;
	private RunConfiguration runConfiguration;
	private int runAnalyzer;
	
	private LinkedList<CGNode> entryPointCGNodes;
	
	public AnalysisSpecification(AnalysisType aType, boolean generateAnalysisReport, CGNode entryPoint) {
		this.analysisType = aType;
		this.generateAnalysisReports = generateAnalysisReport;
		this.entryPointCGNodes = new LinkedList<CGNode>();
		this.entryPointCGNodes.add(entryPoint);
	}
	
	private AnalysisSpecification() {
		this.analysisType = AnalysisType.ALL;
		this.generateAnalysisReports = true;
		this.entryPointCGNodes = new LinkedList<CGNode>();
	}
	
	public static synchronized AnalysisSpecification getAnalysisSpecification() {
		if (singletonObject == null) {
			singletonObject = new AnalysisSpecification();
		}
		return singletonObject;
	}
	
	public String getApplicationJar() {
		return applicationJar;
	}
	
	public void setApplicationJar(String applicationJar) {
		this.applicationJar = applicationJar;
	}
	
	public void setJarIncludesStdLibraries(boolean jarIncludesStdLibraries) {
		this.jarIncludesStdLibraries = jarIncludesStdLibraries;
	}
	
	public boolean getJarIncludesStdLibraries() {
		return jarIncludesStdLibraries;
	}
	
	public String getSourceFilesRootDir() {
		return sourceFilesRootDir;
	}
	
	public void setSourceFilesRootDir(String sourceFilesRootDir) {
		this.sourceFilesRootDir = sourceFilesRootDir;
	}
	
	public String getOutputDir() {
		return outputDir;
	}
	
	public void setOutputDirectoryForReports(String outputDir) {
		this.outputDir = outputDir;
	}
	
	public String getMainClass() {
		return mainClass;
	}
	
	public void setMainClass(String mainClass) {
		this.mainClass = mainClass;
	}
	
	public String[] getEntryPointSignatures() {
		return entryPointSignatures;
	}
	
	public void setEntryPointSignatures(String methods) {
		entryPointSignatures = methods.split(",");		
	}
	
	public AnalysisType getTypeOfAnalysisPerformed() {
		return analysisType;
	}
	
	public void setTypeOfAnalysisPerformed(AnalysisType typeOfAnalysisPerformed) {
		this.analysisType = typeOfAnalysisPerformed;
	}
	
	public Boolean getShouldGenerateAnalysisReports() {
		return generateAnalysisReports;
	}
	
	public void setShouldGenerateAnalysisReports(boolean generateAnalysisReports) {
		this.generateAnalysisReports = generateAnalysisReports;
	}

	public JVMModel getJvmModel() {
		return jvmModel;
	}

	public void setJvmModel(JVMModel jvmModel) {
		this.jvmModel = jvmModel;
	}
	
	public void setJvmModelString(String jvmModel) {
		this.jvmModelString = jvmModel;
	}
	
	public String getJvmModelString() {
		return jvmModelString;
	}
	
	public LinkedList<CGNode> getEntryPointCGNodes() {
		return entryPointCGNodes;
	}
	
	public void setRunConfiguration(RunConfiguration runConfiguration) {
		this.runConfiguration = runConfiguration;
	}
	
	public RunConfiguration getRunConfiguration() {
		return runConfiguration;
	}
		
	public LinkedList<CGNode> setEntryPointCGNodes() {
		AnalysisEnvironment environment = AnalysisEnvironment.getAnalysisEnvironment();
		String[] entryPointSignatures = getEntryPointSignatures();
		if(entryPointSignatures == null) {
			CGNode entryNode = environment.getCallGraph().getEntrypointNodes().iterator().next();
			entryPointCGNodes.add(entryNode);
		} else {
			for(String entryPoint : entryPointSignatures) {
				MethodReference mr = StringStuff.makeMethodReference(entryPoint);
				CGNode entryNode = environment.getCallGraph().getNodes(mr).iterator().next();
				entryPointCGNodes.add(entryNode);
			}	
		}
		return entryPointCGNodes;
	}
	
	public boolean isEntryPointCGNode(CGNode cgNode) {
		return entryPointCGNodes.contains(cgNode);
	}
	
	public void setrunAnalyzer(int num) {
		this.runAnalyzer = num;
	}
	
	public int getrunAnalyzer() {
		return this.runAnalyzer;
	}
}