package io.zeebe.tools.inspector;

import java.util.List;

public interface EntityInspection {
  List<String> list();
  String entity(final String key);
}
