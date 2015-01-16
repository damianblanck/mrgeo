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

package org.mrgeo.data.accumulo.metadata;

import com.google.common.base.Predicates;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.hadoop.io.Text;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.mrgeo.image.MrsImagePyramidMetadata;
import org.mrgeo.data.DataProviderException;
import org.mrgeo.data.accumulo.image.AccumuloMrsImageDataProvider;
import org.mrgeo.data.accumulo.utils.AccumuloConnector;
import org.mrgeo.data.accumulo.utils.MrGeoAccumuloConstants;
import org.mrgeo.data.image.MrsImagePyramidMetadataReader;
import org.mrgeo.data.image.MrsImagePyramidMetadataReaderContext;
import org.reflections.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

public class AccumuloMrsImagePyramidMetadataReader implements MrsImagePyramidMetadataReader
{

  private static final Logger log = LoggerFactory.getLogger(AccumuloMrsImagePyramidMetadataReader.class);

  private MrsImagePyramidMetadata metadata = null;
  private final AccumuloMrsImageDataProvider dataProvider;
  private final String name;

  private Connector conn = null;
  private Authorizations auths = null;
  
  public AccumuloMrsImagePyramidMetadataReader(AccumuloMrsImageDataProvider dataProvider,
      MrsImagePyramidMetadataReaderContext context){
    this.dataProvider = dataProvider;
    name = dataProvider.getResourceName();
    
    //TODO: need to get auths here - perhaps this is coming from the context object
  
  } // end constructor
  
  public AccumuloMrsImagePyramidMetadataReader(String name){
    this.dataProvider = null;
    this.name = name;
  }
  
  @Override
  public MrsImagePyramidMetadata read() throws IOException
  {
//    if(dataProvider == null){
//      throw new IOException("DataProvider not set!");
//    }
//    String name = dataProvider.getResourceName();
  
    if(name == null || name.length() == 0){
      throw new IOException("Can not load metadata, resource name is empty!");
    }

    if(metadata == null)    {
      metadata = loadMetadata();
      log.info("Read metadata for " + name + " with max zoom level at " + metadata.getMaxZoomLevel());
      metadata.setPyramid(name);
    }
    
    return metadata;

  } // end read

  @Override
  public MrsImagePyramidMetadata reload() throws IOException
  {
    if (metadata == null)
    {
      return read();
    }
    
    if (dataProvider == null)
    {
      throw new IOException("DataProvider not set!");
    }

    String name = dataProvider.getResourceName();
    if (name == null || name.length() == 0)
    {
      throw new IOException("Can not load metadata, resource name is empty!");
    }

    MrsImagePyramidMetadata copy = loadMetadata();

    Set<Method> getters = ReflectionUtils.getAllMethods(MrsImagePyramidMetadata.class, 
        Predicates.<Method>and (
            Predicates.<AnnotatedElement>not(ReflectionUtils.withAnnotation(JsonIgnore.class)),
            ReflectionUtils.withModifier(Modifier.PUBLIC), 
            ReflectionUtils.withPrefix("get"), 
            ReflectionUtils. withParametersCount(0)));

    Set<Method> setters = ReflectionUtils.getAllMethods(MrsImagePyramidMetadata.class, 
        Predicates.<Method>and(
            Predicates.<AnnotatedElement>not(ReflectionUtils.withAnnotation(JsonIgnore.class)),
            ReflectionUtils.withModifier(Modifier.PUBLIC), 
            ReflectionUtils.withPrefix("set"), 
            ReflectionUtils. withParametersCount(1)));


    //    System.out.println("getters");
    //    for (Method m: getters)
    //    {
    //      System.out.println("  " + m.getName());
    //    }
    //    System.out.println();
    //  
    //    System.out.println("setters");
    //    for (Method m: setters)
    //    {
    //      System.out.println("  " + m.getName());
    //    }
    //    System.out.println();

    for (Method getter: getters)
    {
      String gettername = getter.getName();
      String settername = gettername.replaceFirst("get", "set");

      for (Method setter: setters)
      {
        if (setter.getName().equals(settername))
        {
          //          System.out.println("found setter: " + setter.getName() + " for " + getter.getName() );
          try
          {
            setter.invoke(metadata, getter.invoke(copy, new Object[] {}));
          }
          catch (IllegalAccessException e)
          {
          }
          catch (IllegalArgumentException e)
          {
          }
          catch (InvocationTargetException e)
          {
          }
          break;
        }
      }
    }

    return metadata;
  } // end reload

  public void setConnector(Connector conn){
    this.conn = conn;
  } // end setConnector

  private MrsImagePyramidMetadata loadMetadata() throws IOException{
//    if(metadata != null){
////      metadata = new MrsImagePyramidMetadata();
////    } else {
//      return metadata;
//    }

    Properties mrgeoAccProps;

    if(dataProvider == null){
      mrgeoAccProps = AccumuloConnector.getAccumuloProperties();
    } else {
      String enc = dataProvider.getResolvedName();
      mrgeoAccProps = AccumuloConnector.decodeAccumuloProperties(enc);
    }
    
    if(mrgeoAccProps.getProperty(MrGeoAccumuloConstants.MRGEO_ACC_KEY_AUTHS) == null){
      auths = new Authorizations();
    } else {
      auths = new Authorizations(mrgeoAccProps.getProperty(MrGeoAccumuloConstants.MRGEO_ACC_KEY_AUTHS).split(","));
    }
    
    if(conn == null){
      try{
        conn = AccumuloConnector.getConnector(mrgeoAccProps);
      } catch(DataProviderException dpe){
        dpe.printStackTrace();
        throw new RuntimeException("No connection to Accumulo!");
      }
    }
    
//    String table = dataProvider.getResourceName();
    //String auths = dataProvider.
    if(name == null || name.length() == 0){
      throw new IOException("Can not load metadata, resource name is empty!");
    }
    
    Scanner scan = null;
    try{
      scan = conn.createScanner(name, auths);
    } catch(Exception e){
      throw new IOException("Can not connect to table " + name + " with auths " + auths + " - " + e.getMessage());
    }

    MrsImagePyramidMetadata retMeta = null;
    Range range = new Range(MrGeoAccumuloConstants.MRGEO_ACC_METADATA, MrGeoAccumuloConstants.MRGEO_ACC_METADATA + " ");
    scan.setRange(range);
    scan.fetchColumn(new Text(MrGeoAccumuloConstants.MRGEO_ACC_METADATA), new Text(MrGeoAccumuloConstants.MRGEO_ACC_CQALL));
    for(Entry<Key, Value> entry : scan){
      //System.out.println("key: " + entry.getKey().toString());
      //System.out.println("value: " + entry.getValue().toString());

      ByteArrayInputStream bis = new ByteArrayInputStream(entry.getValue().get());
      retMeta = MrsImagePyramidMetadata.load(bis);
      bis.close();
      break;
    }
    
    return retMeta;
  } // end loadMetadata
  
  
  private String metadataToString(){
    if(metadata == null){
      return null;
    }
    String retStr = null;
    try{
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      metadata.save(bos);
      retStr = new String(bos.toByteArray());
      bos.close();
    } catch(IOException ioe){
      return null;
    }
    
    return retStr;
  } // end toString
  
} // end AccumuloMrsImagePyramidMetadataReader