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
package net.imagej.modelzoo.consumer.model.node;

import net.imagej.modelzoo.consumer.model.node.processor.NodeProcessor;

import java.util.ArrayList;
import java.util.List;

public abstract class DefaultModelZooNode<T> implements ModelZooNode<T> {

	private String name;
	private List<NodeProcessor> processors;
	private T data;

	public DefaultModelZooNode() {
		processors = new ArrayList<>();
	}

	@Override
	public void setName(final String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public List<NodeProcessor> getProcessors() {
		return processors;
	}

	@Override
	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}

}
