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

package org.hypernomicon.model;

import static org.hypernomicon.model.HyperDB.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.hypernomicon.model.Exceptions.SearchKeyException;
import org.hypernomicon.model.records.HDT_Base;

import static org.hypernomicon.util.Util.*;

public final class SearchKeys
{
  //---------------------------------------------------------------------------
  //---------------------------------------------------------------------------

  public static final class SearchKeyword
  {
    public final String text;
    public final boolean startOnly, endOnly;    
    public final HDT_Base record;
    public boolean expired = false;

  //---------------------------------------------------------------------------
    
    public SearchKeyword(String newKeyword, HDT_Base newRecord)
    {
      record = newRecord;

      if (newKeyword.length() == 0) 
      {
        text = "";
        startOnly = false;
        endOnly = false;
        return;
      }

      if (newKeyword.startsWith("^"))
      {
        startOnly = true;
        newKeyword = newKeyword.substring(1);
      }
      else
        startOnly = false;
      
      if (newKeyword.endsWith("$"))
      {
        endOnly = true;
        newKeyword = newKeyword.substring(0, newKeyword.length() - 1);
      }
      else
        endOnly = false;
      
      text = newKeyword;
    }
    
  //---------------------------------------------------------------------------
  //---------------------------------------------------------------------------
    
    public final String toString()  { return (startOnly ? "^" + text : text) + (endOnly ? "$" : ""); }
    public final String getPrefix() { return text.substring(0, 3).toLowerCase(); } 

  //---------------------------------------------------------------------------
  //---------------------------------------------------------------------------

  }
  
  private final Map<String, Map<String, SearchKeyword>> prefixStrToKeywordStrToKeywordObj;
  private final Map<HDT_Base, Map<String, SearchKeyword>> recordToKeywordStrToKeywordObj;

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public SearchKeys()
  {
    prefixStrToKeywordStrToKeywordObj = Collections.synchronizedMap(new LinkedHashMap<>());
    recordToKeywordStrToKeywordObj = Collections.synchronizedMap(new LinkedHashMap<>());
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public void removeAll()
  { 
    prefixStrToKeywordStrToKeywordObj.clear();
    recordToKeywordStrToKeywordObj.clear();
  };

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------
  
  public List<SearchKeyword> getKeywordsByPrefix(String prefix)
  {
    ArrayList<SearchKeyword> list = new ArrayList<>();
    
    Map<String, SearchKeyword> keyStringToKeyObject = prefixStrToKeywordStrToKeywordObj.get(prefix.toLowerCase());
    
    if (keyStringToKeyObject != null)    
      list.addAll(keyStringToKeyObject.values());
    
    return list;          
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public static String prepSearchKey(String newKey)
  {
    newKey = ultraTrim(newKey);
    newKey = newKey.replaceAll("\\h", " ");
    
    while (newKey.contains("  "))
      newKey = newKey.replace("  ", " ");
    
    newKey = convertToEnglishChars(newKey);
    newKey = newKey.replaceAll("\\p{Pd}", "-"); // treat all dashes the same within search keyword

    return newKey;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public void setSearchKey(HDT_Base record, String newKey, boolean noMod) throws SearchKeyException
  {
    SearchKeyword keyword;
    
    newKey = prepSearchKey(newKey);
       
    if (newKey.equals(getStringForRecord(record))) return;
    
    if ((newKey.length() == 1) || (newKey.length() == 2))
      throw new SearchKeyException(true, record.getID(), record.getType(), newKey);
    
    expireAll(record);
    
  // Loop through new substrings
  // ---------------------------
    for (String subStr : newKey.split(";"))
    {
      keyword = new SearchKeyword(subStr.trim(), record);
  
      if (keyword.text.length() > 0)
      {
  // If the substring is too short, error out
  // ----------------------------------------
        if (keyword.text.length() < 3)
        {
          unexpireAll(record);
          throw new SearchKeyException(true, record.getID(), record.getType(), keyword.text);
        }
  
        HDT_Base existingRecord = getRecordForKeywordStr(keyword.text);
        
        if (existingRecord == record)
        {
          updateKeyword(keyword);
        }
  
  // If the substring was already a key for a different record, error out
  // --------------------------------------------------------------------
        else if (existingRecord != null)
        {
          unexpireAll(record);
          throw new SearchKeyException(false, record.getID(), record.getType(), keyword.text);
        }
        
  // Add new substring
  // -----------------       
        else
        {
          addKeyword(keyword);
        }
      }
    }
  
  // Delete keys for substrings no longer used
  // -----------------------------------------
    purgeExpired(record);
  
    if (noMod == false)
    {
      record.modifyNow();
      db.rebuildMentions();
    }
    
    return;
  }
  
//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  List<SearchKeyword> getKeysByRecord(HDT_Base record)
  {
    ArrayList<SearchKeyword> list = new ArrayList<>();
    
    Map<String, SearchKeyword> keywordStrToKeywordObj = recordToKeywordStrToKeywordObj.get(record);
    
    if (keywordStrToKeywordObj != null)    
      list.addAll(keywordStrToKeywordObj.values());
    
    return list;          
  }
  
//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  SearchKeyword getKeywordObjByKeywordStr(String keywordStr)
  {
    keywordStr = convertToEnglishChars(keywordStr);
    
    if (keywordStr.length() < 3) return null;
    
    Map<String, SearchKeyword> keywordStrToKeywordObj = prefixStrToKeywordStrToKeywordObj.get(keywordStr.substring(0, 3).toLowerCase());
    
    if (keywordStrToKeywordObj == null) return null;    

    SearchKeyword keywordObj = keywordStrToKeywordObj.get(keywordStr.toLowerCase());
    
    return keywordObj;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  String getFirstActiveKeyword(HDT_Base record)
  {
    Map<String, SearchKeyword> keywordStrToKeywordObj = recordToKeywordStrToKeywordObj.get(record);
    
    if (keywordStrToKeywordObj == null) return "";
    
    synchronized (keywordStrToKeywordObj)
    {
      if (keywordStrToKeywordObj.values().isEmpty())
        return "";
      
      return keywordStrToKeywordObj.values().toArray(new SearchKeyword[0])[0].text;
    }
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  String getStringForRecord(HDT_Base record)
  {
    String text = "";

    Map<String, SearchKeyword> keywordStrToKeywordObj = recordToKeywordStrToKeywordObj.get(record);
    
    if (keywordStrToKeywordObj == null) return "";
    
    synchronized (keywordStrToKeywordObj) 
    {
      for (SearchKeyword keyword : keywordStrToKeywordObj.values())
        text = (text.length() == 0) ? keyword.toString() : text + "; " + keyword.toString();
    }
    return text;
  }
 
//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private void addKeyword(SearchKeyword keyword)
  {
    Map<String, SearchKeyword> keywordStrToKeywordObj = recordToKeywordStrToKeywordObj.get(keyword.record);
    
    if (keywordStrToKeywordObj == null)
    {
      keywordStrToKeywordObj = Collections.synchronizedMap(new LinkedHashMap<String, SearchKeyword>());
      recordToKeywordStrToKeywordObj.put(keyword.record, keywordStrToKeywordObj);
    }
    
    keywordStrToKeywordObj.put(keyword.text.toLowerCase(), keyword);
    
    keywordStrToKeywordObj = prefixStrToKeywordStrToKeywordObj.get(keyword.getPrefix());
    
    if (keywordStrToKeywordObj == null)
    {
      keywordStrToKeywordObj = Collections.synchronizedMap(new LinkedHashMap<String, SearchKeyword>());
      prefixStrToKeywordStrToKeywordObj.put(keyword.getPrefix(), keywordStrToKeywordObj);
    }
    
    keywordStrToKeywordObj.put(keyword.text.toLowerCase(), keyword);    
  }
  
//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private void purgeExpired(HDT_Base record)
  {
    Map<String, SearchKeyword> keywordStrToKeywordObj = recordToKeywordStrToKeywordObj.get(record);
    
    if (keywordStrToKeywordObj == null) return;
    
    synchronized (keywordStrToKeywordObj)
    {
      Iterator<Entry<String, SearchKeyword>> it = keywordStrToKeywordObj.entrySet().iterator();
      
      while (it.hasNext())
      {
        Entry<String, SearchKeyword> entry = it.next();
        
        SearchKeyword keyword = entry.getValue();
        
        if (keyword.expired)
        {
          String prefix = keyword.getPrefix();
          
          Map<String, SearchKeyword> keywordStrToKeywordObj2 = prefixStrToKeywordStrToKeywordObj.get(keyword.getPrefix());
          keywordStrToKeywordObj2.remove(keyword.text.toLowerCase());
          
          if (keywordStrToKeywordObj2.isEmpty())
            prefixStrToKeywordStrToKeywordObj.remove(prefix);

          it.remove();
        }
      }
    }
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private void expireAll(HDT_Base record)
  {
    Map<String, SearchKeyword> keywordStrToKeywordObj = recordToKeywordStrToKeywordObj.get(record);
    
    if (keywordStrToKeywordObj == null) return;
    
    synchronized (keywordStrToKeywordObj)
    {
      for (SearchKeyword keyword : keywordStrToKeywordObj.values())
        keyword.expired = true;
    }
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private void unexpireAll(HDT_Base record)
  {
    Map<String, SearchKeyword> keywordStrToKeywordObj = recordToKeywordStrToKeywordObj.get(record);
    
    if (keywordStrToKeywordObj == null) return;
    
    synchronized (keywordStrToKeywordObj)
    {
      for (SearchKeyword keyword : keywordStrToKeywordObj.values())
        keyword.expired = false;
    }
  }
  
//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private void updateKeyword(SearchKeyword keyword)
  {
    Map<String, SearchKeyword> keywordStrToKeywordObj = recordToKeywordStrToKeywordObj.get(keyword.record);   
    keywordStrToKeywordObj.put(keyword.text.toLowerCase(), keyword);
    
    keywordStrToKeywordObj = prefixStrToKeywordStrToKeywordObj.get(keyword.getPrefix());    
    keywordStrToKeywordObj.put(keyword.text.toLowerCase(), keyword);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private HDT_Base getRecordForKeywordStr(String keywordStr)
  {
    SearchKeyword keywordObj = getKeywordObjByKeywordStr(keywordStr);
    
    if (keywordObj == null) return null;
    return keywordObj.record;
  }
  
//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

}
