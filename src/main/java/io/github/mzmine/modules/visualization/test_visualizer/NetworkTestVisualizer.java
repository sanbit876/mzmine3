/*
 * Copyright 2006-2022 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.visualization.test_visualizer;

import java.io.File;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.ui.fx_viewer.FxViewPanel;
import org.graphstream.ui.fx_viewer.FxViewer;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.Viewer.ThreadingModel;

public class NetworkTestVisualizer extends Stage {

  private static final Logger logger = Logger.getLogger(NetworkTestVisualizer.class.getName());
  protected Graph graph;
  protected Viewer viewer;
  protected FxViewPanel view;

  public NetworkTestVisualizer() {

    Graph graph = new MultiGraph("Test-Graph");
    try {
      String filename = "imdb.dgs";
      URL file = getClass().getClassLoader().getResource(filename);
      graph.read(new File(file.toURI()).getAbsolutePath());
      viewer = new FxViewer(graph, ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
      viewer.enableAutoLayout();
      view = (FxViewPanel) viewer.addDefaultView(false);
      view.enableMouseOptions();
      StackPane graphpane = new StackPane(view);
      Scene scene = new Scene(graphpane, 800, 600);
      setTitle("Test_Visualizer");
      setScene(scene);
    } catch (Exception e) {
      // correct logging of exception
      logger.log(Level.SEVERE, e.getMessage(), e);
    }
  }

}
