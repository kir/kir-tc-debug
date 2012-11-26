/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.kir;

import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.db.queries.GenericQuery;
import jetbrains.buildServer.serverSide.impl.BuildTypeImpl;
import jetbrains.buildServer.serverSide.tests.BuildOrderSupport;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * ${DESCR}
 *
 * @author kir
 */
public class DataLogger {

  private static final String TEAMCITY_KIR_LOG = "teamcity-kir.log";

  private final ServerPaths myServerPaths;
  private final BuildsManager myBuilds;
  private final SQLRunner mySQLRunner;

  private FileWriter myFileWriter;

  public DataLogger(ServerPaths serverPaths, BuildsManager builds, SQLRunner sqlRunner) {
    myServerPaths = serverPaths;
    myBuilds = builds;
    mySQLRunner = sqlRunner;
  }

  public void logData(long fromBuildId, long toBuildId) {
    try {
      openLog();
      log("=================== LOG data between " + fromBuildId + " and " + toBuildId + " ===========================");

      final SBuild b1 = myBuilds.findBuildInstanceById(fromBuildId);
      final SBuild b2 = myBuilds.findBuildInstanceById(toBuildId);

      debugBuildData(b1);
      log("------------");
      log("------------");

      debugBuildData(b2);

      log("------------");
      log("------------");
      logBuildStateBetweenBuilds(b1, b2);

    } catch (Exception e) {
      Loggers.SERVER.error("Problem in Kir debug plugin", e);
    }
    finally {
      closeLog();
    }
  }

  private void debugBuildData(SBuild b) throws IOException {
    logFailedTestsFromModel(b);
    logFailedTestsTable(b);
    logBuildsBeforeFromModel(b);
  }

  private void logFailedTestsFromModel(SBuild b) throws IOException {
    log("Failed tests from build " + b);
    log("------------");

    final List<STestRun> failedTests = b.getShortStatistics().getFailedTests();
    for (STestRun test : failedTests) {
      final STest t = test.getTest();
      log(t.getName() + "," + t.getTestId() + "," + t.getTestNameId() + "," + test.isNewFailure() + "," + test.getTestRunId());
    }

    log("------------");
  }

  private void logFailedTestsTable(final SBuild build) throws IOException {
    log("Failed tests table from build " + build);
    log("------------");
    log("ti.test_name_id, ti.test_id, ti.status");

    mySQLRunner.runSql(new SQLRunner.NoResultSQLAction() {
      public void run(Connection connection) throws SQLException {

        new GenericQuery("select ti.test_name_id, ti.test_id, ti.status from test_info ti where ti.build_id = ? and ti.status > 2", new GenericQuery.ResultSetProcessor() {
          /** @noinspection unchecked*/
          @Nullable
          public Object process(ResultSet resultSet) throws SQLException {
            while (resultSet.next()) {
              try {
                log("" + resultSet.getLong(1) + "," +
                        resultSet.getLong(2) + "," +
                        resultSet.getInt(3)
                );
              } catch (IOException e) {
                throw new SQLException(e);
              }
            }
            return null;
          }
        }).execute(connection, build.getBuildId());
      }
    });
    log("------------");

  }

  private void logBuildsBeforeFromModel(SBuild b) throws IOException {
    log("BuildOrderSupport: Builds before " + b);
    log("------------");
    final BuildOrderSupport buildOrderSupport = ((BuildTypeImpl) b.getBuildType()).getBuildOrderSupport();
    final List<OrderedBuild> buildsBefore = buildOrderSupport.getBuildsBefore(b);
    for (int i = 0; i < buildsBefore.size(); i++) {
      OrderedBuild orderedBuild = buildsBefore.get(i);
      log(orderedBuild.toString());
      if (i > 5) {
        break;
      }
    }
    log("------------");
  }

  private void logBuildStateBetweenBuilds(final SBuild b1, final SBuild b2) throws IOException {
    log("------------");
    log("bs.build_id, bs.modification_id, bs.branch_name, bs.is_deleted, bs.is_canceled, bs.is_personal");

    mySQLRunner.runSql(new SQLRunner.NoResultSQLAction() {
      public void run(Connection connection) throws SQLException {

        new GenericQuery("select bs.build_id, bs.modification_id, bs.branch_name, bs.is_deleted, bs.is_canceled, bs.is_personal " +
                "from build_state bs where bs.build_type_id = ? and " +
                "bs.build_id between ? and ? order by bs.build_id desc", new GenericQuery.ResultSetProcessor() {
          /** @noinspection unchecked*/
          @Nullable
          public Object process(ResultSet resultSet) throws SQLException {
            while (resultSet.next()) {
              try {
                log("" + resultSet.getLong(1) + "," +
                        resultSet.getLong(2) + "," +
                        resultSet.getString(3) + "," +
                        resultSet.getBoolean(4) + "," +
                        resultSet.getBoolean(5) + "," +
                        resultSet.getBoolean(6)
                );
              } catch (IOException e) {
                throw new SQLException(e);
              }
            }
            return null;
          }
        }).execute(connection, b1.getBuildTypeId(), b1.getBuildId() - 1, b2.getBuildId() + 1);
      }
    });
  }

  private void log(String s) throws IOException {
    myFileWriter.write(s);
    myFileWriter.write("\n");
  }

  private void closeLog() {
    if (myFileWriter != null) {
      try {
        myFileWriter.close();
      } catch (IOException e) {
        Loggers.SERVER.error("Problem in Kir debug plugin", e);
      }
    }
  }

  private void openLog() throws IOException {
    myFileWriter = new FileWriter(new File(myServerPaths.getLogsPath(), TEAMCITY_KIR_LOG));
  }

}
