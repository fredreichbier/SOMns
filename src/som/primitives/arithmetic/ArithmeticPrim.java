package som.primitives.arithmetic;

import java.math.BigInteger;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import som.interpreter.nodes.nary.BinaryBasicOperation;
import tools.dym.Tags.OpArithmetic;


public abstract class ArithmeticPrim extends BinaryBasicOperation {
  @Override
  protected boolean isTaggedWithIgnoringEagerness(final Class<?> tag) {
    if (tag == OpArithmetic.class) {
      return true;
    } else {
      return super.isTaggedWithIgnoringEagerness(tag);
    }
  }

  @TruffleBoundary
  protected static final Number reduceToLongIfPossible(final BigInteger result) {
    if (result.bitLength() > Long.SIZE - 1) {
      return result;
    } else {
      return result.longValue();
    }
  }
}
