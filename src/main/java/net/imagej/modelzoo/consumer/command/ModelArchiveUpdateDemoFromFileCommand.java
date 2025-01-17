/*-
 * #%L
 * This is the bioimage.io modelzoo library for ImageJ.
 * %%
 * Copyright (C) 2019 - 2020 Center for Systems Biology Dresden
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
package net.imagej.modelzoo.consumer.command;

import io.bioimage.specification.NodeSpecification;
import io.bioimage.specification.OutputNodeSpecification;
import io.scif.services.DatasetIOService;
import net.imagej.modelzoo.ModelZooArchive;
import net.imagej.modelzoo.ModelZooService;
import net.imagej.modelzoo.consumer.DefaultModelZooPrediction;
import net.imagej.modelzoo.consumer.model.DefaultTensorSample;
import net.imagej.modelzoo.consumer.model.TensorSample;
import net.imagej.modelzoo.consumer.model.prediction.PredictionOutput;
import net.imglib2.RandomAccessibleInterval;
import org.scijava.command.Command;
import org.scijava.io.location.FileLocation;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Plugin(type = Command.class, name = "Update modelzoo archive demo image")
public class ModelArchiveUpdateDemoFromFileCommand implements Command {

	@Parameter
	private File inputFile;

	@Parameter(label = "Axes of input (subset of XYZ)")
	private String axes;

	@Parameter
	private ModelZooArchive archive;

	@Parameter
	private ModelZooService modelZooService;

	@Parameter
	private DatasetIOService ioService;

	@Parameter
	private UIService uiService;

	private static String defaultSampleInput = "sample_in.tif";
	private static String defaultSampleOutputBase = "sample_out";
	private static String defaultSampleOutputEnd = ".tif";

	@Override
	public void run() {
		try {
			RandomAccessibleInterval input = ioService.open(new FileLocation(inputFile));
			PredictionOutput outputs = modelZooService.predict(archive, input, axes);
			updateSampleImages(archive, input, outputs);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static void updateSampleImages(ModelZooArchive archive, RandomAccessibleInterval input, PredictionOutput outputs) {
		List<TensorSample> inputSamples = new ArrayList<>();
		inputSamples.add(new DefaultTensorSample(input, defaultSampleInput));
		List<OutputNodeSpecification> outputNodeSpecifications = archive.getSpecification().getOutputs();
		List<TensorSample> outputSamples = getTensorSamples(archive, outputs.asMap(), outputNodeSpecifications);
		archive.setSampleOutputs(outputSamples);
		archive.setSampleInputs(inputSamples);
		List<String> inNames = namesAsList(inputSamples);
		List<String> outNames = namesAsList(outputSamples);
		archive.getSpecification().setSampleInputs(inNames);
		archive.getSpecification().setSampleOutputs(outNames);
	}

	private static List<String> namesAsList(List<TensorSample> samples) {
		List<String> res = new ArrayList<>();
		for (TensorSample sample : samples) {
			res.add(sample.getFileName());
		}
		return res;
	}

	static List<TensorSample> getTensorSamples(ModelZooArchive archive, Map outputs, List<OutputNodeSpecification> outputNodeSpecifications) {
		List<TensorSample> outputSamples = new ArrayList<>();
		for (int i = 0; i < outputNodeSpecifications.size(); i++) {
			NodeSpecification output = outputNodeSpecifications.get(i);
			Object data = outputs.get(DefaultModelZooPrediction.legacyRenaming(output.getName()));
			String name = defaultSampleOutputBase + "_" + i + defaultSampleOutputEnd;
			if(archive.getSpecification().getSampleOutputs() != null
				&& archive.getSpecification().getSampleOutputs().size() > i) {
				name = archive.getSpecification().getSampleOutputs().get(i);
			}
			TensorSample sample = new DefaultTensorSample(data, name);
			outputSamples.add(sample);
		}
		return outputSamples;
	}

}
