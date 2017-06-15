package som.vmobjects;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.RecomputeFieldValue.Kind;
import com.oracle.svm.core.annotate.TargetClass;

import som.vmobjects.SObject.SMutableObject;

@TargetClass(SObject.class)
public final class Target_som_vmobjects_SObject {
  @RecomputeFieldValue(kind = Kind.FieldOffset, declClass = SMutableObject.class, name = "field1")
  @Alias
  private static long FIRST_OBJECT_FIELD_OFFSET;

  @RecomputeFieldValue(kind = Kind.FieldOffset, declClass = SMutableObject.class, name = "primField1")
  @Alias
  private static long FIRST_PRIM_FIELD_OFFSET;
}
