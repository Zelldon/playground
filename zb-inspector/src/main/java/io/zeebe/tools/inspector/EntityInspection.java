package io.zeebe.tools.inspector;

import io.zeebe.engine.state.ZeebeState;
import java.util.List;

public interface EntityInspection {
  EntityInspection use(ZeebeState zeebeState);
  List<String> list();
  String entity(final String key);
}
