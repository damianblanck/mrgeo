/*
 * Copyright 2009-2014 DigitalGlobe, Inc.
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

package org.mrgeo.mapalgebra.parser;

import java.util.List;

//import org.mrgeo.mapalgebra.MapOp;
import org.mrgeo.mapalgebra.MapOpFactory;

/**
 * This interface defines the parsing capabilities required by MrGeo
 * for parsing its map algebra syntax. It defines the functions needed
 * for navigating map algebra syntax as a tree of nodes.
 */
public interface ParserAdapter
{
  public void initialize();
  public void initializeForTesting();
  public void afterFunctionsLoaded();
  
  /**
   * Return all of the functions recognized by the parser.
   * 
   * @return
   */
  public List<String> getFunctionNames();
  public void addFunction(String functionName);
  public ParserNode parse(String expression, MapOpFactory factory) throws ParserException;
//  public ParserNode parse(String expression) throws ParserException;
  public Object evaluate(ParserNode node) throws ParserException;
}