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

package net.imagej.modelzoo.consumer.commands;

import io.scif.MissingLibraryException;
import net.imagej.modelzoo.ModelZooService;
import net.imagej.modelzoo.consumer.DefaultSingleImagePrediction;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import org.scijava.Context;
import org.scijava.ItemIO;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CancellationException;

//@Plugin(type = SingleImagePredictionCommand.class, menuPath = "Plugins>ModelZoo>ModelZoo Prediction")
public class DefaultModelZooPredictionCommand implements SingleImagePredictionCommand {

	@Parameter(label = "Import model (.zip) from file")
	private File modelFile;

	@Parameter
	private Img input;

	@Parameter(label = "Axes (subset of XYZCT)")
	private String axes = "XY";

	@Parameter(label = "Number of tiles (1 = no tiling)", min = "1")
	private int nTiles = 8;

	@Parameter(label = "Batch size")
	private int batchSize = 10;

	@Parameter(type = ItemIO.OUTPUT)
	private RandomAccessibleInterval output;

	@Parameter
	private LogService log;

	@Parameter
	private Context context;

	@Parameter
	private ModelZooService modelZooService;

	public void run() {

		final long startTime = System.currentTimeMillis();

		try {

			DefaultSingleImagePrediction prediction = new DefaultSingleImagePrediction(context);
			prediction.setInput("input", input, axes);
			prediction.setTrainedModel(modelZooService.open(modelFile));
			prediction.setNumberOfTiles(nTiles);
			prediction.setBatchSize(batchSize);
			prediction.run();
			output = prediction.getOutput();

		} catch (CancellationException e) {
			log.warn("ModelZoo prediction canceled.");
		} catch (OutOfMemoryError | IOException | MissingLibraryException e) {
			e.printStackTrace();
		}
		log.info("ModelZoo prediction exit (took " + (System.currentTimeMillis() - startTime) + " milliseconds)");

	}

}
