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

package io.github.mzmine.modules.io.export_image_csv;

import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ImageToCsvExportModule implements MZmineModule {

  public static void showExportDialog(Collection<ModularFeatureListRow> rows, @NotNull Instant moduleCallDate) {

    final List<ModularFeature> features = rows.stream()
        .flatMap(ModularFeatureListRow::streamFeatures)
        .filter(f -> f.getFeatureStatus() != FeatureStatus.UNKNOWN).toList();

    ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(ImageToCsvExportModule.class);

    MZmineCore.runLater(() -> {
      ExitCode code = param.showSetupDialog(true);
      if (code == ExitCode.OK) {
        MZmineCore.getTaskController().addTask(new ImageToCsvExportTask(param, features, moduleCallDate));
      }
    });
  }

  @NotNull
  @Override
  public String getName() {
    return "Image to csv export";
  }

  @Nullable
  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {
    return ImageToCsvExportParameters.class;
  }
}
