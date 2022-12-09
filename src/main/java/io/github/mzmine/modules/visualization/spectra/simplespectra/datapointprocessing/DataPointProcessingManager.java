/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingController.ControllerStatus;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.DPPParameterValueWrapper;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.MSLevel;
import io.github.mzmine.parameters.ParameterSet;

/**
 * There will be a single instance of this class, use getInst(). This class keeps track of every
 * DataPointProcessingController and manages their assignment to the TaskController. Default
 * settings are loaded as set in the preferences.
 * 
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public class DataPointProcessingManager implements MZmineModule {

  private static final DataPointProcessingManager inst = new DataPointProcessingManager();
  private static final int MAX_RUNNING = 3;
  private static final String MODULE_NAME = "Data point processing manager";

  private ParameterSet parameters;

  private static Logger logger = Logger.getLogger(DataPointProcessingManager.class.getName());

  private List<DataPointProcessingController> waiting;
  private List<DataPointProcessingController> running;

  private DPPParameterValueWrapper processingParameters;

  public DataPointProcessingManager() {
    waiting = new ArrayList<>();
    running = new ArrayList<>();

    // parameters =
    // MZmineCore.getConfiguration().getModuleParameters(DataPointProcessingManager.class);

    // [0] if no differentiation between ms levels
    // [1] for ms1 if differentiation is enabled
    // [2] for ms^n if differentiation is enabled
    processingParameters = new DPPParameterValueWrapper();
  }

  public static DataPointProcessingManager getInst() {
    return inst;
  }

  public ParameterSet getParameters() {
    if (parameters == null) {
      parameters =
          MZmineCore.getConfiguration().getModuleParameters(DataPointProcessingManager.class);
      processingParameters =
          parameters.getParameter(DataPointProcessingParameters.processingParameters).getValue();
    }
    return parameters;
  }

  public void updateParameters() {
    processingParameters =
        parameters.getParameter(DataPointProcessingParameters.processingParameters).getValue();
  }

  public MSLevel decideMSLevel(Scan scan) {
    int l = scan.getMSLevel();
    logger.info("MSLevel: " + scan.getMSLevel());

    if (processingParameters.isDifferentiateMSn()) {
      if (l > 1)
        return MSLevel.MSMS;
      else
        return MSLevel.MSONE;
    }
    return MSLevel.MSONE;
  }

  /**
   * Adds a controller to the end of the waiting list. Automatically tries to start a controller
   * after addition.
   * 
   * @param controller Controller to add.
   */
  public void addController(@NotNull DataPointProcessingController controller) {
    synchronized (waiting) {
      if (waiting.contains(controller)) {
        // logger.fine("Warning: Controller was already added to waiting
        // list at index "
        // + waiting.indexOf(controller) + "/" + waiting.size() + ".
        // Skipping.");
        return;
      }
      waiting.add(controller);
    }
    logger.finest("Controller added to waiting list. (size = " + waiting.size() + ")");
    startNextController();
  }

  /**
   * Removes a controller from the waiting list. Use this if the current plot has changed.
   * 
   * @param controller
   */
  public boolean removeWaitingController(DataPointProcessingController controller) {
    // if (!waiting.contains(controller))
    // return false;

    synchronized (waiting) {
      return waiting.remove(controller);
    }
    // logger.finest("Controller removed from wating list. (size = " +
    // waiting.size() + ")");
  }

  /**
   * Adds a controller to the running list. Don't use publicly. Is called by startNextController()
   * if running.size < MAX_RUNNING.
   * 
   * @param controller
   */
  private void addRunningController(@NotNull DataPointProcessingController controller) {
    synchronized (running) {
      if (running.contains(controller)) {
        // logger.fine("Warning: Controller was already added to waiting
        // list at index "
        // + running.indexOf(controller) + "/" + running.size() + ".
        // Skipping.");
        return;
      }
      running.add(controller);
    }
    // logger.finest("Controller added to running list. (size = " +
    // running.size() + ")");
  }

  /**
   * Removes a controller from the running list. Don't use publicly. Is called by the
   * DPControllerStatusListener in the startNextController method, if the task was canceled or
   * finished.
   * 
   * @param controller
   */
  private boolean removeRunningController(DataPointProcessingController controller) {
    // if (!running.contains(controller))
    // return;

    synchronized (running) {
      return running.remove(controller);
    }
    // logger.finest("Controller removed from running list. (size = " +
    // running.size() + ")");
  }

  /**
   * Clears the list of all waiting controllers.
   */
  private void removeAllWaitingControllers() {
    synchronized (waiting) {
      waiting.clear();
    }
  }

  /**
   * Tries to remove the controller from waiting OR running list.
   * 
   * @param controller
   * @return
   */
  public boolean removeController(DataPointProcessingController controller) {
    if (removeRunningController(controller))
      return true;
    else
      return removeWaitingController(controller);
  }

  /**
   * Tries to start the next controller from the waiting list and adds a listener to automatically
   * start the next one when finished. Every SpectraPlot will call this method after adding its
   * controller.
   */
  public void startNextController() {
    if (!isEnabled())
      return;

    DataPointProcessingController next;

    synchronized (waiting) {
      if (running.size() >= MAX_RUNNING) {
        // logger.info("Too much controllers running, cannot start the
        // next one.");
        return;
      }
      if (waiting.isEmpty()) {
        // logger.info("No more waiting controllers, cannot start the
        // next one.");
        return;
      }

      next = waiting.get(0);
      removeWaitingController(next);
    }

    addRunningController(next);
    next.addControllerStatusListener(new DPPControllerStatusListener() {

      @Override
      public void statusChanged(DataPointProcessingController controller,
          ControllerStatus newStatus, ControllerStatus oldStatus) {
        if (newStatus == ControllerStatus.FINISHED) {
          // One controller finished, now we can remove it and start
          // the next one.
          removeController(controller);
          startNextController();
          // logger.finest("Controller finished, trying to start the
          // next one. (size = "
          // + running.size() + ")");
        } else if (newStatus == ControllerStatus.CANCELED) {
          // this will be called, when the controller is forcefully
          // canceled. The current task will
          // be completed, then the status will be changed and this
          // method is called.
          removeController(controller);
          startNextController();
        } else if (newStatus == ControllerStatus.ERROR) {
          // if a controller's task errors out, we should cancel the
          // whole controller here
          // the controller status is set to ERROR automatically, if a
          // task error's out.

          // since the next controller wont be started, just using
          // cancelTasks() here is not
          // sufficient, we have to remove it manually
          removeController(controller);
        }
      }
    });

    next.execute(); // this will start the actual task via the controller
                    // method.
    // logger.finest("Started controller from running list. (size = " +
    // running.size() + ")");
  }

  /**
   * Cancels the execution of a specific controller or removes it from waiting list.
   * 
   * @param controller
   */
  public void cancelController(DataPointProcessingController controller) {
    synchronized (waiting) {
      if (waiting.contains(controller)) {
        controller.cancelTasks();
        // removing the controller will be executed by the
        // statusListener in startNextController()
        // since the forcedStatus of the controller has been set.
        // Controller.execute checks before
        // every task launch if the controller was canceled.
        // removeWaitingController(controller);
      }
    }
    synchronized (running) {
      if (running.contains(controller)) {
        controller.cancelTasks();
        // removing the controller will be executed by the
        // statusListener in startNextController()
        // removeRunningController(controller);
      }
    }
  }

  /**
   * Cancels the execution of all running controllers. Keep in mind that calling only this method
   * will only cancel the running ones, the currently waiting ones will be started afterwards.
   * Consider calling removeAllWaiting first or use cancelAndRemoveAll() instead.
   */
  public void cancelAllRunning() {
    synchronized (running) {
      for (DataPointProcessingController c : running) {
        c.cancelTasks();
      }
    }
  }

  /**
   * Cancels every running task and removed all waiting controllers.
   */
  public void cancelAndRemoveAll() {
    removeAllWaitingControllers();
    synchronized (running) {
      for (DataPointProcessingController c : running) {
        c.cancelTasks();
      }
    }
  }

  /**
   * Convenience method to double check if a controller is allowed to run.
   * 
   * @param controller
   * @return true or false if the controller is still in the running list. If this is false and the
   *         controller wants to execute() something went wrong. Check the setStatus method of the
   *         controller and the tasks.
   */
  public boolean isRunning(DataPointProcessingController controller) {
    return running.contains(controller);
  }

  public boolean isEnabled() {
    getParameters();
    return getParameters().getParameter(DataPointProcessingParameters.enableProcessing).getValue();
  }

  public void setEnabled(boolean enabled) {
    getParameters().getParameter(DataPointProcessingParameters.enableProcessing).setValue(enabled);
    logger.finest("Enabled changed to " + enabled);
  }

  @Override
  public String getName() {
    return "Data point/Spectra processing";
  }

  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {
    return DataPointProcessingParameters.class;
  }

  /**
   * @param mslevel the ms level of the queue Clears the list of processing steps
   */
  public void clearProcessingSteps(MSLevel mslevel) {
    processingParameters.getQueue(mslevel).clear();
  }

  /**
   * Clears all processings queues.
   */
  public void clearProcessingSteps() {
    for (MSLevel mslevel : MSLevel.values())
      processingParameters.getQueue(mslevel).clear();
  }

  /**
   * @param mslevel the ms level of the queue
   * @return
   */
  public @NotNull DataPointProcessingQueue getProcessingQueue(MSLevel mslevel) {
    getParameters();
    return processingParameters.getQueue(mslevel);
  }

  /**
   * Sets the processing list.
   * 
   * @param mslevel the ms level of the queue
   * @param list New processing list.
   */
  public void setProcessingQueue(MSLevel mslevel, @NotNull DataPointProcessingQueue list) {
    if (list != null)
      processingParameters.setQueue(mslevel, list);
    else
      logger.warning(
          "The processing list for " + mslevel.toString() + " was about to be set to null.");
  }

  // public void setProcessingParameters(@NotNull DPPParameterValueWrapper
  // value) {
  // this.processingParameters = value;
  // }

  public DPPParameterValueWrapper getProcessingParameters() {
    return processingParameters;
  }
}
