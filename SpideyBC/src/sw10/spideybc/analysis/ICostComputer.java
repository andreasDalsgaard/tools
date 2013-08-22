package sw10.spideybc.analysis;

import java.util.Map;

import net.sf.javailp.Problem;
import net.sf.javailp.Result;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.collections.Pair;


public interface ICostComputer<T extends ICostResult> {
	T getCostForInstructionInBlock(SSAInstruction instruction, ISSABasicBlock block);
	void addCost(T fromResult, T toResult);
	void addCostAndContext(T fromResult, T toResult);
	T getFinalResultsFromContextResultsAndLPSolutions(T resultsContext, Result result, Problem problem, Map<String, Pair<Integer, Integer>> edgeLabelToNodesIDs, Map<Integer, ICostResult> calleeResultsAtGraphNodeIdByResult, CGNode cgNode);
	public boolean isInstructionInteresting(SSAInstruction instruction); 
}
