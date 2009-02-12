package de.unisb.cs.st.javalanche.mutation.bytecodeMutations.removeCalls;
import java.io.PrintWriter;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

import de.unisb.cs.st.javalanche.mutation.bytecodeMutations.CollectorByteCodeTransformer;
import de.unisb.cs.st.javalanche.mutation.bytecodeMutations.replaceIntegerConstant.PossibilitiesRicClassAdapter;
import de.unisb.cs.st.javalanche.mutation.mutationPossibilities.MutationPossibilityCollector;

public class RemoveMethodCallsCollectorTransformer extends CollectorByteCodeTransformer {

	public RemoveMethodCallsCollectorTransformer(MutationPossibilityCollector mpc) {
		this.mpc = mpc;
	}

	@Override
	protected ClassVisitor classVisitorFactory(ClassWriter cw) {
		ClassVisitor cc = new CheckClassAdapter(cw);
		cc = new TraceClassVisitor(cc, new PrintWriter(System.out));
		return new PossibilitiesRicClassAdapter(cc, mpc);
	}
}