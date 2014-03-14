package sw10.spideybc.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Stack;

import sw10.spideybc.analysis.ICostResult.ResultType;
import sw10.spideybc.analysis.loopanalysis.CFGLoopAnalyzer;
import sw10.spideybc.build.AnalysisEnvironment;
import sw10.spideybc.build.JVMModel;
import sw10.spideybc.program.AnalysisSpecification;
import sw10.spideybc.util.FileScanner;
import sw10.spideybc.util.OutputPrinter;
import sw10.spideybc.util.Util;
import sw10.spideybc.util.OutputPrinter.AnnotationType;
import sw10.spideybc.util.OutputPrinter.ModelType;
import sw10.spideybc.util.annotationextractor.extractor.AnnotationExtractor;
import sw10.spideybc.util.annotationextractor.parser.Annotation;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.ShrikeCFG;
import com.ibm.wala.cfg.ShrikeCFG.BasicBlock;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IBytecodeMethod;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.impl.ExplicitCallGraph;
import com.ibm.wala.ipa.callgraph.impl.ExplicitCallGraph.ExplicitNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.cfa.CallString;
import com.ibm.wala.ipa.callgraph.propagation.cfa.CallStringContext;
import com.ibm.wala.ipa.callgraph.propagation.cfa.CallStringContextSelector;
import com.ibm.wala.shrikeBT.IInstruction;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.graph.traverse.BFSIterator;
import com.ibm.wala.util.intset.IntSet;

public class PlusAnalyzer {

	private static AnalysisSpecification specification;
	private static AnalysisEnvironment analysisEnvironment;
	private Map<Integer, ArrayList<Integer>> loopBlocksByHeaderBlockId; // This
																		// variable
																		// should
																		// be
																		// rename
	private Map<Integer, CostResultMemory> nodecostlistIdtocost;
	private Map<Integer, HashMap<String, ICostResult>> dfsvisitlist; // To allow
																		// recursion
	private static AnalysisResults analysisResults;
	private static AnalysisSpecification analysisSpecification;
	private static JVMModel model;
	private static Map<String, ScopeCost> scopeTotalCost;

	public PlusAnalyzer() {
		nodecostlistIdtocost = new HashMap<Integer, CostResultMemory>();
		dfsvisitlist = new HashMap<Integer, HashMap<String, ICostResult>>();
	}

	public static PlusAnalyzer costpluss() throws WalaException {
		specification = AnalysisSpecification.getAnalysisSpecification();
		analysisEnvironment = AnalysisEnvironment.getAnalysisEnvironment();
		model = specification.getJvmModel();
		analysisResults = AnalysisResults.getAnalysisResults();
		analysisSpecification = AnalysisSpecification
				.getAnalysisSpecification();
		scopeTotalCost = new HashMap<String, ScopeCost>();

		LinkedList<CGNode> entryCGNodes = specification.getEntryPointCGNodes();
		ScopeCost.init(analysisEnvironment.getClassHierarchy());
		for (CGNode entryNode : entryCGNodes) {
			// ICostResult results = new
			// CostComputerMemory(specification.getJvmModel()).dfsVisit(entryNode);
			Stack<String> scopeStack = new Stack<String>();
			scopeStack.add("immortal");
			HashMap<String, ICostResult> results = new PlusAnalyzer().dfsVisit(
					entryNode, scopeStack, new HashMap<String, Integer>());
			for (String s : results.keySet())
				System.out.println("Scope Sum" +
						"" +
						": " + s + " "
						+ results.get(s).getCostScalar());
		}
		return null;
	}

	public HashMap<String, ICostResult> dfsVisit(CGNode node,
			Stack<String> scopeStack, HashMap<String, Integer> recursionMap)
			throws WalaException {
		int recurStart = 0;
		
		if (dfsvisitlist.containsKey(node.getGraphNodeId())) {			
			return dfsvisitlist.get(node.getGraphNodeId());
		} else if (this.model.recursionLimitMap
				.get(node.getMethod().toString()) != null) {
			if (recursionMap.get(node.getMethod().toString()) != null) {		
								
				if (recursionMap.get(node.getMethod().toString()).equals(this.model.recursionLimitMap.get(node.getMethod().toString())))
					return null;				

				recursionMap.put(node.getMethod().toString(),recursionMap.get(node.getMethod().toString())+1);
				System.out.println("Recursion step " + recursionMap.get(node.getMethod().toString()) + " "
						+ node.getMethod());
			} else {
				System.out.println("Recursion start" + node.getMethod());
				recurStart = 1;
				recursionMap.put(node.getMethod().toString(), 1);
			}
		}

		//System.out.println("dfs" + node.getMethod());
		ICostResult maxCost = null, newCost = null;
		HashMap<String, ICostResult> newCostMap, maxCostMap = new HashMap<String, ICostResult>();
		Iterator<CGNode> list = analysisEnvironment.getCallGraph()
				.getSuccNodes(node);

		String thisScope = scopeStack.peek();

		while (list.hasNext()) {
			CGNode succ = list.next();
			String scopeName = ScopeCost.getScopeName(node, succ, scopeStack);

			if (scopeStack.peek() != scopeName) { // scope changed				
				System.out.println("EntryPoint: "+node.getMethod()); //FIXME for benchmarks
				scopeStack.add(scopeName);
				newCostMap = dfsVisit(succ, scopeStack, recursionMap);
				if (newCostMap == null) // Recursion
					newCostMap = new HashMap<String, ICostResult>();

				scopeStack.pop();
				if (newCostMap.get(thisScope) != null) {
					newCost = newCostMap.get(thisScope);
				} else {
					newCost = new CostResultMemory();
					newCostMap.put(thisScope, newCost);
				}
			} else {
				newCostMap = dfsVisit(succ, scopeStack, recursionMap);
				if (newCostMap == null) // Recursion
					newCostMap = new HashMap<String, ICostResult>();

				if (newCostMap.get(thisScope) == null) {
					newCost = new CostResultMemory();
					newCostMap.put(thisScope, newCost);
				} else {
					newCost = newCostMap.get(thisScope);					
				}
			}
			

			if (maxCost == null
					|| newCost.getCostScalar() > maxCost.getCostScalar())
				maxCost = newCost;

			maxCostMap = getMaxCostMap(maxCostMap, newCostMap);
		}
		// System.out.println("Calling nodecost:"+node+" "+scopeStack);
		maxCost = nodeCost(node);
		addCost(maxCost, thisScope);
		CostResultMemory previous = (CostResultMemory) maxCostMap.get(thisScope);
		if (previous != null)
		{
			previous.allocationCost += maxCost.getCostScalar();
			maxCostMap.put(thisScope,previous);
		} else {
			maxCostMap.put(thisScope, maxCost);
		}
		
		this.dfsvisitlist.put(node.getGraphNodeId(), maxCostMap);
		if ( recursionMap.get(node.getMethod().toString()) != null)
			recursionMap.put(node.getMethod().toString(),recursionMap.get(node.getMethod().toString())-1);
		
		if (recurStart == 1){
			
			ICostResult r = maxCostMap.get(thisScope);
			
			System.out.println("Current method:"+maxCost.getCostScalar()+ " sum:"+r.getCostScalar());
		}
		return maxCostMap;
	}

	private HashMap<String, ICostResult> getMaxCostMap(
			HashMap<String, ICostResult> maxCostMap,
			HashMap<String, ICostResult> newCostMap) {
		Set<String> ks = newCostMap.keySet();
		Set<String> ks2 = maxCostMap.keySet();
		HashSet<String> hks = new HashSet<String>(ks);
		hks.addAll(ks2);		
	
		for (String s : ks) {
			if (maxCostMap.get(s) == null) {
				maxCostMap.put(s, newCostMap.get(s));				
			}  else if (newCostMap.get(s) == null) {
				newCostMap.put(s, maxCostMap.get(s));
			} else {
				if (maxCostMap.get(s).getCostScalar() < newCostMap.get(s).getCostScalar())
					maxCostMap.put(s, newCostMap.get(s));
			}
		}

		return maxCostMap;
	}

	private void addCost(ICostResult maxCost, String thisScope) {
		ScopeCost cost;
		if (scopeTotalCost.get(thisScope) != null) // update cost
		{
			cost = scopeTotalCost.get(thisScope);
			cost.totalCost = maxCost.getCostScalar();
		} else {
			cost = new ScopeCost();
			cost.totalCost = maxCost.getCostScalar();
			scopeTotalCost.put(thisScope, cost);
		}
	}

	public ICostResult nodeCost(CGNode node) {
		if (nodecostlistIdtocost.containsKey(node.getGraphNodeId())) {
			return nodecostlistIdtocost.get(node.getGraphNodeId());
		}

		CostResultMemory cost = new CostResultMemory();

		// In case node represent a native function
		if (node.getIR() == null) {

			if (this.model.nativeFunctionMap.get(node.getMethod()
					.getSignature()) != null) {
				cost.allocationCost = this.model.nativeFunctionMap.get(node
						.getMethod().getSignature());
				saveReportData(cost, node);
				nodecostlistIdtocost.put(node.getGraphNodeId(), cost);
				return cost;
			} else {
				Util.error("Missing native function definition for: "
						+ node.getMethod().getSignature());
			}
		}

		ControlFlowGraph<SSAInstruction, ISSABasicBlock> cfg = node.getIR()
				.getControlFlowGraph();
		BFSIterator<ISSABasicBlock> iteratorBFSOrdering = new BFSIterator<ISSABasicBlock>(
				cfg);
		Memutil memutil = new Memutil();
		CFGLoopAnalyzer loopAnalyzer = CFGLoopAnalyzer.makeAnalyzerForCFG(cfg);
		loopAnalyzer.runDfsOrdering(node.getIR().getControlFlowGraph().entry());
		this.loopBlocksByHeaderBlockId = loopAnalyzer
				.getLoopHeaderBasicBlocksGraphIds();
		long loopcost = 1;

		while (iteratorBFSOrdering.hasNext()) {
			ISSABasicBlock currentBlock = iteratorBFSOrdering.next();

			for (SSAInstruction instruction : Iterator2Iterable
					.make(currentBlock.iterator())) {
				if (instruction instanceof SSAInvokeInstruction) {

					if (node.getMethod().isSynthetic()) {
						System.out.println(node);
						System.out.println(node.getMethod().isAbstract());
						System.out.println(getCostForInstructionInvoke(
								instruction, node));
						cost.allocationCost += 0; // FIXME: This is a workaround
													// but potentially unsafe
					} else {
						if (getCostForInstructionInvoke(instruction, node) != null)
							cost.allocationCost += getCostForInstructionInvoke(
									instruction, node).getCostScalar();
						else
							;
					}
				} else if (memutil.isInstructionInteresting(instruction)) {
					cost.allocationCost += getCostForInstructionInBlock(
							instruction, currentBlock).getCostScalar();
				}
			}
			if (loopBlocksByHeaderBlockId.containsKey(currentBlock
					.getGraphNodeId()))
				loopcost *= getLoopBoundCost(node, currentBlock); // MORE TEST
		}

		cost.allocationCost *= loopcost; // This is not awesome code
		saveReportData(cost, node);
		nodecostlistIdtocost.put(node.getGraphNodeId(), cost);
		return cost;
	}

	public void saveReportData(CostResultMemory cost, CGNode node) {
		/*
		 * Save node stack information. Code from
		 * getFinalResultsFromContextResultsAndLPSolutions
		 */
		AnalysisResults results = AnalysisResults.getAnalysisResults(); // This
																		// is
																		// report
																		// code
		IMethod method = node.getMethod();
		Set<Integer> lines = new HashSet<Integer>(); // This is use to highlight
														// code stump in report
														// data.
		IBytecodeMethod selector = null;

		if (method instanceof ShrikeBTMethod) {
			ShrikeBTMethod shrikeMethod = (ShrikeBTMethod) method;
			if (!method.isNative()) {
				cost.setStackUnitSize(model.oneUnitSize);
				cost.setMaxLocals(shrikeMethod.getMaxLocals());
				cost.setMaxStackHeight(shrikeMethod.getMaxStackHeight() - 2); // Substract
																				// two
																				// since
																				// Shrike
																				// adds
																				// two
																				// and
																				// assumes
																				// this
																				// makes
																				// the
																				// number
																				// of
																				// entries
																				// not
																				// word
																				// -
																				// we
																				// actually
																				// want
																				// the
																				// number
																				// of
																				// words
																				// and
																				// what
																				// Shrike
																				// does
																				// is
																				// wrong!
			} else {
				// https://www.artima.com/insidejvm/ed2/jvm9.html
				cost.setStackUnitSize(0);
				cost.setMaxLocals(0);
				cost.setMaxStackHeight(0);
			}
		}

		if (node.getMethod().getDeclaringClass().getClassLoader().getName()
				.toString().equals("Application")) {
			IBytecodeMethod bytecodeMethod = (IBytecodeMethod) method;
			BFSIterator<ISSABasicBlock> iteratorBFSOrdering = new BFSIterator<ISSABasicBlock>(
					node.getIR().getControlFlowGraph());
			while (iteratorBFSOrdering.hasNext()) {
				ISSABasicBlock currentBlock = iteratorBFSOrdering.next();

				try {
					if (currentBlock.getFirstInstructionIndex() >= 0) {
						int line = bytecodeMethod.getLineNumber(bytecodeMethod
								.getBytecodeIndex(currentBlock
										.getFirstInstructionIndex()));
						selector = bytecodeMethod;
						lines.add(line);
					}
				} catch (InvalidClassFileException e) {
					System.err.println(e.getMessage());
				}
			}
		}

		if (analysisSpecification.isEntryPointCGNode(node)) {
			analysisResults.addReportData(FileScanner.getFullPath(method
					.getDeclaringClass().getSourceFileName()), lines, node,
					cost);
		}

		if (node.getMethod().getDeclaringClass().getClassLoader().getName()
				.toString().equals("Application")) {
			if (method.getDeclaringClass().getSourceFileName() == null)
				Util.error("Currently private classes are not supported, missing file for: "
						+ method.getDeclaringClass());

			analysisResults.addNonEntryReportData(
					FileScanner.getFullPath(method.getDeclaringClass()
							.getSourceFileName()), lines, node);
		}

		// cost.worstcaseReferencesMethods.add(node);

		results.saveResultForNode(node, cost);
	}

	public long getLoopBoundCost(CGNode node, ISSABasicBlock currentBlock) {
		AnnotationExtractor extractor = AnnotationExtractor
				.getAnnotationExtractor();
		Map<Integer, Annotation> annotationByLineNumber = extractor
				.getAnnotations(currentBlock.getMethod());
		int lineNumber = 0;
		String loopbound = "";

		try {
			IBytecodeMethod bytecodeMethod = (IBytecodeMethod) node.getMethod();

			if (currentBlock.getFirstInstructionIndex() == -1) {
				System.out.println("Bytecode index -1");
				System.out.println(node);
				return 1;
			}

			lineNumber = bytecodeMethod.getLineNumber(bytecodeMethod
					.getBytecodeIndex(currentBlock.getFirstInstructionIndex()));
			if (annotationByLineNumber == null
					|| (!isForLoop(lineNumber, annotationByLineNumber) || !isWhileLoop(
							lineNumber, annotationByLineNumber))) {
				OutputPrinter.printAnnotationError(
						AnnotationType.AnnotationLoop,
						currentBlock.getMethod(), lineNumber);
			} else {
				if (isForLoop(lineNumber, annotationByLineNumber)) {
					loopbound = annotationByLineNumber.get(lineNumber)
							.getAnnotationValue(); // This return a string. This
													// should instead be a int
													// or long
				} else if (isWhileLoop(lineNumber, annotationByLineNumber)) {
					loopbound = annotationByLineNumber.get(lineNumber - 1)
							.getAnnotationValue(); // This return a string. This
													// should instead be a int
													// or long
				}
				return Integer.parseInt(loopbound);
			}

		} catch (InvalidClassFileException e) {
		}
		return 1; // This line will be execute if "No loop bound detected in" a
					// handler
	}

	public ICostResult getCostForInstructionInvoke(SSAInstruction instruction,
			CGNode node) {
		SSAInvokeInstruction inst = (SSAInvokeInstruction) instruction;
		CallSiteReference callSiteRef = inst.getCallSite();
		Set<CGNode> possibleTargets = analysisEnvironment.getCallGraph().getPossibleTargets(node, callSiteRef);
		
		if (possibleTargets == null) {			
			System.err.println("UNSOUND: Figure out why there are no call targets of: "
					+ callSiteRef);
			System.out.println("invoke type"+instruction.toString());
			return new CostResultMemory();
		}

		ICostResult maximumResult = null;
		ICostResult tempResult = null;
		CallStringContext csContext = (CallStringContext) node.getContext();
		CallString callString = (CallString) csContext
				.get(CallStringContextSelector.CALL_STRING);

		for (CGNode target : Iterator2Iterable.make(possibleTargets.iterator())) {
			if (CGNodeAnalyzer.doesContainMethod(callString.getMethods(),
					target.getMethod())) { // Use of context-sensitivity to
											// eliminate recursion
				continue;
			}
			tempResult = nodeCost(target);
			if (maximumResult == null
					|| tempResult.getCostScalar() > maximumResult
							.getCostScalar())
				maximumResult = tempResult;
		}

		return maximumResult;
	}

	public boolean isForLoop(int line,
			Map<Integer, Annotation> annotationByLineNumber) {
		return (annotationByLineNumber.containsKey(line) ? true : false);
	}

	public boolean isWhileLoop(int line,
			Map<Integer, Annotation> annotationByLineNumber) {
		return (annotationByLineNumber.containsKey(line) ? true : false);
	}

	public CostResultMemory getCostForInstructionInBlock(
			SSAInstruction instruction, ISSABasicBlock block) {
		TypeName typeName = ((SSANewInstruction) instruction).getNewSite()
				.getDeclaredType().getName();
		CostResultMemory cost = new CostResultMemory();
		if (typeName.isArrayType()) {
			setCostForNewArrayObject(cost, typeName, block);
		} else {
			setCostForNewObject(cost, typeName, block);
		}

		return cost;
	}

	private void setCostForNewArrayObject(CostResultMemory cost,
			TypeName typeName, ISSABasicBlock block) {
		Integer arrayLength = null;

		IBytecodeMethod method = (IBytecodeMethod) block.getMethod();
		int lineNumber = -1;
		try {
			lineNumber = method.getLineNumber(method.getBytecodeIndex(block
					.getFirstInstructionIndex()));
		} catch (InvalidClassFileException e1) {
			e1.printStackTrace();
		}

		AnnotationExtractor extractor = AnnotationExtractor
				.getAnnotationExtractor();
		Map<Integer, Annotation> annotationsForMethod = extractor
				.getAnnotations(method);

		if (annotationsForMethod != null
				&& annotationsForMethod.containsKey(lineNumber)) {
			Annotation annotationForArray = annotationsForMethod
					.get(lineNumber);
			arrayLength = Integer.parseInt(annotationForArray
					.getAnnotationValue());
		} else {
			arrayLength = tryGetArrayLength(block);

			if (arrayLength == null) {
				OutputPrinter.printAnnotationError(
						AnnotationType.AnnotationArray, method, lineNumber);
				arrayLength = 0;
			}
		}

		try {
			int ocost;
			TypeName o = TypeName.string2TypeName(typeName.toString().substring(1));
			
			try {
				ocost = model.getSizeofType(o);
			} catch (NoSuchElementException e) {				
				IClass aClass = Util.getIClass(typeName.toString(),this.analysisEnvironment.getClassHierarchy());
				ocost = (int) this.calcCost(aClass);					
			}
			
			int allocationCost = arrayLength * ocost;
			
			cost.allocationCost = allocationCost;
			cost.typeNameByNodeId.put(block.getGraphNodeId(), typeName);
			cost.arraySizeByNodeId.put(block.getGraphNodeId(),
					Pair.make(typeName, arrayLength));
			cost.resultType = ResultType.TEMPORARY_BLOCK_RESULT;
		} catch (NoSuchElementException e) {
			OutputPrinter.printModelError(ModelType.ModelEntry, method,
					lineNumber, typeName);
		}
	}

	private Integer tryGetArrayLength(ISSABasicBlock block) {
		IBytecodeMethod method = (IBytecodeMethod) block.getMethod();
		ShrikeCFG shrikeCFG = ShrikeCFG.make(method);
		BasicBlock shrikeBB = shrikeCFG.getNode(block.getGraphNodeId());

		Integer arraySize = null;
		IInstruction prevInst = null;
		for (IInstruction inst : Iterator2Iterable.make(shrikeBB.iterator())) {
			if (inst.toString().contains("[")) {
				String in = inst.toString();
				if (prevInst != null) {
					arraySize = extractArrayLength(prevInst.toString());
					break;
				} else {
					return null;
				}
			}
			prevInst = inst;
		}

		return arraySize;
	}

	private Integer extractArrayLength(String instruction) {
		String number = instruction.substring(instruction.indexOf(',') + 1,
				instruction.length() - 1);
		Integer length = null;
		try {
			length = Integer.parseInt(number);
		} catch (NumberFormatException e) {

		}
		return length;
	}

	private void setCostForNewObject(CostResultMemory cost, TypeName typeName,
			ISSABasicBlock block) {
		cost.typeNameByNodeId.put(block.getGraphNodeId(), typeName);
		cost.resultType = ResultType.TEMPORARY_BLOCK_RESULT;
		try {
			cost.allocationCost = model.getSizeofType(typeName);
		} catch (NoSuchElementException e) {
			try {
				IClass aClass = Util.getIClass(typeName.toString(),
						this.analysisEnvironment.getClassHierarchy());
				cost.allocationCost = this.calcCost(aClass);
			} catch (NoSuchElementException e2) {
				System.err.println("json model does not contain type: "
						+ typeName.toString());
			}
		}
	}

	
	private long calcCost(IClass aClass) {
		long sum = 0;

		if (!aClass.isReferenceType()) {
			return model.getSizeofType(aClass.getName());
		} else if (aClass.isArrayClass()) {
			sum += model.referenceSize;
		} else {
			for (IField f : aClass.getAllInstanceFields()) {
				TypeReference tr = f.getFieldTypeReference();

				if (tr.isReferenceType()) {
					sum += model.referenceSize;
				} else {
					sum += model.getSizeofType(tr.getName());
				}
			}
			sum += model.jvmObjectOverheadSize;
		}

		if (sum == 0) {
			sum += model.referenceSize;
		}

		model.addType(aClass.getReference().getName(), (int) sum);
		return sum;
	}
}
