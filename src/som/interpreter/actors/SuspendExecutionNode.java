package som.interpreter.actors;

import com.oracle.truffle.api.debug.DebuggerTags.AlwaysHalt;
import com.oracle.truffle.api.dsl.Specialization;

import som.interpreter.nodes.nary.UnaryExpressionNode;


public abstract class SuspendExecutionNode extends UnaryExpressionNode {
  private int skipFrames;

  SuspendExecutionNode(final int skipFrames) {
    this.skipFrames = skipFrames;
  }

  @Specialization
  public final Object doSAbstractObject(final Object receiver) {
    return receiver;
  }

  @Override
  protected boolean isTaggedWithIgnoringEagerness(final Class<?> tag) {
    if (tag == AlwaysHalt.class) {
      return true;
    }
    return super.isTaggedWithIgnoringEagerness(tag);
  }

  public int getSkipFrames() {
    return skipFrames;
  }
}
