package io.zeebe.tools.inspector;

import io.zeebe.db.ColumnFamily;
import io.zeebe.db.impl.DbLong;
import io.zeebe.engine.state.ZbColumnFamilies;
import io.zeebe.engine.state.instance.Incident;
import io.zeebe.protocol.impl.record.value.incident.IncidentRecord;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class IncidentInspection implements EntityInspection {

  @Override
  public List<String> list(final PartitionState partitionState) {

    final var incidents = new ArrayList<String>();

    final ColumnFamily<DbLong, Incident> incidentColumnFamily =
        getIncidentColumnFamily(partitionState);

    incidentColumnFamily.forEach(
        (key, incident) -> {
          final var incidentKey = key.getValue();
          final var incidentRecord = incident.getRecord();

          incidents.add(toString(incidentKey, incidentRecord));
        });

    return incidents;
  }

  @Override
  public String entity(final PartitionState partitionState, final long key) {

    final var incidentState = partitionState.getZeebeState().getIncidentState();

    return Optional.ofNullable(incidentState.getIncidentRecord(key))
        .map(incidentRecord -> toString(key, incidentRecord))
        .orElse("not found");
  }

  private ColumnFamily<DbLong, Incident> getIncidentColumnFamily(
      final PartitionState partitionState) {
    return partitionState
        .getZeebeDb()
        .createColumnFamily(
            ZbColumnFamilies.INCIDENTS,
            partitionState.getDbContext(),
            new DbLong(),
            new Incident());
  }

  private String toString(final long incidentKey, final IncidentRecord incidentRecord) {
    return String.format(
        "Incident[key: %d, workflow-instance-key: %d, BPMN-process-id: \"%s\", error-type: %s, error-message: \"%s\"]",
        incidentKey,
        incidentRecord.getWorkflowInstanceKey(),
        incidentRecord.getBpmnProcessId(),
        incidentRecord.getErrorType(),
        incidentRecord.getErrorMessage());
  }
}
