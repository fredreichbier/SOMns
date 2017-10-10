package som.interpreter.actors;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import bd.primitives.Primitive;
import som.interpreter.actors.SPromise.Resolution;
import som.interpreter.actors.SPromise.SResolver;


@GenerateNodeFactory
@Primitive(primitive = "actorsResolve:with:isBPResolver:isBPResolution:")
public abstract class ResolvePromiseNode extends AbstractPromiseResolutionNode {
  /**
   * Normal case, when the promise is resolved with a value that's not a promise.
   * Here we need to distinguish the explicit promises to ask directly to the promise
   * if a promise resolution breakpoint was set.
   */
  @Specialization(guards = {"notAPromise(result)"})
  public SResolver normalResolution(final VirtualFrame frame,
      final SResolver resolver, final Object result,
      final boolean haltOnResolver, final boolean haltOnResolution) {
    SPromise promise = resolver.getPromise();

    if (haltOnResolver || promise.getHaltOnResolver()) {
      haltNode.executeEvaluated(frame, result);
    }

    resolvePromise(Resolution.SUCCESSFUL, resolver, result,
        haltOnResolution || promise.getHaltOnResolution());
    return resolver;
  }
}
