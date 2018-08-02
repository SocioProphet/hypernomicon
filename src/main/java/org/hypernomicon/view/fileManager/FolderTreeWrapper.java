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

package org.hypernomicon.view.fileManager;

import static org.hypernomicon.App.*;
import static org.hypernomicon.model.HyperDB.*;
import static org.hypernomicon.util.Util.*;
import static org.hypernomicon.util.Util.MessageDialogType.*;

import java.util.ArrayList;
import java.util.Iterator;

import org.hypernomicon.model.HyperDB;
import org.hypernomicon.model.items.HyperPath;
import org.hypernomicon.model.records.HDT_Base;
import org.hypernomicon.model.records.HDT_Folder;
import org.hypernomicon.model.records.SimpleRecordTypes.HDT_RecordWithPath;
import org.hypernomicon.util.filePath.FilePath;

import static org.hypernomicon.model.records.HDT_RecordType.*;
import org.hypernomicon.view.wrappers.AbstractTreeWrapper;
import org.hypernomicon.view.wrappers.DragNDropHoverHelper;
import org.hypernomicon.view.wrappers.TreeModel;
import org.hypernomicon.view.wrappers.DragNDropHoverHelper.DragNDropContainer;
import javafx.scene.control.SelectionModel;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;

public class FolderTreeWrapper extends AbstractTreeWrapper<FileRow> implements DragNDropContainer<FileRow>
{
  private TreeView<FileRow> tv;
  private TreeModel<FileRow> treeModel;
  private DragNDropHoverHelper<FileRow> ddHoverHelper = new DragNDropHoverHelper<>();
  private FileTable fileTable;
  
//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------

  public TreeModel<FileRow> getTreeModel()                               { return treeModel; }
  
  @Override public TreeItem<FileRow> getRoot()                           { return tv.getRoot(); }
  @Override public void expandMainBranches()                             { treeModel.expandMainBranch(); }
  @Override public void removeRecord(HDT_Base record)                    { treeModel.removeRecord(record); }
  @Override public SelectionModel<TreeItem<FileRow>> getSelectionModel() { return tv.getSelectionModel(); }
  @Override public void focusOnTreeCtrl()                                { safeFocus(tv); }
  @Override public void scrollToNdx(int ndx)                             { tv.scrollTo(ndx); }
  @Override public TreeItem<FileRow> getTreeItem(FileRow treeRow)        { return treeRow.getTreeItem(); }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------
  
  public FolderTreeWrapper(TreeView<FileRow> tv, FileTable fileTable)
  {
    this.tv = tv;
    this.fileTable = fileTable;
    
    treeModel = new TreeModel<FileRow>(this, null);
    
    clear();
    
    db.addCloseDBHandler(this::reset);
    db.addPreDBChangeHandler(this::reset);
    
    tv.setCellFactory(treeView -> 
    {
      TreeCell<FileRow> row = new TreeCell<>();
      
      ImageView openImage = getImageViewForRelativePath(ui.getGraphicRelativePathByType(hdtFolder));
      
      row.itemProperty().addListener((observable, oldValue, newValue) ->
      {
        if (oldValue == newValue) return;
           
        if (newValue == null)
        {
          row.setText(null);
          row.setGraphic(null);
          row.setContextMenu(null);
        }
        else
        {
          if (newValue.getFilePath() == null) // happens right before a filerow is deleted sometimes
            return;
                 
          row.setText(newValue.getFileName());
          if (row.getGraphic() == null)
            row.setGraphic(openImage);
          
          row.setContextMenu(FileRow.createContextMenu(newValue, fileTable.contextMenuSchemata));
        }
      });
      
      DragNDropHoverHelper.setupHandlers(row, this);
      
      return row;
    });
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public ArrayList<FileRow> getRowsForRecord(HDT_Base record) 
  { 
    ArrayList<FileRow> list = new ArrayList<>();
    
    list.addAll(treeModel.getRowsForRecord(record));
    
    return list;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public void clear()
  {
    if (tv.getRoot() != null)
    {
      tv.getRoot().getChildren().clear();
      tv.setRoot(null);
    }    
   
    tv.setRoot(new TreeItem<FileRow>(null));
    tv.setShowRoot(false);

    treeModel.clear();
  }
  
//---------------------------------------------------------------------------
//--------------------------------------------------------------------------- 

  @Override public void reset()
  {
    super.reset();
    
    treeModel.reset(db.folders.getByID(HyperDB.ROOT_FOLDER_ID));
  }
    
//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------

  @Override public FileRow newRow(HDT_Base record, TreeModel<FileRow> treeModel)
  {
    if (record.getType() == hdtFolder)
    {    
      return new FileRow(HDT_Folder.class.cast(record).getPath(), treeModel);
    }
      
    messageDialog("Internal error #18726", mtError);
    return null;
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------

  public void prune()
  {
    treeModel.pruningOperationInProgress = true; // prevent ConcurrentModificationException   
    
    pruneNode(getRoot());
    
    treeModel.pruningOperationInProgress = false;
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------

  private void pruneNode(TreeItem<FileRow> nodeItem)
  {
    Iterator<TreeItem<FileRow>> it = nodeItem.getChildren().iterator();
    
    while (it.hasNext())
    {
      boolean removed = false;
      
      TreeItem<FileRow> childItem = it.next();
      
      FileRow fileRow = childItem.getValue();
      if (fileRow != null)
      {
        HyperPath hyperPath = fileRow.getHyperPath();
        if (hyperPath.getRecordsString().length() == 0)
        {
          HDT_RecordWithPath folder = hyperPath.getRecord();
          if (folder.getPath().getFilePath().exists() == false)
          {
            HDT_Folder.deleteFolderRecordTree((HDT_Folder) folder);                        
            it.remove();

            removed = true;
          }
        }
      }
      
      if (removed == false)
        pruneNode(childItem);
    }
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------

  @Override public void dragDone()
  {
    fileTable.dragDone();
    ddHoverHelper.reset();
  }

  @Override public void startDrag(FileRow row)
  {
    fileTable.startDragFromFolderTree(row);
  }

  @Override public void dragDroppedOnto(FileRow row)
  {
    fileTable.dragDroppedOnto(row);
  }
  
//---------------------------------------------------------------------------
//---------------------------------------------------------------------------  

  @Override public boolean acceptDrag(FileRow targetRow, DragEvent dragEvent, TreeItem<FileRow> treeItem)
  {
    ddHoverHelper.scroll(dragEvent, tv);
    
    if (fileTable.draggingRows == null) return false;
    if (targetRow.isDirectory() == false) return false;
    if (fileTable.draggingRows.size() == 1)
    {
      FilePath srcPath = fileTable.draggingRows.get(0).row.getFilePath();
      if (srcPath.equals(targetRow.getFilePath())) return false;
      if (srcPath.getDirOnly().equals(targetRow.getFilePath())) return false;
    }
       
    ddHoverHelper.expand(treeItem);
       
    return true;
  }
  
  @Override public DragNDropHoverHelper<FileRow> getHelper() { return ddHoverHelper; }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------

}
