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

import io.scif.services.DatasetIOService;
import net.imagej.Dataset;
import net.imagej.modelzoo.ModelZooService;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.io.location.FileLocation;
import org.scijava.log.LogService;
import org.scijava.module.Module;
import org.scijava.module.ModuleService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.FileWidget;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Plugin(type = Command.class)
public class DefaultModelZooBatchPredictionCommand extends DynamicCommand {

	@Parameter(label = "Import model (.zip) from file")
	private File modelFile;

	@Parameter
	private Module prediction;

	@Parameter(style = FileWidget.DIRECTORY_STYLE)
	private File inputDirectory;

	@Parameter(style = FileWidget.DIRECTORY_STYLE)
	private File outputDirectory;

	@Parameter
	private LogService log;

	@Parameter
	private DatasetIOService datasetIOService;

	@Parameter
	private ModelZooService modelZooService;

	public void run() {

		if (!valid(inputDirectory, "Input")) return;
		if (!valid(outputDirectory, "Output")) return;

		File[] files = inputDirectory.listFiles();
		if(files == null || files.length == 0) {
			log.warn("No files in input directory.");
			return;
		}

		final long startTime = System.currentTimeMillis();
		log.info("ModelZoo batch prediction start: " + inputDirectory);

		List<File>  inputFiles = Arrays.asList(files);
		Collections.sort(inputFiles);
		boolean firstRun = true;
		for (File inputFile : inputFiles) {
			try {
				Dataset input = datasetIOService.open(inputFile.getAbsolutePath());
				prediction.setInput("input", input);
				prediction.resolveInput("input");
				if(firstRun) {
					// for the first run, execute the command interactively
					// to harvest missing inputs. for consecutive runs,
					// we assume the same input parameters will be used
					firstRun = false;
					Map<String, Object> chosenParameters = prediction.getInputs();
					chosenParameters.forEach(this::setInput);
					context().service(ModuleService.class).run(prediction, true).get();
				} else {
					context().service(ModuleService.class).run(prediction, false).get();
				}
				saveOutputs(inputFile, prediction);
			} catch (IOException | ExecutionException | InterruptedException e) {
				e.printStackTrace();
			}
		}

		log.info("ModelZoo batch prediction exit (took " + (System.currentTimeMillis() - startTime) + " milliseconds)");

	}

	private void saveOutputs(File inputFile, Module prediction) throws IOException {
		HashMap<String, Object> nonNullOutputs = new HashMap<>();
		prediction.getOutputs().forEach((s, o) -> {
			if (o != null) nonNullOutputs.put(s, o);
		});
		for (Map.Entry<String, Object> entry : nonNullOutputs.entrySet()) {
			Object object = entry.getValue();
			if (object instanceof Dataset) {
				Dataset output = (Dataset) object;
				String outName = inputFile.getName();
				if (nonNullOutputs.size() > 1) {
					String[] tokens = outName.split("\\.(?=[^\\.]+$)");
					outName = tokens[0] + "_" + output.getName() + "." + tokens[1];
				}
				File outputFile = new File(outputDirectory, outName);
				if (outputFile.exists()) outputFile.delete();
				datasetIOService.save(output, new FileLocation(outputFile));
			}
		}
	}

	private boolean valid(File dir, String name) {
		if (!dir.exists()) {
			log.error(name + " does not exist");
			return false;
		}
		if (!dir.isDirectory()) {
			log.error(name + " is not a directory");
			return false;
		}
		return true;
	}

}
