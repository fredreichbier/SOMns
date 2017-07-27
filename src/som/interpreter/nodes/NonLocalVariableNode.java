package som.interpreter.nodes;

import static som.interpreter.TruffleCompiler.transferToInterpreter;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.source.SourceSection;

import com.oracle.truffle.dsl.processor.generator.DefaultNodeGenFactory;
import som.compiler.Variable;
import som.compiler.Variable.Local;
import som.interpreter.InliningVisitor;
import som.interpreter.SArguments;
import som.interpreter.nodes.literals.IntegerLiteralNode;
import som.interpreter.nodes.nary.EagerBinaryPrimitiveNode;
import som.primitives.arithmetic.AdditionPrim;
import som.vm.constants.Nil;
import som.vmobjects.SBlock;
import tools.debugger.Tags.LocalVariableTag;
import tools.dym.Tags.LocalVarRead;
import tools.dym.Tags.LocalVarWrite;

import java.util.List;


public abstract class NonLocalVariableNode extends ContextualNode {

  protected final FrameSlot slot;
  protected final Local var;

  private NonLocalVariableNode(final int contextLevel, final Local var,
      final SourceSection source) {
    super(contextLevel, source);
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

  public abstract static class NonLocalVariableReadNode extends NonLocalVariableNode {

    public NonLocalVariableReadNode(final int contextLevel,
        final Local var, final SourceSection source) {
      super(contextLevel, var, source);
    }

    public NonLocalVariableReadNode(final NonLocalVariableReadNode node) {
      this(node.contextLevel, node.var, node.sourceSection);
    }

    @Specialization(guards = "isUninitialized(frame)")
    public final Object doNil(final VirtualFrame frame) {
      return Nil.nilObject;
    }

    protected boolean isBoolean(final VirtualFrame frame) {
      return determineContext(frame).isBoolean(slot);
    }

    protected boolean isLong(final VirtualFrame frame) {
      return determineContext(frame).isLong(slot);
    }

    protected boolean isDouble(final VirtualFrame frame) {
      return determineContext(frame).isDouble(slot);
    }

    protected boolean isObject(final VirtualFrame frame) {
      return determineContext(frame).isObject(slot);
    }

    @Specialization(guards = {"isBoolean(frame)"}, rewriteOn = {FrameSlotTypeException.class})
    public final boolean doBoolean(final VirtualFrame frame) throws FrameSlotTypeException {
      return determineContext(frame).getBoolean(slot);
    }

    @Specialization(guards = {"isLong(frame)"}, rewriteOn = {FrameSlotTypeException.class})
    public final long doLong(final VirtualFrame frame) throws FrameSlotTypeException {
      return determineContext(frame).getLong(slot);
    }

    @Specialization(guards = {"isDouble(frame)"}, rewriteOn = {FrameSlotTypeException.class})
    public final double doDouble(final VirtualFrame frame) throws FrameSlotTypeException {
      return determineContext(frame).getDouble(slot);
    }

    @Specialization(guards = {"isObject(frame)"},
        replaces = {"doBoolean", "doLong", "doDouble"},
        rewriteOn = {FrameSlotTypeException.class})
    public final Object doObject(final VirtualFrame frame) throws FrameSlotTypeException {
      return determineContext(frame).getObject(slot);
    }

    protected final boolean isUninitialized(final VirtualFrame frame) {
      return slot.getKind() == FrameSlotKind.Illegal;
    }

    @Override
    protected boolean isTaggedWith(final Class<?> tag) {
      if (tag == LocalVarRead.class) {
        return true;
      } else {
        return super.isTaggedWith(tag);
      }
    }

    @Override
    public void replaceAfterScopeChange(final InliningVisitor inliner) {
      inliner.updateRead(var, this, contextLevel);
    }
  }

  @NodeChild(value = "exp", type = ExpressionNode.class)
  public abstract static class NonLocalVariableWriteNode extends NonLocalVariableNode {

    public NonLocalVariableWriteNode(final int contextLevel,
        final Local var, final SourceSection source) {
      super(contextLevel, var, source);
    }

    public NonLocalVariableWriteNode(final NonLocalVariableWriteNode node) {
      this(node.contextLevel, node.var, node.sourceSection);
    }

    public abstract ExpressionNode getExp();

    @Specialization(guards = "isBoolKind(frame)")
    public final boolean writeBoolean(final VirtualFrame frame, final boolean expValue) {
      determineContext(frame).setBoolean(slot, expValue);
      return expValue;
    }

    @Specialization(guards = "isLongKind(frame)")
    public final long writeLong(final VirtualFrame frame, final long expValue) {
      determineContext(frame).setLong(slot, expValue);
      return expValue;
    }

    @Specialization(guards = "isDoubleKind(frame)")
    public final double writeDouble(final VirtualFrame frame, final double expValue) {
      determineContext(frame).setDouble(slot, expValue);
      return expValue;
    }

    @Specialization(replaces = {"writeBoolean", "writeLong", "writeDouble"})
    public final Object writeGeneric(final VirtualFrame frame, final Object expValue) {
      ensureObjectKind();
      determineContext(frame).setObject(slot, expValue);
      return expValue;
    }

    protected final boolean isBoolKind(final VirtualFrame frame) {
      if (slot.getKind() == FrameSlotKind.Boolean) {
        return true;
      }
      if (slot.getKind() == FrameSlotKind.Illegal) {
        transferToInterpreter("LocalVar.writeBoolToUninit");
        slot.setKind(FrameSlotKind.Boolean);
        return true;
      }
      return false;
    }

    protected final boolean isLongKind(final VirtualFrame frame) {
      if (slot.getKind() == FrameSlotKind.Long) {
        return true;
      }
      if (slot.getKind() == FrameSlotKind.Illegal) {
        transferToInterpreter("LocalVar.writeIntToUninit");
        slot.setKind(FrameSlotKind.Long);
        return true;
      }
      return false;
    }

    protected final boolean isDoubleKind(final VirtualFrame frame) {
      if (slot.getKind() == FrameSlotKind.Double) {
        return true;
      }
      if (slot.getKind() == FrameSlotKind.Illegal) {
        transferToInterpreter("LocalVar.writeDoubleToUninit");
        slot.setKind(FrameSlotKind.Double);
        return true;
      }
      return false;
    }

    protected final void ensureObjectKind() {
      if (slot.getKind() != FrameSlotKind.Object) {
        transferToInterpreter("LocalVar.writeObjectToUninit");
        slot.setKind(FrameSlotKind.Object);
      }
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
    public void replaceAfterScopeChange(final InliningVisitor inliner) {
      inliner.updateWrite(var, this, getExp(), contextLevel);
    }
  }

  public abstract static class IncrementOperationNode extends NonLocalVariableNode {
    private final long value;
    private final ExpressionNode exp;

    public IncrementOperationNode(final IncrementOperationNode node) {
      super(node.contextLevel, node.var, node.sourceSection);
      this.value = node.getValue();
      this.exp = node.getExp();
    }

    public IncrementOperationNode(final int contextLevel,
                                  final Local variable,
                                  final ExpressionNode exprNode,
                                  final SourceSection source) {
      super(contextLevel, variable, source);
      this.value = ((IntegerLiteralNode) NodeUtil.findNodeChildren(exprNode).get(1)).getValue();
      this.exp = exprNode;
    }

    public static boolean isIncrementOperation(Variable var, ExpressionNode exprNode, int contextLevel) {
      if(exprNode instanceof EagerBinaryPrimitiveNode) {
        List<Node> children = NodeUtil.findNodeChildren(exprNode);
        if(children.get(0) instanceof LocalVariableNode.LocalVariableReadNode) {
          return children.get(1) instanceof IntegerLiteralNode
                 && children.get(2) instanceof AdditionPrim;
        } else if(children.get(0) instanceof NonLocalVariableReadNode
                && children.get(1) instanceof IntegerLiteralNode
                && children.get(2) instanceof AdditionPrim) {
          NonLocalVariableReadNode read = (NonLocalVariableReadNode) children.get(0);
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
      long incremented;
      if(contextLevel > 0) {
        SBlock self = (SBlock) SArguments.rcvr(frame);
        int i = 0;
        int readContextLevel = ((NonLocalVariableReadNode) NodeUtil.findNodeChildren(exp).get(0)).getContextLevel();
        assert contextLevel <= readContextLevel;
        while (i < contextLevel - 1) {
          self = (SBlock) self.getOuterSelf();
          i++;
        }
        MaterializedFrame writeContext = self.getContext();
        while (i < readContextLevel - 1) {
          self = (SBlock) self.getOuterSelf();
          i++;
        }
        incremented = self.getContext().getLong(slot) + value;
        writeContext.setLong(slot, incremented);
      } else {
        incremented = frame.getLong(slot) + value;
        frame.setLong(slot, incremented);
      }
      return incremented;
    }

    @Specialization(replaces = {"writeLong"})
    public final Object writeGeneric(final VirtualFrame frame) {
      // TODO: Does this even work?
      return replaceWithOriginal().writeGeneric(frame, exp.executeGeneric(frame));
    }

    private final NonLocalVariableWriteNode replaceWithOriginal() {
      NonLocalVariableWriteNode replacement = NonLocalVariableNodeFactory.NonLocalVariableWriteNodeGen.create(
              contextLevel, var, sourceSection, exp
      );
      replace(replacement);
      return replacement;
    }

    protected final boolean isLongKind(FrameSlot slot) { // uses slot to make sure guard is not converted to assertion. TODO: does this work?
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
      // for this, replace myself with the original node
      NonLocalVariableWriteNode node = replaceWithOriginal();
      inliner.updateWrite(var, node, node.getExp(), contextLevel);
      // need to manually recurse into children
      node.accept(inliner);
    }
  }
}
