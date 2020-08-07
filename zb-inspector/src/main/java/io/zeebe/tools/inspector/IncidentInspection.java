package io.zeebe.tools.inspector;

import io.zeebe.engine.state.ZeebeState;
import java.util.List;

public class IncidentInspection implements EntityInspection {

  @Override
  public List<String> list(final PartitionState partitionState) {
    return null;
  }

  @Override
  public String entity(final PartitionState partitionState, final long key) {
    return null;
  }
}
