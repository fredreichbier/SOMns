package som.interpreter.transactions;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import som.interpreter.nodes.nary.BinaryBasicOperation;
import som.interpreter.nodes.nary.TernaryExpressionNode;
import som.vmobjects.SArray.SImmutableArray;
import som.vmobjects.SArray.SMutableArray;


public abstract class TxArrayAccess {

  public abstract static class TxBinaryArrayOp extends BinaryBasicOperation {

    @Child protected BinaryBasicOperation arrayOp;

    protected TxBinaryArrayOp(final BinaryBasicOperation arrayOp) {
      this.arrayOp = arrayOp;
    }

    @Specialization
    public final Object doSArray(final VirtualFrame frame,
        final SMutableArray rcvr, final long idx) {
      SMutableArray workingCopy = Transactions.workingCopy(rcvr);
      return arrayOp.executeEvaluated(frame, workingCopy, idx);
    }

    public final Object doSArray(final VirtualFrame frame,
        final SImmutableArray rcvr, final long idx) {
      return arrayOp.executeEvaluated(frame, rcvr, idx);
    }
  }

  public abstract static class TxTernaryArrayOp extends TernaryExpressionNode {
    @Child protected TernaryExpressionNode arrayOp;

    protected TxTernaryArrayOp(final TernaryExpressionNode arrayOp) {
      this.arrayOp = arrayOp;
    }

    @Specialization
    public final Object doSArray(final VirtualFrame frame,
        final SMutableArray rcvr, final long idx, final Object val) {
      SMutableArray workingCopy = Transactions.workingCopy(rcvr);
      return arrayOp.executeEvaluated(frame, workingCopy, idx, val);
    }

    @Specialization
    public final Object doSArray(final VirtualFrame frame,
        final SImmutableArray rcvr, final long idx, final Object val) {
      return arrayOp.executeEvaluated(frame, rcvr, idx, val);
    }
  }
}
