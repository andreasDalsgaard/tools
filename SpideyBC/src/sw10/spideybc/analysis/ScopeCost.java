package sw10.spideybc.analysis;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;



import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import java.util.Stack;

import sw10.spideybc.util.Util;

public class ScopeCost {
	private static Map<Integer, CostResultMemory> scopeTotalCost = new HashMap<Integer, CostResultMemory>();
	public long totalCost = 0;	
	
	static private ClassHierarchy cha;
	static private IClass missionIClass = null;
	static private IClass AEHIClass = null;		
	static private IClass ManagedMemoryIClass;
	static private IClass MemoryAreaIClass;
	static private IClass immortalIClass;
	static private IClass safeletIClass;
	static private IClass PEHIClass;
	static private IClass APEHIClass;
	static private IClass CyclicExecutiveIClass;	
	static private boolean CyclicExecutiveUsed;
	static private IClass MissionSequencer;
	
	
	
	public static void init(ClassHierarchy cha) 
	{
		ScopeCost.cha = cha;
		missionIClass = getIClass("Ljavax/safetycritical/Mission", cha);
				
		if (missionIClass == null)
			throw new IllegalArgumentException("No mission class in ClassHierarchy");
		
		AEHIClass = getIClass("Ljavax/realtime/AbstractAsyncEventHandler", cha);
		ManagedMemoryIClass  = getIClass("Ljavax/safetycritical/ManagedMemory", cha);		
		MemoryAreaIClass  = getIClass("Ljavax/realtime/MemoryArea", cha);		
		immortalIClass = getIClass("Ljavax/realtime/ImmortalMemory", cha);
		safeletIClass = getIClass("Ljavax/safetycritical/Safelet", cha);		
		PEHIClass = getIClass("Ljavax/safetycritical/PeriodicEventHandler", cha);
		APEHIClass = getIClass("Ljavax/safetycritical/AperiodicEventHandler", cha);
		CyclicExecutiveIClass = getIClass("Ljavax/safetycritical/CyclicExecutive", cha);
		MissionSequencer = getIClass("Ljavax/safetycritical/MissionSequencer", cha);		
	}
	
	public static IClass getIClass(String str, IClassHierarchy cha)
	{
		Iterator<IClass> classes = cha.iterator();
	     
		while (classes.hasNext()) {
			IClass aClass = (IClass) classes.next();
			if (aClass.getName().toString().equals(str))
				return aClass;			
		}
		
		return null;		
	}
	
	static public String getScopeName(CGNode caller, CGNode calleeNode, Stack<String> scopeStack) 
	{	
		IMethod callee = calleeNode.getMethod();
		
		if (isSubclassOf(callee, AEHIClass) && 
				isFuncName(callee, "handleAsyncEvent")) 
		{			
			return callee.getDeclaringClass().getName().toString();
		} else if (isImplemented(callee, safeletIClass) && isFuncName(callee, "initializeApplication")) {			
			return "immortal"; 
		}else if (isSubclassOf(callee, ManagedMemoryIClass)) 
		{			
			if (isFuncName(callee, "enterPrivateMemory")) {				
				return getUniquePMName();
			} else if (isFuncName(callee, "executeInOuterArea")) {				
				scopeStack.pop();
				return scopeStack.peek();
			} else if (isFuncName(callee, "executeInAreaOf")) {
				Util.error("Not supported by the analysis");
			} else if (isFuncName(callee, "getCurrentManagedMemory")) {
				Util.error("Not supported by new SCJ revisions");
			} 	
		} else if (isFuncName(callee, "startMission") && callee.getDeclaringClass().getName().toString().equals("Ljavax/safetycritical/JopSystem")) 
		{
			return caller.getMethod().getDeclaringClass().getName().toString();				
		} else if(isFuncName(callee, "getSequencer") && isImplemented(callee, safeletIClass))
		{
			return "immortal";
		}
		
		return scopeStack.peek();
	}

	public static boolean isSubclassOf(IMethod callee, IClass parent)
	{
		if (parent != null)			
			return cha.isSubclassOf(callee.getDeclaringClass(), parent);
		return false;		
	}
	
	public static boolean isImplemented(IMethod callee, IClass parent)
	{
		if (parent != null)					
			return cha.implementsInterface(callee.getDeclaringClass(), parent);
		return false;		
	}
	
	
	public static boolean isFuncName(IMethod callee, String str)
	{
		return callee.getName().toString().equals(str);
	}
	
	public static String getUniquePMName()
	{
		return UUID.randomUUID().toString();
	}
}
