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

package org.mrgeo.opimage;

import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.RenderedOp;
import javax.media.jai.registry.RenderedRegistryMode;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;

/**
 * @author jason.surratt
 * 
 */
public class ReplaceValuesDescriptor extends OperationDescriptorImpl implements
RenderedImageFactory
{
  private static final long serialVersionUID = 1L;
  public final static int COLOR_SCALE = 0;
  public static final String OPERATION_NAME = ReplaceValuesDescriptor.class.getName();

  public static RenderedOp create(RenderedImage src1, double newValue, double min, double max, RenderingHints hints)
  {
    return create(src1, newValue, false, min, max, hints);
  }

  /**
   * 
   * @param src1
   * @param newValue
   * @param newNull
   *          If this is true, then the newValue becomes the new null.
   * @param hints
   * @return
   */
  public static RenderedOp create(RenderedImage src1, double newValue, boolean newNull, double min,
      double max, RenderingHints hints)
  {
    ParameterBlock paramBlock = (new ParameterBlock()).addSource(src1).add(newValue).add(
        newNull ? 1 : 0).add(min).add(max);
    return JAI.create(OPERATION_NAME, paramBlock, hints);
  }

  public ReplaceValuesDescriptor()
  {
    // I realize this formatting is horrendous, but Java won't let me assign
    // variables before
    // calling super.
    super(new String[][] { { "GlobalName", OPERATION_NAME }, { "LocalName", OPERATION_NAME },
        { "Vendor", "com.spadac" }, { "Description", "" }, { "DocURL", "http://www.spadac.com/" },
        { "Version", "1.0" } }, new String[] { RenderedRegistryMode.MODE_NAME }, 1, new String[] {
        "newValue", "newNull", "min", "max" }, new Class[] { Double.class, Integer.class,
        Double.class, Double.class }, new Object[] { Double.NaN, new Integer(0),
        NO_PARAMETER_DEFAULT, NO_PARAMETER_DEFAULT }, null);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * java.awt.image.renderable.RenderedImageFactory#create(java.awt.image.renderable
   * .ParameterBlock, java.awt.RenderingHints)
   */
  @Override
  public RenderedImage create(ParameterBlock paramBlock, RenderingHints hints)
  {
    return ReplaceValuesOpImage.create(paramBlock.getRenderedSource(0), paramBlock
        .getDoubleParameter(0), paramBlock.getIntParameter(1) == 1 ? true : false, paramBlock
            .getDoubleParameter(2), paramBlock.getDoubleParameter(3), hints);
  }

}
