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

import static org.hypernomicon.util.Util.messageDialog;
import static org.hypernomicon.util.Util.safeFocus;
import static org.hypernomicon.util.Util.MessageDialogType.mtError;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

public class NewQueryFavDialogController extends HyperDialog
{
  @FXML private TextField tfName;
  @FXML private CheckBox chkAutoExec;
  @FXML private Button btnOk;
  @FXML private Button btnCancel;
  
//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------  
  
  public String getNewName()   { return tfName.getText(); }
  public boolean getAutoExec() { return chkAutoExec.isSelected(); }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------  

  public static NewQueryFavDialogController create(String title, String newName)
  {
    NewQueryFavDialogController nqf = HyperDialog.create("NewQueryFavDialog.fxml", title, true);
    nqf.init(newName);
    return nqf;        
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------  

  private void init(String newName)
  {
    tfName.setText(newName);
    
    onShown = () -> safeFocus(tfName);
  }
  
//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------  
  
  @Override protected boolean isValid()
  {
    if (tfName.getText().length() == 0)
    {
      messageDialog("Name cannot be zero-length.", mtError);
      return false;
    }
    
    return true;
  }

//---------------------------------------------------------------------------  
//---------------------------------------------------------------------------  
  
}
