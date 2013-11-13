package sw10.spideybc.analysis;

import java.util.Map;

import net.sf.javailp.Problem;
import net.sf.javailp.Result;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.util.collections.Pair;

public class Memutil implements ICostComputer<CostResultMemory>{
	
	@Override
	public CostResultMemory getCostForInstructionInBlock(SSAInstruction instruction, ISSABasicBlock block) {
		TypeName typeName = ((SSANewInstruction) instruction).getNewSite().getDeclaredType().getName();
		CostResultMemory cost = new CostResultMemory();
		
		return null;
	}
	
	@Override
	public boolean isInstructionInteresting(SSAInstruction instruction) {
		return (instruction instanceof SSANewInstruction ? true : false);
	}
	
	
	@Override
	public void addCost(CostResultMemory fromResult, CostResultMemory toResult) {
		toResult.allocationCost += fromResult.getCostScalar();
	}
	
	@Override
	public void addCostAndContext(CostResultMemory fromResult, CostResultMemory toResult) {
		toResult.allocationCost += fromResult.getCostScalar();
		toResult.typeNameByNodeId.putAll(fromResult.typeNameByNodeId);
		toResult.arraySizeByNodeId.putAll(fromResult.arraySizeByNodeId);
	}

	@Override
	public CostResultMemory getFinalResultsFromContextResultsAndLPSolutions(
			CostResultMemory resultsContext, Result result, Problem problem,
			Map<String, Pair<Integer, Integer>> edgeLabelToNodesIDs,
			Map<Integer, ICostResult> calleeResultsAtGraphNodeIdByResult,
			CGNode cgNode) {
		// TODO Auto-generated method stub
		return null;
	}
}
