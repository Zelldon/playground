package io.zeebe.tools.inspector;

import io.zeebe.engine.state.ZeebeState;
import java.util.List;

public class IncidentInspection implements EntityInspection {

  @Override
  public EntityInspection use(final ZeebeState zeebeState) {
    return this;
  }

  @Override
  public List<String> list() {
    return null;
  }

  @Override
  public String entity(final String key) {
    return null;
  }
}
