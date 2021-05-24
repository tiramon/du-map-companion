package de.tiramon.du.map.utils;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import de.tiramon.du.map.model.DUMethod;

public class MethodEnumConverter implements Converter {
	private static MethodEnumConverter instance = new MethodEnumConverter();

	public static MethodEnumConverter getInstance() {
		return instance;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean canConvert(Class clazz) {
		return DUMethod.class.isAssignableFrom(clazz);
	}

	@Override
	public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
		writer.setValue(((DUMethod) source).name());

	}

	@Override
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
		return DUMethod.get(reader.getValue());
	}

}
