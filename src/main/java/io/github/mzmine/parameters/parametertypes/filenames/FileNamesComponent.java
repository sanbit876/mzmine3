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

package io.github.mzmine.parameters.parametertypes.filenames;

import com.google.common.collect.ImmutableList;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

public class FileNamesComponent extends BorderPane {

  public static final Font smallFont = new Font("SansSerif", 10);

  private final TextArea txtFilename;
  private final CheckBox useSubFolders;

  private final List<ExtensionFilter> filters;
  private final Path defaultDir;

  public FileNamesComponent(List<ExtensionFilter> filters, Path defaultDir) {

    this.filters = ImmutableList.copyOf(filters);
    this.defaultDir = defaultDir;

    txtFilename = new TextArea();
    txtFilename.setPrefColumnCount(65);
    txtFilename.setPrefRowCount(6);
    txtFilename.setFont(smallFont);
    initDragDropped();

    Button btnFileBrowser = new Button("Select files");
    btnFileBrowser.setMaxWidth(Double.MAX_VALUE);
    btnFileBrowser.setOnAction(e -> {
      // Create chooser.
      FileChooser fileChooser = new FileChooser();
      if(defaultDir != null) {
        fileChooser.setInitialDirectory(defaultDir.toFile());
      }
      fileChooser.setTitle("Select files");

      fileChooser.getExtensionFilters().addAll(this.filters);

      String[] currentPaths = txtFilename.getText().split("\n");
      if (currentPaths.length > 0) {
        File currentFile = new File(currentPaths[0].trim());
        File currentDir = currentFile.getParentFile();
        if (currentDir != null && currentDir.exists()) {
          fileChooser.setInitialDirectory(currentDir);
        }
      }

      // Open chooser.
      List<File> selectedFiles = fileChooser.showOpenMultipleDialog(null);
      if (selectedFiles == null) {
        return;
      }
      setValue(selectedFiles.toArray(new File[0]));
    });

    useSubFolders = new CheckBox("In sub folders");
    useSubFolders.setSelected(false);

    Button btnClear = new Button("Clear");
    btnClear.setMaxWidth(Double.MAX_VALUE);
    btnClear.setOnAction(e -> txtFilename.setText(""));

    GridPane buttonGrid = new GridPane();
    ColumnConstraints b1 = new ColumnConstraints(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE,
        USE_COMPUTED_SIZE, Priority.ALWAYS, HPos.CENTER, true);
    ColumnConstraints b2 = new ColumnConstraints(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE,
        USE_COMPUTED_SIZE, Priority.ALWAYS, HPos.CENTER, true);
    buttonGrid.getColumnConstraints().addAll(b1, b2);

    buttonGrid.setHgap(1);
    buttonGrid.setVgap(3);
    buttonGrid.setPadding(new Insets(0, 0, 0, 5));

    buttonGrid.add(btnFileBrowser, 0, 0);
    buttonGrid.add(btnClear, 1, 0);
    buttonGrid.add(useSubFolders, 0, 1, 2, 1);

    List<Button> directoryButtons = createFromDirectoryBtns(filters);
    int startRow = 2;
    buttonGrid.add(directoryButtons.remove(0), 0, startRow, 2, 1);
    for (int i = 0; i < directoryButtons.size(); i++) {
      buttonGrid.add(directoryButtons.get(i), i % 2, startRow + 1 + i / 2);
      directoryButtons.get(i).getParent().layout();
    }
    buttonGrid.layout();

    // main gridpane
    this.setCenter(txtFilename);
    this.setRight(buttonGrid);
  }

  private List<Button> createFromDirectoryBtns(List<ExtensionFilter> filters) {
    List<Button> btns = new ArrayList<>();
    for (ExtensionFilter filter : filters) {
      if (filter.getExtensions().isEmpty() || filter.getExtensions().get(0).equals("*.*")) {
        continue;
      }
      String name = filter.getExtensions().size() > 3 ? "From folder"
          : "All " + filter.getExtensions().get(0);

      Button btnFromDirectory = new Button(name);
      btnFromDirectory.setMinWidth(USE_COMPUTED_SIZE);
      btnFromDirectory.setPrefWidth(USE_COMPUTED_SIZE);
      btnFromDirectory.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
      btnFromDirectory.setTooltip(new Tooltip("All files in folder (sub folders)"));
      btns.add(btnFromDirectory);
      btnFromDirectory.setOnAction(e -> {
        // Create chooser.
        DirectoryChooser fileChooser = new DirectoryChooser();
        fileChooser.setTitle("Select a folder");
        setInitialDirectory(fileChooser);

        // Open chooser.
        File dir = fileChooser.showDialog(null);
        if (dir == null) {
          return;
        }

        // list all files in sub directories
        setValue(FileAndPathUtil.findFilesInDirFlat(dir, filter, useSubFolders.isSelected()));
      });
    }
    return btns;
  }

  /**
   * When creating a new chooser set the initial directory to the currently selected files
   *
   * @param fileChooser target chooser
   */
  private void setInitialDirectory(DirectoryChooser fileChooser) {
    String[] currentPaths = txtFilename.getText().split("\n");
    if (currentPaths.length > 0) {
      File currentFile = new File(currentPaths[0].trim());
      File currentDir = currentFile.getParentFile();
      if (currentDir != null && currentDir.exists()) {
        fileChooser.setInitialDirectory(currentDir);
      }
    }
  }

  public File[] getValue() {
    String[] fileNameStrings = txtFilename.getText().split("\n");
    List<File> files = new ArrayList<>();
    for (String fileName : fileNameStrings) {
      if (fileName.trim().equals("")) {
        continue;
      }
      files.add(new File(fileName.trim()));
    }
    return files.toArray(new File[0]);
  }

  public void setValue(File[] value) {
    if (value == null) {
      txtFilename.setText("");
      return;
    }
    StringBuilder b = new StringBuilder();
    for (File file : value) {
      b.append(file.getPath());
      b.append("\n");
    }
    txtFilename.setText(b.toString());
  }

  public void setToolTipText(String toolTip) {
    txtFilename.setTooltip(new Tooltip(toolTip));
  }

  private void initDragDropped() {
    txtFilename.setOnDragOver(e -> {
      if (e.getGestureSource() != this && e.getGestureSource() != txtFilename && e.getDragboard()
          .hasFiles()) {
        e.acceptTransferModes(TransferMode.COPY_OR_MOVE);
      }
      e.consume();
    });
    txtFilename.setOnDragDropped(e -> {
      if (e.getDragboard().hasFiles()) {
        final List<File> files = e.getDragboard().getFiles();
        final List<String> patterns = new ArrayList<>();

        StringBuilder sb = new StringBuilder(txtFilename.getText());
        if (!sb.toString().endsWith("\n")) {
          sb.append("\n");
        }

        filters.stream().flatMap(f -> f.getExtensions().stream()).forEach(
            extension -> patterns.add(extension.toLowerCase().replace("*", "").toLowerCase()));

        for (File file : files) {
          if (patterns.stream()
              .anyMatch(filter -> file.getAbsolutePath().toLowerCase().endsWith(filter))) {
            sb.append(file.getPath()).append("\n");
          }
        }

        txtFilename.setText(sb.toString());

        e.setDropCompleted(true);
        e.consume();
      }
    });
  }
}
