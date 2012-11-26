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

import jetbrains.buildServer.controllers.SimpleActionController;
import jetbrains.buildServer.serverSide.BuildsManager;
import jetbrains.buildServer.serverSide.SQLRunner;
import jetbrains.buildServer.serverSide.ServerPaths;
import jetbrains.buildServer.web.openapi.ControllerAction;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author kir
 */
public class DebugAction implements ControllerAction {

  private final ServerPaths myServerPaths;
  private final BuildsManager myBuilds;
  private final SQLRunner mySQLRunner;


  public DebugAction(SimpleActionController controller, ServerPaths serverPaths, BuildsManager builds, SQLRunner sqlRunner) {
    myServerPaths = serverPaths;
    myBuilds = builds;
    mySQLRunner = sqlRunner;

    controller.registerAction(this);
  }

  public boolean canProcess(@NotNull HttpServletRequest request) {
    return request.getParameter("kir_debug") != null;
  }

  public void process(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @Nullable Element ajaxResponse) {

    final long b1 = Long.parseLong(request.getParameter("b1"));
    final long b2 = Long.parseLong(request.getParameter("b2"));

    new DataLogger(myServerPaths, myBuilds, mySQLRunner).logData(b1, b2);
  }
}
