package sw10.spideybc.util;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;

import sw10.spideybc.program.AnalysisSpecification;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.examples.properties.WalaExamplesProperties;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.graph.labeled.SlowSparseNumberedLabeledGraph;
import com.ibm.wala.util.graph.traverse.BFSIterator;
import com.ibm.wala.viz.DotUtil;
import com.ibm.wala.viz.NodeDecorator;
import com.ibm.wala.util.graph.labeled.AbstractNumberedLabeledGraph;
 
public class Util {
	public static String getClassNameOrOuterMostClassNameIfNestedClass(String fullQualifiedClassName) {
		String fileKey = null;
		if (fullQualifiedClassName.contains("$")) {
			fileKey = fullQualifiedClassName.substring(0, fullQualifiedClassName.indexOf("$"));
		} else {
			fileKey = fullQualifiedClassName;
		}

		return fileKey;
	}

	public static Pair<SlowSparseNumberedLabeledGraph<ISSABasicBlock, String>, Map<String, Pair<Integer, Integer>>> sanitize(IR ir, IClassHierarchy cha) throws IllegalArgumentException, WalaException {
		Map<String, Pair<Integer, Integer>> edgeLabels = new HashMap<String, Pair<Integer,Integer>>();

		ControlFlowGraph<SSAInstruction, ISSABasicBlock> cfg = ir.getControlFlowGraph();
		SlowSparseNumberedLabeledGraph<ISSABasicBlock, String> g = new SlowSparseNumberedLabeledGraph<ISSABasicBlock, String>("");

		// add all nodes to the graph
		for (Iterator<? extends ISSABasicBlock> it = cfg.iterator(); it.hasNext();) {
			g.addNode(it.next());
		}

		int edgeId = 0; 
		// add all edges to the graph, except those that go to exit
		for (Iterator it = cfg.iterator(); it.hasNext();) {
			ISSABasicBlock b = (ISSABasicBlock) it.next();
			for (Iterator it2 = cfg.getSuccNodes(b); it2.hasNext();) {
				ISSABasicBlock b2 = (ISSABasicBlock) it2.next();
				if (!b2.isExitBlock()) {
					String edgeLabel = "f" + edgeId++;
					edgeLabels.put(edgeLabel, Pair.make(b.getGraphNodeId(), b2.getGraphNodeId()));				
					g.addEdge(b, b2, edgeLabel);
				}
			}
		}

		// now add edges to exit, ignoring undeclared exceptions
		ISSABasicBlock exit = cfg.exit();
		int incomingEdgesToExitNodeCounter = 0;
		for (Iterator it = cfg.getPredNodes(exit); it.hasNext();) {
			// for each predecessor of exit ...
			ISSABasicBlock b = (ISSABasicBlock) it.next();

			SSAInstruction s = ir.getInstructions()[b.getLastInstructionIndex()];
			if (s == null) {
				continue;
			}


			g.addEdge(b, exit, "ft" + incomingEdgesToExitNodeCounter);
			edgeLabels.put("ft" + incomingEdgesToExitNodeCounter, Pair.make(b.getGraphNodeId(), exit.getGraphNodeId()));
			incomingEdgesToExitNodeCounter++;
		}

		return Pair.make(g, edgeLabels);
	}
	
	public static void CreatePDFCGF(CallGraph cg, ClassHierarchy cha) throws WalaException {
		AnalysisSpecification spec = AnalysisSpecification.getAnalysisSpecification();
		
		Properties wp = WalaProperties.loadProperties();
	    wp.putAll(WalaExamplesProperties.loadProperties());
	    String outputDir = spec.getOutputDir() + File.separatorChar;

		String psFile = outputDir + "callGraph.pdf";		
		String dotFile = outputDir + "callGraph.dt";
		
	    String dotExe = wp.getProperty(WalaExamplesProperties.DOT_EXE);
	    String gvExe = wp.getProperty(WalaExamplesProperties.PDFVIEW_EXE);
	    
	    final HashMap<CGNode, String> labelMap = HashMapFactory.make();
	    
	    BFSIterator<CGNode> cgIt = new BFSIterator<CGNode>(cg);
	    while(cgIt.hasNext()) {
	    	CGNode node = cgIt.next();
	    	
	        StringBuilder label = new StringBuilder();
	        label.append(node.toString() + "\n" + node.getGraphNodeId());
	        
	        labelMap.put(node, label.toString());
	      
	    }
	    NodeDecorator labels = new NodeDecorator() {
	        public String getLabel(Object o) {
	            return labelMap.get(o);
	        }
	    };
		DotUtil.dotify(cg, labels, dotFile, psFile, dotExe); 
	}
	
	public static void CreatePDFCGF(CallGraph cg) throws WalaException {
		AnalysisSpecification spec = AnalysisSpecification.getAnalysisSpecification();
		
		Properties wp = WalaProperties.loadProperties();
	    wp.putAll(WalaExamplesProperties.loadProperties());
	    String outputDir = spec.getOutputDir() + File.separatorChar;

		String psFile = outputDir + "callGraph.pdf";		
		String dotFile = outputDir + "callGraph.dt";
		
	    String dotExe = wp.getProperty(WalaExamplesProperties.DOT_EXE);
	    
	    final HashMap<CGNode, String> labelMap = HashMapFactory.make();
	    
	    BFSIterator<CGNode> cgIt = new BFSIterator<CGNode>(cg);
	    while(cgIt.hasNext()) {
	    	CGNode node = cgIt.next();
	    	
	        StringBuilder label = new StringBuilder();
	        label.append(node.toString() + " " + node.getGraphNodeId());
	        
	        labelMap.put(node, label.toString());
	      
	    }
	    NodeDecorator labels = new NodeDecorator() {
	        public String getLabel(Object o) {
	            return labelMap.get(o);
	        }
	    };
		DotUtil.dotify(cg, labels, dotFile, psFile, dotExe); 
	}
	
	public static void CreatePDFCFG(SlowSparseNumberedLabeledGraph<ISSABasicBlock, String> cfg, CGNode node) throws WalaException {
		Properties wp = WalaProperties.loadProperties();
		wp.putAll(WalaExamplesProperties.loadProperties());
		String outputDir = wp.getProperty(WalaProperties.OUTPUT_DIR) + File.separatorChar;

		String javaFileName = node.getMethod().getDeclaringClass().getSourceFileName();
		javaFileName = javaFileName.substring(0, javaFileName.lastIndexOf("."));

		String psFile = outputDir + javaFileName + ".pdf";		
		String dotFile = outputDir + javaFileName + ".dt";
		String dotExe = wp.getProperty(WalaExamplesProperties.DOT_EXE);
		
		final HashMap<ISSABasicBlock, String> labelMap = HashMapFactory.make();

		for (Iterator<ISSABasicBlock> it = cfg.iterator(); it.hasNext();) {
			ISSABasicBlock bb = it.next();

			StringBuilder label = new StringBuilder();
			label.append("ID #" + bb.getGraphNodeId() + "\n");
			label.append(bb.toString() + "\n");

			Iterator<SSAInstruction> itInst = bb.iterator();
			while(itInst.hasNext()) {
				SSAInstruction inst = itInst.next();
				label.append(inst.toString() + "\n");
			}

			labelMap.put(bb, label.toString());

		}
		NodeDecorator labels = new NodeDecorator() {
			public String getLabel(Object o) {
				return labelMap.get(o);
			}
		};

		DotUtil.dotify(cfg, labels, dotFile, psFile, dotExe); 
	}
	/*
	public static void CreateCFG(ControlFlowGraph<SSAInstruction, IBasicBlock<SSAInstruction>> cfg, String filename) throws WalaException {
		Properties wp = WalaProperties.loadProperties();
		wp.putAll(WalaExamplesProperties.loadProperties());
		String outputDir = wp.getProperty(WalaProperties.OUTPUT_DIR) + File.separatorChar;
		String psFile = outputDir + filename + ".pdf";		
		String dotFile = outputDir + filename + ".dt";
		String dotExe = wp.getProperty(WalaExamplesProperties.DOT_EXE);
		String gvExe = wp.getProperty(WalaExamplesProperties.PDFVIEW_EXE);
		final HashMap<IBasicBlock<SSAInstruction>, String> labelMap = HashMapFactory.make();

		for (Iterator<IBasicBlock<SSAInstruction>> it = cfg.iterator(); it.hasNext();) {
			IBasicBlock<SSAInstruction> bb = it.next();

			StringBuilder label = new StringBuilder();
			label.append("ID #" + bb.getGraphNodeId() + "\n");
			label.append(bb.toString() + "\n");

			Iterator<SSAInstruction> itInst = bb.iterator();
			while(itInst.hasNext()) {
				SSAInstruction inst = itInst.next();
				label.append(inst.toString() + "\n");
			}

			labelMap.put(bb, label.toString());

		}
		
		NodeDecorator labels = new NodeDecorator() {
			public String getLabel(Object o) {
				return labelMap.get(o);
			}
		};

		DotUtil.dotify(cfg, labels, dotFile, psFile, dotExe); 
	}*/
	
	public static void CreateCVV(ControlFlowGraph<SSAInstruction, ISSABasicBlock> cfg, String filename) throws WalaException {
		Properties wp = WalaProperties.loadProperties();
		wp.putAll(WalaExamplesProperties.loadProperties());
		String outputDir = wp.getProperty(WalaProperties.OUTPUT_DIR) + File.separatorChar;
		String psFile = outputDir + filename + ".pdf";		
		String dotFile = outputDir + filename + ".dt";
		String dotExe = wp.getProperty(WalaExamplesProperties.DOT_EXE);

		final HashMap<ISSABasicBlock, String> labelMap = HashMapFactory.make();

		for (Iterator<ISSABasicBlock> it = cfg.iterator(); it.hasNext();) {
			ISSABasicBlock bb = it.next();

			StringBuilder label = new StringBuilder();
			label.append("ID #" + bb.getGraphNodeId() + "\n");
			label.append(bb.toString() + "\n");

			Iterator<SSAInstruction> itInst = bb.iterator();
			while(itInst.hasNext()) {
				SSAInstruction inst = itInst.next();
				label.append(inst.toString() + "\n");
			}

			labelMap.put(bb, label.toString());

		}
		
		NodeDecorator labels = new NodeDecorator() {
			public String getLabel(Object o) {
				return labelMap.get(o);
			}
		};

		DotUtil.dotify(cfg, labels, dotFile, psFile, dotExe); 
	}
	
	/* TODO make a generic generateCFG function - this function was copied from ReportGenerator but is not based on SSAcfg */
	public static void GenerateCFG(AbstractNumberedLabeledGraph<ISSABasicBlock, String> cfg, String guid) throws WalaException{
		Properties wp = WalaProperties.loadProperties();
		wp.putAll(WalaExamplesProperties.loadProperties());

        String tempDir = System.getProperty("java.io.tmpdir");
		String psFile = tempDir + File.separatorChar + guid + ".pdf";	
		String dotFile = tempDir + File.separatorChar + guid + ".dt";
		String dotExe = wp.getProperty(WalaExamplesProperties.DOT_EXE);

		final HashMap<ISSABasicBlock, String> labelMap = HashMapFactory.make();		
		for (Iterator<ISSABasicBlock> iteratorBasicBlock = cfg.iterator(); iteratorBasicBlock.hasNext();) {
			ISSABasicBlock basicBlock =  iteratorBasicBlock.next();

			StringBuilder label = new StringBuilder();
			
			if(basicBlock.isEntryBlock()) {
				label.append(basicBlock.toString());
				label.append("(entry)");				
			} else if(basicBlock.isExitBlock()) {
				label.append(basicBlock.toString());
				label.append("(exit)");
			} else {
				label.append("BB"+basicBlock.getNumber()+"   ");
				Iterator<SSAInstruction> iteratorInstruction = basicBlock.iterator();
				while(iteratorInstruction.hasNext()) {
					SSAInstruction inst = iteratorInstruction.next();
					label.append(inst.toString() + " ");
				}
			}
			labelMap.put(basicBlock, label.toString());
		}
		NodeDecorator labels = new NodeDecorator() {
			public String getLabel(Object o) {
				return labelMap.get(o);
			}
		};
		DotUtil.dotify(cfg, labels, dotFile, psFile, dotExe);
	}
	
	/*
	public <T> void CreatePDF(Graph<T> g,String filename, int key) throws WalaException {
		Properties wp = WalaProperties.loadProperties();
		wp.putAll(WalaExamplesProperties.loadProperties());
		String outputDir = wp.getProperty(WalaProperties.OUTPUT_DIR) + File.separatorChar;
		String psFile = outputDir + filename + ".pdf";		
		String dotFile = outputDir + filename + ".dt";
		String dotExe = wp.getProperty(WalaExamplesProperties.DOT_EXE);
		
		switch (key) {
		case 1:
			CG(g,labelMap);
			break;
		case 2:
			CFG(g,labelMap);
			break;
		default:
			System.out.println("Error: There is not print out any graph");
			break;
		}
		
		NodeDecorator labels = new NodeDecorator() {
			public String getLabel(Object o) {
				return labelMap.get(o);
			}
		};

		DotUtil.dotify(g, labels, dotFile, psFile, dotExe);
	}
		
	private void CG(CallGraph cg){
		HashMap<CGNode, String> labelMap = HashMapFactory.make();
		BFSIterator<CGNode> cgIt = new BFSIterator<CGNode>(cg);
	    while(cgIt.hasNext()) {
	    	CGNode node = cgIt.next();
	    	
	        StringBuilder label = new StringBuilder();
	        label.append(node.toString() + " " + node.getGraphNodeId());
	        
	        labelMap.put(node, label.toString()); 
	    }
	}
	
	private void CFG(ControlFlowGraph<SSAInstruction, IBasicBlock<SSAInstruction>> cfg){
		HashMap<IBasicBlock<SSAInstruction>, String> labelMap = HashMapFactory.make();
		for (Iterator<IBasicBlock<SSAInstruction>> it = cfg.iterator(); it.hasNext();) {
			IBasicBlock<SSAInstruction> bb = it.next();

			StringBuilder label = new StringBuilder();
			label.append("ID #" + bb.getGraphNodeId() + "\n");
			label.append(bb.toString() + "\n");

			Iterator<SSAInstruction> itInst = bb.iterator();
			while(itInst.hasNext()) {
				SSAInstruction inst = itInst.next();
				label.append(inst.toString() + "\n");
			}

			labelMap.put(bb, label.toString());
		}
	}
	*/


	public static IClass getIClass(String str, IClassHierarchy cha)
	{
		Iterator<IClass> classes = cha.iterator();
	     
		while (classes.hasNext()) {
			IClass aClass = (IClass) classes.next();
			if (aClass.getName().toString().equals(str))
				return aClass;			
		}
		
		throw new NoSuchElementException();	
	}
}
