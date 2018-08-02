/*
 * Copyright 2015-2018 Jason Winning
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package org.hypernomicon.view.dialogs;

import java.util.ArrayList;

import static org.hypernomicon.util.Util.*;

import org.hypernomicon.view.wrappers.HyperTable;
import org.hypernomicon.view.wrappers.HyperTableColumn;
import org.hypernomicon.view.wrappers.HyperTableRow;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

//---------------------------------------------------------------------------  

public class ObjectOrderDialogController extends HyperDialog
{
  @FXML private Button btnMoveUp;
  @FXML private Button btnMoveDown;
  @FXML private Button btnOK;
  @FXML private TableView<HyperTableRow> tv;
  
  private ObservableList<HyperTableRow> rows;
   
//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------
  
  public static ObjectOrderDialogController create(String title, HyperTable ht, ObservableList<HyperTableRow> rows)
  {
    ObjectOrderDialogController ood = HyperDialog.create("ObjectOrderDialog.fxml", title, true);
    ood.init(ht, rows);
    return ood;
  }
  
//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------
  
  private void init(HyperTable ht, ObservableList<HyperTableRow> rows)
  {
    ArrayList<TableColumn<HyperTableRow, String>> tableCols = new ArrayList<>();
    
    this.rows = rows;
    tv.getColumns().clear();
    
    for (HyperTableColumn htCol : ht.getColumns())
    {
      switch (htCol.getCtrlType())
      {
        case ctDropDown: case ctDropDownList: case ctEdit: case ctNone:

          TableColumn<HyperTableRow, String> col = new TableColumn<>();
          
          col.setText(htCol.getHeader());
          col.setSortable(false);
          col.setEditable(false);
          
          col.setCellValueFactory(cellData ->
          {
            return new SimpleStringProperty(cellData.getValue().getText(htCol.getColNdx()));
          });
          
          tv.getColumns().add(col);
          tableCols.add(col);
          
          break;               
        default: break;
      }
    }
    
    tv.itemsProperty().bindBidirectional(ht.tv.itemsProperty());
    
    HyperTable.preventMovingColumns(tv, tableCols);
    
    getStage().setOnHidden(event -> tv.itemsProperty().unbindBidirectional(ht.tv.itemsProperty()));
    
    btnMoveUp.setOnAction(event -> moveUp());
    
    btnMoveDown.setOnAction(event -> moveDown());
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------  
  
  private void moveUp()
  {
    int ndx = tv.getSelectionModel().getSelectedIndex();
    if (ndx < 1) return;
    
    HyperTableRow row = rows.remove(ndx);
    rows.add(ndx - 1, row);
    tv.getSelectionModel().select(row);
    safeFocus(tv);
  }
  
//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------  
  
  private void moveDown()
  {
    int ndx = tv.getSelectionModel().getSelectedIndex();
    if (ndx < 0) return;
    if (ndx == (tv.getItems().size() - 1)) return;
    
    HyperTableRow row = rows.remove(ndx);
    rows.add(ndx + 1, row);
    tv.getSelectionModel().select(row);
    safeFocus(tv);
  }
  
//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------  
  
  @Override protected boolean isValid()
  {
    return true;
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------  
}
