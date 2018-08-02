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

package org.hypernomicon.bib;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hypernomicon.bib.lib.BibCollection;

import javafx.collections.ObservableList;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

import static org.hypernomicon.bib.CollectionTree.BibCollectionType.*;

public class CollectionTree
{
  public static enum BibCollectionType { bctAll, bctUnsorted, bctTrash, bctUser }
  
  private TreeView<BibCollectionRow> treeView;
  private BibCollectionRow treeRowAllEntries, treeRowUnsorted, treeRowTrash;
  private HashMap<String, BibCollectionRow> keyToRow;

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------

  public void selectAllEntries()    { treeView.getSelectionModel().select(treeRowAllEntries.getTreeItem()); }  
  public void selectTrash()         { treeView.getSelectionModel().select(treeRowTrash.getTreeItem()); }
  public void selectKey(String key) { treeView.getSelectionModel().select(keyToRow.get(key).getTreeItem()); }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------

  public CollectionTree(TreeView<BibCollectionRow> treeView)
  {
    this.treeView = treeView;
    
    treeView.setCellFactory(theTreeView -> 
    {
      TreeCell<BibCollectionRow> row = new TreeCell<>();
      
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
          row.setText(newValue.getText());
        }
      });
      
      return row;
    });
  }
  
//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------
  
  public void clear()
  {
    if (treeView.getRoot() != null)
    {
      treeView.getRoot().getChildren().clear();
      treeView.setRoot(null);
    }    
   
    keyToRow = new HashMap<>();
    
    treeView.setRoot(new TreeItem<BibCollectionRow>(null));
    treeView.setShowRoot(false);
    
    treeRowAllEntries = new BibCollectionRow(bctAll); 
    treeRowUnsorted = new BibCollectionRow(bctUnsorted); 
    treeRowTrash = new BibCollectionRow(bctTrash);
    
    treeView.getRoot().getChildren().add(treeRowAllEntries.getTreeItem());
    treeView.getRoot().getChildren().add(treeRowUnsorted.getTreeItem());
    treeView.getRoot().getChildren().add(treeRowTrash.getTreeItem());
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------

  public void refresh(Map<String, BibCollection> keyToColl)
  {
    pruneNode(treeView.getRoot(), keyToColl);
    
    keyToColl.entrySet().forEach(entry ->
    {
      if (keyToRow.containsKey(entry.getKey()) == false)
        addToTree(entry.getKey(), entry.getValue(), keyToColl);
    });
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------

  private TreeItem<BibCollectionRow> addToTree(String childKey, BibCollection childColl, Map<String, BibCollection> keyToColl)
  {
    BibCollectionRow childRow;
    TreeItem<BibCollectionRow> parentItem;
    
    String parentKey = childColl.getParentKey();
    
    if (parentKey == null)
      parentItem = treeView.getRoot();
    else if (keyToRow.containsKey(parentKey))
      parentItem = keyToRow.get(parentKey).getTreeItem();
    else
      parentItem = addToTree(parentKey, keyToColl.get(parentKey), keyToColl);
    
    childRow = new BibCollectionRow(childColl);
    keyToRow.put(childKey, childRow);
    insertTreeItem(parentItem.getChildren(), childRow);
    
    return childRow.getTreeItem();
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------

  private void pruneNode(TreeItem<BibCollectionRow> treeItem, Map<String, BibCollection> keyToColl)
  {
    Iterator<TreeItem<BibCollectionRow>> it = treeItem.getChildren().iterator();
    
    while (it.hasNext())
    {
      boolean removed = false;      
      TreeItem<BibCollectionRow> childItem = it.next();
      
      BibCollectionRow row = childItem.getValue();
      if (row.getType() == BibCollectionType.bctUser)
      {
        String key = row.getKey();
        
        if (keyToColl.containsKey(key) == false)
        {
          it.remove();
          keyToRow.remove(key);
          removed = true;
        }
        else
          row.updateCollObj(keyToColl.get(key));
      }
      
      if (removed == false)
        pruneNode(childItem, keyToColl);
    }
  }
//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------

  public void rebuild(Map<String, BibCollection> keyToColl)
  {
    clear();
    
    keyToColl.entrySet().forEach(entry ->
    {
      BibCollection childColl = entry.getValue();
      String childKey = entry.getKey();
      
      BibCollectionRow childRow = keyToRow.get(childKey);
      if (childRow == null)
      {
        childRow = new BibCollectionRow(childColl);
        keyToRow.put(childKey, childRow);
      }
     
      String parentKey = childColl.getParentKey();
      if (parentKey == null)
      {
        insertTreeItem(treeView.getRoot().getChildren(), childRow);
      }
      else
      {        
        BibCollection parentColl = keyToColl.get(parentKey);
        BibCollectionRow parentRow = keyToRow.get(parentKey);
        if (parentRow == null)
        {          
          parentRow = new BibCollectionRow(parentColl);
          keyToRow.put(parentKey, parentRow);
        }
        
        insertTreeItem(parentRow.getTreeItem().getChildren(), childRow);
      }      
    });
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------

  private void insertTreeItem(ObservableList<TreeItem<BibCollectionRow>> list, BibCollectionRow newRow)
  {
    for (int ndx = 0; ndx < list.size(); ndx++)
    {
      if (newRow.getSortKey().compareTo(list.get(ndx).getValue().getSortKey()) < 0)
      {
        list.add(ndx, newRow.getTreeItem());
        return;
      }
    }
    
    list.add(newRow.getTreeItem());
  }
 
//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------

}
