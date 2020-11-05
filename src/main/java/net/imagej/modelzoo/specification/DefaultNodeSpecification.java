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
package net.imagej.modelzoo.specification;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class DefaultNodeSpecification implements NodeSpecification {

	private final static String idNodeName = "name";
	private final static String idNodeAxes = "axes";
	private final static String idNodeDataType = "data_type";
	private final static String idNodeDataRange = "data_range";
	private final static String idNodeShape = "shape";
	private final static String idNodeHalo = "halo";

	private String name;
	private String axes;
	private String dataType;
	private List<?> dataRange;
	private List<Integer> halo;

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getAxes() {
		return axes;
	}

	@Override
	public String getDataType() {
		return dataType;
	}

	@Override
	public List<?> getDataRange() {
		return dataRange;
	}

	@Override
	public List<Integer> getHalo() {
		return halo;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public void setAxes(String axes) {
		this.axes = axes;
	}

	@Override
	public void setDataType(String dataType) {
		this.dataType = dataType;
	}

	@Override
	public void setDataRange(List<?> dataRange) {
		this.dataRange = dataRange;
	}

	@Override
	public void setHalo(List<Integer> halo) {
		this.halo = halo;
	}

	@Override
	public Map<String, Object> asMap() {
		Map<String, Object> res = new LinkedHashMap<>();
		res.put(idNodeName, name);
		if (axes != null) res.put(idNodeAxes, axes);
		if (dataType != null) res.put(idNodeDataType, dataType);
		if (dataRange != null) res.put(idNodeDataRange, dataRange);
		if (halo != null) res.put(idNodeHalo, halo);
		res.put(idNodeShape, getShape());
		return res;
	}

	@Override
	public void set(Map data) {
		setName((String) data.get(idNodeName));
		setAxes((String) data.get(idNodeAxes));
		setDataType((String) data.get(idNodeDataType));
		setDataRange((List<?>) data.get(idNodeDataRange));
		setShape((Map<String, Object>) data.get(idNodeShape));
		setHalo((List<Integer>) data.get(idNodeHalo));
	}

	protected abstract Map<String, Object> getShape();

	protected abstract void setShape(Map<String, Object> data);
}
