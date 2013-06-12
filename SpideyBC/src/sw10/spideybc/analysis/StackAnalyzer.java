package sw10.spideybc.analysis;

import java.util.Iterator;
import java.util.LinkedList;

import sw10.spideybc.build.AnalysisEnvironment;
import sw10.spideybc.program.AnalysisSpecification;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.cfa.CallString;
import com.ibm.wala.ipa.callgraph.propagation.cfa.CallStringContext;
import com.ibm.wala.ipa.callgraph.propagation.cfa.CallStringContextSelector;

public class StackAnalyzer {
	private CallGraph cg;
	
	private AnalysisEnvironment environment;
	private AnalysisResults analysisResults;
	
	
	public StackAnalyzer() {
		this.environment = AnalysisEnvironment.getAnalysisEnvironment();
		this.cg = environment.getCallGraph();
		this.analysisResults = AnalysisResults.getAnalysisResults();
	}
	
	public void analyze() {	
		AnalysisSpecification specification = AnalysisSpecification.getAnalysisSpecification();
		LinkedList<CGNode> entryNodes = specification.getEntryPointCGNodes();

		for(CGNode entryNode : entryNodes) {
			dist(entryNode);
		}
	}
	
	private long dist(CGNode node) {
		long max = -1;
		CGNode maxSuccessor = null;
		long cost = -1;
		CostResultMemory memCost = null;
		Iterator<CGNode> iteratorSuccessors = cg.getSuccNodes(node);
		if(iteratorSuccessors.hasNext()) {
			do{
				CGNode successor = iteratorSuccessors.next();
				CallStringContext h = (CallStringContext)node.getContext();
				CallString m = (CallString)h.get(CallStringContextSelector.CALL_STRING);
				if (CGNodeAnalyzer.doesContainMethod(m.getMethods(), successor.getMethod())) {
					continue;
				}
				memCost = (CostResultMemory)analysisResults.getResultsForNode(node);
				cost = dist(successor) + memCost.getStackCost();
				if(cost > max) {
					maxSuccessor = successor;
					max = cost;
				}
			} while(iteratorSuccessors.hasNext());
			CostResultMemory nodeCost = (CostResultMemory)analysisResults.getResultsForNode(node);
			analysisResults.setNextWorstCaseCallInStack(node, maxSuccessor);
			nodeCost.setAccumStackCost(max);
			return max;
		} else {
			memCost = (CostResultMemory)analysisResults.getResultsForNode(node);
			memCost.setAccumStackCost(memCost.getStackCost());
			return memCost.getStackCost();
		}
	}
}