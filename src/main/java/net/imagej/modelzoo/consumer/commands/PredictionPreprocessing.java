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

import net.imagej.Dataset;
import net.imagej.modelzoo.consumer.io.DefaultInputProcessor;
import net.imagej.modelzoo.consumer.io.InputProcessor;
import net.imagej.modelzoo.consumer.network.model.Model;
import net.imglib2.RandomAccessibleInterval;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.List;

@Plugin(type = Command.class)
public class PredictionPreprocessing implements Command {

	@Parameter
	private Dataset input;

	@Parameter
	private Model model;

	@Parameter(type = ItemIO.OUTPUT)
	private List<RandomAccessibleInterval> output;

	@Parameter
	private
	LogService log;

	private final InputProcessor inputProcessor = new DefaultInputProcessor();

	@Override
	public void run() {
		final Dataset normalizedInput;
//		if (doInputNormalization()) {
//			setupNormalizer();
//			normalizedInput = inputNormalizer.run(getInput(), opService,
//					datasetService);
//		} else {
			normalizedInput = input;
//		}

		output = inputProcessor.run(
				normalizedInput, model);

		log.info("INPUT NODE: ");
		model.getInputNode().printMapping(inputProcessor);
		log.info("OUTPUT NODE: ");
		model.getOutputNode().printMapping(inputProcessor);
	}

}
