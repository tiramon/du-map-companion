package de.tiramon.du.map.utils;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import de.tiramon.du.map.model.DUMethodsMap;
import de.tiramon.du.tools.model.DUMethod;
import de.tiramon.du.tools.service.IMethodEnumConverter;

public class MethodEnumConverter extends IMethodEnumConverter {
	private static MethodEnumConverter instance = new MethodEnumConverter();

	public static MethodEnumConverter getInstance() {
		return instance;
	}

	@Override
	public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
		writer.setValue(((DUMethod) source).name());
	}

	@Override
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
		String value = reader.getValue();

		DUMethod[] a = DUMethodsMap.values();
		for (int i = 0; i < a.length; i++) {
			if (a[i].getMethodString().equals(value)) {
				return a[i];
			}
		}
		return null;
	}

	@Override
	public boolean canConvert(Class type) {
		return DUMethod.class.isAssignableFrom(type);
	}
}
