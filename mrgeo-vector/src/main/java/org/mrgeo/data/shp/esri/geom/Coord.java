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

package org.mrgeo.data.shp.esri.geom;

public final class Coord extends java.lang.Object implements Cloneable, java.io.Serializable
{
  static final long serialVersionUID = 1L;
  public double x;
  public double y;

  /** Creates new Coord */
  public Coord()
  {
    x = 0;
    y = 0;
  }

  public Coord(double x, double y)
  {
    this.x = x;
    this.y = y;
  }

  @Override
  public Object clone()
  {
    try
    {
      return super.clone();
    }
    catch (CloneNotSupportedException e)
    {
      // note: we don't propagate this exception because Coord is final
      throw new InternalError(e.toString());
    }
  }


  @Override
  public int hashCode()
  {
    return super.hashCode();
  }

  @Override
  public boolean equals(Object obj)
  {
    if (obj != null)
    {
      try
      {
        Coord p = (Coord) obj;
        if (p.x == x && p.y == y)
          return true;
      }
      catch (ClassCastException e)
      {
      }
    }
    return false;
  }

  @Override
  public String toString()
  {
    return x + "," + y;
  }
}
