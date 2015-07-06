/*
 * Copyright 2009-2015 DigitalGlobe, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package org.mrgeo.data.shp.dbase;

import org.mrgeo.data.shp.SeekableDataInput;
import org.mrgeo.data.shp.util.Convert;
import org.mrgeo.data.shp.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class DbaseFile extends java.lang.Object
{
  private static SimpleDateFormat dateFormatter = null;
  public final static int DEFAULT_CACHE_SIZE = 1000;
  public final static byte ROW_DELETED = 42;
  public final static byte ROW_OK = 32;
  static
  {
    dateFormatter = new SimpleDateFormat("yyyyMMdd");
  }

  public static DbaseFile open(SeekableDataInput dbf, boolean cachemode, int cachesize, String mode)
      throws IOException, DbaseException
  {
    return new DbaseFile(dbf, cachemode, cachesize, mode);
  }

  protected boolean cachemode = false; // cache mode indicator flag
  protected int cachepos; // cache position (inclusive); dynamic access only
  protected int cachesize; // size of cache - represents all rows if not dynamic
  protected File file = null;
  protected byte[] flg = null; // row flags!
  protected DbaseHeader header = null;
  private SeekableDataInput in = null;
  protected boolean modData = false; // modified data
  protected String mode = null;

  protected boolean modStructure = false; // modified structure

  @SuppressWarnings("rawtypes")
  protected List[] row = null; // data!

  private DbaseFile(SeekableDataInput in, boolean cachemode, int cachesize, String mode)
      throws IOException, DbaseException
  {
    this.in = in;
    this.cachemode = cachemode;
    this.cachesize = cachesize;
    this.mode = mode;
    header = new DbaseHeader();
    row = new ArrayList[0];
    flg = new byte[0];
    load();
  }

  public synchronized void addColumn(DbaseField field) throws DbaseException
  {
    addColumn(field.name, field.type, field.length, field.decimal, -1);
  }

  public synchronized void addColumn(DbaseField field, int position) throws DbaseException
  {
    addColumn(field.name, field.type, field.length, field.decimal, position);
  }

  public synchronized void addColumn(String name, int type) throws DbaseException
  {
    DbaseField temp = new DbaseField(name, type);
    addColumn(temp, -1);
  }

  public synchronized void addColumn(String name, int type, int length) throws DbaseException
  {
    DbaseField temp = new DbaseField(name, type, length);
    addColumn(temp, -1);
  }

  public synchronized void addColumn(String name, int type, int length, int decimal)
      throws DbaseException
  {
    DbaseField temp = new DbaseField(name, type, length, decimal);
    addColumn(temp, -1);
  }

  public synchronized void addColumn(String name, int type, int length, int decimal, int position)
      throws DbaseException
  {
    if (header.recordCount > 0)
      throw new DbaseException(
          "Columns can't be added after records are in the DBF in this version!");
    name.trim();
    if (name.length() > 11)
      name = name.substring(0, 10);
    if (getColumn(name) != -1)
      throw new DbaseException("Column name already in table!  Cancelled...");
    DbaseField field = new DbaseField();
    field.name = name;
    field.type = type;
    field.length = length;
    field.decimal = decimal;
    if (position == -1)
    {
      header.fields.add(field);
    }
    else
    {
      header.fields.insertElementAt(field, position);
    }
    header.fieldCount++;
    header.update();
  }

  @SuppressWarnings("rawtypes")
  public synchronized void addRow(List data)
  {
    synchronized (row)
    {
      List[] temp = new ArrayList[row.length + 1];
      System.arraycopy(row, 0, temp, 0, row.length);
      temp[row.length] = data;
      row = temp;
      header.recordCount++;
    }
    synchronized (flg)
    {
      byte[] temp = new byte[flg.length + 1];
      System.arraycopy(flg, 0, temp, 0, flg.length);
      temp[flg.length] = ROW_OK;
      flg = temp;
    }
    modData = true;
  }

  public void close()
  {
    try
    {
      if (in != null)
      {
        in.close();
        file = null;
        header = null;
        row = null;
        flg = null;
      }
    }
    catch (Exception e)
    {
    }
  }

  public synchronized boolean delRow(int i)
  {
    try
    {
      synchronized (row)
      {
        flg[i] = ROW_DELETED;
      }
      modData = true;
    }
    catch (Exception e)
    {
      return false;
    }
    return true;
  }

  @Override
  protected void finalize() throws IOException, DbaseException
  {
    if (modData)
      save();
    if (in != null)
      in.close();
  }

  public int getCacheSize()
  {
    return cachesize;
  }

  public int getColumn(String fieldName)
  {
    return header.getColumn(fieldName);
  }

  public DbaseHeader getHeader()
  {
    return header;
  }

  @SuppressWarnings("rawtypes")
  public List getRow(int i) 
  {
    try
    {
      if (i < cachepos || i > (cachepos + row.length - 1))
      {
        if (modData)
          saveRows(cachepos);
        loadRows(i);
      }
      return row[i - cachepos];
    }
    catch (Exception e)
    {
      return null;
    }
  }

  public int getRowCount()
  {
    return header.recordCount;
  }

  public byte getRowFlag(int i) throws DbaseException
  {
    try
    {
      return flg[i];
    }
    catch (Exception e)
    {
      throw new DbaseException(e.getMessage());
    }
  }

  public boolean isCached()
  {
    return cachemode;
  }

  public boolean isModified()
  {
    return modData;
  }

  public void load() throws IOException, DbaseException
  {
    load(in);
  }

  private void load(SeekableDataInput is) throws IOException, DbaseException
  {
    // read header
    is.seek(0L);
    header.load(is);
    // initialize
    flg = new byte[header.recordCount];
    cachepos = -1;
    if (!cachemode)
      cachesize = header.recordCount;
    // read data
    loadRows(is, 0);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private List loadRow(SeekableDataInput is, int i) throws IOException
  {
    List arow = new ArrayList(header.fieldCount);
    byte[] record = new byte[header.recordLength];
    // read record
    is.readFully(record, 0, header.recordLength);
    if (flg != null)
      flg[i] = record[0];
    // load record
    for (int j = 0; j < header.fieldCount; j++)
    {
      DbaseField field = header.fields.get(j);
      String tempStr = null;
      switch (field.type)
      {
      case DbaseField.CHARACTER:
        // legal: ASCII (OEM code page chars); rest= space, not \0 term
        // n = 1..254
        tempStr = Convert.getString(record, field.offset, field.length);
        arow.add(tempStr);
        break;
      case DbaseField.DATE:
        // legal since db3: "0123456789"
        // YYYYMMDD
        // n = 8
        tempStr = Convert.getString(record, field.offset, field.length);
        Date date = null;
        try
        {
          date = dateFormatter.parse(tempStr);
        }
        catch (ParseException e)
        {
        }
        arow.add(date);
        break;
      case DbaseField.FLOAT:
        // legal since db4: "-.0123456789"
        tempStr = Convert.getString(record, field.offset, field.length);
        arow.add(new Double(tempStr));
        break;
      case DbaseField.LOGICAL:
        // legal since db3: "YyNnTtFf space"
        // legal since db4: "YyNnTtFf ?"
        tempStr = Convert.getString(record, field.offset, field.length);
        Boolean logic = null;
        if (tempStr.equalsIgnoreCase("Y") || tempStr.equalsIgnoreCase("T"))
          logic = new Boolean(true);
        if (tempStr.equalsIgnoreCase("N") || tempStr.equalsIgnoreCase("F"))
          logic = new Boolean(false);
        // note, not sure about space or ?, assuming this means "null"
        arow.add(logic);
        break;
      case DbaseField.NUMERIC:
        // legal since db3: "-.0123456789", may find empty string or 1 or more
        // asteriks
        if (field.decimal == 0)
        {
          // int
          tempStr = Convert.getString(record, field.offset, field.length);
          try
          {
            arow.add(new Integer(tempStr));
          }
          catch (NumberFormatException nfe)
          {
            arow.add(new Integer(0));
          }
        }
        else
        {
          // double
          tempStr = Convert.getString(record, field.offset, field.length);
          try
          {
            arow.add(new Double(tempStr));
          }
          catch (NumberFormatException nfe)
          {
            arow.add(new Double(0));
          }
        }
        break;
      default:
        arow.add(null);
        break;
      }
    }
    // return
    return arow;
  }

  private void loadRows(int i) throws IOException, DbaseException
  {
    if (header == null)
      throw new DbaseException("Header never read.  Cannot load!");
    loadRows(in, i);
  }

  private void loadRows(SeekableDataInput is, int i) throws IOException
  {
    // reset row array
    int max = (((header.recordCount - i) > cachesize) ? cachesize : (header.recordCount - i));
    resetCache(i, max);
    int pos = header.headerLength + (header.recordLength * i);
    is.seek(pos);
    // read data
    for (int j = 0; j < row.length; j++)
    {
      // initialize row data
      row[j] = loadRow(is, cachepos + j);
    }
  }

  public void purge()
  {
    row = new ArrayList[0];
    flg = new byte[0];
    header.recordCount = 0;
  }

  @SuppressWarnings("hiding")
  public void resetCache(int cachepos, int size)
  {
    this.cachepos = cachepos;
    row = new ArrayList[size];
  }

  @SuppressWarnings("static-method")
  public void save()
  {
    throw new UnsupportedOperationException();
  }

  @SuppressWarnings({ "static-method", "unused" })
  public void saveRows(int i)
  {
    throw new UnsupportedOperationException();
  }

  public void setCacheSize(int size)
  {
    if (!cachemode)
      return;
    if (size < 1)
      size = 1;
    cachesize = size;
  }

  public synchronized void setColumns(DbaseFile template) throws DbaseException
  {
    if (header.recordCount > 0)
      throw new DbaseException("Invalid operation after records are in the DBF!");
    for (int i = 0; i < template.header.getFieldCount(); i++)
    {
      DbaseField field = template.header.getField(i);
      header.fields.add(field);
      header.fieldCount++;
    }
    header.update();
  }

  public void setModified(boolean state)
  {
    modData = state;
  }

  @SuppressWarnings("rawtypes")
  public void setRow(List data, int i) throws DbaseException
  {
    synchronized (row)
    {
      List temp = null;
      try
      {
        temp = row[i];
      }
      catch (Exception e)
      {
        throw new DbaseException("Row is invalid!");
      }
      if (data != null)
      {
        if (data.size() != temp.size())
          throw new DbaseException("Invalid number of replacement columns in set!");
      }
      row[i] = data;
    }
    modData = true;
  }

  @Override
  public String toString()
  {
    return "\n" + StringUtils.pad("file:", 15) + file + getHeader();
  }
}
