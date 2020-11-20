package net.imagej.modelzoo.consumer.model;

import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.modelzoo.consumer.model.tensorflow.TensorFlowModel;
import net.imagej.modelzoo.consumer.postprocessing.ResizePostprocessor;
import net.imagej.modelzoo.consumer.preprocessing.InputImageConverterProcessor;
import net.imagej.modelzoo.consumer.preprocessing.ResizePreprocessor;
import net.imagej.modelzoo.consumer.tiling.TilingAction;
import net.imagej.modelzoo.specification.InputNodeSpecification;
import net.imagej.modelzoo.specification.ModelSpecification;
import net.imagej.modelzoo.specification.NodeSpecification;
import net.imagej.modelzoo.specification.OutputNodeSpecification;
import net.imagej.modelzoo.specification.TransformationSpecification;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import org.scijava.Context;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;

import java.util.ArrayList;
import java.util.List;

public class DefaultSpecificationLoader {

	@Parameter
	private LogService log;

	@Parameter
	private PluginService pluginService;

	private final ModelSpecification spec;
	private final List<PluginInfo<NodeProcessor>> processors;
	private final ModelZooModel model;

	public DefaultSpecificationLoader(Context context, ModelSpecification spec, ModelZooModel model) {
		context.inject(this);
		processors = pluginService.getPluginsOfType(NodeProcessor.class);
		this.spec = spec;
		this.model = model;
	}

	public List<InputImageNode> processInputs() {
		List<InputImageNode> res = new ArrayList<>();
		try {
			List<InputNodeSpecification> inputs = spec.getInputs();
			for (InputNodeSpecification input : inputs) {
				res.add(buildInputNode(input));
			}
		} catch (ClassCastException e) {
			log.error("Could not process model inputs");
		}
		return res;
	}

	private InputImageNode buildInputNode(InputNodeSpecification data) {
		InputImageNode node = new InputImageNode();
		node.setName(data.getName());
		setInputNodeShape(data, node);
		assignProcessors(node, data.getPreprocessing(), this.processors);
		node.getProcessors().add(new ResizePreprocessor(node, log));
		node.getProcessors().add(new InputImageConverterProcessor(node, getNodeDataType(data)));
		return node;
	}

	private ModelZooNode buildOutputNode(OutputNodeSpecification data) {
		OutputImageNode node = new OutputImageNode();
		node.setName(data.getName());
		setOutputNodeShape(data, node);
		node.setData(new DefaultImageDataReference(null, getNodeDataType(data)));
		assignDefaultImagePostprocessors(node);
		assignProcessors(node, data.getPostprocessing(), this.processors);
		return node;
	}

	private void assignDefaultImagePostprocessors(OutputImageNode node) {
		node.getProcessors().add(new ResizePostprocessor(node));
	}

	private RealType<?> getNodeDataType(NodeSpecification input) {
		String dataType = input.getDataType();
		if (dataType != null && dataType.equals("float32")) {
			return new FloatType();
		}
		return null;
	}

	private void assignProcessors(ImageNode node, List<TransformationSpecification> transformations, List<PluginInfo<NodeProcessor>> availableProcessors) {
		if(transformations == null) return;
		for (TransformationSpecification transformation : transformations) {
			String name = transformation.getName();
			for (PluginInfo<NodeProcessor> info : availableProcessors) {
				if(info.getName().equals(name)) {
					NodeProcessor processor = pluginService.createInstance(info);
					if(ImageNodeProcessor.class.isAssignableFrom(processor.getClass())) {
						((ImageNodeProcessor)processor).setup(node, getImageReference(processor));
					}
					processor.readSpecification(transformation);
					node.getProcessors().add(processor);
				}
			}
		}
	}

	private InputImageNode getImageReference(NodeProcessor node) {
		for (ModelZooNode<?> inputNode : model.getInputNodes()) {
			if(inputNode.getName().equals(node.getReference()) && ImageNode.class.isAssignableFrom(inputNode.getClass())) {
				return (InputImageNode) inputNode;
			}
		}
		return null;
	}

	private void setInputNodeShape(InputNodeSpecification data, InputImageNode node) {
		String axes = data.getAxes();
		List<Integer> min = data.getShapeMin();
		List<Integer> step = data.getShapeStep();
		List<Integer> halo = data.getHalo();
		node.clearAxes();
		for (int i = 0; i < axes.length(); i++) {
			int minVal = min.get(i);
			Integer stepVal = step.get(i);
			String axisName = axes.substring(i, i + 1).toLowerCase();
			AxisType axisType = getAxisType(axisName);
			TilingAction tilingAction = TilingAction.NO_TILING;
			if (axisName.equals("b")) {
				tilingAction = TilingAction.TILE_WITHOUT_PADDING;
			} else {
				if (stepVal > 0) {
					tilingAction = TilingAction.TILE_WITH_PADDING;
				}
			}
			ModelZooAxis axis = new ModelZooAxis(axisType);
			axis.setMin(minVal);
			axis.setStep(stepVal);
			axis.setTiling(tilingAction);
			axis.setHalo(halo.get(i));
			node.addAxis(axis);
		}
	}

	private void setOutputNodeShape(OutputNodeSpecification data, OutputImageNode node) {
		String axes = data.getAxes();
		List<? extends Number> scale = data.getShapeScale();
		List<Integer> offset = data.getShapeOffset();
		String reference = data.getReferenceInputName();
		node.setReference(getInput(reference));
		node.clearAxes();
		for (int i = 0; i < axes.length(); i++) {
			AxisType axisType = getAxisType(axes.substring(i, i + 1));
			ModelZooAxis axis = new ModelZooAxis(axisType);
			axis.setScale(scale.get(i).doubleValue());
			axis.setOffset(offset.get(i));
			node.addAxis(axis);
		}
	}

	private ModelZooNode getInput(String name) {
		for (ModelZooNode<?> inputNode : model.getInputNodes()) {
			if (inputNode.getName().equals(name)) return inputNode;
		}
		return null;
	}

	private AxisType getAxisType(String axis) {
		if (axis.toLowerCase().equals("x")) return Axes.X;
		if (axis.toLowerCase().equals("y")) return Axes.Y;
		if (axis.toLowerCase().equals("z")) return Axes.Z;
		if (axis.toLowerCase().equals("c")) return Axes.CHANNEL;
		if (axis.toLowerCase().equals("b")) return Axes.TIME;
		return Axes.unknown();
	}

	public List<ModelZooNode<?>> processOutputs() {
		List<ModelZooNode<?>> res = new ArrayList<>();
		try {
			List<OutputNodeSpecification> outputs = spec.getOutputs();
			for (OutputNodeSpecification output : outputs) {
				res.add(buildOutputNode(output));
			}
		} catch (ClassCastException e) {
			log.error("Could not process model outputs");
			e.printStackTrace();
		}
		return res;
	}

	public void process() {
		model.getInputNodes().clear();
		model.getInputNodes().addAll(processInputs());
		model.getOutputNodes().clear();
		model.getOutputNodes().addAll(processOutputs());
	}
}
