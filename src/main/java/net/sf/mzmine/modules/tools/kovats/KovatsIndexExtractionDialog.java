/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.modules.tools.kovats;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.apache.commons.lang3.ArrayUtils;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.data.xy.XYDataset;
import com.google.common.collect.Range;
import net.miginfocom.swing.MigLayout;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.framework.listener.DelayedDocumentListener;
import net.sf.mzmine.modules.tools.kovats.KovatsValues.KovatsIndex;
import net.sf.mzmine.modules.visualization.tic.TICDataSet;
import net.sf.mzmine.modules.visualization.tic.TICPlot;
import net.sf.mzmine.modules.visualization.tic.TICPlotType;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;
import net.sf.mzmine.parameters.parametertypes.DoubleComponent;
import net.sf.mzmine.parameters.parametertypes.IntegerComponent;
import net.sf.mzmine.parameters.parametertypes.MassListComponent;
import net.sf.mzmine.parameters.parametertypes.MultiChoiceComponent;
import net.sf.mzmine.parameters.parametertypes.StringComponent;
import net.sf.mzmine.parameters.parametertypes.ranges.MZRangeComponent;
import net.sf.mzmine.parameters.parametertypes.ranges.RTRangeComponent;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesComponent;

public class KovatsIndexExtractionDialog extends ParameterSetupDialog {
  private static final long serialVersionUID = 1L;
  private Logger logger = Logger.getLogger(this.getClass().getName());

  private NumberFormat rtFormat = new DecimalFormat("0.###");

  private ParameterSet parameters;
  private Window parent;
  private JPanel newMainPanel;
  private JPanel pnChart;
  private KovatsIndex[] selectedKovats;

  // accepts saved files
  private Consumer<File> saveFileListener;
  private JTextField txtPeakPick;
  private StringComponent valuesComponent;
  private TICPlot chart;


  private String pickedValuesString;
  private TreeMap<KovatsIndex, Double> parsedValues = new TreeMap<>();
  private Double noiseLevel;

  /**
   * 
   * @param parent
   * @param parameters
   * @param saveFileListener accepts saved files
   */
  public KovatsIndexExtractionDialog(Window parent, ParameterSet parameters,
      Consumer<File> saveFileListener) {
    this(parent, parameters);
    this.saveFileListener = saveFileListener;
  }

  public KovatsIndexExtractionDialog(Window parent, ParameterSet parameters) {
    super(parent, false, parameters);
    this.parent = parent;
    this.parameters = parameters;
  }



  @Override
  protected void addDialogComponents() {
    super.addDialogComponents();
    mainPanel.removeAll();
    mainPanel.getParent().remove(mainPanel);

    DelayedDocumentListener ddlKovats = new DelayedDocumentListener(e -> updateKovatsList());
    DelayedDocumentListener ddlPeakPick = new DelayedDocumentListener(e -> updateChart());


    newMainPanel = new JPanel(new MigLayout("fill", "[right][grow,fill]", ""));
    getContentPane().add(newMainPanel, BorderLayout.SOUTH);

    JPanel pnCenter = new JPanel(new BorderLayout());
    getContentPane().add(pnCenter, BorderLayout.CENTER);
    pnChart = new JPanel(new BorderLayout());
    pnCenter.add(pnChart, BorderLayout.CENTER);

    // left: Kovats: min max and list
    JPanel west = new JPanel(new BorderLayout());
    newMainPanel.add(west);

    // add min max
    JPanel pnKovatsParam = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
    west.add(pnKovatsParam, BorderLayout.NORTH);
    IntegerComponent minc =
        (IntegerComponent) getComponentForParameter(KovatsIndexExtractionParameters.minKovats);
    IntegerComponent maxc =
        (IntegerComponent) getComponentForParameter(KovatsIndexExtractionParameters.maxKovats);
    minc.addDocumentListener(ddlKovats);
    maxc.addDocumentListener(ddlKovats);

    pnKovatsParam.add(new JLabel("Min carbon:"));
    pnKovatsParam.add(minc);
    pnKovatsParam.add(new JLabel("Max carbon:"));
    pnKovatsParam.add(maxc);

    // kovats list
    JPanel pnKovatsSelect = new JPanel(new BorderLayout());
    west.add(pnKovatsSelect, BorderLayout.CENTER);
    MultiChoiceComponent kovatsc =
        (MultiChoiceComponent) getComponentForParameter(KovatsIndexExtractionParameters.kovats);
    kovatsc.addValueChangeListener(() -> handleKovatsSelectionChange());
    pnKovatsSelect.add(kovatsc, BorderLayout.CENTER);

    // center: Chart and parameters
    JPanel center = new JPanel(new BorderLayout());
    newMainPanel.add(center);

    // all parameters on peak pick panel
    JPanel pnSouth = new JPanel(new BorderLayout());
    center.add(pnSouth, BorderLayout.SOUTH);

    JPanel pnPeakPick = new JPanel(new MigLayout("", "[right][][]", ""));
    pnSouth.add(pnPeakPick, BorderLayout.CENTER);

    valuesComponent = (StringComponent) getComponentForParameter(
        KovatsIndexExtractionParameters.pickedKovatsValues);
    RawDataFilesComponent rawc =
        (RawDataFilesComponent) getComponentForParameter(KovatsIndexExtractionParameters.dataFiles);
    MassListComponent massc =
        (MassListComponent) getComponentForParameter(KovatsIndexExtractionParameters.massList);
    MZRangeComponent mzc =
        (MZRangeComponent) getComponentForParameter(KovatsIndexExtractionParameters.mzRange);
    RTRangeComponent rtc =
        (RTRangeComponent) getComponentForParameter(KovatsIndexExtractionParameters.rtRange);
    DoubleComponent noisec =
        (DoubleComponent) getComponentForParameter(KovatsIndexExtractionParameters.noiseLevel);

    valuesComponent.addDocumentListener(new DelayedDocumentListener(e -> kovatsValuesChanged()));
    valuesComponent.setLayout(new GridLayout(1, 1));
    pnCenter.add(valuesComponent, BorderLayout.SOUTH);

    JButton btnUpdateChart = new JButton("Update chart");
    btnUpdateChart.addActionListener(e -> updateChart());
    pnPeakPick.add(btnUpdateChart, "cell 0 0");
    JButton btnPickRT = new JButton("Pick peaks");
    btnPickRT.addActionListener(e -> pickRetentionTimes());
    pnPeakPick.add(btnPickRT, "cell 1 0");
    JButton btnSaveFile = new JButton("Save to file");
    btnSaveFile.addActionListener(e -> saveToFile());
    pnPeakPick.add(btnSaveFile, "cell 2 0");

    pnPeakPick.add(new JLabel("Raw data file"), "cell 0 1");
    pnPeakPick.add(rawc);
    pnPeakPick.add(new JLabel(KovatsIndexExtractionParameters.massList.getName()), "cell 0 2");
    pnPeakPick.add(massc);
    pnPeakPick.add(new JLabel("m/z range"), "cell 0 3");
    pnPeakPick.add(mzc);
    pnPeakPick.add(new JLabel(KovatsIndexExtractionParameters.rtRange.getName()), "cell 0 4");
    pnPeakPick.add(rtc);
    pnPeakPick.add(new JLabel(KovatsIndexExtractionParameters.noiseLevel.getName()), "cell 0 5");
    pnPeakPick.add(noisec);

    // add listeners


    // show
    revalidate();
    updateMinimumSize();
    pack();
  }

  /**
   * replace markers
   */
  private void kovatsValuesChanged() {
    // parse values
    if (parseValues() && chart != null) {
      //
      chart.getChart().getXYPlot().clearDomainMarkers();

      for (Entry<KovatsIndex, Double> e : parsedValues.entrySet()) {
        ValueMarker marker = new ValueMarker(e.getValue());
        marker.setLabel(e.getKey().getShortName());
        chart.getChart().getXYPlot().addDomainMarker(marker);
      }

      revalidate();
      repaint();
    }
  }

  private boolean parseValues() {
    updateParameterSetFromComponents();
    parsedValues.clear();
    try {
      String[] entries = pickedValuesString.split(",");
      for (String s : entries) {
        if (s.isEmpty())
          continue;
        String[] e = s.split(":");
        parsedValues.put(KovatsIndex.getByShortName(e[0]), Double.parseDouble(e[1]));
      }
      return true;
    } catch (Exception e) {
      logger.log(Level.WARNING, "Format of picked values is wrong");
      return false;
    }
  }

  private void saveToFile() {
    File lastFile =
        parameters.getParameter(KovatsIndexExtractionParameters.lastSavedFile).getValue();
    JFileChooser chooser = new JFileChooser();
    FileNameExtensionFilter ff = new FileNameExtensionFilter("Comma-separated values", "csv");
    chooser.addChoosableFileFilter(ff);
    chooser.setFileFilter(ff);
    if (lastFile != null)
      chooser.setSelectedFile(lastFile);

    if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
      File f = chooser.getSelectedFile();
      parameters.getParameter(KovatsIndexExtractionParameters.lastSavedFile).setValue(f);

      // save
      try {
        // TODO save data


        saveFileListener.accept(f);
      } catch (Exception e) {
        logger.log(Level.WARNING, "Error while saving Kovats file to " + f.getAbsolutePath(), e);
      }
    }
  }

  private void updateChart() {
    updateParameterSetFromComponents();
    try {
      RawDataFile file = parameters.getParameter(KovatsIndexExtractionParameters.dataFiles)
          .getValue().getMatchingRawDataFiles()[0];
      Range<Double> rangeMZ =
          parameters.getParameter(KovatsIndexExtractionParameters.mzRange).getValue();
      Range<Double> rangeRT =
          parameters.getParameter(KovatsIndexExtractionParameters.rtRange).getValue();
      Scan[] scans = Arrays.stream(file.getScanNumbers(1, rangeRT))
          .mapToObj(scani -> file.getScan(scani)).toArray(Scan[]::new);

      // create dataset
      TICDataSet data = new TICDataSet(file, scans, rangeMZ, null, TICPlotType.BASEPEAK);
      chart = new TICPlot(this);
      chart.addTICDataset(data);

      kovatsValuesChanged();
      pnChart.removeAll();
      pnChart.add(chart, BorderLayout.CENTER);
      revalidate();
      repaint();
    } catch (Exception e) {
      logger.log(Level.WARNING, "Peak picking parameters incorrect");
    }
  }

  @Override
  protected void updateParameterSetFromComponents() {
    super.updateParameterSetFromComponents();
    selectedKovats = parameterSet.getParameter(KovatsIndexExtractionParameters.kovats).getValue();
    pickedValuesString =
        parameterSet.getParameter(KovatsIndexExtractionParameters.pickedKovatsValues).getValue();
    noiseLevel = parameterSet.getParameter(KovatsIndexExtractionParameters.noiseLevel).getValue();
  }

  private void updateKovatsList() {
    updateParameterSetFromComponents();
    try {
      int min = parameterSet.getParameter(KovatsIndexExtractionParameters.minKovats).getValue();
      int max = parameterSet.getParameter(KovatsIndexExtractionParameters.maxKovats).getValue();
      KovatsIndex[] newValues = KovatsIndex.getRange(min, max);
      KovatsIndex[] newSelected = Stream.of(newValues)
          .filter(k -> ArrayUtils.contains(selectedKovats, k)).toArray(KovatsIndex[]::new);


      parameterSet.getParameter(KovatsIndexExtractionParameters.kovats).setChoices(newValues);
      parameterSet.getParameter(KovatsIndexExtractionParameters.kovats).setValue(newSelected);
      MultiChoiceComponent kovatsc =
          (MultiChoiceComponent) getComponentForParameter(KovatsIndexExtractionParameters.kovats);
      kovatsc.setChoices(newValues);
      kovatsc.setValue(newSelected);
      revalidate();
      repaint();
      // update parameters again
      updateParameterSetFromComponents();
    } catch (Exception e) {
    }
  }


  /**
   * Kovats list has changed
   */
  private void handleKovatsSelectionChange() {
    updateParameterSetFromComponents();
    // keep rt values
    StringBuilder s = new StringBuilder();
    int i = 0;
    double lastRT = 0;
    for (KovatsIndex ki : selectedKovats) {
      Double rt = lastRT + 1;
      if (parsedValues != null)
        rt = parsedValues.getOrDefault(ki, rt);

      s.append(ki.name() + ":" + rtFormat.format(rt) + ",");
      i++;
      lastRT = rt;
    }
    valuesComponent.setText(s.toString());
    kovatsValuesChanged();
  }

  private void pickRetentionTimes() {
    updateParameterSetFromComponents();
    XYDataset data = getData();
    if (data == null)
      return;

    List<Double> results = new ArrayList<>();

    double max = 0;
    double startI = 0;
    double maxRT = 0;

    int items = data.getItemCount(0);
    for (int i = 0; i < items; i++) {
      double rt = data.getXValue(0, i);
      double intensity = data.getYValue(0, i);

      // find max
      if (intensity > max && intensity >= noiseLevel) {
        max = intensity;
        maxRT = rt;
      }

      // minimize startI above noiseLevel
      if (intensity >= noiseLevel && (startI == 0 || startI > intensity)) {
        startI = intensity;
      }

      // is peak?
      if (max / startI > 4 && max / intensity > 4) {
        // add peak
        results.add(maxRT);

        max = 0;
        startI = intensity;
        maxRT = rt;
      }
    }

    // set rt values
    StringBuilder s = new StringBuilder();
    int i = 0;
    double lastRT = 1;
    for (KovatsIndex ki : selectedKovats) {
      double rt = i < results.size() ? results.get(i) : lastRT + 1;
      s.append(ki.name() + ":" + rtFormat.format(rt) + ",");
      i++;
      lastRT = rt;
    }
    valuesComponent.setText(s.toString());
    kovatsValuesChanged();
  }

  private XYDataset getData() {
    return chart == null ? null : chart.getXYPlot().getDataset();
  }
}
