package jetbrains.kir;

import jetbrains.buildServer.serverSide.*;

/**
 * @author kir
 */
public class DebugListener extends BuildServerAdapter implements Runnable {

  public DebugListener(ServerSideEventDispatcher<BuildServerListener> eventDispatcher) {
    eventDispatcher.addListener(this);
  }

  @Override
  public void serverStartup() {
    new Thread(this, "Kir debug thread").start();
  }

  public void run() {

  }

}
