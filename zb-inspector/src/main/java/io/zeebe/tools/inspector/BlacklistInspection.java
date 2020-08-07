package io.zeebe.tools.inspector;

import io.zeebe.db.ColumnFamily;
import io.zeebe.db.impl.DbLong;
import io.zeebe.db.impl.DbNil;
import io.zeebe.engine.state.ZbColumnFamilies;
import io.zeebe.engine.state.instance.Incident;
import io.zeebe.protocol.impl.record.value.incident.IncidentRecord;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlacklistInspection implements EntityInspection {

  @Override
  public List<String> list(final PartitionState partitionState) {

    final var elementInstanceState = partitionState.getZeebeState().getWorkflowState()
        .getElementInstanceState();

    final var blacklistColumnFamily = partitionState.getZeebeDb()
        .createColumnFamily(ZbColumnFamilies.BLACKLIST, partitionState.getDbContext(), new DbLong(),
            DbNil.INSTANCE);

    final var blacklistedInstances = new ArrayList<String>();

    blacklistColumnFamily.forEach((key, nil) -> {
          final var workflowInstanceKey = key.getValue();

          final var workflowInstance = elementInstanceState.getInstance(workflowInstanceKey);

          final var bpmnProcessId = workflowInstance.getValue()
              .getBpmnProcessId();

          blacklistedInstances.add(toString(workflowInstanceKey, bpmnProcessId));
        });

    return blacklistedInstances;
  }

  @Override
  public String entity(final PartitionState partitionState, final long key) {
    final var blacklistColumnFamily = partitionState.getZeebeDb()
        .createColumnFamily(ZbColumnFamilies.BLACKLIST, partitionState.getDbContext(), new DbLong(),
            DbNil.INSTANCE);

    var blacklistedInstance = new AtomicReference<String>("No entity found for given key " + key);
    blacklistColumnFamily.whileTrue((dbKey, nil) -> {
      final var workflowInstanceKey = dbKey.getValue();

      if (workflowInstanceKey == key) {
        blacklistedInstance.set(String.valueOf(workflowInstanceKey));
      }
      return workflowInstanceKey != key;
    });

    return blacklistedInstance.get();
  }

  private static String toString(final long workflowInstanceKey, final String bpmnProcessId) {
    return String.format(
        "Blacklisted Instance [workflow-instance-key: %d, BPMN-process-id: \"%s\"]",
        workflowInstanceKey,
        bpmnProcessId);
  }


}
