package som.primitives.bitops;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import bd.primitives.Primitive;
import som.primitives.arithmetic.ArithmeticPrim;


@GenerateNodeFactory
@Primitive(primitive = "int:bitXor:", selector = "bitXor:")
public abstract class BitXorPrim extends ArithmeticPrim {
  @Specialization
  public final long doLong(final long receiver, final long right) {
    return receiver ^ right;
  }
}
