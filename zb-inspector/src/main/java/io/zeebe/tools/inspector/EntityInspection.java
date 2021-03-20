package io.zeebe.tools.inspector;

import io.zeebe.engine.state.ZeebeState;
import java.util.List;

public interface EntityInspection {
  List<String> list(final PartitionState partitionState);
  String entity(final PartitionState partitionState, final long key);
}
