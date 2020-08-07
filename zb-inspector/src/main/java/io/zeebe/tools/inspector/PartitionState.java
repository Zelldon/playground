package io.zeebe.tools.inspector;

import io.zeebe.db.DbContext;
import io.zeebe.db.ZeebeDb;
import io.zeebe.engine.state.ZbColumnFamilies;
import io.zeebe.engine.state.ZeebeState;

class PartitionState {

  private final ZeebeDb<ZbColumnFamilies> zeebeDb;
  private final ZeebeState zeebeState;
  private final DbContext dbContext;

  public static PartitionState of(ZeebeDb<ZbColumnFamilies> zeebeDb, ZeebeState zeebeState, DbContext dbContext) {
    return new PartitionState(zeebeDb, zeebeState, dbContext);
  }

  private PartitionState(
      final ZeebeDb<ZbColumnFamilies> zeebeDb, final ZeebeState zeebeState,
      final DbContext dbContext) {
    this.zeebeDb = zeebeDb;
    this.zeebeState = zeebeState;
    this.dbContext = dbContext;
  }

  public ZeebeDb<ZbColumnFamilies> getZeebeDb() {
    return zeebeDb;
  }

  public ZeebeState getZeebeState() {
    return zeebeState;
  }

  public DbContext getDbContext() {
    return dbContext;
  }
}
