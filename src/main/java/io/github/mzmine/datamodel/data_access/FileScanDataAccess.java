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

package io.github.mzmine.datamodel.data_access;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;

/**
 * The intended use of this memory access is to loop over all scans and access data points via
 * {@link #getMzValue(int)} and {@link #getIntensityValue(int)}
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class FileScanDataAccess extends ScanDataAccess {

  protected final int totalScans;

  /**
   * The intended use of this memory access is to loop over all scans and access data points via
   * {@link #getMzValue(int)} and {@link #getIntensityValue(int)}
   *
   * @param dataFile target data file to loop over all scans or mass lists
   * @param type     processed or raw data
   */
  protected FileScanDataAccess(RawDataFile dataFile,
      ScanDataType type) {
    super(dataFile, type);
    // count matching scans
    totalScans = dataFile.getScans().size();
  }

  @Override
  public Scan getCurrentScan() {
    return scanIndex >= 0 && scanIndex < totalScans ? dataFile.getScan(scanIndex) : null;
  }

  @Override
  public int getNumberOfScans() {
    return totalScans;
  }


}
