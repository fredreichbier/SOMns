package som.interpreter.nodes;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.source.SourceSection;

import som.compiler.Variable;
import som.compiler.Variable.Local;
import som.interpreter.InliningVisitor;
import som.interpreter.nodes.literals.IntegerLiteralNode;
import som.interpreter.nodes.nary.EagerBinaryPrimitiveNode;
import som.interpreter.nodes.nary.ExprWithTagsNode;
import som.primitives.arithmetic.AdditionPrim;
import som.vm.constants.Nil;
import tools.debugger.Tags.LocalVariableTag;
import tools.dym.Tags.LocalVarRead;
import tools.dym.Tags.LocalVarWrite;

import java.util.List;


public abstract class LocalVariableNode extends ExprWithTagsNode {
  protected final FrameSlot slot;
  protected final Local var;

  private LocalVariableNode(final Local var, final SourceSection source) {
    super(source);
    this.slot = var.getSlot();
    this.var  = var;
  }

  @Override
  protected boolean isTaggedWith(final Class<?> tag) {
    if (tag == LocalVariableTag.class) {
      return true;
    } else {
      return super.isTaggedWith(tag);
    }
  }

  public abstract static class LocalVariableReadNode extends LocalVariableNode {

    public LocalVariableReadNode(final Local variable, final SourceSection source) {
      super(variable, source);
    }

    public LocalVariableReadNode(final LocalVariableReadNode node) {
      this(node.var, node.sourceSection);
    }

    @Specialization(guards = "isUninitialized(frame)")
    public final Object doNil(final VirtualFrame frame) {
      return Nil.nilObject;
    }

    protected boolean isBoolean(final VirtualFrame frame) {
      return frame.isBoolean(slot);
    }

    protected boolean isLong(final VirtualFrame frame) {
      return frame.isLong(slot);
    }

    protected boolean isDouble(final VirtualFrame frame) {
      return frame.isDouble(slot);
    }

    protected boolean isObject(final VirtualFrame frame) {
      return frame.isObject(slot);
    }

    @Specialization(guards = {"isBoolean(frame)"}, rewriteOn = {FrameSlotTypeException.class})
    public final boolean doBoolean(final VirtualFrame frame) throws FrameSlotTypeException {
      return frame.getBoolean(slot);
    }

    @Specialization(guards = {"isLong(frame)"}, rewriteOn = {FrameSlotTypeException.class})
    public final long doLong(final VirtualFrame frame) throws FrameSlotTypeException {
      return frame.getLong(slot);
    }

    @Specialization(guards = {"isDouble(frame)"}, rewriteOn = {FrameSlotTypeException.class})
    public final double doDouble(final VirtualFrame frame) throws FrameSlotTypeException {
      return frame.getDouble(slot);
    }

    @Specialization(guards = {"isObject(frame)"},
        replaces = {"doBoolean", "doLong", "doDouble"},
        rewriteOn = {FrameSlotTypeException.class})
    public final Object doObject(final VirtualFrame frame) throws FrameSlotTypeException {
      return frame.getObject(slot);
    }

    protected final boolean isUninitialized(final VirtualFrame frame) {
      return slot.getKind() == FrameSlotKind.Illegal;
    }

    @Override
    protected final boolean isTaggedWith(final Class<?> tag) {
      if (tag == LocalVarRead.class) {
        return true;
      } else {
        return super.isTaggedWith(tag);
      }
    }

    @Override
    public String toString() {
      return this.getClass().getSimpleName() + "[" + var.name + "]";
    }

    @Override
    public void replaceAfterScopeChange(final InliningVisitor inliner) {
      inliner.updateRead(var, this, 0);
    }
  }

  @NodeChild(value = "exp", type = ExpressionNode.class)
  public abstract static class LocalVariableWriteNode extends LocalVariableNode {

    public LocalVariableWriteNode(final Local variable, final SourceSection source) {
      super(variable, source);
    }

    public LocalVariableWriteNode(final LocalVariableWriteNode node) {
      super(node.var, node.sourceSection);
    }

    public abstract ExpressionNode getExp();

    @Specialization(guards = "isBoolKind(expValue)")
    public final boolean writeBoolean(final VirtualFrame frame, final boolean expValue) {
      frame.setBoolean(slot, expValue);
      return expValue;
    }

    @Specialization(guards = "isLongKind(expValue)")
    public final long writeLong(final VirtualFrame frame, final long expValue) {
      frame.setLong(slot, expValue);
      return expValue;
    }

    @Specialization(guards = "isDoubleKind(expValue)")
    public final double writeDouble(final VirtualFrame frame, final double expValue) {
      frame.setDouble(slot, expValue);
      return expValue;
    }

    @Specialization(replaces = {"writeBoolean", "writeLong", "writeDouble"})
    public final Object writeGeneric(final VirtualFrame frame, final Object expValue) {
      slot.setKind(FrameSlotKind.Object);
      frame.setObject(slot, expValue);
      return expValue;
    }

    protected final boolean isBoolKind(final boolean expValue) { // uses expValue to make sure guard is not converted to assertion
      if (slot.getKind() == FrameSlotKind.Boolean) {
        return true;
      }
      if (slot.getKind() == FrameSlotKind.Illegal) {
        slot.setKind(FrameSlotKind.Boolean);
        return true;
      }
      return false;
    }

    protected final boolean isLongKind(final long expValue) { // uses expValue to make sure guard is not converted to assertion
      if (slot.getKind() == FrameSlotKind.Long) {
        return true;
      }
      if (slot.getKind() == FrameSlotKind.Illegal) {
        slot.setKind(FrameSlotKind.Long);
        return true;
      }
      return false;
    }

    protected final boolean isDoubleKind(final double expValue) { // uses expValue to make sure guard is not converted to assertion
      if (slot.getKind() == FrameSlotKind.Double) {
        return true;
      }
      if (slot.getKind() == FrameSlotKind.Illegal) {
        slot.setKind(FrameSlotKind.Double);
        return true;
      }
      return false;
    }

    @Override
    protected final boolean isTaggedWith(final Class<?> tag) {
      if (tag == LocalVarWrite.class) {
        return true;
      } else {
        return super.isTaggedWith(tag);
      }
    }

    @Override
    public String toString() {
      return this.getClass().getSimpleName() + "[" + var.name + "]";
    }

    @Override
    public void replaceAfterScopeChange(final InliningVisitor inliner) {
      inliner.updateWrite(var, this, getExp(), 0);
    }
  }

  public abstract static class IncrementOperationNode extends LocalVariableNode {
    private final long value;
    private final ExpressionNode exp;

    public IncrementOperationNode(final IncrementOperationNode node) {
      super(node.var, node.sourceSection);
      this.value = node.getValue();
      this.exp = node.getExp();
    }

    public IncrementOperationNode(final Local variable, final ExpressionNode exprNode, final SourceSection source) {
      super(variable, source);
      this.value = ((IntegerLiteralNode)NodeUtil.findNodeChildren(exprNode).get(1)).getValue();
      this.exp = exprNode;
    }

    public static boolean isIncrementOperation(Variable var, ExpressionNode exprNode) {
      if(exprNode instanceof EagerBinaryPrimitiveNode) {
        List<Node> children = NodeUtil.findNodeChildren(exprNode);
        if(children.get(0) instanceof LocalVariableReadNode
                && children.get(1) instanceof IntegerLiteralNode
                && children.get(2) instanceof AdditionPrim) {
          LocalVariableReadNode read = (LocalVariableReadNode)children.get(0);
          if(read.var.equals(var)) {
            return true;
          }
        }
      }
      return false;
    }

    public long getValue() {
      return value;
    }

    @Specialization(guards = "isLongKind(slot)", rewriteOn = {FrameSlotTypeException.class})
    public final long writeLong(final VirtualFrame frame) throws FrameSlotTypeException {
      long incremented = frame.getLong(slot) + value;
      frame.setLong(slot, incremented);
      return incremented;
    }

    @Specialization(replaces = {"writeLong"})
    public final Object writeGeneric(final VirtualFrame frame) {
      // TODO: Does this even work?
      LocalVariableWriteNode replacement = LocalVariableNodeFactory.LocalVariableWriteNodeGen.create(
              var, sourceSection, exp
      );
      replace(replacement);
      return replacement.writeGeneric(frame, exp.executeGeneric(frame));
    }

    protected final boolean isLongKind(FrameSlot slot) { // uses slot to make sure guard is not converted to assertion
      if (slot.getKind() == FrameSlotKind.Long) {
        return true;
      }
      if (slot.getKind() == FrameSlotKind.Illegal) {
        slot.setKind(FrameSlotKind.Long);
        return true;
      }
      return false;
    }

    public ExpressionNode getExp() {
      return exp;
    }

    @Override
    protected final boolean isTaggedWith(final Class<?> tag) {
      if (tag == LocalVarWrite.class) {
        return true;
      } else {
        return super.isTaggedWith(tag);
      }
    }

    @Override
    public String toString() {
      return this.getClass().getSimpleName() + "[" + var.name + "]";
    }

    @Override
    public void replaceAfterScopeChange(final InliningVisitor inliner) {
      inliner.updateWrite(var, this, getExp(), 0);
    }
  }
}
