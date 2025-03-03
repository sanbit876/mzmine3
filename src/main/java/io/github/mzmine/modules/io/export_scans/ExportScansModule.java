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

package io.github.mzmine.modules.io.export_scans;

import org.jetbrains.annotations.NotNull;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.ExitCode;

/**
 * Module for identifying peaks by searching custom databases file.
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster)
 */
public class ExportScansModule implements MZmineModule {

  private static final String MODULE_NAME = "Export spectra module";
  private static final String MODULE_DESCRIPTION = "Export spectra to different formats";

  @Override
  public @NotNull String getName() {
    return MODULE_NAME;
  }

  public @NotNull String getDescription() {
    return MODULE_DESCRIPTION;
  }

  /**
   * Show dialog for identifying a single peak-list row.
   *
   */
  public static void showSetupDialog(final Scan scan) {
    showSetupDialog(new Scan[] {scan});
  }

  public static void showSetupDialog(Scan[] scans) {
    final ExportScansParameters parameters = (ExportScansParameters) MZmineCore.getConfiguration()
        .getModuleParameters(ExportScansModule.class);;

    // Run task.
    if (parameters.showSetupDialog(true) == ExitCode.OK) {
      MZmineCore.getTaskController().addTask(new ExportScansTask(scans, parameters));
    }
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return ExportScansParameters.class;
  }

}
