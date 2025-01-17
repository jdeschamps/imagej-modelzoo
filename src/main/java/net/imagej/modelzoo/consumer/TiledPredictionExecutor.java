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

package net.imagej.modelzoo.consumer;

import net.imagej.modelzoo.consumer.model.ModelZooModel;
import net.imagej.modelzoo.consumer.model.node.InputImageNode;
import net.imagej.modelzoo.consumer.model.node.ModelZooNode;
import net.imagej.modelzoo.consumer.model.node.OutputImageNode;
import net.imagej.modelzoo.consumer.tiling.DefaultTiling;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.scijava.Cancelable;
import org.scijava.Context;
import org.scijava.app.StatusService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.CancellationException;
import java.util.concurrent.RejectedExecutionException;

public class TiledPredictionExecutor implements Cancelable {

	@Parameter
	private LogService log;

	@Parameter
	private StatusService statusService;

	private final ModelZooModel model;
	private DefaultTiling tiling;
	private int nTiles = 8;
	private int oldNTiles;
	private int oldBatchesSize;
	private boolean processedTiles = false;
	private boolean canceled = false;
	private int batchSize = 10;
	private boolean tilingEnabled = true;

	private Path cacheDir = null;
	private ArrayList<OutputImageNode> tilingOutputs;
	private InputImageNode tilingInput;

	TiledPredictionExecutor(ModelZooModel model, Context context) {
		this.model = model;
		context.inject(this);
	}

	public void run() throws OutOfMemoryError, IllegalArgumentException {

		try {
			assignTilingNodes();
			if (!tilingEnabled || tilingInput == null) {
				model.predict();
			} else {
				initTiling();
				while (processNextTile()) {
					model.predict();
					tiling.resolveCurrentTile();
				}
				statusService.showProgress(1, 1);
				if(!isCanceled()) tiling.finish();
			}
		} catch (final CancellationException | RejectedExecutionException e) {
			//canceled
			String PROGRESS_CANCELED = "Canceled";
			log.warn(PROGRESS_CANCELED);
			cancel(PROGRESS_CANCELED);
		} catch (final IllegalArgumentException e) {
			throw e;
		} catch (final IllegalStateException exc) {
			if (exc.getMessage() != null && exc.getMessage().contains("OOM")) {
				throw new OutOfMemoryError();
			}
			exc.printStackTrace();
			throw exc;
		}
	}

	/**
	 * Take the first image output which has a reference to an input image, collect all other output nodes connected to this input
	 */
	private void assignTilingNodes() {
		tilingInput = null;
		tilingOutputs = new ArrayList<>();
		for (ModelZooNode<?> outputNode : model.getOutputNodes()) {
			// check for each output node if there is a reference to the input node
			// if yes, we assume it might be possible to tile
			if(OutputImageNode.class.isAssignableFrom(outputNode.getClass())) {
				OutputImageNode outputImageNode = (OutputImageNode) outputNode;
				if(outputImageNode.getReference() != null) {
					if(tilingInput == null) {
						tilingInput = outputImageNode.getReference();
						tilingOutputs.add(outputImageNode);
					} else {
						if(outputImageNode.getReference().equals(tilingInput)) {
							tilingOutputs.add(outputImageNode);
						}
					}
				}
			}
		}
	}

	public void setNumberOfTiles(int nTiles) {
		this.nTiles = nTiles;
	}

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	public boolean increaseTiling() {

		// We expect it to be an out of memory exception and
		// try it again with more tiles or smaller batches.
		int nTiles = tiling.getTilesNum();
		int batchSize = tiling.getBatchSize();
		if (oldNTiles == nTiles && oldBatchesSize == batchSize) {
			return false;
		}
		oldNTiles = nTiles;
		oldBatchesSize = batchSize;

		batchSize /= 2;
		if (batchSize < 1) {
			batchSize = 1;
			nTiles *= 2;
		}

		setNumberOfTiles(nTiles);
		setBatchSize(batchSize);
		tiling.resetInputData();

		log.warn(
				"Out of memory exception occurred. Trying with " + nTiles +
						" tiles, batch size " + batchSize + "...");

		return true;
	}

	private boolean processNextTile() {
		// go to next tile
		if(isCanceled()) return false;
		if (processedTiles || !tiling.hasTilesLeft()) {
			processedTiles = true;
			return false;
		}
		tiling.assignNextTile();
		statusService.showStatus(tiling.getDoneTileCount()-1, (int) tiling.getTilesTotalCount(), "Predicting tile " + (tiling.getDoneTileCount()) + " of " + tiling.getTilesTotalCount() + "..");
		log.info("Processing tile " + (tiling.getDoneTileCount()) + "..");
		return true;
	}

	private <TO extends RealType<TO> & NativeType<TO>, TI extends RealType<TI> & NativeType<TI>> void initTiling() {
		processedTiles = false;
		tiling = new DefaultTiling(tilingInput, tilingOutputs, cacheDir);
		tiling.setNumberOfTiles(nTiles);
		tiling.setBatchSize(batchSize);
		tiling.init();
	}

	@Override
	public boolean isCanceled() {
		return canceled;
	}

	@Override
	public void cancel(final String reason) {
		canceled = true;
		statusService.showProgress(0, 0);
		statusService.clearStatus();
		statusService.showStatus("Prediction canceled.");
	}

	@Override
	public String getCancelReason() {
		return null;
	}

	public void setTilingEnabled(boolean enabled) {
		this.tilingEnabled = enabled;
	}

	public int getNumberOfTiles() {
		return nTiles;
	}

	public int getBatchSize() {
		return batchSize;
	}

	public void setCacheDir(Path cacheDir) {
		this.cacheDir = cacheDir;
	}

	public void dispose() {
		if(tiling != null) tiling.dispose();
	}
}
