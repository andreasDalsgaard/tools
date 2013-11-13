package sw10.spideybc.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import sw10.spideybc.analysis.loopanalysis.CFGLoopAnalyzer;
import sw10.spideybc.build.AnalysisEnvironment;
import sw10.spideybc.build.JVMModel;
import sw10.spideybc.program.AnalysisSpecification;
import sw10.spideybc.util.FileScanner;
import sw10.spideybc.util.OutputPrinter;
import sw10.spideybc.util.OutputPrinter.AnnotationType;
import sw10.spideybc.util.annotationextractor.extractor.AnnotationExtractor;
import sw10.spideybc.util.annotationextractor.parser.Annotation;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IBytecodeMethod;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.cfa.CallString;
import com.ibm.wala.ipa.callgraph.propagation.cfa.CallStringContext;
import com.ibm.wala.ipa.callgraph.propagation.cfa.CallStringContextSelector;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.graph.traverse.BFSIterator;

public class Costplus {
	
	private AnalysisSpecification specification;
	private AnalysisEnvironment analysisEnvironment;
	private Map<Integer, ArrayList<Integer>> loopBlocksByHeaderBlockId; // This variable should be rename
	private Map<Integer, CostResultMemory> nodecostlistIdtocost;
	private AnalysisResults analysisResults;
	private AnalysisSpecification analysisSpecification;
	private JVMModel model;
	
	public int costpluss() throws WalaException{
		this.specification = AnalysisSpecification.getAnalysisSpecification();
		this.analysisEnvironment = AnalysisEnvironment.getAnalysisEnvironment();
		//this.model = model;
		this.analysisResults = AnalysisResults.getAnalysisResults();
		this.analysisSpecification = AnalysisSpecification.getAnalysisSpecification();
		
		LinkedList<CGNode> entryCGNodes = specification.getEntryPointCGNodes();	

		for(CGNode entryNode : entryCGNodes) {
			ICostResult results = new CostComputerMemory(specification.getJvmModel()).dfsVisit(entryNode);
			System.out.println(results.getCostScalar());
		}
		return 1;
	}
	 
	public ICostResult dfsVisit(CGNode node) throws WalaException {		
		ICostResult maxCost = null, newCost = null;  
		Iterator<CGNode> list = analysisEnvironment.getCallGraph().getSuccNodes(node);
		nodecostlistIdtocost = new HashMap<Integer, CostResultMemory>();
		
		while (list.hasNext()) {
			CGNode succ = list.next();
			
			newCost = dfsVisit(succ);
			if (maxCost == null  || newCost.getCostScalar() > maxCost.getCostScalar())
				maxCost = newCost;
		}
		System.out.println("Calling nodecost");
		maxCost = nodeCost(node);
		return maxCost;		
	}
		
	public ICostResult nodeCost(CGNode node) {
		ControlFlowGraph<SSAInstruction, ISSABasicBlock> cfg = node.getIR().getControlFlowGraph();
		BFSIterator<ISSABasicBlock> iteratorBFSOrdering = new BFSIterator<ISSABasicBlock>(cfg);
		CostResultMemory cost = new CostResultMemory();
		Memutil memutil = new Memutil();
		CFGLoopAnalyzer loopAnalyzer = CFGLoopAnalyzer.makeAnalyzerForCFG(cfg);
		loopAnalyzer.runDfsOrdering(node.getIR().getControlFlowGraph().entry());
		this.loopBlocksByHeaderBlockId = loopAnalyzer.getLoopHeaderBasicBlocksGraphIds();
		long loopcost = 1;
		
		if (nodecostlistIdtocost != null && nodecostlistIdtocost.containsKey(node.getGraphNodeId())) {
			System.out.println("Cache is used");
			return nodecostlistIdtocost.get(node.getGraphNodeId());
		}
			
		
		while(iteratorBFSOrdering.hasNext()){
			ISSABasicBlock currentBlock = iteratorBFSOrdering.next();
			
			for(SSAInstruction instruction : Iterator2Iterable.make(currentBlock.iterator())) {
				if(instruction instanceof SSAInvokeInstruction) {
					cost.allocationCost += getCostForInstructionInvoke(instruction, node).getCostScalar();
				} else if(memutil.isInstructionInteresting(instruction)) {
					cost.allocationCost += memutil.getCostForInstructionInBlock(instruction, currentBlock).getCostScalar();
				}		
			}
			if (loopBlocksByHeaderBlockId.containsKey(currentBlock.getGraphNodeId()))
				loopcost *= getLoopBoundCost(node, currentBlock); // MORE TEST 
		}
		
		cost.allocationCost *= loopcost; // This is not awesome code
		saveReportData(cost, node);
		nodecostlistIdtocost.put(node.getGraphNodeId(), cost);
		return cost;
	}
	
	public void saveReportData(CostResultMemory cost, CGNode node) {
		/* Save node stack information. Code from getFinalResultsFromContextResultsAndLPSolutions */
		AnalysisResults results = AnalysisResults.getAnalysisResults(); // This is report code
		IMethod method = node.getMethod();
		Set<Integer> lines = new HashSet<Integer>(); // This is use to highlight code stump in report data.
		
		if(method instanceof ShrikeBTMethod) {		
			ShrikeBTMethod shrikeMethod = (ShrikeBTMethod)method;
			cost.setStackUnitSize(model.oneUnitSize);
			cost.setMaxLocals(shrikeMethod.getMaxLocals());
			cost.setMaxStackHeight(shrikeMethod.getMaxStackHeight()-2);	//Substract two since Shrike adds two and assumes this makes the number of entries not word - we actually want the number of words and what Shrike does is wrong!
		}
		
		if(node.getMethod().getDeclaringClass().getClassLoader().getName().toString().equals("Application")) {
			IBytecodeMethod bytecodeMethod = (IBytecodeMethod)method;
			BFSIterator<ISSABasicBlock> iteratorBFSOrdering = new BFSIterator<ISSABasicBlock>(node.getIR().getControlFlowGraph());
			while(iteratorBFSOrdering.hasNext()){
				ISSABasicBlock currentBlock = iteratorBFSOrdering.next();
				
				try {
					if(currentBlock.getFirstInstructionIndex() >= 0) {
						int line = bytecodeMethod.getLineNumber(bytecodeMethod.getBytecodeIndex(currentBlock.getFirstInstructionIndex()));
						lines.add(line);
					}
				} catch(InvalidClassFileException e) {
					System.err.println(e.getMessage());
				}
			}
		}
		
		if(analysisSpecification.isEntryPointCGNode(node)) {
			analysisResults.addReportData(FileScanner.getFullPath(method.getDeclaringClass().getSourceFileName()), lines, node, cost);
		}
		
		if (node.getMethod().getDeclaringClass().getClassLoader().getName().toString().equals("Application")) {
			analysisResults.addNonEntryReportData(FileScanner.getFullPath(method.getDeclaringClass().getSourceFileName()), lines, node);
		}
		
		//cost.worstcaseReferencesMethods.add(node);
		
		results.saveResultForNode(node, cost);
	}
	
	public long getLoopBoundCost(CGNode node, ISSABasicBlock currentBlock){
		AnnotationExtractor extractor = AnnotationExtractor.getAnnotationExtractor();
		Map<Integer, Annotation> annotationByLineNumber = extractor.getAnnotations(currentBlock.getMethod());
		int lineNumber = 0;
		String loopbound = "";
			
		try {
			IBytecodeMethod bytecodeMethod = (IBytecodeMethod)node.getMethod();
			lineNumber = bytecodeMethod.getLineNumber(bytecodeMethod.getBytecodeIndex(currentBlock.getFirstInstructionIndex()));
			if (annotationByLineNumber == null || (!isForLoop(lineNumber, annotationByLineNumber) || !isWhileLoop(lineNumber, annotationByLineNumber))) {
				OutputPrinter.printAnnotationError(AnnotationType.AnnotationLoop, currentBlock.getMethod(), lineNumber);
			} else { 
				if (isForLoop(lineNumber,annotationByLineNumber)) {
					loopbound = annotationByLineNumber.get(lineNumber).getAnnotationValue(); // This return a string. This should instead be a int or long
				} else if (isWhileLoop(lineNumber,annotationByLineNumber)) {
					loopbound = annotationByLineNumber.get(lineNumber - 1).getAnnotationValue(); // This return a string. This should instead be a int or long
				}
				return Integer.parseInt(loopbound);
			}
			
		} catch (InvalidClassFileException e) {
		}    								
		return 1; // This line will be execute if "No loop bound detected in" a handler
	}
		
	public ICostResult getCostForInstructionInvoke(SSAInstruction instruction, CGNode node){
		SSAInvokeInstruction inst = (SSAInvokeInstruction)instruction;
		CallSiteReference callSiteRef = inst.getCallSite();
		Set<CGNode> possibleTargets = analysisEnvironment.getCallGraph().getPossibleTargets(node, callSiteRef);
		ICostResult maximumResult = null;
		ICostResult tempResult = null;
		CallStringContext csContext = (CallStringContext)node.getContext();
		CallString callString = (CallString)csContext.get(CallStringContextSelector.CALL_STRING);
	
		for(CGNode target : Iterator2Iterable.make(possibleTargets.iterator())) {
			if (CGNodeAnalyzer.doesContainMethod(callString.getMethods(), target.getMethod())) { // Use of context-sensitivity to eliminate recursion
				continue;
			}
			tempResult = nodeCost(target);
			if(maximumResult == null || tempResult.getCostScalar() > maximumResult.getCostScalar())
				maximumResult = tempResult;
		}
		
		return maximumResult;
	}
	
	public boolean isForLoop(int line, Map<Integer, Annotation> annotationByLineNumber) {
		return (annotationByLineNumber.containsKey(line) ? true : false);
	}
	
	public boolean isWhileLoop(int line, Map<Integer, Annotation> annotationByLineNumber) {
		return (annotationByLineNumber.containsKey(line) ? true : false);
	}
}
