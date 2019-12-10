/*-
 * #%L
 * ImageJ ModelZoo Consumer
 * %%
 * Copyright (C) 2019 MPI-CBG
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package net.imagej.modelzoo.consumer.network;

import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.modelzoo.consumer.network.model.Model;
import net.imagej.modelzoo.consumer.util.DatasetHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultInputMapper implements InputMapper {

	private AxisType[] mapping = null;

	private static final Map<Character, AxisType> axesMap = Collections.unmodifiableMap(new HashMap<Character, AxisType>() {
		{
			put('X', Axes.X);
			put('Y', Axes.Y);
			put('Z', Axes.Z);
			put('T', Axes.TIME);
			put('C', Axes.CHANNEL);
		}
	});

	@Override
	public void setMapping(final AxisType[] mapping) {
		this.mapping = mapping;
	}

	@Override
	public void run(final Dataset input, final Model model) {

		DatasetHelper.assignUnknownDimensions(input);

		model.loadInputNode(input);
		model.loadOutputNode(input);

		if (model.isInitialized()) {
			model.initMapping();
		}

		model.preprocess();

//		if (model.getInputNode() != null) {
//			if(mapping != null) {
//				//TODO
//				// model input and output have default dimension reduction.
//				// if the mapping is set to something different for the input, make sure to remove the same dimension slots
//				model.getInputNode().setMapping(mapping);
//			}else {
//				mapping = model.getInputNode().getMapping();
//			}
//		}

		mapping = model.getInputNode().getMapping();
		model.getInputNode().generateMapping();
		model.getOutputNode().generateMapping();

	}

	public static List<AxisType> parseMappingStr(String mappingStr) {
		List<AxisType> mapping = new ArrayList<>();
		for(int i = 0; i < mappingStr.length(); i++) {
			mapping.add(axesMap.get(mappingStr.charAt(i)));
		}
		return mapping;
	}

	@Override
	public AxisType[] getMapping() {
		return mapping;
	}


}
