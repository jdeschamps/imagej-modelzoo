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

package net.imagej.modelzoo.consumer.sanitycheck;

import io.scif.services.DatasetIOService;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.modelzoo.ModelZooArchive;
import net.imagej.modelzoo.ModelZooService;
import net.imagej.modelzoo.display.InfoWidget;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import org.scijava.ItemIO;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.log.LogService;
import org.scijava.module.Module;
import org.scijava.module.ModuleService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

@Plugin(type = Command.class)
public class DefaultModelZooSanityCheckFromFileCommand extends DynamicCommand {

	@Parameter(label = "Import model (.zip) from file", persist = false)
	private File modelFile;

	@Parameter(persist = false)
	private Module prediction;

	final static String descriptionText =
			"<html>This is what's going to happen:" +
			"<ol style='width: 350px;'>" +
				"<li style='margin: 5px;'>In this dialog, choose an image to run the prediction on</li>" +
				"<li style='margin: 5px;'>In this dialog, choose an image representing the expected result</li>" +
				"<li style='margin: 5px;'>In the next dialog, choose the prediction parameters you want to use for validation</li>" +
				"<li style='margin: 5px;'>The prediction will be performed</li>" +
				"<li style='margin: 5px;'>The result will be displayed and compared to the expected result (also displayed)</li>" +
			"</ol>";

	@Parameter(label="<html><h1>Sanity check</h1>", description = descriptionText, required = false, style = InfoWidget.STYLE)
	private String description = "";

	@Parameter(label = "Prediction input image")
	private File inputFile;

	@Parameter(label = "Expected result image")
	private File inputGroundTruthFile;

	@Parameter(label = "Difference input - prediction", type = ItemIO.OUTPUT)
	private Dataset difference;

	@Parameter
	private LogService log;

	@Parameter
	private StatusService status;

	@Parameter
	private DatasetIOService datasetIOService;

	@Parameter
	private UIService uiService;

	@Parameter
	private OpService opService;

	@Parameter
	private DatasetService datasetService;

	@Parameter
	private ModelZooService modelZooService;

	private static final int numBins = 20;

	public void run() {

		final long startTime = System.currentTimeMillis();
		log("ModelZoo sanity check start");

		try {
			Dataset input = datasetIOService.open(inputFile.getAbsolutePath());
			Dataset gt = datasetIOService.open(inputGroundTruthFile.getAbsolutePath());
			String inputName = "input";
			String outputName = prediction.getInfo().outputs().iterator().next().getName();
			prediction.setInput(inputName, input);
			prediction.resolveInput(inputName);
			context().service(ModuleService.class).run(prediction, true).get();
			Dataset output = (Dataset) prediction.getOutput(outputName);
			uiService.show("expected", gt);
			uiService.show("result after prediction", output);
			difference = datasetService.create(getDifference((RandomAccessibleInterval)input, (RandomAccessibleInterval)output, new FloatType()));
			ModelZooArchive model = modelZooService.io().open(modelFile);
			SanityCheck sanityCheck = modelZooService.getPrediction(model).getSanityCheck();
			sanityCheck.checkInteractive(Collections.singletonList(input), Collections.singletonList(output), Collections.singletonList(gt), model);
		} catch (IOException | ExecutionException | InterruptedException e) {
			e.printStackTrace();
		}

		log("ModelZoo sanity check exit (took " + (System.currentTimeMillis() - startTime) + " milliseconds)");

	}

	<TI extends RealType<TI>, TO extends RealType<TO>, TR extends RealType<TR> & NativeType<TR>> RandomAccessibleInterval<TR> getDifference(RandomAccessibleInterval<TI> input, RandomAccessibleInterval<TO> output, TR resultType) {
		Img<TR> res = opService.create().img(input, resultType);
		LoopBuilder.setImages(input, output, res).multiThreaded().forEachPixel((ti, to, tr) -> {
			tr.setReal(ti.getRealDouble()-to.getRealDouble());
		});
		return res;
	}

	private void log(String msg) {
		log.info(msg);
		status.showStatus(msg);
	}

}
