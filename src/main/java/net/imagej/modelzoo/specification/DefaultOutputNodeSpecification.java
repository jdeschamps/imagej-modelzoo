package net.imagej.modelzoo.specification;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DefaultOutputNodeSpecification extends DefaultNodeSpecification implements OutputNodeSpecification {

	private final static String idNodeShapeReferenceInput = "reference_input";
	private final static String idNodeShapeScale = "scale";
	private final static String idNodeShapeOffset = "offset";
	private String referenceInputName;
	private List<? extends Number> shapeScale;
	private List<Integer> shapeOffset;

	@Override
	public Map<String, Object> getShape() {
		Map<String, Object> shape = new LinkedHashMap<>();
		shape.put(idNodeShapeReferenceInput, referenceInputName);
		shape.put(idNodeShapeScale, shapeScale);
		shape.put(idNodeShapeOffset, shapeOffset);
		return shape;
	}

	@Override
	protected void setShape(Map<String, Object> data) {
		if (data == null) return;
		setShapeReferenceInput((String) data.get(idNodeShapeReferenceInput));
		setShapeScale((List<Number>) data.get(idNodeShapeScale));
		setShapeOffset((List<Integer>) data.get(idNodeShapeOffset));
	}

	@Override
	public DefaultOutputNodeSpecification fromMap(Map data) {
		super.fromMap(data);
		return this;
	}

	@Override
	public void setShapeReferenceInput(String referenceInputName) {
		this.referenceInputName = referenceInputName;
	}

	@Override
	public void setShapeScale(List<? extends Number> shapeScale) {
		this.shapeScale = shapeScale;
	}

	@Override
	public void setShapeOffset(List<Integer> shapeOffset) {
		this.shapeOffset = shapeOffset;
	}

	@Override
	public String getReferenceInputName() {
		return referenceInputName;
	}

	@Override
	public List<? extends Number> getShapeScale() {
		return shapeScale;
	}

	@Override
	public List<Integer> getShapeOffset() {
		return shapeOffset;
	}
}