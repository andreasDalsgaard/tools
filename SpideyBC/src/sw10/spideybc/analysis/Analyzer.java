package sw10.spideybc.analysis;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.Map.Entry;

import sw10.spideybc.build.JVMModel;
import sw10.spideybc.program.AnalysisSpecification;
import sw10.spideybc.program.AnalysisSpecification.AnalysisType;
import sw10.spideybc.reports.ReportGenerator;
import sw10.spideybc.util.OutputPrinter;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.util.WalaException;

public class Analyzer {

	private AnalysisSpecification specification;
	private ICostComputer<ICostResult> costComputer;
	private StackAnalyzer stackAnalyzer;
	
	private Analyzer() {
		this.specification = AnalysisSpecification.getAnalysisSpecification();
		this.stackAnalyzer = new StackAnalyzer(specification.getJvmModel());
	}

	public Analyzer(AnalysisSpecification spec) {
		this.specification = spec;
		
		if ( specification.getTypeOfAnalysisPerformed() != AnalysisType.ALLOCATIONS) {
			this.stackAnalyzer = new StackAnalyzer(specification.getJvmModel());
		}
	}

	
	public static Analyzer makeAnalyzer() {
		return new Analyzer();
	}

	public void start(Class<? extends ICostComputer<ICostResult>> costComputerType) throws InstantiationException, IllegalAccessException, IllegalArgumentException, WalaException, IOException, SecurityException, InvocationTargetException, NoSuchMethodException {
		this.costComputer = costComputerType.getDeclaredConstructor(JVMModel.class).newInstance(specification.getJvmModel());

		LinkedList<CGNode> entryCGNodes = specification.getEntryPointCGNodes();	

		for(CGNode entryNode : entryCGNodes) {
			//OutputPrinter.printInfo("Starting entry node " + entryNode.getMethod().toString());
			//ICostResult results = new CGNodeAnalyzer(entryNode, costComputer).analyzeNode();
			// Test code
			//AnalysisResults result = AnalysisResults.getAnalysisResults();;
			ICostResult results = new CostComputerMemory(specification.getJvmModel()).dfsVisit(entryNode);
			//ICostResult res = ;
			//result.saveResultForNode(entryNode,res); // Try to use the code which is made to make a report. 
			// Test code
			CostResultMemory memRes = (CostResultMemory)results;				
			OutputPrinter.printInfo("Worst case allocation for " + entryNode.getMethod().toString() + ":\t" + results.getCostScalar());
			for(Entry<TypeName, Integer> i : memRes.aggregatedCountByTypename.entrySet()) {
				OutputPrinter.printInfo("\t TYPE_NAME\t" + i.getKey().toString() + "\tCOUNT " + i.getValue());
			}
		}
		
		if ( specification.getTypeOfAnalysisPerformed() != AnalysisType.ALLOCATIONS) {
			stackAnalyzer.analyze();
		}
		
		if ( specification.getShouldGenerateAnalysisReports() == true) {
			ReportGenerator gen = new ReportGenerator();
			gen.Generate(AnalysisResults.getAnalysisResults().getReportEntries());
		}
	}
}