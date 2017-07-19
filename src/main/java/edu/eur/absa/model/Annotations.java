package edu.eur.absa.model;

import java.util.HashMap;

public class Annotations extends HashMap<String, Object>{

	/**
	 * 
	 */
	private static final long serialVersionUID = -8058150766908873877L;
	private Dataset dataset;
	
	public Annotations(Dataset dataset){
		this.dataset = dataset;
	}
	
	public <T> T get(String annotationType, Class<T> dataType){
		Object value = super.get(annotationType);
		if (value == null)
			return null;
		if (dataType.isInstance(value)){
			return (T)value;
		} else {
			throw new ClassCastException("The annotation you requested is not of the type you specified");
		}
		
	}
	
	@SuppressWarnings("unchecked")
	public <T> T get(String annotationType){
		Class<T> classT = (Class<T>) dataset.getAnnotationDataTypes().get(annotationType);
		return get(annotationType, classT);
	}
	
	public String getEntryText(String annotationType){
		return annotationType+": "+get(annotationType).toString();
	}
	
	@Override
	public Object put(String annotationType, Object value){
		if (!dataset.getAnnotationDataTypes().containsKey(annotationType))
			dataset.getAnnotationDataTypes().put(annotationType, value.getClass());
		return super.put(annotationType, dataset.getAnnotationDataTypes().get(annotationType).cast(value));
	}
	
}
